import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lean analytics layer that turns raw block data into actionable summary insights.
 */
public final class Insights {
    private Insights() {}

    public static final class BlockCostStat {
        private final int blockNumber;
        private final int txCount;
        private final double avgCostEth;

        public BlockCostStat(int blockNumber, int txCount, double avgCostEth) {
            this.blockNumber = blockNumber;
            this.txCount = txCount;
            this.avgCostEth = avgCostEth;
        }

        public int getBlockNumber() {
            return blockNumber;
        }

        public int getTxCount() {
            return txCount;
        }

        public double getAvgCostEth() {
            return avgCostEth;
        }
    }

    public static void printDashboard(ArrayList<Blocks> blocks, int topN) {
        if (blocks == null || blocks.isEmpty()) {
            System.out.println("No block data loaded.");
            return;
        }

        int totalBlocks = blocks.size();
        long totalTransactions = 0L;
        double totalAvgCostAcrossBlocks = 0.0;
        int blocksWithTransactions = 0;
        Map<String, Integer> minerFrequency = new HashMap<>();

        for (Blocks block : blocks) {
            totalTransactions += block.getTransactionCount();
            minerFrequency.put(block.getMiner(), minerFrequency.getOrDefault(block.getMiner(), 0) + 1);

            if (!block.getTransactions().isEmpty()) {
                totalAvgCostAcrossBlocks += block.avgTransactionCost();
                blocksWithTransactions++;
            }
        }

        double avgTransactionsPerBlock = totalBlocks == 0 ? 0.0 : (double) totalTransactions / totalBlocks;
        double avgCostPerActiveBlock = blocksWithTransactions == 0 ? 0.0 : totalAvgCostAcrossBlocks / blocksWithTransactions;

        System.out.println("\n========== ETHEREUM DASHBOARD ==========");
        System.out.println("Blocks loaded: " + totalBlocks);
        System.out.println("Unique miners: " + minerFrequency.size());
        System.out.println("Total transactions: " + totalTransactions);
        System.out.printf("Avg transactions / block: %.2f%n", avgTransactionsPerBlock);
        System.out.printf("Avg tx cost / active block: %.8f ETH%n", avgCostPerActiveBlock);

        printTopMiners(minerFrequency, topN);
        printTopBlocksByTransactionCount(blocks, topN);
        printTopBlocksByAverageCost(blocks, topN);
        printPotentialOutlierBlocksByCost(blocks, topN);
        System.out.println("========================================\n");
    }

    public static void printActionBrief(ArrayList<Blocks> blocks, int topN) {
        System.out.println(buildActionBrief(blocks, topN));
    }

    public static void printAddressIntel(ArrayList<Blocks> blocks, String address, int topN) {
        System.out.println(buildAddressIntel(blocks, address, topN));
    }

    public static String buildAddressIntel(ArrayList<Blocks> blocks, String rawAddress, int topN) {
        if (blocks == null || blocks.isEmpty()) {
            return "No block data loaded.";
        }
        if (rawAddress == null || rawAddress.trim().isEmpty()) {
            return "Address is required.";
        }

        String address = rawAddress.trim().toLowerCase();
        if (!address.startsWith("0x") || address.length() != 42) {
            return "Invalid Ethereum address. Expected 0x + 40 hex chars.";
        }

        int inboundCount = 0;
        int outboundCount = 0;
        double inboundEth = 0.0;
        double outboundEth = 0.0;
        int firstBlock = Integer.MAX_VALUE;
        int lastBlock = Integer.MIN_VALUE;
        Map<String, Integer> counterparties = new HashMap<>();
        Map<Integer, Integer> activityByBlock = new HashMap<>();

        for (Blocks block : blocks) {
            for (Transaction tx : block.getTransactions()) {
                String from = tx.getFromAddress().toLowerCase();
                String to = tx.getToAddress().toLowerCase();
                boolean isOutbound = from.equals(address);
                boolean isInbound = to.equals(address);

                if (!isInbound && !isOutbound) {
                    continue;
                }

                firstBlock = Math.min(firstBlock, block.getNumber());
                lastBlock = Math.max(lastBlock, block.getNumber());
                activityByBlock.put(block.getNumber(), activityByBlock.getOrDefault(block.getNumber(), 0) + 1);

                if (isOutbound) {
                    outboundCount++;
                    outboundEth += tx.transactionCost();
                    counterparties.put(to, counterparties.getOrDefault(to, 0) + 1);
                }
                if (isInbound) {
                    inboundCount++;
                    inboundEth += tx.transactionCost();
                    counterparties.put(from, counterparties.getOrDefault(from, 0) + 1);
                }
            }
        }

        int totalTouches = inboundCount + outboundCount;
        if (totalTouches == 0) {
            return "No transactions found for address " + address + " in loaded blocks.";
        }

        List<Map.Entry<String, Integer>> topCounterparties = counterparties.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .collect(Collectors.toList());

        List<Map.Entry<Integer, Integer>> busiestBlocks = activityByBlock.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .collect(Collectors.toList());

        StringBuilder out = new StringBuilder();
        out.append("\n========== ADDRESS INTEL ==========");
        out.append("\nAddress: ").append(address);
        out.append("\nTouches (in + out): ").append(totalTouches);
        out.append("\nInbound tx: ").append(inboundCount)
            .append(String.format(" (%.8f ETH)", inboundEth));
        out.append("\nOutbound tx: ").append(outboundCount)
            .append(String.format(" (%.8f ETH)", outboundEth));
        out.append("\nNet flow (in - out): ").append(String.format("%.8f ETH", inboundEth - outboundEth));
        out.append("\nActive range: block ").append(firstBlock).append(" -> ").append(lastBlock);

        out.append("\n\nTop counterparties:");
        for (int i = 0; i < topCounterparties.size(); i++) {
            Map.Entry<String, Integer> cp = topCounterparties.get(i);
            out.append("\n").append(i + 1).append(". ").append(cp.getKey()).append(" (").append(cp.getValue()).append(" interactions)");
        }

        out.append("\n\nBusiest blocks:");
        for (int i = 0; i < busiestBlocks.size(); i++) {
            Map.Entry<Integer, Integer> hit = busiestBlocks.get(i);
            out.append("\n").append(i + 1).append(". Block ").append(hit.getKey()).append(" (").append(hit.getValue()).append(" tx)");
        }

        out.append("\n\nAction:\n");
        out.append("1) Investigate top counterparties for clustering patterns.\n");
        out.append("2) Alert on activity spikes in busiest blocks.\n");
        out.append("3) Compare net flow trend against future block windows.\n");
        out.append("===================================\n");
        return out.toString();
    }

    public static String buildActionBrief(ArrayList<Blocks> blocks, int topN) {
        if (blocks == null || blocks.isEmpty()) {
            return "No block data loaded.";
        }

        Map<String, Integer> minerFrequency = new HashMap<>();
        Map<String, Double> senderSpend = new HashMap<>();
        long totalTransactions = 0L;
        int busiestBlockNumber = -1;
        int busiestBlockTx = -1;
        int highestCostBlockNumber = -1;
        double highestAvgCost = -1.0;
        int knownTransactions = 0;

        for (Blocks block : blocks) {
            totalTransactions += block.getTransactionCount();
            minerFrequency.put(block.getMiner(), minerFrequency.getOrDefault(block.getMiner(), 0) + 1);

            if (block.getTransactionCount() > busiestBlockTx) {
                busiestBlockTx = block.getTransactionCount();
                busiestBlockNumber = block.getNumber();
            }

            ArrayList<Transaction> txs = block.getTransactions();
            if (!txs.isEmpty()) {
                double avgCost = block.avgTransactionCost();
                if (avgCost > highestAvgCost) {
                    highestAvgCost = avgCost;
                    highestCostBlockNumber = block.getNumber();
                }
            }

            for (Transaction tx : txs) {
                senderSpend.put(tx.getFromAddress(), senderSpend.getOrDefault(tx.getFromAddress(), 0.0) + tx.transactionCost());
                knownTransactions++;
            }
        }

        List<Map.Entry<String, Integer>> topMiners = minerFrequency.entrySet()
            .stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .collect(Collectors.toList());

        List<Map.Entry<String, Double>> topSenders = senderSpend.entrySet()
            .stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .collect(Collectors.toList());

        double topMinerShare = topMiners.isEmpty() ? 0.0 : (double) topMiners.get(0).getValue() / blocks.size();
        List<BlockCostStat> costOutliers = findCostOutliers(blocks, topN);

        StringBuilder out = new StringBuilder();
        out.append("\n========== ETHEREUM ACTION BRIEF ==========\n");
        out.append("Mission: Identify concentration risk + cost hotspots fast.\n\n");
        out.append("Core facts:\n");
        out.append("- Blocks analyzed: ").append(blocks.size()).append("\n");
        out.append("- Metadata transactions: ").append(totalTransactions).append("\n");
        out.append("- Parsed transactions: ").append(knownTransactions).append("\n");
        out.append(String.format("- Top miner concentration: %.2f%% (%s)%n",
            topMinerShare * 100,
            topMiners.isEmpty() ? "n/a" : topMiners.get(0).getKey()));
        out.append("- Busiest block: ").append(busiestBlockNumber).append(" (").append(busiestBlockTx).append(" tx)\n");
        out.append(String.format("- Highest avg-cost block: %d (%.8f ETH)\n\n", highestCostBlockNumber, Math.max(0.0, highestAvgCost)));

        out.append("Strategic signals:\n");
        if (topMinerShare >= 0.25) {
            out.append("- ⚠ Miner concentration is high; monitor for centralization risk.\n");
        } else {
            out.append("- ✅ Miner concentration is healthy in this sample.\n");
        }
        if (!costOutliers.isEmpty()) {
            out.append("- ⚠ Cost outlier blocks detected (potential volatility windows).\n");
        } else {
            out.append("- ✅ No major cost outliers detected.\n");
        }

        out.append("\nTop spenders (known transactions):\n");
        if (topSenders.isEmpty()) {
            out.append("- none\n");
        } else {
            for (int i = 0; i < topSenders.size(); i++) {
                Map.Entry<String, Double> sender = topSenders.get(i);
                out.append(String.format("%d. %s (%.8f ETH)%n", i + 1, sender.getKey(), sender.getValue()));
            }
        }

        out.append("\nNext moves:\n");
        out.append("1) Track top spender addresses block-over-block.\n");
        out.append("2) Alert on avg-cost z-score >= 1.5 for early anomaly detection.\n");
        out.append("3) Watch miner concentration > 25% as a governance risk threshold.\n");
        out.append("===========================================\n");

        return out.toString();
    }

    public static String buildReport(ArrayList<Blocks> blocks, int topN) {
        if (blocks == null || blocks.isEmpty()) {
            return "# Ethereum Report\n\nNo block data loaded.\n";
        }

        int totalBlocks = blocks.size();
        long totalTransactions = 0L;
        Map<String, Integer> minerFrequency = new HashMap<>();
        double totalCostAcrossAllTransactions = 0.0;
        long totalKnownTransactions = 0L;
        Set<String> uniqueAddresses = new LinkedHashSet<>();

        for (Blocks block : blocks) {
            totalTransactions += block.getTransactionCount();
            minerFrequency.put(block.getMiner(), minerFrequency.getOrDefault(block.getMiner(), 0) + 1);

            ArrayList<Transaction> txs = block.getTransactions();
            totalKnownTransactions += txs.size();
            for (Transaction tx : txs) {
                totalCostAcrossAllTransactions += tx.transactionCost();
                uniqueAddresses.add(tx.getFromAddress());
                uniqueAddresses.add(tx.getToAddress());
            }
        }

        double avgTxPerBlock = (double) totalTransactions / totalBlocks;
        double avgKnownTxCost = totalKnownTransactions == 0 ? 0.0 : totalCostAcrossAllTransactions / totalKnownTransactions;

        StringBuilder out = new StringBuilder();
        out.append("# Ethereum Blocks: Lean Report\n\n");
        out.append("## Core KPIs\n");
        out.append("- Blocks loaded: ").append(totalBlocks).append("\n");
        out.append("- Unique miners: ").append(minerFrequency.size()).append("\n");
        out.append("- Total transactions (metadata): ").append(totalTransactions).append("\n");
        out.append(String.format("- Avg transactions / block: %.2f%n", avgTxPerBlock));
        out.append(String.format("- Avg known transaction cost: %.8f ETH%n", avgKnownTxCost));
        out.append("- Unique addresses seen in loaded transactions: ").append(uniqueAddresses.size()).append("\n\n");

        out.append("## Top Miners\n");
        appendTopMiners(out, minerFrequency, topN);
        out.append("\n## Highest-Cost Blocks\n");
        appendTopCostBlocks(out, blocks, topN);
        out.append("\n## Potential Cost Outliers (z-score >= 1.5)\n");
        appendCostOutliers(out, blocks, topN);
        out.append("\n## Action Brief\n");
        out.append("```text\n");
        out.append(buildActionBrief(blocks, topN));
        out.append("```\n");

        return out.toString();
    }

    private static void printTopMiners(Map<String, Integer> minerFrequency, int topN) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(minerFrequency.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        System.out.println("\nTop miners by block production:");
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            System.out.println((i + 1) + ". " + entry.getKey() + "  (" + entry.getValue() + " blocks)");
        }
    }

    private static void printTopBlocksByTransactionCount(ArrayList<Blocks> blocks, int topN) {
        ArrayList<Blocks> sorted = new ArrayList<>(blocks);
        sorted.sort((a, b) -> Integer.compare(b.getTransactionCount(), a.getTransactionCount()));

        System.out.println("\nTop blocks by transaction volume:");
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            Blocks block = sorted.get(i);
            System.out.println((i + 1) + ". Block " + block.getNumber() + "  (" + block.getTransactionCount() + " tx)");
        }
    }

    private static void printTopBlocksByAverageCost(ArrayList<Blocks> blocks, int topN) {
        ArrayList<Blocks> activeBlocks = new ArrayList<>();
        for (Blocks block : blocks) {
            if (!block.getTransactions().isEmpty()) {
                activeBlocks.add(block);
            }
        }

        activeBlocks.sort((a, b) -> Double.compare(b.avgTransactionCost(), a.avgTransactionCost()));

        System.out.println("\nTop blocks by average transaction cost:");
        for (int i = 0; i < Math.min(topN, activeBlocks.size()); i++) {
            Blocks block = activeBlocks.get(i);
            System.out.printf("%d. Block %d  (%.8f ETH)%n", i + 1, block.getNumber(), block.avgTransactionCost());
        }
    }

    private static void printPotentialOutlierBlocksByCost(ArrayList<Blocks> blocks, int topN) {
        List<BlockCostStat> outliers = findCostOutliers(blocks, topN);
        System.out.println("\nPotential cost outlier blocks:");
        if (outliers.isEmpty()) {
            System.out.println("(none)");
            return;
        }
        for (int i = 0; i < outliers.size(); i++) {
            BlockCostStat stat = outliers.get(i);
            System.out.printf("%d. Block %d  (%.8f ETH avg, %d tx)%n", i + 1, stat.getBlockNumber(), stat.getAvgCostEth(), stat.getTxCount());
        }
    }

    private static List<BlockCostStat> findCostOutliers(ArrayList<Blocks> blocks, int limit) {
        ArrayList<BlockCostStat> stats = new ArrayList<>();
        for (Blocks block : blocks) {
            ArrayList<Transaction> txs = block.getTransactions();
            if (!txs.isEmpty()) {
                stats.add(new BlockCostStat(block.getNumber(), txs.size(), block.avgTransactionCost()));
            }
        }
        if (stats.size() < 3) {
            return new ArrayList<>();
        }

        double mean = stats.stream().mapToDouble(BlockCostStat::getAvgCostEth).average().orElse(0.0);
        double variance = stats.stream()
            .mapToDouble(s -> {
                double delta = s.getAvgCostEth() - mean;
                return delta * delta;
            })
            .average()
            .orElse(0.0);
        double std = Math.sqrt(variance);
        if (std == 0.0) {
            return new ArrayList<>();
        }

        return stats.stream()
            .filter(s -> (s.getAvgCostEth() - mean) / std >= 1.5)
            .sorted(Comparator.comparingDouble(BlockCostStat::getAvgCostEth).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    private static void appendTopMiners(StringBuilder out, Map<String, Integer> minerFrequency, int topN) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(minerFrequency.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (int i = 0; i < Math.min(topN, sorted.size()); i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            out.append(i + 1).append(". ").append(entry.getKey()).append(" (blocks: ").append(entry.getValue()).append(")\n");
        }
    }

    private static void appendTopCostBlocks(StringBuilder out, ArrayList<Blocks> blocks, int topN) {
        ArrayList<Blocks> active = new ArrayList<>();
        for (Blocks block : blocks) {
            if (!block.getTransactions().isEmpty()) {
                active.add(block);
            }
        }
        active.sort((a, b) -> Double.compare(b.avgTransactionCost(), a.avgTransactionCost()));

        for (int i = 0; i < Math.min(topN, active.size()); i++) {
            Blocks block = active.get(i);
            out.append(String.format("%d. Block %d (avg cost: %.8f ETH, tx: %d)%n", i + 1, block.getNumber(), block.avgTransactionCost(), block.getTransactions().size()));
        }
    }

    private static void appendCostOutliers(StringBuilder out, ArrayList<Blocks> blocks, int topN) {
        List<BlockCostStat> outliers = findCostOutliers(blocks, topN);
        if (outliers.isEmpty()) {
            out.append("- none\n");
            return;
        }
        for (int i = 0; i < outliers.size(); i++) {
            BlockCostStat stat = outliers.get(i);
            out.append(String.format("%d. Block %d (%.8f ETH avg, tx: %d)%n", i + 1, stat.getBlockNumber(), stat.getAvgCostEth(), stat.getTxCount()));
        }
    }
}
