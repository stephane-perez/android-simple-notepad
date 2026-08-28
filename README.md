# Simple Notepad (Android)

A native Kotlin + Jetpack Compose implementation of the design in
`design_handoff_android_notepad/README.md` — a local-only, plain-text notepad in the
"Classical" editorial design language (warm near-white ground, gold accent used only as
stroke/text, hairline dividers, serif chrome with monospace editor content).

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
- **Every push/PR**: builds `simple-notepad.apk` and uploads it as a workflow artifact
  (Actions tab → the run → Artifacts).
- **Pushing a tag starting with `v`** (e.g. `v1.0.0`, created from the Releases page or
  `git tag v1.0.0 && git push origin v1.0.0`): additionally publishes a public GitHub
  Release with `simple-notepad.apk` attached, downloadable by anyone if the repo is
  public.

## Versioning
`versionName` / `versionCode` are **not** hardcoded in `app/build.gradle.kts` — they're
passed in at build time by the CI workflow:
- On a tag push (`v1.2.3`): `versionName = "1.2.3"` (matches the GitHub Release name
  exactly), `versionCode` = the workflow run number (strictly increasing).
- On a plain push/PR (no tag): `versionName = "0.0.0-dev.<run number>"`, so a dev build
  can never be mistaken for a tagged release.
- Building locally without those flags (e.g. `./gradlew assembleDebug` straight from
  Android Studio) falls back to `versionName = "0.0.0-dev"`, `versionCode = 1`.

## Chiffrement des fichiers

**Tout fichier enregistré par Simple Notepad est chiffré sur le disque** — impossible de
sauvegarder en clair. Seule cette app, sur cet appareil précis, peut relire le contenu.

**Comment ça marche :**
- Une clé AES-256 est générée par l'**Android Keystore** (pas par l'app elle-même), lors
  du tout premier enregistrement. Cette clé ne quitte jamais le hardware sécurisé du
  téléphone (TEE / StrongBox si disponible) — elle est **non exportable**, même en root
  dans la grande majorité des cas.
- Chaque sauvegarde chiffre le texte avec **AES-256-GCM** (authentifié, donc toute
  modification du fichier chiffré est détectée) et une IV aléatoire à chaque écriture.
  Format sur disque : `SNPD` (4 octets magiques) + version (1 octet) + IV (12 octets) +
  texte chiffré + tag d'authentification (16 octets).
- À l'ouverture, l'app tente de déchiffrer avec sa clé Keystore. **Si ça échoue** (fichier
  pas chiffré par cette app, ou clé perdue — voir plus bas), **le fichier est ouvert tel
  quel, en clair**, sans erreur bloquante : c'est le comportement demandé, qui permet
  aussi d'ouvrir sans friction d'anciens fichiers `.txt` non chiffrés.

**Garanties et limites, à bien comprendre avant de s'en servir pour des notes sensibles :**
- ✅ Un fichier `.txt` produit par Simple Notepad est illisible pour n'importe quelle
  autre app du téléphone, un ordinateur sur lequel on le copierait, ou quelqu'un qui le
  récupère via Drive/email/USB.
- ✅ Authentifié (GCM) : une modification externe du fichier chiffré est détectée (échec
  de déchiffrement) plutôt que silencieusement acceptée.
- ⚠️ **La clé ne survit ni à une désinstallation de l'app, ni à une réinitialisation
  d'usine, ni à un changement de téléphone** (les entrées Android Keystore sont exclues
  des sauvegardes par le système). Dans ces cas, les fichiers déjà chiffrés deviennent
  **définitivement illisibles** — pas de mot de passe de secours, pas de récupération
  possible. Ce n'est donc **pas** un système de sauvegarde chiffrée portable, seulement
  une protection "cet appareil, cette app".
- ⚠️ Ce n'est **pas** un chiffrement protégé par mot de passe : tant que le téléphone est
  déverrouillé et l'app installée, l'ouverture est automatique et transparente — il n'y a
  pas de verrou supplémentaire à l'intérieur de l'app elle-même.
- ⚠️ Un utilisateur avec un accès root actif au moment où l'app tourne pourrait
  potentiellement observer le texte en clair en mémoire (comme pour n'importe quelle
  app) ; la protection porte sur le fichier au repos, pas sur la RAM.

Implémentation : `data/CryptoManager.kt` (chiffrement/déchiffrement), branché dans
`NotepadViewModel.loadDocument()` (déchiffrement à l'ouverture, avec repli en clair) et
`NotepadViewModel.save()` (chiffrement systématique à l'écriture).

### Signature de debug fixe — pourquoi c'est indispensable ici

Une **désinstallation** (et donc une réinstallation) supprime la clé Keystore avec le
reste des données de l'app — les fichiers déjà chiffrés deviennent alors illisibles pour
toujours (voir garanties ci-dessus). Le risque, c'est qu'**une simple mise à jour se
transforme en désinstallation forcée sans qu'on s'en rende compte** : chaque run
GitHub Actions tourne sur une machine neuve, et sans configuration explicite, AGP signe
chaque build debug avec une clé de debug générée à la volée sur cette machine
(`~/.android/debug.keystore`, différente à chaque run). Deux APK signés avec des clés
différentes ne peuvent pas se mettre à jour l'un l'autre — Android exige une
désinstallation complète avant d'installer le suivant, ce qui détruit la clé Keystore et
donc l'accès à tout fichier déjà chiffré.

**La solution retenue** : une clé de debug fixe, `app/debug.keystore`, générée une seule
fois et committée dans le dépôt (mot de passe `android`, alias `androiddebugkey` —
volontairement les valeurs par défaut d'AGP, sans secret à protéger : c'est une clé de
*debug*, jamais utilisée pour une release Play Store). `app/build.gradle.kts` la référence
explicitement pour le `buildType debug`, donc **tous les builds — locaux et CI — partagent
désormais la même signature**. Une nouvelle version s'installe alors normalement
par-dessus l'ancienne, sans désinstallation, et la clé Keystore (donc l'accès aux
fichiers déjà chiffrés) survit d'une version à l'autre.

⚠️ **Friction unique à prévoir** : si vous avez déjà une version installée signée avec
l'ancienne clé de debug éphémère (tout ce qui précède ce changement), la toute première
installation après ce changement nécessitera encore une désinstallation manuelle — les
signatures ne correspondent pas. Toutes les mises à jour suivantes, elles, se feront sans
y toucher.

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
  "Wrap is on" and goes inert while wrap is enabled, per spec.
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
  "Chiffrement des fichiers" above.

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
