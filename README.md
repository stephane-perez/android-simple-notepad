# Simple Notepad (Android)

A native Kotlin + Jetpack Compose implementation of the design in
`design_handoff_android_notepad/README.md` — a local-only, plain-text notepad in the
"Classical" editorial design language (warm near-white ground, gold accent used only as
stroke/text, hairline dividers, serif chrome with monospace editor content).

## Requirements
- Android Studio Koala (2024.1) or newer
- JDK 17
- Android SDK 34 (minSdk 26)

## Getting started
Open this folder (`android-simple-notepad/`) directly in Android Studio — it's a
self-contained Gradle project (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24) — and hit Run.
No API keys, accounts, or network access are required; the app never touches the network.

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
