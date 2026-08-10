# Contributing to GitUpload

Thanks for your interest in improving GitUpload! 🎉

## Development setup

1. Fork & clone the repository.
2. Copy `.env.example` to `.env` and (optionally) fill in your
   `GEMINI_API_KEY`. The build works without it — AI features just use a
   placeholder until you set a real key in-app.
3. Open in **Android Studio** (Ladybug or newer) or build from the CLI:

   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest
   ```

## Before opening a Pull Request

- Create a feature branch from `main` (`git checkout -b feat/your-feature`).
- Use [Conventional Commits](https://www.conventionalcommits.org/):
  - `feat: add …`
  - `fix: …`
  - `refactor: …`
  - `docs: …`
  - `test: …`
  - `chore: …`
- Keep the diff focused — one logical change per PR.
- Make sure the CI checks pass locally:

  ```bash
  ./gradlew testDebugUnitTest lintDebug assembleDebug
  ```

- Don't commit secrets (`.env`, keystores, `google-services.json`) — they are
  gitignored; double-check `git status` before pushing.
- If you add a new dependency, justify it in the PR description and pin it in
  `gradle/libs.versions.toml`.

## Code style

- Follow the existing Kotlin style (official Kotlin conventions, 2-space
  indent).
- Prefer Compose for all UI; keep business logic in the `data/` layer and
  state in `MainViewModel`.
- New GitHub API endpoints go through `GitHubApiService` + DTOs in
  `data/models/`, orchestrated by `GitUploadRepository`.

## Reporting bugs

Use the **Bug report** issue template. Include: Android version, device,
app version, steps to reproduce, expected vs actual behaviour, and logs if
possible.

## Requesting features

Use the **Feature request** issue template and explain the use case, not
just the solution.

Thank you! 💚
