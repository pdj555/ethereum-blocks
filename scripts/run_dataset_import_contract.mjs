import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";

const root = resolve(import.meta.dirname, "..");
const outputDirectory = mkdtempSync(resolve(tmpdir(), "ethereum-blocks-contract-"));
const tsc = resolve(root, "web/node_modules/.bin/tsc");

try {
  const compile = spawnSync(
    tsc,
    [
      "--module",
      "commonjs",
      "--moduleResolution",
      "node",
      "--target",
      "ES2020",
      "--lib",
      "ES2020,DOM",
      "--types",
      "node",
      "--typeRoots",
      resolve(root, "web/node_modules/@types"),
      "--esModuleInterop",
      "--skipLibCheck",
      "--strict",
      "--rootDir",
      root,
      "--outDir",
      outputDirectory,
      resolve(root, "scripts/dataset_import_contract.test.ts"),
      resolve(root, "web/lib/dataset-import-core.ts"),
      resolve(root, "web/lib/dataset.ts"),
      resolve(root, "web/lib/parser.ts"),
      resolve(root, "web/lib/series.ts"),
      resolve(root, "web/lib/types.ts"),
      resolve(root, "web/lib/utils.ts")
    ],
    { cwd: root, encoding: "utf8" }
  );

  if (compile.status !== 0) {
    process.stdout.write(compile.stdout);
    process.stderr.write(compile.stderr);
    process.exitCode = compile.status ?? 1;
  } else {
    const run = spawnSync(
      process.execPath,
      [resolve(outputDirectory, "scripts/dataset_import_contract.test.js")],
      { cwd: root, encoding: "utf8" }
    );
    process.stdout.write(run.stdout);
    process.stderr.write(run.stderr);
    process.exitCode = run.status ?? 1;
  }
} finally {
  rmSync(outputDirectory, { recursive: true, force: true });
}
