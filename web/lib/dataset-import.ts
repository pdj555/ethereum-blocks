import { buildDataset, parseBlocksToViews } from "./dataset";
import { parseBlocks, parseCsvRecords, parseTransactions } from "./parser";
import type { Dataset } from "./types";
import { looksLikeAddress } from "./utils";

export const MAX_IMPORT_FILE_BYTES = 25 * 1024 * 1024;

export type DatasetSource =
  | { kind: "sample"; label: "Bundled 100-block sample" }
  | { kind: "local"; label: string };

export class DatasetImportError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "DatasetImportError";
  }
}

export function createDatasetFromCsv(blocksCsv: string, transactionsCsv: string): Dataset {
  const blockRows = readRows(blocksCsv, "Blocks CSV");
  if (blockRows.length === 0) {
    throw new DatasetImportError("Blocks CSV is empty. Choose a block export and try again.");
  }

  const blockNumbers = new Set<number>();
  blockRows.forEach((row, index) => {
    const rowNumber = index + 1;
    if (row.length < 18) {
      throw new DatasetImportError(
        `Blocks CSV row ${rowNumber} needs at least 18 columns. Export the block data again and retry.`
      );
    }

    const blockNumber = Number(row[0].trim());
    if (!Number.isSafeInteger(blockNumber) || blockNumber <= 0) {
      throw new DatasetImportError(
        `Blocks CSV row ${rowNumber} has an invalid block number. Export the block data again and retry.`
      );
    }
    if (blockNumbers.has(blockNumber)) {
      throw new DatasetImportError(
        `Block ${blockNumber} appears more than once. Remove duplicate block rows and retry.`
      );
    }
    if (!looksLikeAddress(row[9].trim())) {
      throw new DatasetImportError(
        `Blocks CSV row ${rowNumber} has an invalid miner address. Export the block data again and retry.`
      );
    }
    if (!isNonNegativeSafeInteger(row[16])) {
      throw new DatasetImportError(
        `Blocks CSV row ${rowNumber} has an invalid timestamp. Export the block data again and retry.`
      );
    }
    if (!isNonNegativeSafeInteger(row[17])) {
      throw new DatasetImportError(
        `Blocks CSV row ${rowNumber} has an invalid transaction count. Export the block data again and retry.`
      );
    }

    blockNumbers.add(blockNumber);
  });

  const transactionRows = readRows(transactionsCsv, "Transactions CSV");
  if (transactionRows.length === 0) {
    throw new DatasetImportError(
      "Transactions CSV is empty. Choose a transaction export and try again."
    );
  }
  transactionRows.forEach((row, index) => {
    const rowNumber = index + 1;
    if (row.length < 10) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} needs at least 10 columns. Export the transaction data again and retry.`
      );
    }
    if (!isPositiveSafeInteger(row[3])) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid block number. Export the transaction data again and retry.`
      );
    }
    if (!isNonNegativeSafeInteger(row[4])) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid transaction index. Export the transaction data again and retry.`
      );
    }
    if (!looksLikeAddress(row[5].trim())) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid from address. Export the transaction data again and retry.`
      );
    }
    const to = row[6].trim();
    if (to && !looksLikeAddress(to)) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid to address. Export the transaction data again and retry.`
      );
    }
    if (!isFiniteNonNegativeNumber(row[8])) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid gas limit. Export the transaction data again and retry.`
      );
    }
    if (!isFiniteNonNegativeNumber(row[9])) {
      throw new DatasetImportError(
        `Transactions CSV row ${rowNumber} has an invalid gas price. Export the transaction data again and retry.`
      );
    }
  });

  const blocks = parseBlocksToViews(parseBlocks(blocksCsv));
  const transactions = parseTransactions(transactionsCsv).filter((transaction) =>
    blockNumbers.has(transaction.blockNumber)
  );
  return buildDataset(blocks, transactions);
}

export async function loadDatasetFiles(
  blocksFile: File,
  transactionsFile: File
): Promise<Dataset> {
  assertFileSize(blocksFile);
  assertFileSize(transactionsFile);
  const [blocksCsv, transactionsCsv] = await Promise.all([
    blocksFile.text(),
    transactionsFile.text()
  ]);
  return createDatasetFromCsv(blocksCsv, transactionsCsv);
}

function readRows(csvText: string, label: string): string[][] {
  try {
    return parseCsvRecords(csvText);
  } catch {
    throw new DatasetImportError(
      `${label} contains malformed quoting. Export the data again and retry.`
    );
  }
}

function assertFileSize(file: File) {
  if (file.size > MAX_IMPORT_FILE_BYTES) {
    throw new DatasetImportError(
      `${file.name} is larger than 25 MiB. Export a smaller slice and try again.`
    );
  }
}

function isPositiveSafeInteger(value: string): boolean {
  const normalized = value.trim();
  const parsed = Number(normalized);
  return normalized !== "" && Number.isSafeInteger(parsed) && parsed > 0;
}

function isNonNegativeSafeInteger(value: string): boolean {
  const normalized = value.trim();
  const parsed = Number(normalized);
  return normalized !== "" && Number.isSafeInteger(parsed) && parsed >= 0;
}

function isFiniteNonNegativeNumber(value: string): boolean {
  const normalized = value.trim();
  const parsed = Number(normalized);
  return normalized !== "" && Number.isFinite(parsed) && parsed >= 0;
}
