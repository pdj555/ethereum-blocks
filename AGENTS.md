# Repository Guidelines

## Project Structure & Module Organization

`src/` contains the Java CLI and domain code, with `EthereumBlockExplorer.java` as the entry point. JUnit 5 tests live beside production code as `src/Test*.java`.

`web/` is a strict TypeScript Next.js static export. Routes live in `web/app/`, UI in `web/components/`, data code in `web/lib/`, and assets in `web/public/`. Smoke harnesses are in `scripts/`; the vendored JUnit runner is in `tools/`. Keep both CSV datasets at the repository root. Treat `bin/`, `web/.next/`, `web/out/`, and copied `web/public/*.csv` files as generated.

## Build, Test, and Development Commands

- `make build`: compile production Java classes into `bin/`.
- `make dashboard`: compile and print a quick dataset summary.
- `make ui`: build the static site and serve it at `http://localhost:4173` (override with `UI_PORT` in `.env`).
- `make test`: compile and run all JUnit tests.
- `make verify`: run the same checks as CI: JUnit, CLI smoke, dependency audit, and Playwright UI smoke checks.
- `cd web && npm run dev`: run the Next.js development server.

Before browser smoke tests, run `npm ci` at the root and `npm run ui:install-browsers`.

## For agents

Needs a JDK and Node.js. Everything runs offline on the two CSVs in the repository root.

```bash
npm ci
make build

java -cp bin EthereumBlockExplorer --json help
java -cp bin EthereumBlockExplorer --json snapshot
java -cp bin EthereumBlockExplorer --json anomalies 3.5

make test
make cli-smoke
```

- `--json` prints one JSON document on stdout; warnings go to stderr. `--json help` lists every command, its argument, and which definition in `schemas/cli-output.v1.schema.json` its output matches.
- Exit codes, the same with and without `--json`:

  | Code | Meaning |
  | --- | --- |
  | `0` | ok |
  | `1` | ran, but no answer: unknown block, or a dataset with no blocks |
  | `2` | the command line or its inputs were rejected: unknown command or option, missing, malformed or extra argument, dataset CSVs not found or unreadable, report path that cannot be written |
  | `3` | internal error |
  | `4` | stdout could not be written (reader closed the pipe, device full); one line on stderr, no document |

- Every failure under `--json` prints one shape: `{"error": {"code", "message", "command", "exit_code"}}`, plus `block_number`, `file` or `directory` where they apply. `exit_code` equals the process exit code. Successful documents keep their shapes.
- Arguments are strict: a threshold is plain digits with an optional fraction (`3.5`, not `3.5f`, `1e2` or `0x1p3`), a block number is plain digits, and an argument a command does not take is rejected.
- The two CSVs are read from `--data <directory>`, else the `ETHEREUM_BLOCKS_DATA` variable, else the working directory, else the directory above the compiled classes. So `java -cp /path/to/repo/bin EthereumBlockExplorer --json snapshot` works from any directory.
- `report <file>` writes the Markdown report to that file. `report` with no file writes nothing to disk: it prints the Markdown, or returns it as `markdown` in the document under `--json`.
- Call `java` directly when the exit code matters. The `make` targets print the same JSON, but `make` reports any failure as its own exit status 2.
- Output has no clock or random input: the same CSVs give the same bytes.
- `npm ci` is only needed for the validators: `make cli-smoke` checks every command against the schema, and `node scripts/agent_smoke.mjs` runs the commands above as written.

Behaviour changes from earlier versions: failed commands used to exit `0` and print `{"error": "<text>"}`. They now exit non-zero with the error shape above, so `make address` and `make anomalies` with a bad argument fail. `--json` used to work only before the command.

## Coding Style & Naming Conventions

Match the surrounding Java style: `PascalCase` classes and `camelCase` methods and fields. TypeScript uses indentation with two spaces, semicolons, double quotes, `PascalCase` components, and `camelCase` helpers. Preserve strict checks and the `@/` import alias. Avoid unrelated reformatting.

Both product surfaces consume the same CSV contract. Use the parsers that handle quoted fields instead of splitting on commas, and keep Java and browser behavior synchronized when changing fields or validation.

## Testing Guidelines

Name Java test classes `Test<ClassName>.java` and methods `testBehavior`. Call `Blocks.resetState()` when tests reload data because its caches are static. Add focused JUnit coverage and extend the relevant smoke script when public CLI JSON or browser behavior changes. There is no numeric coverage threshold; `make verify` must pass before review.

## Commit & Pull Request Guidelines

Use short, imperative subjects in sentence case, consistent with history: `Add wallet intel command` or `Harden CSV ingestion and runtime setup`. Keep each commit to one logical change.

Pull requests should explain the affected surface, link the issue when applicable, and list verification commands. Include before/after screenshots for UI changes. Call out changes to CSV parsing, JSON field names, generated assets, or deployment behavior. Do not add agent attribution to commits or pull requests.

## Security & Configuration

Copy `.env.example` to `.env` for local overrides. Never commit credentials, local environment files, build output, or generated reports.
