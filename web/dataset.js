import { formatTimestamp, looksLikeAddress } from "./utils.js";

export function buildDataset(blocks, transactions) {
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
