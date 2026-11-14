// Runs the commands in the AGENTS.md "For agents" block exactly as written and
// checks each printed document against the committed schema. Needs only a
// built checkout (make build) and `npm ci` for the validator.
import { spawnSync } from "node:child_process";
import { existsSync, openSync, readFileSync } from "node:fs";

import Ajv2020 from "ajv/dist/2020.js";

const schemaFile = "schemas/cli-output.v1.schema.json";
const schema = JSON.parse(readFileSync(schemaFile, "utf8"));
const ajv = new Ajv2020({ allErrors: true, strict: true });
const validate = ajv.compile(schema);

function fail(message) {
  console.error(message);
  process.exit(1);
}

function run(words, stdout = "pipe") {
  const result = spawnSync(words[0], words.slice(1), { encoding: "utf8", stdio: ["ignore", stdout, "pipe"] });
  if (result.error) {
    fail(`${words.join(" ")} could not start: ${result.error.message}`);
  }
  return result;
}

const block = readFileSync("AGENTS.md", "utf8").split("## For agents")[1].split("```bash")[1].split("```")[0];
const commands = block
  .split("\n")
  .filter(function (line) {
    return line.startsWith("java ");
  })
  .map(function (line) {
    return line.split(/\s+/);
  });
if (commands.length < 3) {
  fail("AGENTS.md 'For agents' block lost its java commands.");
}

commands.forEach(function (words) {
  const result = run(words);
  if (result.status !== 0) {
    fail(`${words.join(" ")} exited ${result.status}.\n${result.stderr}`);
  }
  if (!validate(JSON.parse(result.stdout))) {
    fail(`${words.join(" ")} does not match ${schemaFile}:\n${JSON.stringify(validate.errors, null, 2)}`);
  }
});

// The failure half of the contract: a rejected command line and an unwritable stdout.
const entry = commands[0].slice(0, 4);
const rejected = run([...entry, "--json", "no-such-command"]);
const rejectedDocument = JSON.parse(rejected.stdout);
if (rejected.status !== 2 || !validate(rejectedDocument) || rejectedDocument.error.exit_code !== 2) {
  fail(`Unknown command: expected exit 2 and a matching error document, got exit ${rejected.status}.`);
}
if (existsSync("/dev/full")) {
  const full = run([...entry, "--json", "snapshot"], openSync("/dev/full", "w"));
  if (full.status !== 4) {
    fail(`Writing to a full device exited ${full.status}, expected 4.`);
  }
}

console.log(`Agent smoke passed: ${commands.length} documented commands match ${schemaFile}.`);
