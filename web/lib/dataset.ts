import { formatTimestamp } from "./utils";
import type {
  AddressProfile,
  BlockView,
  Dataset,
  MinerEntry,
  TransactionRecord
} from "./types";

type MutableAddressProfile = AddressProfile & {
  counterparties: Map<string, number>;
  activityByBlock: Map<number, number>;
  inbound: (tx: TransactionRecord) => void;
  outbound: (tx: TransactionRecord) => void;
};

export function buildDataset(blocks: BlockView[], transactions: TransactionRecord[]): Dataset {
  const blockMap = new Map<number, BlockView>();
  const minerCounts = new Map<string, number>();
  const addressMap = new Map<string, MutableAddressProfile>();
  let totalTransactionsMetadata = 0;
  let contractCreations = 0;
  let largestTransaction: { blockNumber: number; costEth: number } | null = null;

  blocks.forEach((block) => {
    totalTransactionsMetadata += block.transactionCountMetadata;
    minerCounts.set(block.miner, (minerCounts.get(block.miner) || 0) + 1);
    blockMap.set(block.number, {
      ...block,
      parsedTransactionCount: 0,
      totalCostEth: 0,
      avgCostEth: 0,
      uniqueSenders: 0,
      uniqueReceivers: 0,
      topSenders: [],
      topReceivers: [],
      transactions: [],
      senderCounts: new Map<string, number>(),
      receiverCounts: new Map<string, number>()
    } as BlockView & {
      senderCounts: Map<string, number>;
      receiverCounts: Map<string, number>;
    });
  });

  transactions.forEach((tx) => {
    const block = blockMap.get(tx.blockNumber) as BlockView & {
      senderCounts: Map<string, number>;
      receiverCounts: Map<string, number>;
    };
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

    if (!largestTransaction || tx.costEth > largestTransaction.costEth) {
      largestTransaction = { blockNumber: tx.blockNumber, costEth: tx.costEth };
    }

    ensureAddressProfile(addressMap, tx.from, tx.blockNumber).outbound(tx);
    if (!tx.contractCreation) {
      ensureAddressProfile(addressMap, tx.to, tx.blockNumber).inbound(tx);
    }
  });

  const blocksWithParsedTransactions: BlockView[] = [];
  blockMap.forEach((block) => {
    const mutable = block as BlockView & {
      senderCounts: Map<string, number>;
      receiverCounts: Map<string, number>;
    };
    mutable.avgCostEth = mutable.parsedTransactionCount
      ? mutable.totalCostEth / mutable.parsedTransactionCount
      : 0;
    mutable.uniqueSenders = mutable.senderCounts.size;
    mutable.uniqueReceivers = mutable.receiverCounts.size;
    mutable.topSenders = topEntries(mutable.senderCounts, 4);
    mutable.topReceivers = topEntries(mutable.receiverCounts, 4);
    mutable.transactions.sort((left, right) => left.index - right.index);
    delete (mutable as { senderCounts?: Map<string, number> }).senderCounts;
    delete (mutable as { receiverCounts?: Map<string, number> }).receiverCounts;
    if (mutable.parsedTransactionCount > 0) {
      blocksWithParsedTransactions.push(mutable);
    }
  });

  const addressProfiles: AddressProfile[] = [];
  addressMap.forEach((profile) => {
    profile.totalInteractions = profile.inboundCount + profile.outboundCount;
    profile.activeBlocks = profile.activityByBlock.size;
    profile.uniqueCounterparties = profile.counterparties.size;
    profile.netFlowEth = profile.inboundEth - profile.outboundEth;
    profile.behaviorClass = classifyBehavior(profile);
    profile.topCounterparties = topEntries(profile.counterparties, 6);
    profile.busiestBlocks = topNumericEntries(profile.activityByBlock, 5);
    delete (profile as Partial<MutableAddressProfile>).counterparties;
    delete (profile as Partial<MutableAddressProfile>).activityByBlock;
    delete (profile as Partial<MutableAddressProfile>).inbound;
    delete (profile as Partial<MutableAddressProfile>).outbound;
    addressProfiles.push(profile);
  });

  addressProfiles.sort(
    (left, right) =>
      right.totalInteractions - left.totalInteractions ||
      Math.abs(right.netFlowEth) - Math.abs(left.netFlowEth)
  );

  const blocksList = Array.from(blockMap.values()).sort((left, right) => left.number - right.number);
  const topMiners = topEntriesWithBlocks(minerCounts, blocksList, 5);
  const blockNumbers = blocksList.map((block) => block.number);
  const txSeries = blocksList.map((block) => block.parsedTransactionCount);
  const costSeries = blocksList.map((block) => block.totalCostEth);

  const overview = {
    blocksLoaded: blocksList.length,
    totalTransactionsMetadata,
    parsedTransactions: transactions.length,
    uniqueMiners: minerCounts.size,
    activeAddresses: addressProfiles.length,
    contractCreations,
    blockRangeStart: blocksList[0]?.number ?? 0,
    blockRangeEnd: blocksList[blocksList.length - 1]?.number ?? 0,
    avgTransactionsPerBlock: blocksList.length ? totalTransactionsMetadata / blocksList.length : 0,
    avgCostPerTxEth: transactions.length
      ? transactions.reduce((total, tx) => total + tx.costEth, 0) / transactions.length
      : 0,
    blocksWithParsedTransactions: blocksWithParsedTransactions.length,
    topMinerShare: topMiners.length ? topMiners[0].count / blocksList.length : 0
  };

  return {
    overview,
    topMiners,
    hotBlocks: blocksList
      .slice()
      .sort(
        (left, right) =>
          right.parsedTransactionCount - left.parsedTransactionCount ||
          right.transactionCountMetadata - left.transactionCountMetadata
      )
      .slice(0, 6),
    activeAddresses: addressProfiles.slice(0, 6),
    heaviestSender:
      addressProfiles.slice().sort((left, right) => right.outboundEth - left.outboundEth)[0] ?? null,
    heaviestReceiver:
      addressProfiles.slice().sort((left, right) => right.inboundEth - left.inboundEth)[0] ?? null,
    largestTransaction,
    blockMap,
    addressMap: new Map(addressProfiles.map((profile) => [profile.address, profile])),
    txSeries,
    costSeries,
    blockNumbers
  };
}

export function parseBlocksToViews(csvBlocks: Array<{
  number: number;
  miner: string;
  timestamp: number;
  transactionCountMetadata: number;
}>): BlockView[] {
  return csvBlocks.map((block) => ({
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
    topSenders: [],
    topReceivers: [],
    transactions: []
  }));
}

function ensureAddressProfile(
  addressMap: Map<string, MutableAddressProfile>,
  address: string,
  blockNumber: number
): MutableAddressProfile {
  if (!addressMap.has(address)) {
    addressMap.set(address, {
      address,
      inboundCount: 0,
      outboundCount: 0,
      inboundEth: 0,
      outboundEth: 0,
      firstBlock: blockNumber,
      lastBlock: blockNumber,
      totalInteractions: 0,
      activeBlocks: 0,
      uniqueCounterparties: 0,
      netFlowEth: 0,
      behaviorClass: "balanced",
      topCounterparties: [],
      busiestBlocks: [],
      counterparties: new Map<string, number>(),
      activityByBlock: new Map<number, number>(),
      inbound(tx: TransactionRecord) {
        this.inboundCount += 1;
        this.inboundEth += tx.costEth;
        this.firstBlock = Math.min(this.firstBlock, tx.blockNumber);
        this.lastBlock = Math.max(this.lastBlock, tx.blockNumber);
        this.activityByBlock.set(tx.blockNumber, (this.activityByBlock.get(tx.blockNumber) || 0) + 1);
        this.counterparties.set(tx.from, (this.counterparties.get(tx.from) || 0) + 1);
      },
      outbound(tx: TransactionRecord) {
        this.outboundCount += 1;
        this.outboundEth += tx.costEth;
        this.firstBlock = Math.min(this.firstBlock, tx.blockNumber);
        this.lastBlock = Math.max(this.lastBlock, tx.blockNumber);
        this.activityByBlock.set(tx.blockNumber, (this.activityByBlock.get(tx.blockNumber) || 0) + 1);
        this.counterparties.set(tx.to, (this.counterparties.get(tx.to) || 0) + 1);
      }
    });
  }

  return addressMap.get(address)!;
}

function classifyBehavior(profile: AddressProfile): string {
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

function topEntries(map: Map<string, number>, limit: number) {
  return Array.from(map.entries())
    .sort((left, right) => right[1] - left[1])
    .slice(0, limit)
    .map(([address, count]) => ({ address, count }));
}

function topEntriesWithBlocks(
  map: Map<string, number>,
  blocks: BlockView[],
  limit: number
): MinerEntry[] {
  return Array.from(map.entries())
    .sort((left, right) => right[1] - left[1])
    .slice(0, limit)
    .map(([address, count]) => {
      const featuredBlock =
        blocks
          .slice()
          .reverse()
          .find((block) => block.miner === address && block.parsedTransactionCount > 0) ||
        blocks
          .slice()
          .reverse()
          .find((block) => block.miner === address);

      return {
        address,
        count,
        featuredBlock: featuredBlock ? featuredBlock.number : blocks[blocks.length - 1]?.number ?? 0
      };
    });
}

function topNumericEntries(map: Map<number, number>, limit: number) {
  return Array.from(map.entries())
    .sort((left, right) => right[1] - left[1])
    .slice(0, limit)
    .map(([blockNumber, count]) => ({ blockNumber, count }));
}
