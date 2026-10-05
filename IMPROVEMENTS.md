# Ethereum Block Explorer - Improvement Log

## Overview

This repo has been tightened around one supported product surface: a make-first explorer with a lean browser UI, a small interactive menu, and stable JSON outputs for automation.

## Current Workflow

Use the repo through the supported make targets:

```bash
make help
make dashboard
make ui
make verify
```

- `make help` is the command guide and runtime checklist.
- `make dashboard` is the fastest human-readable value path.
- `make ui` serves the browser explorer at `http://localhost:4173`.
- `make verify` runs the same local health gate used by CI.
- `make anomalies THRESHOLD=1.5` is the supported way to override the anomaly z-score threshold.

## Verification Repair (October 1, 2026)

The [baseline main verification run](https://github.com/pdj555/ethereum-blocks/actions/runs/36959401879) stopped at the production dependency audit with four vulnerable packages (three high, one critical), before exercising the browser. The [weekly revenue audit](https://github.com/pdj555/ethereum-blocks/actions/runs/36475788650) also failed before doing any work because the action requires a supported credential input, even when a custom endpoint is selected.

- Upgraded Next.js from 15.5.21 to 15.5.27 and React/React DOM from 19.1.0 to 19.1.9. Next.js still pins vulnerable transitive dependencies, so scoped overrides select PostCSS 8.5.28 and sharp 0.35.5; the lockfile resolves nanoid 3.3.19. The production audit now reports zero vulnerabilities. Keep these overrides until upstream versions and the audit are clean.
- Moved the production audit into `make verify`, alongside provider contracts, JUnit, CLI smoke, the CSV domain suite, full TypeScript checks, static export, and Playwright smoke. Shared `ui-deps` installs the locked web packages once per verification invocation. CI uses Node 24, caches both lockfiles, has a 15-minute limit, and uses a read-only contents token.
- Aligned Java help, Makefile help, and README commands, including the existing CSV contract target and the real static output directory, `web/out/`. Replaced the unconfigured `next lint` script with `npm run typecheck`.
- Preserved optional workflow provider priority: OAuth, then Ollama, then Anthropic API. Only the selected credential reaches the action. Ollama receives a supported API-key input plus its required Bearer header, custom endpoint, and model. Native providers receive the explicit Anthropic endpoint. No credentials or endpoint state are persisted through `GITHUB_ENV`.
- Optional workflows skip their AI step when no credentials exist, with an Actions notice and job summary explaining that no review, triage, assistant, or audit ran. Configured provider failures still fail the job. Manual PR review selects the provider from the trusted checkout before fetching PR code.

Verification evidence: the full `make verify` gate passed locally, including the clean static build, exported-browser smoke, all 59 JUnit tests, CLI smoke, zero-vulnerability production audit, TypeScript, and 15 CSV tests. The provider regression script executes the real selector shell for eight fixture credential combinations across all four workflows and verifies selected input, endpoint, header, model, skip, and credential isolation behavior. `actionlint` validates workflow syntax. Provider testing uses fixtures; no live AI credentials or API calls were used. The strict production audit remains network-dependent and detects advisories known to npm at run time.

Primary sources:

- [Next.js security advisory](https://github.com/advisories/GHSA-p293-qw3h-jr36)
- [PostCSS source-map disclosure advisory](https://github.com/advisories/GHSA-6g55-p6wh-862q)
- [sharp/libheif security advisory](https://github.com/advisories/GHSA-rgj7-g3m4-5g8c)
- [Claude action configuration](https://github.com/anthropics/claude-code-action/blob/main/docs/configuration.md)
- [Ollama API authentication](https://docs.ollama.com/api/authentication)
- [Claude custom gateway configuration](https://code.claude.com/docs/en/llm-gateway-connect)

## Key Improvements Landed

### 1. Fast Block and Transaction Access
- Added indexed block lookup so common explorer queries stay constant-time.
- Added a transaction cache keyed by block number so repeated block construction does not rescan the transaction CSV.

### 2. Safer Data Loading
- Standardized the default dataset contract on `ethereumP1data.csv` and `ethereumtransactions1.csv`.
- Failures now report the missing dataset file directly so the recovery step is obvious.
- Malformed transaction rows are skipped with warnings instead of silently corrupting downstream analysis.

### 3. Cleaner Product Surface
- Repositioned the repo around the explorer instead of coursework-era entrypoints.
- Made the browser UI a supported surface built from the same CSV data contract as the CLI.
- Kept the interactive menu as a secondary path for humans who want to browse from the terminal.

### 4. Leaner Verification
- Added CLI smoke coverage for the supported commands and common failure paths.
- Added browser smoke coverage for the static explorer.
- Added `make verify` so local verification and CI use the same contract.

### 5. Less Structural Drift
- Kept generated `.class` files and built browser artifacts out of the supported source surface.
- Reused `make ui-build` in deployment paths so the browser build stays consistent locally and in Vercel.

## Follow-Ups Worth Considering

- Replace the ad hoc Java build with Maven or Gradle if the repo grows beyond this lightweight scope.
- Harden CSV parsing further for larger or less trusted datasets.
- Add richer CI artifacts or snapshots if browser/UI work becomes a larger part of the project.
