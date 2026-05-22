# Ethereum Block Explorer

A bundled 100-block Ethereum dataset. Browser UI, JSON CLI, no node required.

```mermaid
flowchart LR
  CSV[Dataset] --> Core[Explorer]
  Core --> CLI[Makefile]
  Core --> UI[Browser]
```

## Get started

```bash
make dashboard   # summary in the terminal
make ui          # http://localhost:4173
make verify      # same gate as CI
```

```bash
make block N=15049311
make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f
make report      # writes ethereum-report.md
```

## Supported Commands

| Command | Output |
| :-- | :-- |
| `make help` | Command guide and runtime requirements |
| `make dashboard` | Human-readable dataset summary |
| `make ui` | Static browser explorer |
| `make block N=15049311` | Block JSON |
| `make address ADDR=0x...` | Address profile JSON |
| `make network` | Network analysis JSON |
| `make report` | Writes ethereum-report.md |
| `make run` | Interactive menu |
| `make brief` | Action brief |
| `make snapshot` | Compact JSON context packet |
| `make anomalies THRESHOLD=1.5` | Anomaly analysis JSON |
| `make miners` | Unique miner breakdown JSON |
| `make run-json` | JSON overview |
| `make verify` | JUnit + CLI + browser smoke |
| `make test` | JUnit suite |
| `make cli-smoke` | Core explorer command smoke test |
| `make ui-build` | Prepare static web files |
| `make ui-smoke` | Browser explorer smoke test |
| `make build` | Compile explorer |
| `make clean` | Remove compiled artifacts |
| `make ui-clean` | Remove generated web preview files |

Dataset files live in the repo root: `ethereumP1data.csv`, `ethereumtransactions1.csv`.

Port override: copy `.env.example` → `.env`, set `UI_PORT`.

Static deploy: `vercel.json` publishes `web/dist/`.

## Reference

Requires Python 3 (`make ui`), Java (CLI + tests), Node.js (browser build).

```bash
make test
make verify
```

MIT · [LICENSE](LICENSE)
