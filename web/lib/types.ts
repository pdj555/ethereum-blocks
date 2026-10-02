export type BlockRecord = {
  number: number;
  miner: string;
  timestamp: number;
  transactionCountMetadata: number;
};

export type TransactionRecord = {
  blockNumber: number;
  index: number;
  from: string;
  to: string;
  contractCreation: boolean;
  gasLimit: number;
  gasPrice: number;
  costEth: number;
};

export type BlockView = {
  number: number;
  miner: string;
  timestamp: number;
  displayTimestamp: string;
  transactionCountMetadata: number;
  parsedTransactionCount: number;
  totalCostEth: number;
  avgCostEth: number;
  uniqueSenders: number;
  uniqueReceivers: number;
  topSenders: Array<{ address: string; count: number }>;
  topReceivers: Array<{ address: string; count: number }>;
  transactions: TransactionRecord[];
};

export type AddressProfile = {
  address: string;
  inboundCount: number;
  outboundCount: number;
  inboundEth: number;
  outboundEth: number;
  firstBlock: number;
  lastBlock: number;
  totalInteractions: number;
  activeBlocks: number;
  uniqueCounterparties: number;
  netFlowEth: number;
  behaviorClass: string;
  topCounterparties: Array<{ address: string; count: number }>;
  busiestBlocks: Array<{ blockNumber: number; count: number }>;
};

export type Overview = {
  blocksLoaded: number;
  totalTransactionsMetadata: number;
  parsedTransactions: number;
  uniqueMiners: number;
  activeAddresses: number;
  contractCreations: number;
  blockRangeStart: number;
  blockRangeEnd: number;
  avgTransactionsPerBlock: number;
  avgCostPerTxEth: number;
  blocksWithParsedTransactions: number;
  topMinerShare: number;
};

export type MinerEntry = {
  address: string;
  count: number;
  featuredBlock: number;
};

export type Dataset = {
  overview: Overview;
  topMiners: MinerEntry[];
  hotBlocks: BlockView[];
  activeAddresses: AddressProfile[];
  heaviestSender: AddressProfile | null;
  heaviestReceiver: AddressProfile | null;
  largestTransaction: { blockNumber: number; costEth: number } | null;
  blockMap: Map<number, BlockView>;
  addressMap: Map<string, AddressProfile>;
  txSeries: number[];
  costSeries: number[];
  blockNumbers: number[];
};

export type SearchMode = "block" | "address";
