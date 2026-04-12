(function () {
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
        "Run 'make ui-build', then reopen 'make ui' and refresh the page. The browser surface expects ethereumP1data.csv and ethereumtransactions1.csv beside index.html."
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
          "The block search expects digits only."
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
          "Use 0x followed by 40 hex characters, for example 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f."
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
      renderLoading();
      return;
    }

    if (!state.data) {
      return;
    }

    renderSummary();
    renderRail();

    if (state.mode === "address") {
      renderAddress(state.query);
    } else {
      renderBlock(state.query);
    }
  }

  function renderLoading() {
    elements.summaryStrip.innerHTML = [
      renderSummaryItem("Sample", "Loading", "Reading CSV data"),
      renderSummaryItem("Miners", "-", "Preparing counts"),
      renderSummaryItem("Transactions", "-", "Preparing activity"),
      renderSummaryItem("Addresses", "-", "Preparing search")
    ].join("");
    elements.snapshotMeta.innerHTML = "";
    elements.topMiners.innerHTML = "";
    elements.hotBlocks.innerHTML = "";
    elements.activeAddresses.innerHTML = "";
    elements.networkGlance.innerHTML = "";
    elements.resultKicker.textContent = "Loading the sample...";
    elements.resultTitle.textContent = "Preparing the explorer";
    elements.resultMeta.textContent = "Reading block and transaction CSV data.";
    elements.resultBody.innerHTML =
      "<div class='empty-state'><p>The browser UI parses the dataset locally so you can search without a backend.</p></div>";
  }

  function renderFatal(title, body) {
    elements.resultKicker.textContent = "Setup";
    elements.resultTitle.textContent = title;
    elements.resultMeta.textContent = "";
    elements.resultBody.innerHTML = "<div class='empty-state'><p>" + escapeHtml(body) + "</p></div>";
  }

  function renderSummary() {
    const overview = state.data.overview;
    elements.summaryStrip.innerHTML = [
      renderSummaryItem(
        "Sample",
        formatRange(overview.blockRangeStart, overview.blockRangeEnd),
        overview.blocksLoaded + " loaded blocks"
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

  function renderRail() {
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

  function renderBlock(rawBlock) {
    const blockNumber = Number(rawBlock);
    const block = state.data.blockMap.get(blockNumber);

    if (!block) {
      renderMessage(
        "Block lookup",
        "Block " + escapeHtml(rawBlock) + " is outside the loaded sample.",
        "Choose a block between " + state.data.overview.blockRangeStart + " and " + state.data.overview.blockRangeEnd + "."
      );
      return;
    }

    swapResult(function () {
      elements.resultKicker.textContent = "Block";
      elements.resultTitle.textContent = "Block " + block.number;
      elements.resultMeta.textContent = block.displayTimestamp + "  |  Miner " + shorten(block.miner);

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
        }).join("") : "<div class='empty-state'><p>No parsed senders for this block in the loaded sample.</p></div>")
      ].join("");

      const transactions = block.transactions.length
        ? renderTransactionsTable(block.transactions)
        : "<div class='empty-state'><p>No parsed transactions were loaded for this block. The block metadata is still available above.</p></div>";

      elements.resultBody.innerHTML =
        "<div class='stat-grid'>" + statGrid + "</div>" +
        "<div class='detail-grid'>" + detailGrid + "</div>" +
        "<div class='transactions-wrap'><h3>Transactions</h3>" + transactions + "</div>";
    });
  }

  function renderAddress(rawAddress) {
    const address = (rawAddress || "").toLowerCase();
    const profile = state.data.addressMap.get(address);

    if (!profile) {
      renderMessage(
        "Address profile",
        "No parsed activity was loaded for " + escapeHtml(address) + ".",
        "Paste another address or open one from the block and miner lists."
      );
      return;
    }

    swapResult(function () {
      elements.resultKicker.textContent = "Address";
      elements.resultTitle.textContent = shorten(profile.address);
      elements.resultMeta.textContent =
        "Active from block " + profile.firstBlock + " to " + profile.lastBlock + "  |  " + profile.behaviorClass.replace("_", " ");

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
    });
  }

  function renderMessage(kicker, title, body) {
    swapResult(function () {
      elements.resultKicker.textContent = kicker;
      elements.resultTitle.textContent = title;
      elements.resultMeta.textContent = "";
      elements.resultBody.innerHTML = "<div class='empty-state'><p>" + escapeHtml(body) + "</p></div>";
    });
  }

  function swapResult(renderFn) {
    elements.resultBody.classList.add("is-swapping");
    window.setTimeout(function () {
      renderFn();
      elements.resultBody.classList.remove("is-swapping");
    }, 90);
  }

  function fetchText(url) {
    return fetch(url).then(function (response) {
      if (!response.ok) {
        throw new Error("Failed to load " + url);
      }
      return response.text();
    });
  }

  function parseBlocks(csvText) {
    return csvText.split(/\r?\n/).filter(Boolean).map(function (line) {
      const parts = line.split(",");
      return {
        number: Number(parts[0]),
        miner: parts[9].toLowerCase(),
        timestamp: Number(parts[16]),
        transactionCountMetadata: Number(parts[17])
      };
    });
  }

  function parseTransactions(csvText) {
    const transactions = [];

    csvText.split(/\r?\n/).forEach(function (line) {
      if (!line) {
        return;
      }

      const parts = line.split(",");
      if (parts.length < 10) {
        return;
      }

      const from = (parts[5] || "").toLowerCase();
      const rawTo = (parts[6] || "").toLowerCase();
      const to = rawTo || "contract_creation";
      const gasLimit = Number(parts[8]);
      const gasPrice = Number(parts[9]);
      const blockNumber = Number(parts[3]);
      const index = Number(parts[4]);

      if (!looksLikeAddress(from)) {
        return;
      }
      if (rawTo && !looksLikeAddress(rawTo)) {
        return;
      }

      transactions.push({
        blockNumber: blockNumber,
        index: index,
        from: from,
        to: to,
        contractCreation: !rawTo,
        gasLimit: gasLimit,
        gasPrice: gasPrice,
        costEth: gasLimit * gasPrice / 1e18
      });
    });

    return transactions;
  }

  function buildDataset(blocks, transactions) {
    const blockMap = new Map();
    const minerCounts = new Map();
    const addressMap = new Map();
    let totalTransactionsMetadata = 0;
    let contractCreations = 0;
    let largestTransaction = { blockNumber: 0, costEth: 0 };

    blocks.forEach(function (block) {
      totalTransactionsMetadata += block.transactionCountMetadata;
      minerCounts.set(block.miner, (minerCounts.get(block.miner) || 0) + 1);
      blockMap.set(block.number, {
        number: block.number,
        miner: block.miner,
        timestamp: block.timestamp,
        displayTimestamp: formatTimestamp(block.timestamp),
        transactionCountMetadata: block.transactionCountMetadata,
        parsedTransactionCount: 0,
        totalCostEth: 0,
        avgCostEth: 0,
        uniqueSenders: 0,
        uniqueReceivers: 0,
        senderCounts: new Map(),
        receiverCounts: new Map(),
        transactions: []
      });
    });

    transactions.forEach(function (tx) {
      const block = blockMap.get(tx.blockNumber);
      if (!block) {
        return;
      }

      block.transactions.push(tx);
      block.parsedTransactionCount += 1;
      block.totalCostEth += tx.costEth;
      block.senderCounts.set(tx.from, (block.senderCounts.get(tx.from) || 0) + 1);
      block.receiverCounts.set(tx.to, (block.receiverCounts.get(tx.to) || 0) + 1);

      if (tx.contractCreation) {
        contractCreations += 1;
      }

      if (tx.costEth > largestTransaction.costEth) {
        largestTransaction = {
          blockNumber: tx.blockNumber,
          costEth: tx.costEth
        };
      }

      ensureAddressProfile(addressMap, tx.from, tx.blockNumber).outbound(tx);
      if (!tx.contractCreation) {
        ensureAddressProfile(addressMap, tx.to, tx.blockNumber).inbound(tx);
      }
    });

    const blocksWithParsedTransactions = [];
    blockMap.forEach(function (block) {
      block.avgCostEth = block.parsedTransactionCount ? block.totalCostEth / block.parsedTransactionCount : 0;
      block.uniqueSenders = block.senderCounts.size;
      block.uniqueReceivers = block.receiverCounts.size;
      block.topSenders = topEntries(block.senderCounts, 4);
      block.topReceivers = topEntries(block.receiverCounts, 4);
      block.transactions.sort(function (left, right) {
        return left.index - right.index;
      });
      delete block.senderCounts;
      delete block.receiverCounts;
      if (block.parsedTransactionCount > 0) {
        blocksWithParsedTransactions.push(block);
      }
    });

    const addressProfiles = [];
    addressMap.forEach(function (profile) {
      profile.totalInteractions = profile.inboundCount + profile.outboundCount;
      profile.activeBlocks = profile.activityByBlock.size;
      profile.uniqueCounterparties = profile.counterparties.size;
      profile.netFlowEth = profile.inboundEth - profile.outboundEth;
      profile.behaviorClass = classifyBehavior(profile);
      profile.topCounterparties = topEntries(profile.counterparties, 6);
      profile.busiestBlocks = topNumericEntries(profile.activityByBlock, 5);
      delete profile.counterparties;
      delete profile.activityByBlock;
      addressProfiles.push(profile);
    });

    addressProfiles.sort(function (left, right) {
      return right.totalInteractions - left.totalInteractions || Math.abs(right.netFlowEth) - Math.abs(left.netFlowEth);
    });

    const blocksList = Array.from(blockMap.values()).sort(function (left, right) {
      return left.number - right.number;
    });

    const topMiners = topEntriesWithBlocks(minerCounts, blocksList, 5);
    const overview = {
      blocksLoaded: blocksList.length,
      totalTransactionsMetadata: totalTransactionsMetadata,
      parsedTransactions: transactions.length,
      uniqueMiners: minerCounts.size,
      activeAddresses: addressProfiles.length,
      contractCreations: contractCreations,
      blockRangeStart: blocksList[0].number,
      blockRangeEnd: blocksList[blocksList.length - 1].number,
      avgTransactionsPerBlock: totalTransactionsMetadata / blocksList.length,
      avgCostPerTxEth: transactions.length ? transactions.reduce(function (total, tx) {
        return total + tx.costEth;
      }, 0) / transactions.length : 0,
      blocksWithParsedTransactions: blocksWithParsedTransactions.length,
      topMinerShare: topMiners.length ? topMiners[0].count / blocksList.length : 0
    };

    return {
      overview: overview,
      topMiners: topMiners,
      hotBlocks: blocksList
        .slice()
        .sort(function (left, right) {
          return right.parsedTransactionCount - left.parsedTransactionCount ||
            right.transactionCountMetadata - left.transactionCountMetadata;
        })
        .slice(0, 6),
      activeAddresses: addressProfiles.slice(0, 6),
      heaviestSender: addressProfiles
        .slice()
        .sort(function (left, right) { return right.outboundEth - left.outboundEth; })[0],
      heaviestReceiver: addressProfiles
        .slice()
        .sort(function (left, right) { return right.inboundEth - left.inboundEth; })[0],
      largestTransaction: largestTransaction,
      blockMap: blockMap,
      addressMap: new Map(addressProfiles.map(function (profile) {
        return [profile.address, profile];
      }))
    };
  }

  function ensureAddressProfile(addressMap, address, blockNumber) {
    if (!addressMap.has(address)) {
      addressMap.set(address, {
        address: address,
        inboundCount: 0,
        outboundCount: 0,
        inboundEth: 0,
        outboundEth: 0,
        firstBlock: blockNumber,
        lastBlock: blockNumber,
        counterparties: new Map(),
        activityByBlock: new Map(),
        inbound: function (tx) {
          this.inboundCount += 1;
          this.inboundEth += tx.costEth;
          this.firstBlock = Math.min(this.firstBlock, tx.blockNumber);
          this.lastBlock = Math.max(this.lastBlock, tx.blockNumber);
          this.activityByBlock.set(tx.blockNumber, (this.activityByBlock.get(tx.blockNumber) || 0) + 1);
          this.counterparties.set(tx.from, (this.counterparties.get(tx.from) || 0) + 1);
        },
        outbound: function (tx) {
          this.outboundCount += 1;
          this.outboundEth += tx.costEth;
          this.firstBlock = Math.min(this.firstBlock, tx.blockNumber);
          this.lastBlock = Math.max(this.lastBlock, tx.blockNumber);
          this.activityByBlock.set(tx.blockNumber, (this.activityByBlock.get(tx.blockNumber) || 0) + 1);
          this.counterparties.set(tx.to, (this.counterparties.get(tx.to) || 0) + 1);
        }
      });
    }

    return addressMap.get(address);
  }

  function classifyBehavior(profile) {
    if (profile.outboundCount > 0 && profile.inboundCount === 0) {
      return "pure_sender";
    }
    if (profile.inboundCount > 0 && profile.outboundCount === 0) {
      return "pure_receiver";
    }
    if (profile.outboundCount > profile.inboundCount * 3) {
      return "heavy_sender";
    }
    if (profile.inboundCount > profile.outboundCount * 3) {
      return "heavy_receiver";
    }
    return "balanced";
  }

  function topEntries(map, limit) {
    return Array.from(map.entries())
      .sort(function (left, right) {
        return right[1] - left[1];
      })
      .slice(0, limit)
      .map(function (entry) {
        return { address: entry[0], count: entry[1] };
      });
  }

  function topEntriesWithBlocks(map, blocks, limit) {
    return Array.from(map.entries())
      .sort(function (left, right) {
        return right[1] - left[1];
      })
      .slice(0, limit)
      .map(function (entry) {
        const address = entry[0];
        const featuredBlock = blocks
          .slice()
          .reverse()
          .find(function (block) {
            return block.miner === address && block.parsedTransactionCount > 0;
          }) || blocks
          .slice()
          .reverse()
          .find(function (block) {
            return block.miner === address;
          });

        return {
          address: address,
          count: entry[1],
          featuredBlock: featuredBlock ? featuredBlock.number : blocks[blocks.length - 1].number
        };
      });
  }

  function topNumericEntries(map, limit) {
    return Array.from(map.entries())
      .sort(function (left, right) {
        return right[1] - left[1];
      })
      .slice(0, limit)
      .map(function (entry) {
        return { blockNumber: entry[0], count: entry[1] };
      });
  }

  function renderTransactionsTable(transactions) {
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

  function formatTimestamp(unixSeconds) {
    return new Date(unixSeconds * 1000).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit"
    });
  }

  function formatInteger(value) {
    return new Intl.NumberFormat("en-US").format(value);
  }

  function formatDecimal(value, digits) {
    return new Intl.NumberFormat("en-US", {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits
    }).format(value);
  }

  function formatPercent(value) {
    return new Intl.NumberFormat("en-US", {
      style: "percent",
      maximumFractionDigits: 1
    }).format(value);
  }

  function formatEth(value) {
    return formatDecimal(value, 6) + " ETH";
  }

  function formatSignedEth(value) {
    const prefix = value > 0 ? "+" : "";
    return prefix + formatEth(value);
  }

  function formatRange(start, end) {
    return String(start) + "-" + String(end);
  }

  function shorten(value) {
    if (!value || value.length < 15) {
      return value || "";
    }
    return value.slice(0, 8) + "..." + value.slice(-6);
  }

  function looksLikeAddress(value) {
    return typeof value === "string" && value.startsWith("0x") && value.length === ADDRESS_LENGTH;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }
})();
