# Data format

Imports read these column positions:

- Blocks CSV: block number at column 1 (`[0]`), miner address at column 10 (`[9]`), Unix timestamp at column 17 (`[16]`), transaction count at column 18 (`[17]`).
- Transactions CSV: block number at column 4 (`[3]`), transaction index at column 5 (`[4]`), sender at column 6 (`[5]`), recipient at column 7 (`[6]`, blank for contract creation), gas limit at column 9 (`[8]`), gas price at column 10 (`[9]`).

Imports reject malformed rows and duplicate block numbers. Parsing stops at 64 columns, 25,000 block rows, or 100,000 transaction rows with an actionable error. Gas values must resolve to base-10 integers of zero or more; the bounds are 1,000,000,000 gas and 1,000,000,000,000,000 wei per gas. Transactions outside the imported block range are excluded. Use the bundled CSV links in the import dialog as schema examples.

Large views stay bounded: the timeline renders at most 400 cells, each spark chart renders at most 240 sampled points, transaction tables page at 50 rows. All imported blocks and matching transactions remain indexed for search.

Dataset files live in the repo root: `ethereumP1data.csv`, `ethereumtransactions1.csv`.

Port override: copy `.env.example` to `.env` and set `UI_PORT`. Static deploy: `vercel.json` publishes `web/out/`.
