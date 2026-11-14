import { execFileSync, spawnSync } from "node:child_process";
import { existsSync, mkdtempSync, readFileSync, renameSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import Ajv2020 from "ajv/dist/2020.js";

const blocksFile = "ethereumP1data.csv";
const defaultAddress = "0x00000000006c3852cbef3e08e8df289169ede581";
const invalidAddress = `0x${"z".repeat(40)}`;
const reportFile = "ethereum-report.md";

function assertCondition(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function runMake(args) {
  try {
    return execFileSync("make", args, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"]
    });
  } catch (error) {
    const stdout = error.stdout || "";
    const stderr = error.stderr || "";
    throw new Error(
      `make ${args.join(" ")} failed.\n\nstdout:\n${stdout}\n\nstderr:\n${stderr}`
    );
  }
}

function expectMakeFailure(args, expectedText) {
  const expectedTexts = Array.isArray(expectedText) ? expectedText : [expectedText];

  try {
    execFileSync("make", args, {
      encoding: "utf8",
      stdio: ["ignore", "pipe", "pipe"]
    });
  } catch (error) {
    const combinedOutput = `${error.stdout || ""}\n${error.stderr || ""}`;
    expectedTexts.forEach(function (text) {
      assertCondition(
        combinedOutput.includes(text),
        `make ${args.join(" ")} failed, but did not include:\n${text}\n\nOutput was:\n${combinedOutput}`
      );
    });
    return;
  }

  throw new Error(`make ${args.join(" ")} unexpectedly succeeded.`);
}

function withTemporarilyMissingFile(filename, runCheck) {
  const backupFile = `${filename}.cli-smoke-backup`;
  renameSync(filename, backupFile);
  try {
    runCheck();
  } finally {
    renameSync(backupFile, filename);
  }
}

function parseJsonOutput(args) {
  return JSON.parse(runMake(args));
}

const schemaFile = "schemas/cli-output.v1.schema.json";
const schema = JSON.parse(readFileSync(schemaFile, "utf8"));
const ajv = new Ajv2020({ allErrors: true, strict: true });
ajv.addSchema(schema);

// The entry point behind every make target. Called directly because make turns
// any failing recipe into its own exit status 2, which hides the CLI's 1.
function runCli(args) {
  const result = spawnSync("java", ["-cp", "bin", "EthereumBlockExplorer", ...args], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"]
  });
  if (result.error) {
    throw result.error;
  }
  return result;
}

// Runs one --json command, checks its exit code, and checks that stdout is a
// single document matching the named definition in the committed schema.
function expectCli(args, expectedExit, definition) {
  const result = runCli(args);
  const label = `java -cp bin EthereumBlockExplorer ${args.join(" ")}`;
  assertCondition(
    result.status === expectedExit,
    `${label} exited ${result.status}, expected ${expectedExit}.\n\nstdout:\n${result.stdout}\n\nstderr:\n${result.stderr}`
  );
  let document;
  try {
    document = JSON.parse(result.stdout);
  } catch (error) {
    throw new Error(`${label} did not print one JSON document: ${error.message}\n\nstdout:\n${result.stdout}`);
  }
  const validate = ajv.getSchema(`${schema.$id}#/$defs/${definition}`);
  assertCondition(typeof validate === "function", `${schemaFile} has no definition named ${definition}.`);
  assertCondition(
    validate(document),
    `${label} does not match ${schemaFile}#/$defs/${definition}:\n${JSON.stringify(validate.errors, null, 2)}`
  );
  assertCondition(
    ajv.validate(schema, document),
    `${label} does not match exactly one definition in ${schemaFile}:\n${JSON.stringify(ajv.errors, null, 2)}`
  );
  return { document, stdout: result.stdout };
}

const priorReport = existsSync(reportFile) ? readFileSync(reportFile, "utf8") : null;

try {
  const help = runMake(["help"]);
  assertCondition(help.includes("make dashboard"), "Help output is missing make dashboard.");
  assertCondition(help.includes("make verify"), "Help output is missing make verify.");
  assertCondition(help.includes("make cli-smoke"), "Help output is missing make cli-smoke.");
  assertCondition(help.includes("make report"), "Help output is missing make report.");

  const dashboard = runMake(["dashboard"]);
  assertCondition(dashboard.includes("ETHEREUM DASHBOARD"), "Dashboard output did not render.");

  const brief = runMake(["brief"]);
  assertCondition(brief.includes("ETHEREUM ACTION BRIEF"), "Brief output did not render.");
  assertCondition(brief.includes("Strategic signals:"), "Brief output is missing strategic signals.");

  const snapshot = parseJsonOutput(["snapshot"]);
  assertCondition(snapshot.schema === "ethereum-block-explorer.agent-snapshot.v1", "Snapshot schema mismatch.");
  assertCondition(snapshot.status === "ready", "Snapshot is not ready.");
  assertCondition(snapshot.data_contract.blocks_file === "ethereumP1data.csv", "Snapshot data contract missing blocks file.");
  assertCondition(Array.isArray(snapshot.recommended_next_actions), "Snapshot is missing recommended next actions.");

  const overview = parseJsonOutput(["run-json"]);
  assertCondition(overview.blocks_loaded === 100, "Overview blocks_loaded mismatch.");
  assertCondition(Array.isArray(overview.top_miners), "Overview is missing top_miners.");
  withTemporarilyMissingFile(blocksFile, function () {
    expectMakeFailure(
      ["run-json"],
      ['"error": "data_file_not_found"', '"file": "ethereumP1data.csv"']
    );
    const missingData = expectCli(["--json", "snapshot"], 1, "error").document;
    assertCondition(missingData.error === "data_file_not_found", "Missing dataset did not report data_file_not_found.");
    assertCondition(missingData.file === blocksFile, "Missing dataset error did not name the file.");
    // The guide describes the CLI, so it must still answer without the dataset.
    expectCli(["--json", "help"], 0, "help");
  });

  const block = parseJsonOutput(["block", "N=15049311"]);
  assertCondition(block.block_number === 15049311, "Block lookup returned the wrong block.");
  assertCondition(typeof block.miner === "string", "Block lookup is missing the miner.");
  expectMakeFailure(["block"], "Missing block number. Try: make block N=15049311");

  const address = parseJsonOutput(["address", `ADDR=${defaultAddress}`]);
  assertCondition(address.address === defaultAddress, "Address lookup returned the wrong address.");
  assertCondition(typeof address.behavior_class === "string", "Address lookup is missing behavior_class.");
  expectMakeFailure(["address", `ADDR=${invalidAddress}`], '"error": "invalid_address_format"');
  expectMakeFailure(
    ["address"],
    "Missing address. Try: make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f"
  );

  const network = parseJsonOutput(["network"]);
  assertCondition(typeof network.total_addresses === "number", "Network analysis is missing total_addresses.");
  assertCondition(Array.isArray(network.whales), "Network analysis is missing whales.");

  const anomalies = parseJsonOutput(["anomalies"]);
  assertCondition(Array.isArray(anomalies.cost_anomalies), "Anomalies output is missing cost_anomalies.");
  assertCondition(Array.isArray(anomalies.volume_anomalies), "Anomalies output is missing volume_anomalies.");
  expectMakeFailure(
    ["anomalies", "THRESHOLD=not-a-number"],
    '{"error": "Expected: anomalies <zScoreThreshold>. Try: make anomalies THRESHOLD=3.5."}'
  );

  const miners = parseJsonOutput(["miners"]);
  assertCondition(typeof miners.unique_miners === "number", "Miner analysis is missing unique_miners.");
  assertCondition(Array.isArray(miners.miners), "Miner analysis is missing miners.");

  if (existsSync(reportFile)) {
    rmSync(reportFile);
  }
  runMake(["report"]);
  assertCondition(existsSync(reportFile), "Report command did not write ethereum-report.md.");
  const report = readFileSync(reportFile, "utf8");
  assertCondition(report.includes("# Ethereum Blocks: Lean Report"), "Report output is missing the report heading.");
  assertCondition(report.includes("## Action Brief"), "Report output is missing the action brief section.");

  // --json contract: one schema-valid document per command, exit 0.
  const guide = expectCli(["--json", "help"], 0, "help").document;
  const sampleArgument = { block: "15049311", address: defaultAddress, anomalies: "3.5" };
  const reportDir = mkdtempSync(join(tmpdir(), "cli-smoke-"));
  try {
    sampleArgument.report = join(reportDir, "report.md");
    guide.commands.forEach(function (command) {
      const args = ["--json", command.name];
      if (sampleArgument[command.name]) {
        args.push(sampleArgument[command.name]);
      }
      const first = expectCli(args, 0, command.output);
      // Nothing in the output depends on the clock or on chance.
      const second = expectCli(args, 0, command.output);
      assertCondition(first.stdout === second.stdout, `--json ${command.name} printed different bytes on a second run.`);
    });
    assertCondition(existsSync(sampleArgument.report), "--json report did not write the requested file.");
  } finally {
    rmSync(reportDir, { recursive: true, force: true });
  }
  expectCli(["--json"], 0, "overview");
  expectCli(["--json", "block", "15049400"], 0, "block");
  const quietAddress = expectCli(["--json", "address", `0x${"0".repeat(39)}1`], 0, "address").document;
  assertCondition(quietAddress.status === "no_activity", "Address with no transactions did not report no_activity.");

  // The anomaly document is the one the browser is held to.
  const parity = readFileSync("scripts/anomaly_parity.json", "utf8");
  assertCondition(expectCli(["--json", "anomalies"], 0, "anomalies").stdout === parity, "--json anomalies no longer matches scripts/anomaly_parity.json.");

  // --json is honored wherever it is written; it used to work only before the command.
  const trailingFlag = expectCli(["anomalies", "5", "--json"], 0, "anomalies").document;
  assertCondition(trailingFlag.z_threshold === 5, "Trailing --json dropped the command's argument.");
  assertCondition(
    expectCli(["anomalies", "--json", "5"], 0, "anomalies").stdout === expectCli(["--json", "anomalies", "5"], 0, "anomalies").stdout,
    "--json placement changed the anomalies output."
  );

  // Exit 1: the command ran and has no answer.
  const missingBlock = expectCli(["--json", "block", "1"], 1, "error").document;
  assertCondition(missingBlock.error === "block_not_found" && missingBlock.block_number === 1, "Unknown block did not report block_not_found.");

  // Exit 2: the command line was rejected.
  [
    ["no-such-command"],
    ["block"],
    ["block", "not-a-number"],
    ["address"],
    ["address", invalidAddress],
    ["anomalies", "not-a-number"],
    ["anomalies", "NaN"],
    ["anomalies", "0"],
    ["anomalies", "-1"]
  ].forEach(function (args) {
    expectCli(["--json", ...args], 2, "error");
  });
  const usageText = runCli(["no-such-command"]);
  assertCondition(usageText.status === 2, `Unknown command without --json exited ${usageText.status}, expected 2.`);
  assertCondition(usageText.stdout === "", "Unknown command without --json wrote to stdout.");
  assertCondition(usageText.stderr.includes("Unknown command 'no-such-command'"), "Unknown command without --json did not explain itself on stderr.");

  // The schema has teeth: a document with a wrong type or a missing field is refused.
  const validateAnomalies = ajv.getSchema(`${schema.$id}#/$defs/anomalies`);
  const goodAnomalies = JSON.parse(parity);
  assertCondition(validateAnomalies(goodAnomalies), "Parity document does not match the anomalies definition.");
  assertCondition(!validateAnomalies({ ...goodAnomalies, z_threshold: "3.5" }), "Schema accepted a string z_threshold.");
  const { method, ...withoutMethod } = goodAnomalies;
  assertCondition(typeof method === "string" && !validateAnomalies(withoutMethod), "Schema accepted anomalies without method.");
  assertCondition(!ajv.validate(schema, { unexpected: true }), "Schema accepted an unknown document.");

  // The commands AGENTS.md hands to agents must run exactly as written.
  const agentsGuide = readFileSync("AGENTS.md", "utf8");
  const agentsBlock = agentsGuide.split("## For agents")[1].split("```bash")[1].split("```")[0];
  const documentedCalls = agentsBlock
    .split("\n")
    .filter(function (line) {
      return line.startsWith("java -cp bin EthereumBlockExplorer ");
    });
  assertCondition(documentedCalls.length >= 3, "AGENTS.md 'For agents' block lost its java commands.");
  documentedCalls.forEach(function (line) {
    const args = line.split(/\s+/).slice(4);
    const result = runCli(args);
    assertCondition(result.status === 0, `AGENTS.md command failed with exit ${result.status}: ${line}`);
    assertCondition(
      ajv.validate(schema, JSON.parse(result.stdout)),
      `AGENTS.md command output does not match ${schemaFile}: ${line}`
    );
  });

  console.log("CLI smoke passed.");
} finally {
  if (priorReport === null) {
    if (existsSync(reportFile)) {
      rmSync(reportFile);
    }
  } else {
    writeFileSync(reportFile, priorReport, "utf8");
  }
}
