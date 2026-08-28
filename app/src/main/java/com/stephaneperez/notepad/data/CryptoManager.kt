package com.stephaneperez.notepad.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts note content with an AES-256-GCM key held in the Android
 * Keystore.
 *
 * The key is generated once (lazily, on first save) and is non-exportable and scoped to
 * this app's UID — no other app, and no other device, can use it to decrypt an Angerona
 * file. Whether the key is backed by dedicated secure hardware (TEE/StrongBox) or a
 * software Keystore implementation depends on the device; the Keystore API guarantees
 * non-exportability either way, but not hardware backing specifically. The key is not
 * recoverable across an app uninstall or a factory reset: a fresh install always gets a
 * fresh key. Files encrypted under a since-lost key can no longer be decrypted — see
 * [decrypt] / [looksEncrypted] and the README for how the app surfaces that.
 */
object CryptoManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "simple_notepad_file_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    /** Files above this size are refused on open and on save — see README, "Size limit". */
    const val MAX_PLAINTEXT_BYTES = 100 * 1024 // 100 KB

    // On-disk container: "SNPD" magic + 1-byte format version + 12-byte GCM IV +
    // ciphertext (the 16-byte GCM auth tag is appended to the ciphertext by the cipher).
    // MAGIC + version are passed to GCM as AAD, so tampering with the header itself
    // (not just the ciphertext) is also caught by authentication.
    private val MAGIC = byteArrayOf('S'.code.toByte(), 'N'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val HEADER_LENGTH = 4 + 1 // MAGIC + version
    private val HEADER = MAGIC + byteArrayOf(FORMAT_VERSION)

    private val keyLock = Any()

    /** Only used by [encrypt] — decryption must never have the side effect of minting a key. */
    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        getExistingKey() ?: run {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // No biometric/lock-screen prompt on every save/open — encryption here
                // is meant to keep files opaque to *other apps and other devices*, not
                // to gate access behind an unlock inside the app itself.
                .setUserAuthenticationRequired(false)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getExistingKey(): SecretKey? = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    /**
     * Encrypts [plainText] (UTF-8) into the on-disk container format described above.
     * Throws [IllegalArgumentException] if the encoded text exceeds [MAX_PLAINTEXT_BYTES]
     * — callers should check size before calling this, this is a last-resort guard.
     */
    fun encrypt(plainText: String): ByteArray {
        val plainBytes = plainText.toByteArray(Charsets.UTF_8)
        require(plainBytes.size <= MAX_PLAINTEXT_BYTES) { "Text exceeds MAX_PLAINTEXT_BYTES" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(HEADER)
        val iv = cipher.iv // Keystore-generated random IV
        require(iv.size == IV_LENGTH_BYTES) { "Unexpected GCM IV length: ${iv.size}" }
        val ciphertext = cipher.doFinal(plainBytes)
        return HEADER + iv + ciphertext
    }

    /**
     * True if [data] starts with our magic header and format version — i.e. this is (or
     * was) one of our encrypted files, regardless of whether it can still be decrypted.
     * Used to distinguish "not ours, treat as plain text" from "ours, but undecryptable"
     * — see the README, "File encryption".
     */
    fun looksEncrypted(data: ByteArray): Boolean {
        if (data.size < HEADER_LENGTH + IV_LENGTH_BYTES) return false
        for (i in MAGIC.indices) if (data[i] != MAGIC[i]) return false
        return data[MAGIC.size] == FORMAT_VERSION
    }

    /**
     * Attempts to decrypt [data] written by [encrypt]. Returns null if [data] doesn't
     * look like one of our files at all, or if decryption fails for any reason (key
     * lost to a reinstall/factory reset, corrupted data, tampering — GCM authentication
     * covers both the ciphertext and the header via AAD). Only ever reads an existing
     * Keystore key — never creates one as a side effect of a failed open.
     */
    fun decrypt(data: ByteArray): String? {
        if (!looksEncrypted(data)) return null
        val key = getExistingKey() ?: return null

        val iv = data.copyOfRange(HEADER_LENGTH, HEADER_LENGTH + IV_LENGTH_BYTES)
        val ciphertext = data.copyOfRange(HEADER_LENGTH + IV_LENGTH_BYTES, data.size)

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.updateAAD(HEADER)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }
}
