import { looksLikeAddress } from "./utils";
import type { BlockRecord, TransactionRecord } from "./types";

export function parseBlocks(csvText: string): BlockRecord[] {
  return parseCsvRecords(csvText)
    .filter((parts) => parts.length >= 18)
    .map((parts) => ({
      number: Number(parts[0].trim()),
      miner: parts[9].trim().toLowerCase(),
      timestamp: Number(parts[16].trim()),
      transactionCountMetadata: Number(parts[17].trim())
    }));
}

export function parseTransactions(csvText: string): TransactionRecord[] {
  const transactions: TransactionRecord[] = [];

  parseCsvRecords(csvText).forEach((parts) => {
    if (parts.length < 10) {
      return;
    }

    const from = (parts[5] || "").trim().toLowerCase();
    const rawTo = (parts[6] || "").trim().toLowerCase();
    const to = rawTo || "contract_creation";
    const gasLimit = Number(parts[8].trim());
    const gasPrice = Number(parts[9].trim());
    const blockNumber = Number(parts[3].trim());
    const index = Number(parts[4].trim());

    if (!looksLikeAddress(from)) {
      return;
    }
    if (rawTo && !looksLikeAddress(rawTo)) {
      return;
    }

    transactions.push({
      blockNumber,
      index,
      from,
      to,
      contractCreation: !rawTo,
      gasLimit,
      gasPrice,
      costEth: (gasLimit * gasPrice) / 1e18
    });
  });

  return transactions;
}

export type CsvParseLimits = {
  maxColumns?: number;
  maxRecords?: number;
};

export class CsvParseLimitError extends Error {
  constructor(
    readonly kind: "columns" | "records",
    readonly rowNumber: number,
    readonly limit: number
  ) {
    super(
      kind === "columns"
        ? `CSV row ${rowNumber} exceeds the ${limit}-column limit.`
        : `CSV input exceeds the ${limit}-record limit.`
    );
    this.name = "CsvParseLimitError";
  }
}

export function parseCsvRecords(csvText: string, limits: CsvParseLimits = {}): string[][] {
  const rows: string[][] = [];
  visitCsvRecords(csvText, (row) => rows.push(row), limits);
  return rows;
}

export function visitCsvRecords(
  csvText: string,
  onRecord: (row: string[], rowNumber: number) => void,
  limits: CsvParseLimits = {}
): number {
  let row: string[] = [];
  let field = "";
  let inQuotes = false;
  let quoteClosed = false;
  let recordHasSyntax = false;
  let recordCount = 0;

  function assertAnotherColumn() {
    if (limits.maxColumns !== undefined && row.length + 1 >= limits.maxColumns) {
      throw new CsvParseLimitError("columns", recordCount + 1, limits.maxColumns);
    }
  }

  function emitRecord() {
    if (!recordHasSyntax) {
      row = [];
      field = "";
      quoteClosed = false;
      return;
    }
    if (limits.maxRecords !== undefined && recordCount >= limits.maxRecords) {
      throw new CsvParseLimitError("records", recordCount + 1, limits.maxRecords);
    }
    row.push(field);
    recordCount += 1;
    onRecord(row, recordCount);
    row = [];
    field = "";
    quoteClosed = false;
    recordHasSyntax = false;
  }

  for (let i = 0; i < csvText.length; i += 1) {
    const current = csvText[i];

    if (inQuotes) {
      if (current === '"') {
        if (csvText[i + 1] === '"') {
          field += '"';
          i += 1;
        } else {
          inQuotes = false;
          quoteClosed = true;
        }
      } else {
        field += current;
      }
      continue;
    }

    if (quoteClosed) {
      if (current === ",") {
        assertAnotherColumn();
        row.push(field);
        field = "";
        quoteClosed = false;
        continue;
      }
      if (current === "\n" || current === "\r") {
        if (current === "\r" && csvText[i + 1] === "\n") {
          i += 1;
        }
        emitRecord();
        continue;
      }
      throw new Error("Unexpected character after closing quoted CSV field.");
    }

    if (current === '"') {
      if (field.length > 0) {
        throw new Error("Unexpected quote in unquoted CSV field.");
      }
      recordHasSyntax = true;
      inQuotes = true;
      continue;
    }

    if (current === ",") {
      recordHasSyntax = true;
      assertAnotherColumn();
      row.push(field);
      field = "";
      continue;
    }

    if (current === "\n" || current === "\r") {
      if (current === "\r" && csvText[i + 1] === "\n") {
        i += 1;
      }
      emitRecord();
      continue;
    }

    recordHasSyntax = true;
    field += current;
  }

  if (inQuotes) {
    throw new Error("Unclosed quoted CSV field.");
  }

  emitRecord();
  return recordCount;
}
