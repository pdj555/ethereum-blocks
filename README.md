# Ethereum Block Explorer

Explore a 100-block Ethereum dataset in a lean browser UI, with fast block, address, dashboard, network, anomaly, and report workflows.

The browser UI needs the dataset files [`ethereumP1data.csv`](./ethereumP1data.csv) and [`ethereumtransactions1.csv`](./ethereumtransactions1.csv) in the repo root plus Python 3 for `make ui`. CLI commands and tests also need a working Java runtime and JDK.

## Start Here

Open the browser UI:

```bash
make ui
```

Get value immediately:

```bash
make dashboard
```

See the supported commands:

```bash
make help
```

Inspect one block:

```bash
make block N=15049311
```

Inspect one address:

```bash
make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f
```

Write a shareable markdown report:

```bash
make report
```

## Supported Commands

| Command | What it does |
| --- | --- |
| `make ui` | Build and serve the browser explorer at `http://localhost:4173` |
| `make ui-build` | Prepare the static browser files in `web/dist/` |
| `make dashboard` | Print the fastest human-readable summary of the dataset |
| `make block N=15049311` | Return one block in JSON |
| `make address ADDR=0x...` | Return one address profile in JSON |
| `make network` | Return network analysis in JSON |
| `make report` | Write `ethereum-report.md` |
| `make run` | Open the small interactive menu |
| `make help` | Show the command guide and runtime requirements |
| `make brief` | Print the action brief |
| `make anomalies` | Return anomaly analysis in JSON |
| `make miners` | Return the unique miner breakdown in JSON |
| `make run-json` | Print the JSON overview |
| `make test` | Run the existing JUnit suite |
| `make cli-smoke` | Smoke test the core explorer commands |
| `make ui-smoke` | Smoke test the browser explorer |
| `make build` | Compile the explorer into `bin/` |
| `make clean` | Remove compiled explorer artifacts |
| `make ui-clean` | Remove generated browser preview files |

`make test` uses the vendored JUnit console runner in the repo, so it does not need to fetch test tooling before it runs.
`make cli-smoke` uses Node.js only. `make ui-smoke` uses Node.js plus Playwright after `npm ci`.

## Browser UI

The browser UI is a static surface built from the repo's CSV files:

```bash
make ui
```

Use it when you want one lean visual workspace for:

- block lookup
- address lookup
- miner concentration glance
- jumping between blocks and addresses without a backend

## Interactive Menu

The interactive menu is available, but it is a secondary surface:

```bash
make run
```

Use it when you want to browse from the terminal instead of calling a specific command.
It stays focused on the core jobs: dashboard, block, address, network, report, and help.

## Repo Notes

- The default dataset contract is CSV-based: [`ethereumP1data.csv`](./ethereumP1data.csv) and [`ethereumtransactions1.csv`](./ethereumtransactions1.csv).
- [`src/Driver.java`](./src/Driver.java) is kept only as a legacy coursework demo. It is not part of the default build surface.
- Generated artifacts such as `.class` files and Javadoc output are not part of the supported product surface.
- [`vercel.json`](./vercel.json) reuses `make ui-build` and publishes `web/dist/` for Vercel deployments.

## Legacy Reference

This repo started as a coursework-style Ethereum blocks project. The material below is kept as reference, not as the primary way to approach the explorer.

<details>
<summary>Background and original class reference</summary>

A blockchain is a database of transactions that is updated and shared across many computers in a network. Every time a new set of transactions is added, it is called a block. This project uses a dataset of 100 Ethereum blocks plus a transaction dataset covering the first 15 blocks in that sample.

### Transaction UML

<img src="./imgs/TransactionUML.PNG" width="50%" height="50%">

Key `Transaction` behaviors:

- `Transaction(int number, int index, int gasLimit, long gasPrice, String fromAdr, String toAdr)`
- `getBlockNumber()`, `getIndex()`, `getGasLimit()`, `getGasPrice()`
- `getFromAddress()`, `getToAddress()`
- `transactionCost()`
- `compareTo(Transaction t)`
- `toString()` returns `Transaction <index> for Block <blockNumber>`

### Blocks UML

<img src="./imgs/BlocksUML.PNG" width="50%" height="50%">

Key `Blocks` behaviors:

- Constructors for empty, numbered, mined, and fully loaded blocks
- `getNumber()`, `getMiner()`, `getDate()`, `getTransactionCount()`, `getTransactions()`
- `calUniqMiners()`
- `getBlockByNumber(int num)`
- `blockDiff(Blocks a, Blocks b)`, `timeDiff(Blocks first, Blocks second)`, `transactionDiff(Blocks first, Blocks second)`
- `sortBlocksByNumber()`
- `readFile(String filename)` and transaction loading from [`ethereumtransactions1.csv`](./ethereumtransactions1.csv)
- `avgTransactionCost()`
- `uniqFromTo()`

`uniqFromTo()` sample output reference:

- Example image: ![uniqFromTo example](./imgs/uniq1.PNG)
- Full sample output: [`imgs/sampleoutput`](./imgs/sampleoutput)

</details>
