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

export function parseCsvRecords(csvText: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let field = "";
  let inQuotes = false;
  let quotedField = false;

  for (let i = 0; i < csvText.length; i += 1) {
    const current = csvText[i];

    if (current === '"') {
      if (inQuotes && csvText[i + 1] === '"') {
        field += '"';
        i += 1;
        continue;
      }

      if (!inQuotes && field.length === 0) {
        quotedField = true;
        inQuotes = true;
        continue;
      }

      if (inQuotes) {
        inQuotes = false;
        continue;
      }
    }

    if (current === "," && !inQuotes) {
      row.push(field);
      field = "";
      quotedField = false;
      continue;
    }

    if ((current === "\n" || current === "\r") && !inQuotes) {
      if (current === "\r" && csvText[i + 1] === "\n") {
        i += 1;
      }
      row.push(field);
      if (row.some((value) => value.length > 0)) {
        rows.push(row);
      }
      row = [];
      field = "";
      quotedField = false;
      continue;
    }

    if (quotedField || current !== "\r") {
      field += current;
    }
  }

  if (inQuotes) {
    throw new Error("Unclosed quoted CSV field.");
  }

  row.push(field);
  if (row.some((value) => value.length > 0)) {
    rows.push(row);
  }
  return rows;
}
