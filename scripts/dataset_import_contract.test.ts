import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import test from "node:test";

import {
  createDatasetFromCsv,
  DatasetImportError,
  MAX_BLOCK_RECORDS,
  MAX_CSV_COLUMNS,
  MAX_TRANSACTION_RECORDS
} from "../web/lib/dataset-import-core";
import { buildDataset, parseBlocksToViews } from "../web/lib/dataset";
import { parseCsvRecords } from "../web/lib/parser";
import { downsampleSeries, MAX_SPARKLINE_POINTS } from "../web/lib/series";
import type { TransactionRecord } from "../web/lib/types";

const miner = "0x1111111111111111111111111111111111111111";
const sender = "0x2222222222222222222222222222222222222222";

function blockRow(number = 900000): string {
  const fields = Array(18).fill("");
  fields[0] = String(number);
  fields[9] = miner;
  fields[16] = "1700000000";
  fields[17] = "1";
  return fields.join(",");
}

function transactionRow({
  blockNumber = 900000,
  gasLimit = "21000",
  gasPrice = "1000000000",
  to = ""
}: {
  blockNumber?: number;
  gasLimit?: string;
  gasPrice?: string;
  to?: string;
} = {}): string {
  return [
    "0xhash",
    "21000",
    "0xblockhash",
    String(blockNumber),
    "0",
    sender,
    to,
    "0",
    gasLimit,
    gasPrice
  ].join(",");
}

test("CSV grammar preserves delimiter-only and quoted-empty records", () => {
  assert.deepEqual(parseCsvRecords(",,,,,,,,,\n\"\"\n\n"), [
    Array(10).fill(""),
    [""]
  ]);
});

test("CSV parsing aborts before allocating excess columns or records", () => {
  assert.throws(
    () => parseCsvRecords("a,b,c", { maxColumns: 2, maxRecords: 10 }),
    /row 1 exceeds the 2-column limit/
  );
  assert.throws(
    () => parseCsvRecords("a\nb\nc", { maxColumns: 2, maxRecords: 2 }),
    /exceeds the 2-record limit/
  );
  assert.equal(MAX_CSV_COLUMNS, 64);
  assert.equal(MAX_BLOCK_RECORDS, 25_000);
  assert.equal(MAX_TRANSACTION_RECORDS, 100_000);
});

test("import conversion uses the streaming record visitor without reparsing typed rows", () => {
  const source = readFileSync(resolve(process.cwd(), "web/lib/dataset-import-core.ts"), "utf8");
  assert.match(source, /return visitCsvRecords\(csvText, onRecord/);
  assert.doesNotMatch(source, /\bparseCsvRecords\(/);
  assert.doesNotMatch(source, /\bparseBlocks\(/);
  assert.doesNotMatch(source, /\bparseTransactions\(/);
});

test("block imports stop at the operational record cap", () => {
  const oversizedBlocks = Array.from(
    { length: MAX_BLOCK_RECORDS + 1 },
    (_, index) => blockRow(index + 1)
  ).join("\n");
  assert.throws(
    () => createDatasetFromCsv(oversizedBlocks, transactionRow({ blockNumber: 1 })),
    (error: unknown) =>
      error instanceof DatasetImportError &&
      error.message ===
        "Blocks CSV has more than 25,000 rows. Export a smaller slice and try again."
  );
});

test("transaction imports stop at the operational record cap", () => {
  const unmatchedRow = transactionRow({ blockNumber: 999999 });
  const oversizedTransactions = Array(MAX_TRANSACTION_RECORDS + 1).fill(unmatchedRow).join("\n");
  assert.throws(
    () => createDatasetFromCsv(blockRow(), oversizedTransactions),
    (error: unknown) =>
      error instanceof DatasetImportError &&
      error.message ===
        "Transactions CSV has more than 100,000 rows. Export a smaller slice and try again."
  );
});

test("nonmatching transactions produce a valid empty-network dataset", () => {
  const dataset = createDatasetFromCsv(blockRow(), transactionRow({ blockNumber: 999999 }));
  assert.equal(dataset.overview.parsedTransactions, 0);
  assert.equal(dataset.heaviestSender, null);
  assert.equal(dataset.heaviestReceiver, null);
  assert.equal(dataset.largestTransaction, null);
});

test("an empty transaction file remains actionable", () => {
  assert.throws(
    () => createDatasetFromCsv(blockRow(), ""),
    (error: unknown) =>
      error instanceof DatasetImportError &&
      error.message === "Transactions CSV is empty. Choose a transaction export and try again."
  );
});

for (const [label, gasLimit, gasPrice] of [
  ["fractional gas limit", "21000.5", "1000000000"],
  ["non-decimal gas limit", "0x5208", "1000000000"],
  ["unsafe gas price", "21000", "9007199254740992"]
] as const) {
  test(`rejects ${label}`, () => {
    assert.throws(
      () => createDatasetFromCsv(blockRow(), transactionRow({ gasLimit, gasPrice })),
      (error: unknown) =>
        error instanceof DatasetImportError &&
        /invalid gas (limit|price).*Export the transaction data again and retry\./.test(error.message)
    );
  });
}

test("rejects gas quantities beyond the explicit import bound", () => {
  assert.throws(
    () => createDatasetFromCsv(blockRow(), transactionRow({ gasLimit: "1000000001" })),
    (error: unknown) =>
      error instanceof DatasetImportError &&
      /gas limit exceeds the supported maximum.*Export a smaller transaction slice and retry\./.test(
        error.message
      )
  );
});

test("accepts integer-valued decimal scientific notation used by the bundled export", () => {
  const dataset = createDatasetFromCsv(blockRow(), transactionRow({ gasPrice: "1.35E+11" }));
  assert.equal(dataset.overview.parsedTransactions, 1);
  assert.ok(Number.isFinite(dataset.largestTransaction?.costEth));
});

test("contract-only activity has no heaviest receiver", () => {
  const blocks = parseBlocksToViews([
    { number: 900000, miner, timestamp: 1700000000, transactionCountMetadata: 1 }
  ]);
  const transaction: TransactionRecord = {
    blockNumber: 900000,
    index: 0,
    from: sender,
    to: "contract_creation",
    contractCreation: true,
    gasLimit: 21000,
    gasPrice: 1_000_000_000,
    costEth: 0.000021
  };
  const dataset = buildDataset(blocks, [transaction]);
  assert.equal(dataset.heaviestSender?.address, sender);
  assert.equal(dataset.heaviestReceiver, null);
});

test("dataset aggregation refuses non-finite transaction totals", () => {
  const blocks = parseBlocksToViews([
    { number: 900000, miner, timestamp: 1700000000, transactionCountMetadata: 2 }
  ]);
  const transaction: TransactionRecord = {
    blockNumber: 900000,
    index: 0,
    from: sender,
    to: "contract_creation",
    contractCreation: true,
    gasLimit: 1,
    gasPrice: 1,
    costEth: Number.MAX_VALUE
  };
  assert.throws(
    () => buildDataset(blocks, [transaction, { ...transaction, index: 1 }]),
    /Transaction cost totals exceed the supported numeric range/
  );
});

test("chart series are downsampled to a fixed display bound with endpoints", () => {
  const values = Array.from({ length: 10_000 }, (_, index) => index);
  const blockNumbers = values.map((index) => 900000 + index);
  const sampled = downsampleSeries(values, blockNumbers);
  assert.equal(sampled.values.length, MAX_SPARKLINE_POINTS);
  assert.equal(sampled.blockNumbers.length, MAX_SPARKLINE_POINTS);
  assert.equal(sampled.values[0], 0);
  assert.equal(sampled.values.at(-1), 9_999);
  assert.equal(sampled.blockNumbers.at(-1), 909_999);
});
