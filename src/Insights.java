import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lean analytics layer that turns raw block data into actionable summary insights.
 */
public final class Insights {
    private Insights() {}

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
        System.out.println("========================================\n");
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
}
