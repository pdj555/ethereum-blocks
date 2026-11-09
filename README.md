# Ethereum Block Explorer

Analyze Ethereum block and transaction exports privately in the browser, or explore the bundled 100-block sample. No backend, no account, no uploads.

## How it works

1. Load. Pick your CSV exports in the browser. Parsing, validation, and indexing run in a web worker.
2. Explore. Block timelines, address profiles, miner breakdowns, gas analysis, and anomaly detection scored on the median and median absolute deviation, so one extreme block cannot hide another.
3. Report. `make report` writes a Markdown summary.

The browser UI and the JSON CLI share one engine, so results agree.

## Quickstart

```bash
make dashboard   # summary in the terminal
make ui          # http://localhost:4173
make verify      # same gate as CI
```

Your own data: run `make ui`, select **Load your CSVs**, and choose both exports (up to 25 MiB each). Column layout and limits: [docs/data-format.md](docs/data-format.md).

## Commands

| Command | Output |
| :-- | :-- |
| `make help` | Command guide and runtime requirements |
| `make dashboard` | Human-readable dataset summary |
| `make ui` | Static browser explorer |
| `make block N=15049311` | Block JSON |
| `make address ADDR=0x...` | Address profile JSON |
| `make network` | Network analysis JSON |
| `make anomalies THRESHOLD=3.5` | Anomaly analysis JSON (modified z-score, median and MAD) |
| `make miners` | Unique miner breakdown JSON |
| `make report` | Writes ethereum-report.md |
| `make brief` | Action brief |
| `make snapshot` | Compact JSON context packet |
| `make run` | Interactive menu |
| `make verify` | JUnit + CLI + dependency audit + TypeScript + browser smoke |

## Verify

Requires Python 3 (`make ui`), a JDK (CLI + tests), and Node.js 24 LTS (browser build and verification).

```bash
npm ci
npm run ui:install-browsers
make verify
```

MIT · [LICENSE](LICENSE)
