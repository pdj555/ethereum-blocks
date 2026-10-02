# Private Local Dataset Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the bundled Ethereum demo into a reusable, private browser analyzer that accepts compatible block and transaction CSV files without adding a backend.

**Architecture:** Keep the static Next.js export and existing parser/indexer as the only data path. Add one strict import boundary that validates local files before calling `buildDataset`, one accessible import dialog that owns file-selection state, and bounded timeline rendering for large block sets. The app retains the bundled dataset in memory so users can reset instantly.

**Tech Stack:** Next.js 15 Maintenance LTS, React 19, TypeScript, native File/Dialog APIs, Playwright smoke coverage, static Vercel export.

## Global Constraints

- Stay fully static and client-side. No API route, server action, database, telemetry, or upload request.
- Preserve `ethereumP1data.csv` and `ethereumtransactions1.csv` as the default dataset and preserve all existing block/address hash routes.
- Accept the repository's current positional CSV contracts: blocks use columns 1, 10, 17, and 18; transactions use columns 4, 5, 6, 7, 9, and 10.
- Reject an import before replacing the active dataset when a file is empty, larger than 25 MiB, structurally invalid, contains invalid required fields, or repeats a block number.
- Allow a valid block file with zero matching transactions; empty-network metrics must render as `—` rather than throw.
- Render at most 400 timeline cells while keeping every block available to search, keyboard stepping, and previous/next navigation.
- Pin Next.js to `15.5.21`, the July 2026 Maintenance LTS security release, with no major-version migration.
- Add no runtime or development dependencies.
- User-facing errors must state what failed and the next action. Imported filenames must never be treated as HTML.
- `make ui-build` and `make ui-smoke` must pass. `make verify` remains required when a Java runtime is available.

---

### Task 1: Secure the supported web baseline

**Files:**
- Modify: `web/package.json`
- Modify: `web/package-lock.json`

**Interfaces:**
- Consumes: the existing Next.js App Router/static-export application.
- Produces: the same `npm run build` and `next build` commands on pinned Next.js `15.5.21`.

- [ ] **Step 1: Capture the failing security gate**

Run:

```bash
cd web && npm ci && npm audit --omit=dev
```

Expected before the change: non-zero exit with the current Next.js `15.3.3` dependency reported through high/critical advisories.

- [ ] **Step 2: Install the exact Maintenance LTS patch**

Run:

```bash
cd web && npm install --save-exact next@15.5.21
```

Confirm `web/package.json` contains exactly:

```json
"next": "15.5.21"
```

Do not change React, React DOM, TypeScript, or add packages.

- [ ] **Step 3: Prove the patched dependency and static export**

Run:

```bash
cd web && npm audit --omit=dev && npm run build
```

Expected: audit exits zero with no production vulnerabilities and the static `/` route exports successfully.

- [ ] **Step 4: Commit the security baseline**

```bash
git add web/package.json web/package-lock.json
git commit -m "fix: patch Next.js security baseline"
```

---

### Task 2: Ship private local CSV analysis end to end

**Files:**
- Create: `web/lib/dataset-import.ts`
- Create: `web/components/dataset-importer.tsx`
- Modify: `web/lib/types.ts`
- Modify: `web/lib/dataset.ts`
- Modify: `web/components/block-timeline.tsx`
- Modify: `web/components/explorer-panels.tsx`
- Modify: `web/components/explorer-app.tsx`
- Modify: `web/app/globals.css`
- Modify: `web/app/layout.tsx`
- Modify: `scripts/ui_smoke.mjs`
- Modify: `README.md`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: `parseCsvRecords(csvText)`, `parseBlocks(csvText)`, `parseTransactions(csvText)`, `parseBlocksToViews(records)`, and `buildDataset(blocks, transactions)`.
- Produces: `MAX_IMPORT_FILE_BYTES = 25 * 1024 * 1024`, `DatasetImportError`, `DatasetSource`, `createDatasetFromCsv(blocksCsv, transactionsCsv)`, and `loadDatasetFiles(blocksFile, transactionsFile)` from `web/lib/dataset-import.ts`.
- Produces: `<DatasetImporter source onLoad onReset />`, where `onLoad(blocksFile, transactionsFile)` returns `Promise<void>` and `onReset()` restores the sample.
- Produces: `Dataset.heaviestSender`, `Dataset.heaviestReceiver`, and `Dataset.largestTransaction` as nullable values for valid datasets with no matched transactions.

- [ ] **Step 1: Write the failing browser contract**

Extend `scripts/ui_smoke.mjs` with deterministic local-file fixtures. Generate 450 block rows so each row has 18 fields and these values:

```javascript
const importMiner = "0x1111111111111111111111111111111111111111";
const importSender = "0x2222222222222222222222222222222222222222";
const importReceiver = "0x3333333333333333333333333333333333333333";

function blockRow(number) {
  const fields = Array(18).fill("");
  fields[0] = String(number);
  fields[9] = importMiner;
  fields[16] = "1700000000";
  fields[17] = number === 900449 ? "1" : "0";
  return fields.join(",");
}

const importedBlocks = Array.from({ length: 450 }, (_, index) => blockRow(900000 + index)).join("\n");
const importedTransactions = [
  "0xhash",
  "21000",
  "0xblockhash",
  "900449",
  "0",
  importSender,
  importReceiver,
  "0",
  "21000",
  "1000000000"
].join(",");
```

The smoke flow must prove all of these behaviors:

```javascript
await page.getByRole("button", { name: "Load your CSVs" }).click();
await page.getByLabel("Blocks CSV").setInputFiles({
  name: "invalid-blocks.csv",
  mimeType: "text/csv",
  buffer: Buffer.from("invalid")
});
await page.getByLabel("Transactions CSV").setInputFiles({
  name: "transactions.csv",
  mimeType: "text/csv",
  buffer: Buffer.from(importedTransactions)
});
await page.getByRole("button", { name: "Analyze locally" }).click();
await page.getByRole("alert").getByText("Blocks CSV row 1 needs at least 18 columns. Export the block data again and retry.").waitFor();

await page.getByLabel("Blocks CSV").setInputFiles({
  name: "blocks.csv",
  mimeType: "text/csv",
  buffer: Buffer.from(importedBlocks)
});
await page.getByRole("button", { name: "Analyze locally" }).click();
await page.getByRole("heading", { name: "Block 900000" }).waitFor();
assertCondition((await page.getByRole("listitem").count()) <= 400, "Large imports rendered too many timeline cells.");
await page.getByText("blocks.csv + transactions.csv", { exact: true }).waitFor();

await page.getByRole("button", { name: "Use bundled sample" }).click();
await page.getByRole("heading", { name: "Block 15049311" }).waitFor();
```

Run `make ui-smoke`. Expected: FAIL because the import controls do not exist.

- [ ] **Step 2: Add a strict, isolated import boundary**

Create `web/lib/dataset-import.ts` with these public declarations:

```typescript
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

export function createDatasetFromCsv(blocksCsv: string, transactionsCsv: string): Dataset;

export async function loadDatasetFiles(
  blocksFile: File,
  transactionsFile: File
): Promise<Dataset>;
```

Validate raw rows from `parseCsvRecords` before calling the typed parsers. Use one-based row numbers in errors. Blocks require at least 18 columns, a positive safe-integer block number, a valid miner address, a non-negative safe-integer timestamp, and a non-negative safe-integer transaction count. Transactions require at least 10 columns, a positive safe-integer block number, a non-negative safe-integer index, valid `from`, blank-or-valid `to`, and finite non-negative gas limit and gas price. Reject duplicate block numbers. Filter parsed transactions to the imported block-number set so overview counts only analyzeable rows. Preserve zero matching transactions.

Use these exact recovery messages for the tested boundaries:

```typescript
`${file.name} is larger than 25 MiB. Export a smaller slice and try again.`
"Blocks CSV is empty. Choose a block export and try again."
`Blocks CSV row ${rowNumber} needs at least 18 columns. Export the block data again and retry.`
`Transactions CSV row ${rowNumber} needs at least 10 columns. Export the transaction data again and retry.`
`Block ${blockNumber} appears more than once. Remove duplicate block rows and retry.`
```

- [ ] **Step 3: Make empty-network datasets safe**

Change the three `Dataset` properties to nullable:

```typescript
heaviestSender: AddressProfile | null;
heaviestReceiver: AddressProfile | null;
largestTransaction: { blockNumber: number; costEth: number } | null;
```

Initialize the largest transaction as `null` and set it on the first or higher-cost matched transaction. Return `null` for missing sender/receiver profiles. In `SideRail`, render `—` for all four network-glance values when their source is null. Do not invent block `0` or a zero-cost transaction.

- [ ] **Step 4: Bound timeline rendering without reducing navigation data**

In `web/components/block-timeline.tsx`, derive timeline points with `useMemo`. Return all points when the dataset has 400 or fewer blocks. For larger sets, choose an evenly spaced stride with `Math.ceil(total / 400)`, always include the final block, and include the active block if sampling skipped it. If adding the active block exceeds 400 points, remove its nearest non-endpoint neighbor. Sort points by original index before rendering. Keep `pickFromClientX`, keyboard stepping, search, and block navigation bound to the full `dataset.blockNumbers` array.

- [ ] **Step 5: Build the accessible import surface**

Create `web/components/dataset-importer.tsx` as a compact source strip plus native `<dialog>`. The strip must show the source label, the fixed privacy statement `Local parsing · files never uploaded`, and one primary action: `Load your CSVs` for the sample or `Replace CSVs` for local data. Show `Use bundled sample` only for local data.

The dialog must include:

```text
Analyze your own data
Choose exports that match the bundled block and transaction CSV layouts.
Blocks CSV
Transactions CSV
25 MiB per file · processed only in this browser
Cancel
Analyze locally
```

Use `FormData` on submit, reject missing files with `Choose both CSV files, then try again.`, catch `DatasetImportError`, and render the message in `role="alert"`. Disable submit while awaiting `onLoad`. On success, close and reset the form. The sample filenames may be linked as schema examples.

- [ ] **Step 6: Integrate import and instant reset into the explorer**

Build the bundled dataset through `createDatasetFromCsv` and retain it in `sampleDatasetRef`. Add `source` state initialized to `{ kind: "sample", label: "Bundled 100-block sample" }`.

On local import:

1. Await `loadDatasetFiles`.
2. Replace the dataset only after validation succeeds.
3. Set source label to `${blocksFile.name} + ${transactionsFile.name}`.
4. Switch to block mode and navigate to the imported dataset's first block.
5. Clear fatal/message state and show `Local dataset ready` in the existing toast.

On reset, restore `sampleDatasetRef.current`, navigate to block `15049311`, clear errors, and show `Bundled sample restored`.

Place `<DatasetImporter>` after the hero copy and before the command dock. Replace fixed `100blk`/`One hundred blocks`/footer copy with dataset-derived copy. The hero promise must say: `Explore the sample or load your own compatible exports. Every row is indexed locally; nothing leaves your browser.`

- [ ] **Step 7: Apply the high-taste and discoverability pass**

Add focused CSS for `.dataset-source`, `.dataset-source__meta`, `.dataset-source__actions`, `.dataset-dialog`, `.dataset-dialog__header`, `.dataset-dialog__fields`, `.dataset-dialog__field`, `.dataset-dialog__hint`, `.dataset-dialog__error`, and `.dataset-dialog__actions`. Match existing variables, borders, type scale, focus-visible treatment, dark mode, and the current mobile breakpoint. The dialog must remain within the viewport with `max-height: calc(100vh - 32px)` and `overflow: auto`.

Update metadata descriptions to lead with private analysis of the user's own compatible exports. Update README onboarding with `Load your data` and the two positional schemas. Update `CLAUDE.md` architecture notes with the import boundary, 25 MiB limit, strict validation, nullable empty-network fields, and 400-cell timeline bound.

- [ ] **Step 8: Prove the complete browser flow**

Run:

```bash
make ui-build
make ui-smoke
```

Expected: static export passes; the original block/address flow, invalid-import recovery, 450-block local import, bounded timeline, filename source state, and bundled reset all pass.

Run, when Java is available:

```bash
make verify
```

Expected: all JUnit, CLI smoke, and browser smoke checks pass. If Java is unavailable, record the exact environment error and do not describe the full gate as passing.

- [ ] **Step 9: Commit the product slice**

```bash
git add web/lib/dataset-import.ts web/components/dataset-importer.tsx web/lib/types.ts web/lib/dataset.ts web/components/block-timeline.tsx web/components/explorer-panels.tsx web/components/explorer-app.tsx web/app/globals.css web/app/layout.tsx scripts/ui_smoke.mjs README.md CLAUDE.md
git commit -m "feat: analyze local Ethereum datasets"
```
