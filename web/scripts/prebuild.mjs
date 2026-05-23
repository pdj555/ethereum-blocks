import { copyFileSync, existsSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "../..");
const publicDir = resolve(root, "web/public");
const files = ["ethereumP1data.csv", "ethereumtransactions1.csv"];

mkdirSync(publicDir, { recursive: true });

for (const file of files) {
  const source = resolve(root, file);
  if (!existsSync(source)) {
    console.error(`Missing dataset: ${source}`);
    process.exit(1);
  }
  copyFileSync(source, resolve(publicDir, file));
}
