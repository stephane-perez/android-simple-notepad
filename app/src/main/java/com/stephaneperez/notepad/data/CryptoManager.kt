package com.stephaneperez.notepad.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts note content with an AES-256-GCM key that lives entirely
 * inside the Android Keystore.
 *
 * The key is generated once (lazily, on first save) and never leaves the device's
 * secure hardware/keystore — it is non-exportable and scoped to this app's UID, so no
 * other app, and no other device, can use it to decrypt a Simple Notepad file. It is
 * also excluded from Android's auto-backup by the OS itself, and it is **not**
 * recoverable across an app uninstall or a factory reset: a fresh install always gets a
 * fresh key. Files encrypted under a since-lost key simply fail to decrypt — see
 * [decrypt] and the README for how the app handles that.
 */
object CryptoManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "simple_notepad_file_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12

    // On-disk container: "SNPD" magic + 1-byte format version + 12-byte GCM IV +
    // ciphertext (the 16-byte GCM auth tag is appended to the ciphertext by the cipher).
    private val MAGIC = byteArrayOf('S'.code.toByte(), 'N'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte())
    private const val FORMAT_VERSION: Byte = 1
    private const val HEADER_LENGTH = 4 + 1 // MAGIC + version

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // No biometric/lock-screen prompt on every save/open — encryption here is
            // meant to keep files opaque to *other apps and other devices*, not to gate
            // access behind an unlock inside the app itself.
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Encrypts [plainText] (UTF-8) into the on-disk container format described above. */
    fun encrypt(plainText: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // Keystore-generated random IV, IV_LENGTH_BYTES long for GCM
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return MAGIC + byteArrayOf(FORMAT_VERSION) + iv + ciphertext
    }

    /**
     * Attempts to decrypt [data] written by [encrypt]. Returns null if the header is
     * missing/unrecognized (i.e. this isn't one of our encrypted files at all — an
     * ordinary plain-text file), or if decryption fails for any other reason (key lost
     * to a reinstall/factory reset, corrupted data, tampering). The caller is expected
     * to fall back to treating [data] as plain UTF-8 text in either case.
     */
    fun decrypt(data: ByteArray): String? {
        if (data.size < HEADER_LENGTH + IV_LENGTH_BYTES) return null
        for (i in MAGIC.indices) if (data[i] != MAGIC[i]) return null
        if (data[MAGIC.size] != FORMAT_VERSION) return null

        val iv = data.copyOfRange(HEADER_LENGTH, HEADER_LENGTH + IV_LENGTH_BYTES)
        val ciphertext = data.copyOfRange(HEADER_LENGTH + IV_LENGTH_BYTES, data.size)

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }
}
