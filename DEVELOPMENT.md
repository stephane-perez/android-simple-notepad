# Development notes

Technical details that don't need to be in the main README for someone just trying to
build or use the app, but are worth keeping around.

## Versioning

`versionName` / `versionCode` are **not** hardcoded in `app/build.gradle.kts`:
- **Our own CI** (GitHub Actions) passes them explicitly at build time:
  - On a tag push (`v1.2.3`): `versionName = "1.2.3"` (matches the GitHub Release name
    exactly), `versionCode` = the workflow run number (strictly increasing).
  - On a plain push/PR (no tag): `versionName = "0.0.0-dev.<run number>"`, so a dev
    build can never be mistaken for a tagged release.
- **Any other build environment** (F-Droid's build server in particular, which checks
  out a tagged commit and runs a plain `gradle assembleRelease` with none of the flags
  above) falls back to deriving the version straight from git: `versionName` from the
  nearest tag, `versionCode` from the total commit count (monotonic — it only grows).
  This means F-Droid's reproducible build doesn't depend on our CI at all.
- With no git history available at all (e.g. a bare source zip), it falls back further
  to `versionName = "0.0.0-dev"`, `versionCode = 1`.

## Fixed debug signing key — why it matters here

An **uninstall** (and therefore a reinstall) wipes the Android Keystore encryption key
along with the rest of the app's data — already-encrypted files then become unreadable
forever (see the README, "File encryption"). The risk is that **an ordinary update can
silently turn into a forced uninstall**: every GitHub Actions run happens on a
brand-new machine, and without explicit configuration, AGP signs each debug build with
a debug key generated on the fly on that machine (`~/.android/debug.keystore`,
different every run). Two APKs signed with different keys can't update one another —
Android requires a full uninstall before installing the next one, which destroys the
Keystore key and, with it, access to any already-encrypted file.

**The fix**: a fixed debug key, `app/debug.keystore`, generated once and committed to
the repo (password `android`, alias `androiddebugkey` — deliberately AGP's own
defaults, nothing secret to protect here: it's a *debug* key, never used for a Play
Store release). `app/build.gradle.kts` references it explicitly for the `debug` build
type, so **every build — local and CI — now shares the same signature**. A new version
then installs normally over the previous one, no uninstall needed, and the Keystore
key (and thus access to already-encrypted files) survives from one version to the
next.

## Encryption: how it works, and its security review history

**On-disk format**: `SNPD` (4-byte magic) + version (1 byte) + IV (12 bytes) +
ciphertext + 16-byte GCM authentication tag. The header (magic + version) is passed
to AES-256-GCM as AAD, so tampering with it — not just the ciphertext — is detected
too. Implementation: `data/CryptoManager.kt`, wired into
`NotepadViewModel.loadDocument()` / `.save()`.

This design was reviewed by a second AI (ChatGPT) across four rounds — the first three
focused on `CryptoManager.kt` itself, the fourth on the integration code around it
(`NotepadViewModel.kt`, `PreferencesRepository.kt`). Kept for anyone who wants the
reasoning behind a specific decision, or is doing their own review and wants to avoid
re-litigating settled points.

### Round 1 — `CryptoManager.kt`, first pass
Fixed: key created as a side effect of a failed decrypt (`getExistingKey()` vs.
`getOrCreateKey()` split); no size limit (added `MAX_PLAINTEXT_BYTES`); IV length
unchecked; key creation not thread-safe; `android:allowBackup="true"` risked an
encrypted file being backed up without its (never-backed-up) key. Declined:
requiring user authentication (biometric/PIN) on every open/save — a deliberate
product choice, not an oversight; documented in the README's guarantees/limits list.

### Round 2 — `CryptoManager.kt`, second pass
Fixed: the size limit wasn't actually enforced *inside* `decrypt()` itself, only by
its one caller; decrypted bytes weren't strictly validated as UTF-8 (silent `�`
replacement could mask corruption as garbled text). Declined: a key/format version
byte (nothing to version yet — no second key or algorithm exists) and anti-rollback
protection (replaying an old, still-authentic ciphertext) — for a personal notes app,
worst case is reading a stale draft, not worth a separately-tracked protected counter.

### Round 3 — `CryptoManager.kt`, third pass
Fixed: Keystore access inside `decrypt()` could throw outside its `runCatching`,
contradicting the documented "never throws" contract; `looksEncrypted()` accepted
anything at least header+IV long, when a real GCM container can never be shorter than
header+IV+tag; a doc comment overstated "no other device" when the accurate claim is
"not without the corresponding key". Declined: binding ciphertext to a specific note's
identity via AAD (an attacker able to swap the raw bytes of two encrypted files would
have both decrypt successfully, each showing the other's content under the wrong
filename) — would need a stable per-note ID independent of the file's URI/name
(renaming a note via an external file manager must not break decryption), which this
app has no concept of today; exploiting this already requires raw write access to the
encrypted files, at which point the attacker gains no content they didn't already
control. Also declined: the file size revealing an approximation of the plaintext
length — a standard, inherent property of unpadded authenticated encryption, not
specific to this implementation.

### Round 4 — the integration code (`NotepadViewModel.kt`, `PreferencesRepository.kt`)
With `CryptoManager` itself judged solid, this round found real functional bugs, not
just crypto hygiene:
- **A failed save could silently lose data.** `save()` didn't report success/failure,
  so "Save first" (in the unsaved-changes dialog) proceeded to discard the buffer or
  open another document even if the write itself had failed. `save()` now returns a
  `Boolean`; callers that chain further actions check it.
- **`completeSaveAs()` could point the UI at a file that was never written** — it
  updated `uri`/`filename`/`filePath` in state, then wrote. Reordered: writes first,
  only commits the new document into state once that write actually succeeded.
- **Wrong size bound on open.** Incoming raw bytes were compared against
  `MAX_PLAINTEXT_BYTES`, up to 33 bytes smaller than a real encrypted container — a
  legitimately maximal note of our own could be wrongly rejected on reopen. Now
  compared against `CryptoManager.MAX_CONTAINER_BYTES` (exposed publicly for this).
- **Size checked after loading the whole file into memory.** Used to call
  `readBytes()` before checking size at all. Now reads in 8 KB chunks and aborts as
  soon as the limit is exceeded, never buffering an oversized file fully.
- **A dead `lastUri` was retried forever.** If the last-opened document became
  inaccessible (deleted, permission revoked, moved), the app silently failed to
  restore it on every launch. It now clears the stored reference after one failed
  restore.

Declined, with reasoning:
- **Non-atomic writes.** `save()` still opens in truncate mode (`"wt"`) and writes in
  one step; a write that fails partway (disk full, provider error) can leave the file
  empty or partial rather than restoring the previous content. True atomic
  write-temp-then-rename would need reliable `DocumentsContract.renameDocument`
  support, which varies by SAF provider, for a narrow failure window on files capped
  at 100 KB.
- **The legacy plain-text path stays lenient UTF-8**, unlike the strict decoder used
  for our own encrypted format. Deliberate: the encrypted path only ever contains
  bytes this app itself produced (always valid UTF-8), while the plain-text fallback
  handles arbitrary external `.txt` files that may be in any encoding — rejecting
  those outright would break opening legitimate legacy notes.
- **`LAST_URI` in DataStore isn't itself encrypted** and can reveal a filename/path.
  Acceptable for a personal app, same spirit as the file listing already showing
  size/modified date in plain sight.
- **A possible external-Intent attack surface was raised and checked — none exists.**
  `MainActivity` has only a `MAIN`/`LAUNCHER` intent-filter (a `VIEW`/`EDIT` handler
  was prototyped once and deliberately removed) and `openDocument(uri)` is only ever
  called from in-app UI, never from externally-supplied Intent data.
- **`Uri.fromFile()`** in the in-app file browser is only ever used locally via
  `ContentResolver`, never passed to another app through an `Intent`.
- **DataStore vs. Android Backup** was already resolved in round 1
  (`android:allowBackup="false"`).
