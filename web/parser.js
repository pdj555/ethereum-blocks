import { looksLikeAddress } from "./utils.js";

export function parseBlocks(csvText) {
  return parseCsvRecords(csvText).filter(function (parts) {
    return parts.length >= 18;
  }).map(function (parts) {
    return {
      number: Number(parts[0].trim()),
      miner: parts[9].trim().toLowerCase(),
      timestamp: Number(parts[16].trim()),
      transactionCountMetadata: Number(parts[17].trim())
    };
  });
}

export function parseTransactions(csvText) {
  const transactions = [];

  parseCsvRecords(csvText).forEach(function (parts) {
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
      blockNumber: blockNumber,
      index: index,
      from: from,
      to: to,
      contractCreation: !rawTo,
      gasLimit: gasLimit,
      gasPrice: gasPrice,
      costEth: gasLimit * gasPrice / 1e18
    });
  });

  return transactions;
}

export function parseCsvRecords(csvText) {
  const rows = [];
  let row = [];
  let field = "";
  let inQuotes = false;
  let quotedField = false;

  for (let i = 0; i < csvText.length; i++) {
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
      if (row.some(function (value) { return value.length > 0; })) {
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
  if (row.some(function (value) { return value.length > 0; })) {
    rows.push(row);
  }
  return rows;
}
