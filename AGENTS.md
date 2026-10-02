# Repository Guidelines

## Project Structure & Module Organization

`src/` contains the Java CLI and domain code, with `EthereumBlockExplorer.java` as the entry point. JUnit 5 tests live beside production code as `src/Test*.java`.

`web/` is a strict TypeScript Next.js static export. Routes live in `web/app/`, UI in `web/components/`, data code in `web/lib/`, and assets in `web/public/`. Smoke harnesses are in `scripts/`; the vendored JUnit runner is in `tools/`. Keep both CSV datasets at the repository root. Treat `bin/`, `web/.next/`, `web/out/`, and copied `web/public/*.csv` files as generated.

## Build, Test, and Development Commands

- `make build`: compile production Java classes into `bin/`.
- `make dashboard`: compile and print a quick dataset summary.
- `make ui`: build the static site and serve it at `http://localhost:4173` (override with `UI_PORT` in `.env`).
- `make test`: compile and run all JUnit tests.
- `make verify`: run the CI-equivalent JUnit, CLI smoke, and Playwright UI smoke checks.
- `cd web && npm run dev`: run the Next.js development server.

Before browser smoke tests, run `npm ci` at the root and `npm run ui:install-browsers`.

## Coding Style & Naming Conventions

Match the surrounding Java style: `PascalCase` classes and `camelCase` methods and fields. TypeScript uses two-space indentation, semicolons, double quotes, `PascalCase` components, and `camelCase` helpers. Preserve strict checks and the `@/` import alias. Avoid unrelated reformatting.

Both product surfaces consume the same CSV contract. Use the quote-aware parsers instead of splitting on commas, and keep Java and browser behavior synchronized when changing fields or validation.

## Testing Guidelines

Name Java test classes `Test<ClassName>.java` and methods `testBehavior`. Call `Blocks.resetState()` when tests reload data because its caches are static. Add focused JUnit coverage and extend the relevant smoke script when public CLI JSON or browser behavior changes. There is no numeric coverage threshold; `make verify` must pass before review.

## Commit & Pull Request Guidelines

Use short, imperative, sentence-case subjects, consistent with history: `Add wallet intel command` or `Harden CSV ingestion and runtime setup`. Keep each commit to one logical change.

Pull requests should explain the affected surface, link the issue when applicable, and list verification commands. Include before/after screenshots for UI changes. Call out changes to CSV parsing, JSON field names, generated assets, or deployment behavior. Do not add agent attribution to commits or pull requests.

## Security & Configuration

Copy `.env.example` to `.env` for local overrides. Never commit credentials, local environment files, build output, or generated reports.
