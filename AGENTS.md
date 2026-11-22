# AGENTS.md: Soundryn

Always-on rules for the coding agent. Read on every task. Prompts say "Follow AGENTS.md" and do not repeat these rules.
Last updated: 2026-11-22. Status legend: ✅ confirmed by owner, 🟡 assumed (owner may veto), ⚠️ unverified.

## 1. Project

Soundryn is an offline-first, extremely customizable Android music player. Kotlin, Jetpack Compose, Material 3 Expressive as the default design language, Clean Architecture. Min Android 12 (API 31). Package id `com.soundryn.music`; the dev version is `com.soundryn.music.dev`.
Spec: `docs/SOUNDRYN_PROJECT_MEMORY.md`. Prompt format: `docs/PROMPT_STRUCTURE.md`.

## 2. Workflow rules

1. Read the memory sections listed in the prompt before coding.
2. If the prompt conflicts with the memory file, STOP and report. Never choose silently.
3. If a fact marked verified proves wrong, STOP and report.
4. If a file to create already exists, STOP and report.
5. No dependency that is not in the spec. STOP and ask.
6. Write a short plan first. Proceed unless a stop trigger fires.
7. If the prompt has a Recon section: run it before any edit, save the output with `tee` to `docs/reports/<ID>_recon.txt`, then make the empty marker commit. The marker must be the oldest commit on the branch.
8. Keep diffs small. Do not refactor unrelated files. Do not add features, settings or screens the prompt does not list. List unrelated issues in the report.
9. Run the single verify command and make it pass before reporting done. The command is ./scripts/verify.
10. Use the exact report headings from the prompt. A report in another format is incomplete.
11. Paste code and test bodies from `cat` or `grep -n -A<N>` output. Never retype them. Never write "pasted above".
12. Use the exact test names from the prompt. Never rename or swap tests. Tests with endless loops use a child scope and a timeout. Run long commands with a timeout.
13. Code quality: write the least code that does the job. Every function and class must have a reason in the prompt. No code that makes no sense, no speculative abstractions, no pointless wrappers, no copy-paste duplicates, no dead code, no dummy declarations to satisfy a linter. If the same result takes less code, use less code. Reuse existing code. Avoid obvious inefficiencies (main-thread work, needless recomposition or allocation in hot paths, repeated queries). Before reporting, review every new function and delete or merge what is not needed. List every inline lint suppression in the report.
14. Git: one branch per prompt (🟡). Commit locally. Never push. Never merge. Never commit files the memory file marks local-only.
15. Never print, log or commit tokens, passwords, API keys, client secrets, session strings or private URLs.
16. Execute only the prompt file named in the instruction. Do not start, write, plan or read ahead into other prompt files, and do not do work that belongs to another prompt. If the instruction names no file or more than one, STOP.
17. Fix build, test and lint failures inside this prompt's scope and rerun until the verify command passes or a STOP trigger fires. Do not return a red build as "partial" unless a STOP trigger fired.

## 3. Architecture rules (Clean Architecture)

- Layers: `domain` (pure Kotlin, no `android.*`, no Compose), `data` (implements domain interfaces), `presentation` (Compose UI and state holders), `app` (wiring only).
- Dependency direction: presentation depends on domain; data depends on domain; domain depends on nothing. Presentation never imports data. Only `app` wires them.
- Every external system (music source, lyrics provider, storage, player engine) sits behind a domain interface.
- Each screen uses unidirectional flow with State, Event and Effect types (contract in memory §4).
- Package and module layout: per memory §4. DI framework: Hilt (memory D108). Room 3 with KSP, no kapt.
- Never crash on bad external data. Handle empty, malformed, offline, timeout and permission-denied cases and show them as UI state. No `!!` on external data. Catch, map and surface errors at the data boundary.

## 4. Design and motion rules

- No hardcoded design values in UI code. Colors, shapes, spacing, type, elevation, blur radius, durations, spring specs and haptic strengths come from design tokens and motion tokens (memory §5).
- Every visual state change uses a spring from the motion tokens. Linear and tween animations are forbidden unless the prompt allows them. Animations are interruptible and retarget from the current value and velocity.
- Animate only draw/transform properties (`graphicsLayer`: scale, translation, rotation, alpha). Never animate size, padding or other layout-affecting values.
- Drags track the finger 1:1 and pass release velocity into the settle spring. Ripples are bounded to the component shape. Haptics go through the single haptics controller.
- Screen-to-screen navigation uses shared-element container transforms.
- Respect reduce-motion: the user toggle and the system animation scale (memory §5).
- Blur and glass effects honour the blur toggle and the strength slider, and fall back on slow devices.
- Every user-facing feature and effect is registered in the settings registry with a toggle and a default. No feature without a toggle.
- Every component the prompt marks customizable reads its look from the active design language plus user overrides.
- UI is the owner's top priority. Build every state in the prompt's state table (loading, content, empty, error, offline) and match the wireframe, tokens and motion spec. A screen is not done from code alone: attach a screenshot of every state and a short screen recording of the interactions. Follow the prompt's reference study and anti-slop checklist. No placeholder UI in the `main` source set.

## 5. Dev version rules

- The `debug` build type is the dev version (`applicationIdSuffix ".dev"`). Logic is built and checked there first; screens, strings, themes and design languages come afterwards.
- Dev tools (the Dev Console and its panels) live only in the `debug` source set. Release code never references them.
- In the `debug` source set, hardcoded English strings and the plain default Material theme are allowed. In the `main` source set, user-visible text comes from string resources and visuals come from tokens.
- Every logic prompt adds or extends a Dev Console panel so the owner can check the feature on the phone.

## 6. Dependency, data and privacy rules

- Soundryn must stay buildable and distributable on F-Droid and GitHub: FLOSS dependencies only; no Google Play Services; no Firebase; no proprietary analytics, ads or tracking SDKs; no prebuilt binaries outside trusted Maven repositories.
- No telemetry. Network calls go only to endpoints the user configured or enabled.
- Credentials are stored encrypted. Mechanism: memory §8 (TBD).
- Spotify support is metadata and library import only. Never play, stream, record, capture or process Spotify audio. Spotify login uses only Spotify's official OAuth (Authorization Code with PKCE) with the user's own client ID. Never use cookies, web-player tokens or unofficial endpoints.
- No APK extensions and no code from outside the app. Third-party content comes only through add-ons added by URL (memory §7.2): HTTP JSON resources the app calls. Add-on responses are untrusted input: validate, size-limit, time out, never execute, never render as HTML. Built-in sources and lyrics providers sit behind the same domain interfaces (`MusicSource`, `LyricsProvider`).
- Streaming and library features only for commercial catalogs (YouTube and other services read through NewPipe Extractor). Never download, rip or cache audio files from them. Never add DRM, paywall, VIP or region bypass. Offline downloads exist only for the user's own servers and files (Jellyfin, Subsonic, Drive, Telegram) and for add-on streams that the add-on marks downloadable, after a risk warning. The built-in YouTube source stays stream-only.
- Out of scope: unofficial reverse-engineered APIs of commercial streaming services (JioSaavn, NetEase and similar) and scraping lyric sites. Only the sources named in memory §9 may be added; anything else needs a new decision in the memory file.
- Source-specific licenses apply: the app is GPL-3.0 because it bundles NewPipe Extractor (memory D67).
- The app ships no music and no source lists that point to infringing material. 🟡

## 7. Stop and ask

Stop and report when: an ambiguity changes architecture; a new dependency is needed; the prompt conflicts with the memory file; a verified fact is wrong; a permission or system behaviour differs from the prompt; a required API or symbol does not exist.
