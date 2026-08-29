# Angerona (Android)

A native Kotlin + Jetpack Compose implementation of the design in
`design_handoff_android_notepad/README.md` — a local-only, encrypted, plain-text notepad
in the "Classical" editorial design language (warm near-white ground, gold accent used
only as stroke/text, hairline dividers, serif chrome with monospace editor content).

### Why "Angerona"?

Angerona was a minor Roman goddess of silence and secrecy. Her statue was shown with a
bandaged mouth, or a finger held to her lips. She was the keeper of Rome's *secret
name* — a name the city deliberately never spoke aloud, since revealing it was believed
to leave the city vulnerable to enemies invoking it in a curse. A fitting, appropriately
understated namesake for an app whose entire job is to keep written words unreadable to
anyone but their author.

> **AI authorship disclosure.** Every line of code, configuration, and documentation in
> this repository — the app itself, the Gradle setup, the CI/CD workflow, and this
> README — was designed and written by an AI (Claude, Anthropic), working from the
> design handoff and iterating on human feedback (bug reports, CI fixes, build
> troubleshooting). No line was hand-written by a person; a human reviewed the result,
> ran it, and reported back issues that were then fixed by the same AI.

## Requirements
- JDK 17
- Android SDK 34 (minSdk 26)
- Android Studio Koala (2024.1) or newer — optional, see "Building without Android
  Studio" below.

## Getting started
Open this folder (`android-simple-notepad/`) directly in Android Studio — it's a
self-contained Gradle project (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24) — and hit Run.
No API keys, accounts, or network access are required; the app never touches the network.

## Building without Android Studio
No local Android SDK is required. Every push to `main` (and every pull request) builds
a debug APK via GitHub Actions — see `.github/workflows/build.yml`:
- **Every push/PR**: builds `angerona-notepad.apk` and uploads it as a workflow artifact
  (Actions tab → the run → Artifacts).
- **Pushing a tag starting with `v`** (e.g. `v1.0.0`, created from the Releases page or
  `git tag v1.0.0 && git push origin v1.0.0`): additionally publishes a public GitHub
  Release with `angerona-notepad.apk` attached, downloadable by anyone if the repo is
  public.

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

## License
WTFPL — see [`LICENSE`](LICENSE). Do what you want with it.

## F-Droid readiness
This repo includes what F-Droid's inclusion process expects beyond the code itself:
- [`LICENSE`](LICENSE) — a FOSS license (FSF-approved) is a hard requirement.
- `fastlane/metadata/android/{en-US,fr-FR,es-ES}/` — store listing text (title, short
  and full description) in each supported language, plus a `changelogs/` folder (add
  one `<versionCode>.txt` file per release going forward).
- Self-contained versioning from git (see "Versioning" above) — no external CI needed
  to produce a correct, real version number.
- No Google Play Services, no analytics, no ads, no network permission at all — nothing
  in the anti-features list applies.

Not yet done: actually opening the inclusion request (a merge request against
[fdroiddata](https://gitlab.com/fdroid/fdroiddata) with a recipe describing this repo,
license, and build steps) and adding a few screenshots under
`fastlane/metadata/android/<locale>/images/phoneScreenshots/`.

## File encryption

**Every file saved by Angerona is encrypted on disk** — there is no way to save in
plain text. Only this app, on this specific device, can read the content back.

> This design was reviewed by a second AI (ChatGPT), asked specifically to find flaws
> in `CryptoManager.kt`, across three rounds. Round 1 caught the header-authentication,
> key-separation, size-limit, and backup fixes below. Round 2 caught two real remaining
> gaps: the 100 KB limit wasn't enforced *inside* `decrypt()` itself (only by its one
> caller), and decrypted bytes weren't strictly validated as UTF-8 — both fixed, no
> format change. Round 3 found three more small but real gaps, all fixed without any
> format change: Keystore access inside `decrypt()` could throw outside its `runCatching`
> (contradicting the documented "never throws" contract — now the whole function body is
> covered); `looksEncrypted()` accepted anything at least header+IV long, when a real GCM
> container can never be shorter than header+IV+tag; and a doc comment overstated "no
> other device" when the accurate claim is "not without the corresponding key". One
> substantive point was raised and deliberately not acted on: **ciphertext isn't bound to
> a specific note's identity** — the AAD only covers the format header, so an attacker
> able to swap the raw bytes of two encrypted files would have both decrypt
> successfully, each showing the other's content under the wrong filename. Binding to a
> stable per-note ID would require an identifier independent of the file's URI/name
> (renaming a note through an external file manager must not break decryption), which
> this app has no concept of today. Given that exploiting this already requires raw
> write access to the encrypted files — at which point the attacker already had both
> ciphertexts and gains no content they didn't already control — this was judged
> disproportionate to add for a personal notes app, same as the already-declined
> anti-rollback protection and key-generation version byte (no second key or algorithm
> exists yet to version). The file size revealing an approximation of the plaintext
> length was also raised and accepted as a standard, inherent property of unpadded
> authenticated encryption, not something specific to fix here.

**How it works:**
- An AES-256 key is generated by the **Android Keystore** (not by the app itself), on
  first save. This key is **non-exportable**, even under root in the vast majority of
  cases. Whether it's additionally backed by dedicated secure hardware (TEE/StrongBox)
  depends on the device — the Keystore API guarantees non-exportability, not hardware
  backing specifically.
- Every save encrypts the text with **AES-256-GCM** (authenticated — both the ciphertext
  and the format header are covered, via GCM's AAD mechanism, so tampering with either
  is detected) and a fresh random IV each time. On-disk format: `SNPD` (4-byte magic) +
  version (1 byte) + IV (12 bytes) + ciphertext + 16-byte authentication tag.
- On open, the app checks whether the file even *looks* like one of ours (the `SNPD`
  header) before doing anything else:
  - **No header at all** → an ordinary file that was never encrypted by this app (e.g.
    an older plain-text `.txt`). Opens as-is, no error, no friction.
  - **Header present, decryption fails** → this file was one of ours, but can no longer
    be read (key lost to a reinstall/reset, or the file was corrupted/tampered with).
    The app shows a warning and does **not** attempt to display the raw ciphertext
    bytes as if they were text — an earlier version of this logic conflated the two
    cases, which a second-opinion security review (see above) rightly flagged.
- **Size limit**: files over 100 KB are refused on both open and save, with a clear
  message rather than risking an out-of-memory crash on a maliciously (or accidentally)
  huge file. This is enforced both where files are opened/saved *and* inside
  `CryptoManager.decrypt()` itself, so the guarantee holds regardless of caller. Nobody
  is editing a 100 KB+ plain-text note in this app anyway.

**Guarantees and limits — read this before trusting it with anything sensitive:**
- ✅ A `.txt` file produced by Angerona is unreadable by any other app on the
  phone, a computer it gets copied to, or anyone who picks it up via Drive/email/USB.
- ✅ Authenticated (GCM, header included via AAD): external tampering with the
  encrypted file — ciphertext or header — is detected and surfaced, not silently
  swallowed.
- ⚠️ **The key survives neither an app uninstall, nor a factory reset, nor a device
  change.** In those cases, already-encrypted files become **permanently unreadable** —
  no recovery password, no way back. This is therefore **not** a portable
  encrypted-backup system, only a "this device, this app" protection. Android's
  Auto Backup is disabled entirely for this app (`android:allowBackup="false"`)
  specifically to avoid the confusing scenario where an encrypted *file* gets backed up
  to the cloud while its Keystore *key* — which is never backed up by the OS regardless
  — doesn't, leaving a file that looks restored but can never be opened again.
- ⚠️ This is **not** password-protected encryption: as long as the phone is unlocked and
  the app installed, opening is automatic and transparent — there is no additional lock
  inside the app itself.
- ⚠️ Someone with active root access while the app is running could potentially observe
  the plain text in memory (as with any app); the protection covers the file at rest,
  not RAM.

Implementation: `data/CryptoManager.kt` (encrypt/decrypt), wired into
`NotepadViewModel.loadDocument()` (decrypt on open, with plain-text fallback) and
`NotepadViewModel.save()` (always encrypts on write).

### Second-opinion review, round 4: the code around `CryptoManager`

With `CryptoManager` itself judged solid, the same second AI reviewed
`NotepadViewModel.kt` and `PreferencesRepository.kt` — the integration code that
actually opens, saves, and persists references to files. This round found real
functional bugs, not just crypto hygiene:

**Fixed:**
- **A failed save could silently lose data.** `save()` didn't report success/failure,
  so "Save first" (in the unsaved-changes dialog) proceeded to discard the buffer or
  open another document *even if the write itself had failed* — a genuine data-loss
  bug. `save()` now returns a `Boolean`; callers that chain further actions check it.
- **`completeSaveAs()` could point the UI at a file that was never written** — it
  updated `uri`/`filename`/`filePath` in state, *then* wrote. Reordered: it now writes
  first and only commits the new document into state once that write actually
  succeeded.
- **Wrong size bound on open.** `loadDocument()` compared incoming raw bytes against
  `MAX_PLAINTEXT_BYTES`, which is up to 33 bytes smaller than a real encrypted
  container — a legitimately maximal note of our own could be wrongly rejected on
  reopen. Now compared against `CryptoManager.MAX_CONTAINER_BYTES` (exposed publicly
  for this).
- **Size checked after loading the whole file into memory.** `loadDocument()` used to
  call `readBytes()` before checking size at all, so `CryptoManager`'s own internal
  guard couldn't help — the ViewModel had already allocated the whole buffer. Now reads
  in 8 KB chunks and aborts as soon as the limit is exceeded, never buffering an
  oversized file fully.
- **A dead `lastUri` was retried forever.** If the last-opened document became
  inaccessible (deleted, permission revoked, moved), the app silently failed to restore
  it on *every* launch. It now clears the stored reference after one failed restore.

**Explicitly not changed, with reasoning:**
- **Non-atomic writes.** `save()` still opens in truncate mode (`"wt"`) and writes in
  one step; a write that fails partway (disk full, provider error) can leave the file
  empty or partial rather than restoring the previous content. A true atomic
  write-temp-then-rename would need reliable `DocumentsContract.renameDocument` support,
  which varies by SAF provider, for a narrow failure window on files capped at 100 KB.
  Accepted as a known limitation rather than solved.
- **The legacy plain-text path stays lenient UTF-8**, unlike the strict decoder in
  `CryptoManager.decrypt()`. This is deliberate, not an inconsistency to fix: the
  encrypted path only ever contains bytes this app itself produced (always valid
  UTF-8, so any deviation is real corruption), while the plain-text fallback handles
  arbitrary external `.txt` files that may be in any encoding — rejecting those outright
  would break opening perfectly legitimate legacy notes.
- **`LAST_URI` in DataStore isn't itself encrypted** and can reveal a filename/path via
  the content URI string. Judged acceptable for a personal app in the same spirit as
  the file listing already showing size/modified date in plain sight; encrypting it
  would need the same Keystore key this whole design is built around, adding
  complexity for metadata, not content.
- **A possible external-Intent attack surface was raised and checked — none exists.**
  `MainActivity` has only a `MAIN`/`LAUNCHER` intent-filter (a `VIEW`/`EDIT` handler was
  prototyped once earlier in development and deliberately removed, see git history) and
  `openDocument(uri)` is only ever called from in-app UI (the Open overlay, or a
  system-picker result) — never from externally-supplied Intent data.
- **`Uri.fromFile()` in the in-app file browser** doesn't need SAF-style URI grants
  since it's only ever used locally via `ContentResolver`, never passed to another app
  through an `Intent` (which is the actual trigger for `FileUriExposedException`).
- **DataStore vs. Android Backup**, raised for `PreferencesRepository`, was already
  resolved in an earlier round (`android:allowBackup="false"`) — DataStore's file lives
  in the app's private storage, excluded from backup entirely regardless.

### Fixed debug signing key — why it matters here

An **uninstall** (and therefore a reinstall) wipes the Keystore key along with the rest
of the app's data — already-encrypted files then become unreadable forever (see the
guarantees above). The risk is that **an ordinary update can silently turn into a forced
uninstall**: every GitHub Actions run happens on a brand-new machine, and without
explicit configuration, AGP signs each debug build with a debug key generated on the fly
on that machine (`~/.android/debug.keystore`, different every run). Two APKs signed with
different keys can't update one another — Android requires a full uninstall before
installing the next one, which destroys the Keystore key and, with it, access to any
already-encrypted file.

**The fix**: a fixed debug key, `app/debug.keystore`, generated once and committed to the
repo (password `android`, alias `androiddebugkey` — deliberately AGP's own defaults,
nothing secret to protect here: it's a *debug* key, never used for a Play Store release).
`app/build.gradle.kts` references it explicitly for the `debug` build type, so **every
build — local and CI — now shares the same signature**. A new version then installs
normally over the previous one, no uninstall needed, and the Keystore key (and thus
access to already-encrypted files) survives from one version to the next.

⚠️ **One-time friction to expect**: if you already have a version installed that was
signed with the old, ephemeral debug key (anything before this change), the very first
install after this change still requires a manual uninstall — the signatures don't
match. Every update after that will install cleanly on top.

## Localization

The app ships in **English, French, and Spanish** using Android's native string
resources (`res/values/strings.xml`, `res/values-fr/strings.xml`,
`res/values-es/strings.xml`) — no library, no in-app language switcher. Android picks
the matching resource file automatically based on the phone's system language, and
falls back to English for any other language.

All user-facing text (button labels, dialog copy, toasts, status bar counts) goes
through `stringResource()` / `pluralStringResource()` in Compose, or
`Context.getString()` / `Resources.getQuantityString()` in `NotepadViewModel` for
toasts, which are generated outside a `@Composable` scope. Plurals (line/lines,
char/chars, match/matches replaced) use Android's `<plurals>` resource so each language
handles singular/plural correctly rather than concatenating English-shaped fragments.

Two things are deliberately **not** translated: the app name, and the default filename
`untitled.txt` — kept invariant so files stay portable and unambiguous regardless of the
phone's language.

To add a fourth language: add `res/values-<code>/strings.xml` with the same keys as
`res/values/strings.xml` (Android Studio's Translations Editor lists any missing keys
for you); no code changes needed.


## What's implemented
- **Editor** — top app bar (New / Open / Save / Find / More) with a dirty-state dot,
  monospace editor with an optional line-number gutter (hidden while wrap is on), and a
  bottom status bar with file path + live line/char counts.
- **Open document** — full-screen overlay listing `.txt` files from the app's own
  documents directory (the design's stand-in for the system picker), plus a "Browse
  device storage…" row that launches `ACTION_OPEN_DOCUMENT` for anything else.
- **Find & replace** — literal, case-sensitive substring search, match counter (`3 / 7`
  / `none`), replace-all with a toast (`7 matches replaced` / `1 match replaced` /
  `No matches`), previous/next with wraparound.
- **Overflow menu** — Word wrap / Line numbers toggles; the Line numbers row shows
  "Wrap is on" and goes inert while wrap is enabled, per spec. Word wrap defaults to
  **on**.
- **Unsaved changes dialog** — Cancel / Discard / Save first, with the exact copy from
  the handoff, gating New and Open when the buffer is dirty.
- **Toast** — bottom-anchored, monospace, 2200ms auto-dismiss.
- **State** — a single `NotepadViewModel` (`StateFlow`) matching the handoff's state
  table exactly, with `wrap` / `lineNumbers` / last-opened URI persisted via DataStore
  and restored on relaunch.
- **File access** — Storage Access Framework: `ACTION_OPEN_DOCUMENT` for Open,
  `ACTION_CREATE_DOCUMENT` for a new buffer's first Save, `ContentResolver` streams after
  that, with persistable URI permissions taken so re-opened files survive restarts.
- **Edge-to-edge display** — content is padded with `WindowInsets.safeDrawing` so the
  top app bar and bottom status bar/toast clear the status and navigation bars, while
  the background still paints edge-to-edge behind them.
- **System Back button** — only intercepted when something is open to close first
  (overflow menu, find strip, Open overlay, unsaved-changes dialog); otherwise it falls
  through to the system default so the app exits/minimizes normally.
- **Encryption** — every save is encrypted (AES-256-GCM, Android Keystore-backed);
  every open tries to decrypt and falls back to plain text if that fails. See
  "File encryption" above.

## Design tokens
All colors, type sizes, spacing, and radii are transcribed from the handoff into
`ui/theme/Color.kt`, `ui/theme/TextStyles.kt`, and used directly by each component —
there's no separate "port to Material3 defaults" step; the handoff's numbers are the
source of truth.

Fonts: **Cormorant Garamond** and **Lora** (both OFL) are bundled as actual font files
under `res/font/` per the handoff's "bundle" instruction, rather than fetched at runtime
via the Downloadable Fonts API. License texts are included under
`app/src/main/assets/font_licenses/`.

## Known adaptations / simplifications
- **Icons**: the eight required glyphs (`file-plus`, `folder-open`, `save`, `search`,
  `more-vertical`, `arrow-left`, `chevron-up`, `chevron-down`) are recreated as Android
  vector drawables directly from Lucide's own SVG source (ISC licensed), tinted at
  runtime to match `ink` (`#201f1d`).
- **Line-number gutter**: implemented with a shared `ScrollState` between the gutter and
  the text field so numbers track the text vertically. This works well for the common
  case; extremely large documents may want a virtualized editor for best performance —
  out of scope for this handoff-fidelity pass.
- **"Save first" from the unsaved-changes dialog** when the *current* buffer has never
  been saved (`uri == null`) routes through the `ACTION_CREATE_DOCUMENT` picker and then
  closes the dialog; because that picker is asynchronous, the pending New/Open action is
  not auto-resumed after it returns in this pass — the person can re-trigger New/Open
  once the save completes. All other paths (existing file, Discard, Cancel) behave
  exactly as specified.
- No dark theme: the handoff is explicitly light-only ("warm near-white ground"), so
  none is offered.

## Not implemented (explicitly out of scope per the handoff)
- Save-as dialog / confirmation step on Save.
- Save on `onStop`.
- Any network access, accounts, or formatting.
