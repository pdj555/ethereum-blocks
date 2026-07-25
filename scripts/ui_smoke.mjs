import { createReadStream, existsSync } from "node:fs";
import { stat } from "node:fs/promises";
import http from "node:http";
import { extname, normalize, relative, resolve, sep } from "node:path";
import { chromium } from "playwright";

const root = resolve(process.cwd(), "web/out");
const defaultAddress = "0x00000000006c3852cbef3e08e8df289169ede581";
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

function contentTypeFor(filePath) {
  switch (extname(filePath)) {
    case ".css":
      return "text/css; charset=utf-8";
    case ".js":
      return "application/javascript; charset=utf-8";
    case ".csv":
      return "text/csv; charset=utf-8";
    case ".svg":
      return "image/svg+xml";
    case ".html":
    default:
      return "text/html; charset=utf-8";
  }
}

function startStaticServer(directory) {
  const server = http.createServer(async (request, response) => {
    const requestUrl = new URL(request.url || "/", "http://127.0.0.1");
    let relativePath = decodeURIComponent(requestUrl.pathname);
    if (relativePath === "/") {
      relativePath = "/index.html";
    }

    const filePath = resolve(directory, "." + normalize(relativePath));
    const relativeFilePath = relative(directory, filePath);
    if (relativeFilePath.startsWith(".." + sep) || relativeFilePath === ".." || relativeFilePath === "") {
      response.writeHead(403, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Forbidden");
      return;
    }

    if (!existsSync(filePath)) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not found");
      return;
    }

    const info = await stat(filePath);
    response.writeHead(200, {
      "Content-Length": info.size,
      "Content-Type": contentTypeFor(filePath)
    });
    createReadStream(filePath).pipe(response);
  });

  return new Promise((resolveServer) => {
    server.listen(0, "127.0.0.1", () => resolveServer(server));
  });
}

function assertCondition(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const server = await startStaticServer(root);
const addressInfo = server.address();
const baseUrl = `http://127.0.0.1:${addressInfo.port}`;
const invalidAddress = `0x${"z".repeat(40)}`;
let browser;

try {
  browser = await launchBrowser();
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  await page.goto(baseUrl, { waitUntil: "networkidle" });

  assertCondition((await page.title()).startsWith("Ethereum Block Explorer"), "Unexpected browser title.");
  await page.getByRole("heading", { name: "Block 15049311" }).waitFor();
  assertCondition(
    await page.getByText("100 blocks", { exact: true }).first().isVisible(),
    "Overview summary did not render."
  );

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

  await page.getByRole("button", { name: "Address" }).click();
  await page.getByLabel("Search query").fill(invalidAddress);
  await page.getByRole("button", { name: "Inspect" }).click();
  await page.getByText("Enter a full Ethereum address.").waitFor();
  assertCondition(
    await page.getByText("Enter a full Ethereum address.").isVisible(),
    "Invalid address guidance did not render."
  );
  await page.getByText("Use 0x followed by 40 hex characters").waitFor();
  assertCondition(
    await page.getByText("Use 0x followed by 40 hex characters").isVisible(),
    "Invalid address recovery copy did not render."
  );
  assertCondition(
    !page.url().endsWith(`#address/${invalidAddress}`),
    "Invalid address unexpectedly updated the URL hash."
  );

  await page.getByLabel("Search query").fill(defaultAddress);
  await page.getByRole("button", { name: "Inspect" }).click();
  await page.getByRole("heading", { name: /0x000000\.\.\.ede581/ }).waitFor();
  assertCondition(
    page.url().endsWith(`#address/${defaultAddress}`),
    "Address navigation did not update the URL hash."
  );
  assertCondition(
    await page.getByText("pure receiver").isVisible(),
    "Address profile details did not render."
  );

  await page.getByRole("button", { name: "Block 15049319" }).first().click();
  await page.getByRole("heading", { name: "Block 15049319" }).waitFor();
  assertCondition(
    page.url().endsWith("#block/15049319"),
    "Block navigation from the address view did not update the URL hash."
  );

  console.log("UI smoke passed.");
} finally {
  if (browser) {
    await browser.close();
  }
  await new Promise((resolveClose, rejectClose) => {
    server.close((error) => (error ? rejectClose(error) : resolveClose()));
  });
}

async function launchBrowser() {
  try {
    return await chromium.launch({ headless: true });
  } catch (error) {
    if (error && String(error.message || "").includes("Executable doesn't exist")) {
      throw new Error(
        "Playwright Chromium is not installed for the current Playwright version. Run `npm run ui:install-browsers`, then rerun `make ui-smoke`.",
        { cause: error }
      );
    }
    throw error;
  }
}
