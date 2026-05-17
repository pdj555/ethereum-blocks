import { parseBlocks, parseTransactions } from "./parser.js";
import { buildDataset } from "./dataset.js";
import {
  renderAddress,
  renderBlock,
  renderFatal,
  renderLoading,
  renderMessage,
  renderRail,
  renderSummary
} from "./renderer.js";
import { looksLikeAddress } from "./utils.js";

const DEFAULT_BLOCK = "15049311";
const ADDRESS_LENGTH = 42;

const elements = {
  form: document.getElementById("query-form"),
  modeButtons: Array.from(document.querySelectorAll(".mode-switch__item")),
  input: document.getElementById("query-input"),
  hint: document.getElementById("query-hint"),
  summaryStrip: document.getElementById("summary-strip"),
  resultKicker: document.getElementById("result-kicker"),
  resultTitle: document.getElementById("result-title"),
  resultMeta: document.getElementById("result-meta"),
  resultBody: document.getElementById("result-body"),
  snapshotMeta: document.getElementById("snapshot-meta"),
  topMiners: document.getElementById("top-miners"),
  hotBlocks: document.getElementById("hot-blocks"),
  activeAddresses: document.getElementById("active-addresses"),
  networkGlance: document.getElementById("network-glance")
};

const state = {
  loading: true,
  data: null,
  mode: "block",
  query: DEFAULT_BLOCK
};

init();

async function init() {
  bindEvents();
  render();

  try {
    const [blocksCsv, transactionsCsv] = await Promise.all([
      fetchText("./ethereumP1data.csv"),
      fetchText("./ethereumtransactions1.csv")
    ]);

    const blocks = parseBlocks(blocksCsv);
    const transactions = parseTransactions(transactionsCsv);
    state.data = buildDataset(blocks, transactions);
    state.loading = false;
    hydrateFromHash();
    render();
  } catch (error) {
    state.loading = false;
    state.data = null;
    renderFatal(
      "UI data is missing.",
      "Run 'make ui-build', then reopen 'make ui' and refresh the page. The browser surface expects ethereumP1data.csv and ethereumtransactions1.csv beside index.html.",
      elements
    );
    console.error(error);
  }
}

function bindEvents() {
  elements.form.addEventListener("submit", function (event) {
    event.preventDefault();
    applyQuery(elements.input.value);
  });

  elements.modeButtons.forEach(function (button) {
    button.addEventListener("click", function () {
      setMode(button.dataset.mode, true);
    });
  });

  document.body.addEventListener("click", function (event) {
    const jump = event.target.closest("[data-jump-mode][data-jump-value]");
    if (!jump) {
      return;
    }

    setMode(jump.dataset.jumpMode, true);
    elements.input.value = jump.dataset.jumpValue;
    applyQuery(jump.dataset.jumpValue);
  });

  window.addEventListener("hashchange", function () {
    hydrateFromHash();
    render();
  });
}

function setMode(mode, syncInput) {
  state.mode = mode === "address" ? "address" : "block";
  elements.modeButtons.forEach(function (button) {
    const active = button.dataset.mode === state.mode;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-pressed", active ? "true" : "false");
  });

  if (state.mode === "block") {
    elements.input.placeholder = DEFAULT_BLOCK;
    elements.hint.textContent = "Start with block 15049311 or choose one from hot blocks.";
    if (syncInput && looksLikeAddress(elements.input.value)) {
      elements.input.value = DEFAULT_BLOCK;
    }
  } else {
    elements.input.placeholder = "0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f";
    elements.hint.textContent = "Paste an Ethereum address or jump from a block transaction.";
    if (syncInput && !looksLikeAddress(elements.input.value)) {
      elements.input.value = "";
    }
  }
}

function applyQuery(rawValue) {
  const cleaned = (rawValue || "").trim();

  if (state.mode === "block") {
    if (!/^\d+$/.test(cleaned)) {
      renderMessage(
        "Block lookup",
        "Enter a block number like 15049311.",
        "The block search expects digits only.",
        elements
      );
      return;
    }
    state.query = cleaned;
  } else {
    const normalized = cleaned.toLowerCase();
    if (!looksLikeAddress(normalized)) {
      renderMessage(
        "Address profile",
        "Enter a full Ethereum address.",
        "Use 0x followed by 40 hex characters, for example 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f.",
        elements
      );
      return;
    }
    state.query = normalized;
  }

  const nextHash = state.mode + "/" + state.query;
  if (window.location.hash.slice(1) !== nextHash) {
    window.location.hash = nextHash;
    return;
  }
  render();
}

function hydrateFromHash() {
  const rawHash = window.location.hash.replace(/^#/, "");
  if (!rawHash) {
    setMode("block", true);
    elements.input.value = DEFAULT_BLOCK;
    state.query = DEFAULT_BLOCK;
    return;
  }

  const parts = rawHash.split("/");
  const mode = parts[0] === "address" ? "address" : "block";
  const value = decodeURIComponent(parts.slice(1).join("/")) || DEFAULT_BLOCK;
  setMode(mode, true);
  state.query = mode === "address" ? value.toLowerCase() : value;
  elements.input.value = state.query;
}

function render() {
  if (state.loading) {
    renderLoading(elements);
    return;
  }

  if (!state.data) {
    return;
  }

  renderSummary(state, elements);
  renderRail(state, elements);

  if (state.mode === "address") {
    renderAddress(state.query, state, elements);
  } else {
    renderBlock(state.query, state, elements);
  }
}

function fetchText(url) {
  return fetch(url).then(function (response) {
    if (!response.ok) {
      throw new Error("Failed to load " + url);
    }
    return response.text();
  });
}
