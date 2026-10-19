# Contributing to Mentara

First off — thank you. Mentara is built in the open and every Issue / PR matters.

## Ground rules

1. **No secrets. Ever.** Never commit API keys, tokens, `local.properties`, keystores, or model weights.
2. **One purpose per PR.** Bug fix, feature, docs — separate PRs.
3. **Discuss before big changes.** Open an Issue first for anything touching `shared/` public APIs or the parse pipeline.

## Workflow

```bash
git clone https://github.com/<your-name>/mentara.git
cd mentara
git checkout -b feat/your-feature   # or fix/… or docs/…
```

Build & test before pushing:

```bash
./gradlew :shared:jvmTest :androidApp:assembleDebug --offline
./gradlew :desktopApp:build --offline
```

## Commit style (Conventional Commits)

- `feat: add subtitle-style ASR segmentation`
- `fix: stop OCR garbage before LLM`
- `docs: …` / `refactor: …` / `chore: …`

## PR checklist

- [ ] Builds locally (`:shared:jvmTest`, `:androidApp:assembleDebug`)
- [ ] No secrets / no model files / no build outputs staged
- [ ] UI changes include a screenshot or short screen recording
- [ ] New logic has a test in `shared/src/commonTest`
- [ ] Primarily Chinese UI strings stay Chinese

## Architecture note

`shared/` is the single source of truth (KMP). Platform apps are thin shells:

```
shared/       # core: parser, ASR routing, OCR pipeline, debate engine, storage
androidApp/   # Jetpack Compose UI + sherpa-onnx JNI + ML Kit
desktopApp/   # Compose Desktop UI + Windows toolchain glue
```

Do **not** duplicate logic into platform modules — extend `shared/` instead.

Thank you for helping a teenager build something real.