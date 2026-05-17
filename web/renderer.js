import {
  escapeHtml,
  formatDecimal,
  formatEth,
  formatInteger,
  formatPercent,
  formatRange,
  formatSignedEth,
  formatTimestamp,
  looksLikeAddress,
  shorten
} from "./utils.js";

export function renderLoading(elements) {
  elements.summaryStrip.innerHTML = [
    renderSummaryItem("Blocks", "Loading", "Reading CSV data"),
    renderSummaryItem("Miners", "-", "Preparing counts"),
    renderSummaryItem("Transactions", "-", "Preparing activity"),
    renderSummaryItem("Addresses", "-", "Preparing search")
  ].join("");
  elements.snapshotMeta.innerHTML = "";
  elements.topMiners.innerHTML = "";
  elements.hotBlocks.innerHTML = "";
  elements.activeAddresses.innerHTML = "";
  elements.networkGlance.innerHTML = "";
  elements.resultKicker.textContent = "Loading";
  elements.resultTitle.textContent = "Preparing the explorer";
  elements.resultMeta.textContent = "Reading block and transaction CSV data.";
  elements.resultBody.innerHTML =
    "<div class='empty-state'><p>The browser UI parses the dataset locally so you can search without a backend.</p></div>";
}

export function renderFatal(title, body, elements) {
  elements.resultKicker.textContent = "Setup";
  elements.resultTitle.textContent = title;
  elements.resultMeta.textContent = "";
  elements.resultBody.innerHTML = "<div class='empty-state'><p>" + escapeHtml(body) + "</p></div>";
}

export function renderSummary(state, elements) {
  const overview = state.data.overview;
  elements.summaryStrip.innerHTML = [
    renderSummaryItem(
      "Loaded",
      formatInteger(overview.blocksLoaded) + " blocks",
      formatRange(overview.blockRangeStart, overview.blockRangeEnd)
    ),
    renderSummaryItem(
      "Miners",
      formatInteger(overview.uniqueMiners),
      formatPercent(overview.topMinerShare) + " top share"
    ),
    renderSummaryItem(
      "Transactions",
      formatInteger(overview.parsedTransactions),
      formatInteger(overview.totalTransactionsMetadata) + " in metadata"
    ),
    renderSummaryItem(
      "Addresses",
      formatInteger(overview.activeAddresses),
      formatInteger(overview.contractCreations) + " contract creations"
    )
  ].join("");
}

export function renderRail(state, elements) {
  const overview = state.data.overview;

  elements.snapshotMeta.innerHTML = [
    renderSnapshotRow("Blocks", formatRange(overview.blockRangeStart, overview.blockRangeEnd)),
    renderSnapshotRow("Avg tx / block", formatDecimal(overview.avgTransactionsPerBlock, 2)),
    renderSnapshotRow("Avg cost / parsed tx", formatEth(overview.avgCostPerTxEth)),
    renderSnapshotRow("Parsed blocks", formatInteger(overview.blocksWithParsedTransactions))
  ].join("");

  elements.topMiners.innerHTML = state.data.topMiners
    .map(function (item) {
      return renderActionRow(
        shorten(item.address),
        item.count + " blocks  |  latest " + item.featuredBlock,
        "block",
        String(item.featuredBlock)
      );
    })
    .join("");

  elements.hotBlocks.innerHTML = state.data.hotBlocks
    .map(function (item) {
      const meta = item.transactionCountMetadata + " tx metadata";
      return renderActionRow(
        "Block " + item.number,
        meta,
        "block",
        String(item.number)
      );
    })
    .join("");

  elements.activeAddresses.innerHTML = state.data.activeAddresses
    .map(function (item) {
      const meta = item.totalInteractions + " touches";
      return renderActionRow(shorten(item.address), meta, "address", item.address);
    })
    .join("");

  elements.networkGlance.innerHTML = [
    renderGlanceRow("Largest tx", "Block " + state.data.largestTransaction.blockNumber),
    renderGlanceRow("Cost", formatEth(state.data.largestTransaction.costEth)),
    renderGlanceRow("Heaviest sender", shorten(state.data.heaviestSender.address)),
    renderGlanceRow("Heaviest receiver", shorten(state.data.heaviestReceiver.address))
  ].join("");
}

export function renderBlock(rawBlock, state, elements) {
  const blockNumber = Number(rawBlock);
  const block = state.data.blockMap.get(blockNumber);

  if (!block) {
    renderMessage(
      "Block lookup",
      "Block " + escapeHtml(rawBlock) + " is not in the loaded dataset.",
      "Choose a block between " + state.data.overview.blockRangeStart + " and " + state.data.overview.blockRangeEnd + ".",
      elements
    );
    return;
  }

  swapResult(function () {
    elements.resultKicker.textContent = "Block";
    elements.resultTitle.textContent = "Block " + block.number;
    elements.resultMeta.textContent = block.displayTimestamp + "  ·  Miner " + shorten(block.miner);

    const statGrid = [
      renderStat(
        "Miner",
        state.data.addressMap.has(block.miner)
          ? renderJump("address", block.miner, shorten(block.miner))
          : escapeHtml(shorten(block.miner))
      ),
      renderStat("Parsed tx", formatInteger(block.parsedTransactionCount)),
      renderStat("Metadata tx", formatInteger(block.transactionCountMetadata)),
      renderStat("Avg cost", formatEth(block.avgCostEth))
    ].join("");

    const detailGrid = [
      renderDetailBlock("Block view", [
        renderDetailRow("Timestamp", escapeHtml(block.displayTimestamp)),
        renderDetailRow("Total cost", formatEth(block.totalCostEth)),
        renderDetailRow("Unique senders", formatInteger(block.uniqueSenders)),
        renderDetailRow("Unique receivers", formatInteger(block.uniqueReceivers))
      ].join("")),
      renderDetailBlock("Top senders", block.topSenders.length ? block.topSenders.map(function (item) {
        return renderDetailRow(
          renderJump("address", item.address, shorten(item.address)),
          formatInteger(item.count) + " tx"
        );
      }).join("") : "<div class='empty-state'><p>No parsed senders for this block.</p></div>")
    ].join("");

    const transactions = block.transactions.length
      ? renderTransactionsTable(block.transactions)
      : "<div class='empty-state'><p>No parsed transactions were loaded for this block. The block metadata is still available above.</p></div>";

    elements.resultBody.innerHTML =
      "<div class='stat-grid'>" + statGrid + "</div>" +
      "<div class='detail-grid'>" + detailGrid + "</div>" +
      "<div class='transactions-wrap'><h3>Transactions</h3>" + transactions + "</div>";
  }, elements);
}

export function renderAddress(rawAddress, state, elements) {
  const address = (rawAddress || "").toLowerCase();

  if (!looksLikeAddress(address)) {
    renderMessage(
      "Address profile",
      "Enter a full Ethereum address.",
      "Use 0x followed by 40 hex characters, for example 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f.",
      elements
    );
    return;
  }

  const profile = state.data.addressMap.get(address);

  if (!profile) {
    renderMessage(
      "Address profile",
      "No parsed activity was loaded for " + escapeHtml(address) + ".",
      "Paste another address or open one from the block and miner lists.",
      elements
    );
    return;
  }

  swapResult(function () {
    elements.resultKicker.textContent = "Address";
    elements.resultTitle.textContent = shorten(profile.address);
    elements.resultMeta.textContent =
      "Active from block " + profile.firstBlock + " to " + profile.lastBlock + "  ·  " + profile.behaviorClass.replace("_", " ");

    const statGrid = [
      renderStat("Touches", formatInteger(profile.totalInteractions)),
      renderStat("Inbound", formatEth(profile.inboundEth)),
      renderStat("Outbound", formatEth(profile.outboundEth)),
      renderStat("Net flow", formatSignedEth(profile.netFlowEth))
    ].join("");

    const detailGrid = [
      renderDetailBlock("Profile", [
        renderDetailRow("Inbound tx", formatInteger(profile.inboundCount)),
        renderDetailRow("Outbound tx", formatInteger(profile.outboundCount)),
        renderDetailRow("Active blocks", formatInteger(profile.activeBlocks)),
        renderDetailRow("Counterparties", formatInteger(profile.uniqueCounterparties))
      ].join("")),
      renderDetailBlock("Top counterparties", profile.topCounterparties.map(function (item) {
        return renderDetailRow(
          looksLikeAddress(item.address)
            ? renderJump("address", item.address, shorten(item.address))
            : escapeHtml(item.address.replace("_", " ")),
          formatInteger(item.count) + " touches"
        );
      }).join(""))
    ].join("");

    const busiestBlocks = profile.busiestBlocks.length
      ? "<div class='detail-block'><h3>Busiest blocks</h3><div class='detail-list'>" + profile.busiestBlocks.map(function (item) {
          return renderDetailRow(
            renderJump("block", String(item.blockNumber), "Block " + item.blockNumber),
            formatInteger(item.count) + " tx"
          );
        }).join("") + "</div></div>"
      : "";

    elements.resultBody.innerHTML =
      "<div class='stat-grid'>" + statGrid + "</div>" +
      "<div class='detail-grid'>" + detailGrid + "</div>" +
      busiestBlocks;
  }, elements);
}

export function renderMessage(kicker, title, body, elements) {
  swapResult(function () {
    elements.resultKicker.textContent = kicker;
    elements.resultTitle.textContent = title;
    elements.resultMeta.textContent = "";
    elements.resultBody.innerHTML = "<div class='empty-state'><p>" + escapeHtml(body) + "</p></div>";
  }, elements);
}

export function swapResult(renderFn, elements) {
  elements.resultBody.classList.add("is-swapping");
  window.setTimeout(function () {
    renderFn();
    elements.resultBody.classList.remove("is-swapping");
  }, 90);
}

export function renderTransactionsTable(transactions) {
  return (
    "<div class='table-scroll'><table><thead><tr>" +
    "<th>Index</th><th>From</th><th>To</th><th>Type</th><th>Cost</th>" +
    "</tr></thead><tbody>" +
    transactions.map(function (tx) {
      return (
        "<tr>" +
        "<td>" + escapeHtml(String(tx.index)) + "</td>" +
        "<td>" + renderJump("address", tx.from, shorten(tx.from), "tx-link") + "</td>" +
        "<td>" + (tx.contractCreation
          ? "<span class='pill alert'>Contract creation</span>"
          : renderJump("address", tx.to, shorten(tx.to), "tx-link")) + "</td>" +
        "<td>" + (tx.contractCreation ? "<span class='pill alert'>Create</span>" : "<span class='pill'>Transfer</span>") + "</td>" +
        "<td>" + formatEth(tx.costEth) + "</td>" +
        "</tr>"
      );
    }).join("") +
    "</tbody></table></div>"
  );
}

function renderSummaryItem(label, value, note) {
  return (
    "<article class='summary-item'>" +
    "<span class='summary-label'>" + escapeHtml(label) + "</span>" +
    "<strong class='summary-value'>" + escapeHtml(value) + "</strong>" +
    "<span class='summary-note'>" + escapeHtml(note) + "</span>" +
    "</article>"
  );
}

function renderStat(label, value) {
  return (
    "<div class='stat'>" +
    "<span class='stat-label'>" + escapeHtml(label) + "</span>" +
    "<span class='stat-value'>" + value + "</span>" +
    "</div>"
  );
}

function renderDetailBlock(title, rowsHtml) {
  return (
    "<div class='detail-block'>" +
    "<h3>" + escapeHtml(title) + "</h3>" +
    "<div class='detail-list'>" + rowsHtml + "</div>" +
    "</div>"
  );
}

function renderDetailRow(label, value) {
  return (
    "<div class='detail-row'>" +
    "<span>" + label + "</span>" +
    "<strong>" + value + "</strong>" +
    "</div>"
  );
}

function renderActionRow(label, meta, mode, value) {
  return (
    "<button class='action-row' type='button' data-jump-mode='" + escapeHtml(mode) + "' data-jump-value='" + escapeHtml(value) + "'>" +
    "<span class='action-row__label'>" + escapeHtml(label) + "</span>" +
    "<span class='action-row__meta'>" + escapeHtml(meta) + "</span>" +
    "</button>"
  );
}

function renderGlanceRow(label, value) {
  return (
    "<div class='glance-row'>" +
    "<span>" + escapeHtml(label) + "</span>" +
    "<strong>" + escapeHtml(value) + "</strong>" +
    "</div>"
  );
}

function renderSnapshotRow(label, value) {
  return (
    "<div class='snapshot-row'>" +
    "<span>" + escapeHtml(label) + "</span>" +
    "<strong>" + escapeHtml(value) + "</strong>" +
    "</div>"
  );
}

function renderJump(mode, value, label, className) {
  return (
    "<button class='" + (className || "address-chip") + "' type='button' data-jump-mode='" + escapeHtml(mode) + "' data-jump-value='" + escapeHtml(value) + "'>" +
    escapeHtml(label) +
    "</button>"
  );
}
