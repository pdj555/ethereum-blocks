# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

The repo is `make`-first. CI runs the same targets you run locally.

- `make verify` — full local health gate (mirrors `.github/workflows/verify.yml`): JUnit suite + CLI smoke + browser smoke. Run this before claiming work is done.
- `make test` — JUnit suite via the vendored runner in `tools/junit-platform-console-standalone-1.10.2.jar` (no network fetch).
- `make build` / `make compile` — `javac` everything under `src/` into `bin/`. Re-runs implicitly before `make run*`.
- `make ui` — build static site into `web/dist/` and serve at `http://localhost:${UI_PORT:-4173}` via `python3 -m http.server`.
- `make ui-build` — produce `web/out/` (static Next export; same target Vercel uses; see `vercel.json`).
- `make cli-smoke` / `make ui-smoke` — Node smoke harnesses in `scripts/`. `ui-smoke` needs `npm ci` and `npx playwright install --with-deps chromium`.
- `make dashboard`, `make block N=...`, `make address ADDR=0x...`, `make network`, `make snapshot`, `make anomalies THRESHOLD=...`, `make miners`, `make report` — the supported CLI surface. `--json` mode is wired through every command in `EthereumBlockExplorer.runCommandMode`.

Running a single JUnit test: `make test` always scans the full classpath. To run one class, compile then invoke the runner directly:
```
make compile && javac -cp tools/junit-platform-console-standalone-1.10.2.jar:bin -d bin/test-classes src/TestBlocks.java && \
  java -jar tools/junit-platform-console-standalone-1.10.2.jar --class-path bin:bin/test-classes --select-class TestBlocks
```

Requirements: JDK + Java runtime, Node.js (for smoke scripts), Python 3 (for `make ui`). The dataset files `ethereumP1data.csv` and `ethereumtransactions1.csv` MUST live at the repo root — both Java and browser ingestion read them from there.

## Architecture

Two product surfaces share one CSV data contract. Keep them consistent.

**Java CLI (`src/`)** — the entrypoint is `EthereumBlockExplorer.java`. It dispatches three modes from one `main`:
1. Interactive menu (no args).
2. Command mode (`dashboard`, `block`, `address`, `network`, `snapshot`, `anomalies`, `miners`, `report`, `brief`).
3. JSON mode (`--json` flag, accepted before the subcommand). Every supported command has a JSON branch; the structured payloads come from `AgentAPI.java`.

Layering inside `src/`:
- `Blocks.java` — domain model + CSV loader. **Holds static caches** (`blockMap`, `transactionsByBlock`, `loadWarnings`). Always call `Blocks.resetState()` in tests that re-load data; the static state will otherwise leak across cases.
- `Transaction.java` — single transaction record.
- `CsvReader.java` — RFC 4180-style quote-aware record parser. Use this, not `String.split(",")` — exported fields can contain commas inside quotes.
- `Insights.java` — human-readable analytics (printed dashboards, briefs, reports).
- `AgentAPI.java` — same analytics, returned as `Map<String,Object>` for JSON output. This is the machine contract; do not break field names without updating CLI smoke expectations in `scripts/cli_smoke.mjs`.
- `NetworkAnalyzer.java` — graph analytics (whales, hubs, components, flow concentration) returned as structured maps.
- `JsonWriter.java` — zero-dep JSON serializer used by `--json` output. Accepts Map/Collection/primitive trees.
- `EthereumAddressValidator.java` — single source of truth for address-format checks.

**Browser UI (`web/`)** — Next.js static export (App Router) built with `make ui-build`, served locally via `make ui`, shipped to Vercel via `vercel.json`. The browser still parses the same CSVs locally — no backend.
- `app/page.tsx` + `components/explorer-app.tsx` — client shell: fetches `/ethereumP1data.csv` + `/ethereumtransactions1.csv`, hash routing (`#block/N`, `#address/0x…`), drives renders.
- `lib/parser.ts` — quote-aware CSV parser (mirror of `CsvReader.java`'s rules).
- `lib/dataset.ts` — builds the in-memory dataset (block map, address profiles, miner counts) — the browser-side analog of `Blocks` + `AgentAPI`.
- `components/explorer-panels.tsx` — result/rail/search UI.
- `lib/utils.ts` — formatters + address heuristics shared by components.
- `app/globals.css` — Distro-inspired dashboard styling (bordered panels, spark charts, light/dark).
- `public/` — static assets; CSVs copied from repo root at build time via `web/scripts/prebuild.mjs`.

The bridge between the two surfaces is the dataset contract. `make ui-build` exports to `web/out/` with CSVs in the static bundle, so the same file paths work locally, in `make ui-smoke`, and on Vercel.

## Conventions worth knowing

- Dataset filenames are constants in `Blocks.java` (`DEFAULT_BLOCKS_FILE`, `DEFAULT_TRANSACTIONS_FILE`). Change both Java + `web/app.js` together if you ever rename them, and update `make ui-build` which copies the files explicitly.
- Malformed transaction rows are skipped with warnings (`Blocks.transactionLoadWarning`, `Blocks.skippedTransactionRows`). Don't tighten this into a hard failure without updating the smoke expectations.
- Generated artifacts (`bin/`, `web/dist/`, `*.class` next to sources, `ethereum-report.md`) are not part of the supported surface — clean them with `make clean` / `make ui-clean`.
- `vercel.json` reuses `make ui-build` and publishes `web/dist/`. Headers (HSTS, cache, COOP/CORP) are defined there; treat them as part of the deploy contract.
- The repo ships GitHub Actions that talk back to PRs/issues (`.github/workflows/claude*.yml`). They are revenue-tone reviewers — useful context for why review comments are blunt.
