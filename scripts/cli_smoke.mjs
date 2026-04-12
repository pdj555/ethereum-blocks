import { execFileSync } from "node:child_process";
import { existsSync, readFileSync, rmSync, writeFileSync } from "node:fs";

const defaultAddress = "0x00000000006c3852cbef3e08e8df289169ede581";
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

function parseJsonOutput(args) {
  return JSON.parse(runMake(args));
}

const priorReport = existsSync(reportFile) ? readFileSync(reportFile, "utf8") : null;

try {
  const help = runMake(["help"]);
  assertCondition(help.includes("make dashboard"), "Help output is missing make dashboard.");
  assertCondition(help.includes("make cli-smoke"), "Help output is missing make cli-smoke.");
  assertCondition(help.includes("make report"), "Help output is missing make report.");

  const dashboard = runMake(["dashboard"]);
  assertCondition(dashboard.includes("ETHEREUM DASHBOARD"), "Dashboard output did not render.");

  const brief = runMake(["brief"]);
  assertCondition(brief.includes("ETHEREUM ACTION BRIEF"), "Brief output did not render.");
  assertCondition(brief.includes("Strategic signals:"), "Brief output is missing strategic signals.");

  const overview = parseJsonOutput(["run-json"]);
  assertCondition(overview.blocks_loaded === 100, "Overview blocks_loaded mismatch.");
  assertCondition(Array.isArray(overview.top_miners), "Overview is missing top_miners.");

  const block = parseJsonOutput(["block", "N=15049311"]);
  assertCondition(block.block_number === 15049311, "Block lookup returned the wrong block.");
  assertCondition(typeof block.miner === "string", "Block lookup is missing the miner.");

  const address = parseJsonOutput(["address", `ADDR=${defaultAddress}`]);
  assertCondition(address.address === defaultAddress, "Address lookup returned the wrong address.");
  assertCondition(typeof address.behavior_class === "string", "Address lookup is missing behavior_class.");

  const network = parseJsonOutput(["network"]);
  assertCondition(typeof network.total_addresses === "number", "Network analysis is missing total_addresses.");
  assertCondition(Array.isArray(network.whales), "Network analysis is missing whales.");

  const anomalies = parseJsonOutput(["anomalies"]);
  assertCondition(Array.isArray(anomalies.cost_anomalies), "Anomalies output is missing cost_anomalies.");
  assertCondition(Array.isArray(anomalies.volume_anomalies), "Anomalies output is missing volume_anomalies.");

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
