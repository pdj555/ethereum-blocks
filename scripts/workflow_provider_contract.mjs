import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { resolve } from "node:path";
import { runInNewContext } from "node:vm";

const root = resolve(import.meta.dirname, "..");

// Read the small block/scalar subset used by these workflows. actionlint checks
// full YAML and GitHub expression syntax; this harness executes provider routing.
function steps(source, indent) {
  const pattern = new RegExp(`^ {${indent}}- (?:name|id):`, "gm");
  const starts = [...source.matchAll(pattern)].map((match) => match.index);
  return starts.map((start, index) => ({
    source: source.slice(start, starts[index + 1] ?? source.length)
      .replace(new RegExp(`^ {${indent}}- `), " ".repeat(indent + 2)),
    indent: indent + 2,
  }));
}

function scalar(step, key) {
  const match = step.source.match(new RegExp(`^ {${step.indent}}${key}: (.+)$`, "m"));
  assert.ok(match, `Missing ${key} in workflow step`);
  return match[1];
}

function section(step, key) {
  const lines = step.source.split("\n");
  const start = lines.findIndex((line) => line.startsWith(" ".repeat(step.indent) + key + ":"));
  assert.ok(start >= 0, `Missing ${key} section`);
  const end = lines.findIndex((line, index) => index > start &&
    line.trim() !== "" && line.match(/^ */)[0].length <= step.indent);
  return lines.slice(start + 1, end < 0 ? lines.length : end)
    .map((line) => line.slice(step.indent + 2)).join("\n").trimEnd();
}

function mapping(step, key) {
  return Object.fromEntries(section(step, key).split("\n")
    .filter((line) => line && !line.startsWith("#") && !/^\s/.test(line))
    .map((line) => {
      const match = line.match(/^([A-Za-z_][A-Za-z_0-9]*): (.*)$/);
      assert.ok(match, `Unsupported mapping in ${key}: ${line}`);
      return [match[1], match[2]];
    }));
}

function evaluate(value, context) {
  if (value.startsWith('"') && value.endsWith('"')) value = JSON.parse(value);
  const expression = value.match(/^\$\{\{ (.*) \}\}$/);
  return expression ? runInNewContext(expression[1], {
    ...context,
    format: (template, ...values) => template.replace(/\{(\d+)\}/g, (_, index) => values[index]),
  }, { timeout: 100 }) : value;
}

function evaluateMapping(values, context) {
  return Object.fromEntries(Object.entries(values).map(([key, value]) => [key, evaluate(value, context)]));
}

function outputValues(source) {
  return Object.fromEntries(source.trim().split("\n").filter(Boolean).map((line) => {
    const index = line.indexOf("=");
    assert.ok(index > 0, "Malformed action output");
    return [line.slice(0, index), line.slice(index + 1)];
  }));
}

const action = readFileSync(resolve(root, ".github/actions/configure-ai/action.yml"), "utf8");
const select = steps(action, 4).find((step) => scalar(step, "id") === "select");
assert.ok(select, "Missing credential selector");
const shell = section(select, "run");
const selectorEnv = mapping(select, "env");
const declaredOutputs = Object.fromEntries([...section({ source: action, indent: 0 }, "outputs")
  .matchAll(/^([a-z_]+):\n  description: [^\n]+\n  value: (.+)$/gm)]
  .map((match) => [match[1], match[2]]));
const workflowDirectory = resolve(root, ".github/workflows");
const workflows = readdirSync(workflowDirectory).filter((name) => /^claude.*\.yml$/.test(name))
  .map((name) => ({ name, steps: steps(readFileSync(resolve(workflowDirectory, name), "utf8"), 6) }));
assert.equal(workflows.length, 4, "Check all four optional Claude workflows");

const directory = mkdtempSync(resolve(tmpdir(), "ethereum-blocks-provider-"));
try {
  for (let mask = 0; mask < 8; mask++) {
    const secrets = {
      CLAUDE_CODE_OAUTH_TOKEN: mask & 4 ? "fixture-oauth-token" : "",
      OLLAMA_API_KEY: mask & 2 ? "fixture-ollama-key" : "",
      ANTHROPIC_API_KEY: mask & 1 ? "fixture-anthropic-key" : "",
    };
    const inputs = {
      claude_code_oauth_token: secrets.CLAUDE_CODE_OAUTH_TOKEN,
      ollama_api_key: secrets.OLLAMA_API_KEY,
      anthropic_api_key: secrets.ANTHROPIC_API_KEY,
    };
    const output = resolve(directory, "output");
    const persistedEnv = resolve(directory, "env");
    const summary = resolve(directory, "summary");
    for (const path of [output, persistedEnv, summary]) writeFileSync(path, "");
    const result = spawnSync("bash", ["--noprofile", "--norc", "-c", shell], {
      encoding: "utf8",
      env: {
        PATH: process.env.PATH,
        ...Object.fromEntries(Object.entries(evaluateMapping(selectorEnv, { inputs }))
          .map(([key, value]) => [key, String(value)])),
        GITHUB_OUTPUT: output,
        GITHUB_ENV: persistedEnv,
        GITHUB_STEP_SUMMARY: summary,
      },
    });
    assert.equal(result.status, 0, `Credential selector failed: ${result.stderr}`);
    const selected = evaluateMapping(declaredOutputs, {
      steps: { select: { outputs: outputValues(readFileSync(output, "utf8")) } },
    });
    const provider = mask & 4 ? "claude-oauth" : mask & 2 ? "ollama-cloud" : mask & 1 ? "anthropic-api" : "none";
    const ollama = provider === "ollama-cloud";
    const model = provider === "none" ? "" : ollama ? "glm-5.2:cloud" : "opus";
    const baseUrl = provider === "none" ? "" : ollama ? "https://ollama.com" : "https://api.anthropic.com";
    assert.deepEqual(selected, { provider, model, base_url: baseUrl });
    assert.equal(readFileSync(persistedEnv, "utf8"), "", "Do not persist any credential or endpoint to GITHUB_ENV");
    if (provider === "none") {
      assert.match(result.stdout, /::notice::AI automation skipped/);
      assert.match(readFileSync(summary, "utf8"), /No AI assistant, review, triage, or audit was performed/);
      for (const name of Object.keys(secrets)) assert.ok(readFileSync(summary, "utf8").includes(name));
    } else {
      assert.equal(readFileSync(summary, "utf8"), "", "Configured provider must not claim a skip");
    }

    for (const workflow of workflows) {
      const configure = workflow.steps.find((step) => step.source.includes("uses: ./.github/actions/configure-ai"));
      const claude = workflow.steps.find((step) => step.source.includes("uses: anthropics/claude-code-action@v1"));
      assert.ok(configure && claude, `${workflow.name}: missing provider/action step`);
      assert.equal(scalar(configure, "id"), "ai");
      assert.deepEqual(evaluateMapping(mapping(configure, "with"), { secrets }), inputs);
      const context = { secrets, steps: { ai: { outputs: selected } } };
      assert.equal(Boolean(evaluate("${{ " + scalar(claude, "if") + " }}", context)), provider !== "none",
        `${workflow.name}: run only when a provider is selected`);
      assert.doesNotMatch(claude.source, /continue-on-error:/, "Configured provider failures must remain visible");
      const credentials = mapping(claude, "with");
      assert.equal(evaluate(credentials.claude_code_oauth_token, context), provider === "claude-oauth" ? secrets.CLAUDE_CODE_OAUTH_TOKEN : "");
      assert.equal(evaluate(credentials.anthropic_api_key, context), ollama ? secrets.OLLAMA_API_KEY : provider === "anthropic-api" ? secrets.ANTHROPIC_API_KEY : "");
      const environment = evaluateMapping(mapping(claude, "env"), context);
      assert.equal(environment.ANTHROPIC_BASE_URL, baseUrl);
      assert.equal(environment.ANTHROPIC_CUSTOM_HEADERS, ollama ? "Authorization: Bearer " + secrets.OLLAMA_API_KEY : "");
      for (const family of ["SONNET", "OPUS", "HAIKU"]) {
        assert.equal(environment[`ANTHROPIC_DEFAULT_${family}_MODEL`], ollama ? model : "");
      }
      // Native credentials must never reach the Ollama endpoint, and fallback
      // keys must never override a selected OAuth credential.
      for (const value of Object.values(environment)) {
        for (const [name, secret] of Object.entries(secrets)) {
          if (secret && (!ollama || name !== "OLLAMA_API_KEY")) assert.ok(!value.includes(secret), `${workflow.name}: unselected credential in environment`);
        }
      }
      // claude_args is nested in with; extract it at the correct indentation.
      const args = section({ source: claude.source, indent: 10 }, "claude_args");
      assert.equal(evaluate(args.match(/^--model (.+)$/m)[1], context), model);
      if (workflow.name === "claude-code-review.yml") {
        const fetch = workflow.steps.find((step) => step.source.includes("name: Fetch pull request"));
        assert.ok(workflow.steps.indexOf(configure) < workflow.steps.indexOf(fetch), "Select credentials from trusted checkout before fetching PR code");
        assert.equal(Boolean(evaluate("${{ " + scalar(fetch, "if") + " }}", context)), provider !== "none");
      }
    }
  }
  console.log("Workflow provider contract passed: 8 credential combinations across 4 optional AI workflows; missing-credential summaries and credential isolation checked.");
} finally {
  rmSync(directory, { recursive: true, force: true });
}
