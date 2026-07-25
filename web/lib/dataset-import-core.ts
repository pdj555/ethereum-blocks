import { buildDataset, parseBlocksToViews } from "./dataset";
import { CsvParseLimitError, visitCsvRecords } from "./parser";
import type { BlockRecord, Dataset, TransactionRecord } from "./types";
import { looksLikeAddress } from "./utils";

export const MAX_CSV_COLUMNS = 64;
export const MAX_BLOCK_RECORDS = 25_000;
export const MAX_TRANSACTION_RECORDS = 100_000;
export const MAX_GAS_LIMIT = 1_000_000_000;
export const MAX_GAS_PRICE_WEI = 1_000_000_000_000_000;

export class DatasetImportError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "DatasetImportError";
  }
}

export function createDatasetFromCsv(blocksCsv: string, transactionsCsv: string): Dataset {
  const blocks: BlockRecord[] = [];
  const blockNumbers = new Set<number>();
  let transactionMetadataTotal = 0;

  const blockRecordCount = readRecords(
    blocksCsv,
    "Blocks CSV",
    "block",
    MAX_BLOCK_RECORDS,
    (row, rowNumber) => {
      if (row.length < 18) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} needs at least 18 columns. Export the block data again and retry.`
        );
      }

      const blockNumber = parsePositiveSafeInteger(row[0]);
      if (blockNumber === null) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} has an invalid block number. Export the block data again and retry.`
        );
      }
      if (blockNumbers.has(blockNumber)) {
        throw new DatasetImportError(
          `Block ${blockNumber} appears more than once. Remove duplicate block rows and retry.`
        );
      }

      const miner = row[9].trim().toLowerCase();
      if (!looksLikeAddress(miner)) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} has an invalid miner address. Export the block data again and retry.`
        );
      }
      const timestamp = parseNonNegativeSafeInteger(row[16]);
      if (timestamp === null) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} has an invalid timestamp. Export the block data again and retry.`
        );
      }
      const transactionCountMetadata = parseNonNegativeSafeInteger(row[17]);
      if (transactionCountMetadata === null) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} has an invalid transaction count. Export the block data again and retry.`
        );
      }
      transactionMetadataTotal += transactionCountMetadata;
      if (!Number.isSafeInteger(transactionMetadataTotal)) {
        throw new DatasetImportError(
          `Blocks CSV row ${rowNumber} makes the transaction-count total too large. Export a smaller block slice and retry.`
        );
      }

      blockNumbers.add(blockNumber);
      blocks.push({ number: blockNumber, miner, timestamp, transactionCountMetadata });
    }
  );

  if (blockRecordCount === 0) {
    throw new DatasetImportError("Blocks CSV is empty. Choose a block export and try again.");
  }

  const transactions: TransactionRecord[] = [];
  let matchedCostTotal = 0;
  const transactionRecordCount = readRecords(
    transactionsCsv,
    "Transactions CSV",
    "transaction",
    MAX_TRANSACTION_RECORDS,
    (row, rowNumber) => {
      if (row.length < 10) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} needs at least 10 columns. Export the transaction data again and retry.`
        );
      }

      const blockNumber = parsePositiveSafeInteger(row[3]);
      if (blockNumber === null) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} has an invalid block number. Export the transaction data again and retry.`
        );
      }
      const index = parseNonNegativeSafeInteger(row[4]);
      if (index === null) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} has an invalid transaction index. Export the transaction data again and retry.`
        );
      }

      const from = row[5].trim().toLowerCase();
      if (!looksLikeAddress(from)) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} has an invalid from address. Export the transaction data again and retry.`
        );
      }
      const rawTo = row[6].trim().toLowerCase();
      if (rawTo && !looksLikeAddress(rawTo)) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} has an invalid to address. Export the transaction data again and retry.`
        );
      }

      const gasLimit = parseGasQuantity(
        row[8],
        MAX_GAS_LIMIT,
        `Transactions CSV row ${rowNumber}`,
        "gas limit"
      );
      const gasPrice = parseGasQuantity(
        row[9],
        MAX_GAS_PRICE_WEI,
        `Transactions CSV row ${rowNumber}`,
        "gas price"
      );
      const gasProduct = gasLimit * gasPrice;
      const costEth = gasProduct / 1e18;
      if (!Number.isFinite(gasProduct) || !Number.isFinite(costEth) || costEth < 0) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} has a gas-cost product outside the supported numeric range. Export a smaller transaction slice and retry.`
        );
      }

      if (!blockNumbers.has(blockNumber)) {
        return;
      }
      matchedCostTotal += costEth;
      if (!Number.isFinite(matchedCostTotal)) {
        throw new DatasetImportError(
          `Transactions CSV row ${rowNumber} makes the transaction-cost total too large. Export a smaller transaction slice and retry.`
        );
      }

      transactions.push({
        blockNumber,
        index,
        from,
        to: rawTo || "contract_creation",
        contractCreation: !rawTo,
        gasLimit,
        gasPrice,
        costEth
      });
    }
  );

  if (transactionRecordCount === 0) {
    throw new DatasetImportError(
      "Transactions CSV is empty. Choose a transaction export and try again."
    );
  }

  return buildDataset(parseBlocksToViews(blocks), transactions);
}

function readRecords(
  csvText: string,
  label: "Blocks CSV" | "Transactions CSV",
  dataKind: "block" | "transaction",
  maxRecords: number,
  onRecord: (row: string[], rowNumber: number) => void
): number {
  try {
    return visitCsvRecords(csvText, onRecord, {
      maxColumns: MAX_CSV_COLUMNS,
      maxRecords
    });
  } catch (error) {
    if (error instanceof DatasetImportError) {
      throw error;
    }
    if (error instanceof CsvParseLimitError) {
      if (error.kind === "records") {
        throw new DatasetImportError(
          `${label} has more than ${maxRecords.toLocaleString("en-US")} rows. Export a smaller slice and try again.`
        );
      }
      throw new DatasetImportError(
        `${label} row ${error.rowNumber} has more than ${MAX_CSV_COLUMNS} columns. Export the ${dataKind} data again and retry.`
      );
    }
    throw new DatasetImportError(
      `${label} contains malformed quoting. Export the data again and retry.`
    );
  }
}

function parsePositiveSafeInteger(value: string): number | null {
  const parsed = parseNonNegativeSafeInteger(value);
  return parsed !== null && parsed > 0 ? parsed : null;
}

function parseNonNegativeSafeInteger(value: string): number | null {
  const normalized = value.trim();
  if (!/^(?:0|[1-9]\d*)$/.test(normalized)) {
    return null;
  }
  const parsed = Number(normalized);
  return Number.isSafeInteger(parsed) ? parsed : null;
}

function parseGasQuantity(
  value: string,
  maximum: number,
  rowLabel: string,
  fieldLabel: "gas limit" | "gas price"
): number {
  const normalized = value.trim();
  if (!/^(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(normalized)) {
    throw new DatasetImportError(
      `${rowLabel} has an invalid ${fieldLabel}. Export the transaction data again and retry.`
    );
  }
  const parsed = Number(normalized);
  if (!Number.isSafeInteger(parsed) || parsed < 0) {
    throw new DatasetImportError(
      `${rowLabel} has an invalid ${fieldLabel}. Export the transaction data again and retry.`
    );
  }
  if (parsed > maximum) {
    throw new DatasetImportError(
      `${rowLabel} ${fieldLabel} exceeds the supported maximum of ${maximum.toLocaleString("en-US")}. Export a smaller transaction slice and retry.`
    );
  }
  return parsed;
}
