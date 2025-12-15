import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DatasetInsights {
	private DatasetInsights() {}

	public static String buildContext(ArrayList<Blocks> blocks, Blocks focusBlock) {
		StringBuilder sb = new StringBuilder();
		sb.append(buildDatasetSummary(blocks));

		if (focusBlock != null) {
			sb.append("\n\n");
			sb.append(buildBlockSummary(focusBlock));
		}

		sb.append("\n\nConstraints:\n");
		sb.append("- If the answer requires data not present in this local dataset, say so explicitly.\n");
		sb.append("- Be precise and avoid speculation.\n");
		return sb.toString();
	}

	private static String buildDatasetSummary(ArrayList<Blocks> blocks) {
		if (blocks == null || blocks.isEmpty()) {
			return "Dataset summary: no blocks loaded.";
		}

		int minBlock = Integer.MAX_VALUE;
		int maxBlock = Integer.MIN_VALUE;

		Map<String, Integer> minerCounts = new HashMap<>();
		for (Blocks b : blocks) {
			minBlock = Math.min(minBlock, b.getNumber());
			maxBlock = Math.max(maxBlock, b.getNumber());
			minerCounts.merge(b.getMiner(), 1, Integer::sum);
		}

		List<Map.Entry<String, Integer>> topMiners = new ArrayList<>(minerCounts.entrySet());
		topMiners.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

		StringBuilder sb = new StringBuilder();
		sb.append("Dataset summary:\n");
		sb.append("- blocks loaded: ").append(blocks.size()).append("\n");
		sb.append("- block number range: ").append(minBlock).append(" .. ").append(maxBlock).append("\n");
		sb.append("- unique miners: ").append(minerCounts.size()).append("\n");
		sb.append("- top miners (by block count):\n");

		int limit = Math.min(5, topMiners.size());
		for (int i = 0; i < limit; i++) {
			Map.Entry<String, Integer> e = topMiners.get(i);
			sb.append("  - ").append(e.getKey()).append(" (").append(e.getValue()).append(" blocks)\n");
		}

		return sb.toString().trim();
	}

	private static String buildBlockSummary(Blocks block) {
		ArrayList<Transaction> txs = block.getTransactions();

		Map<String, Integer> fromCounts = new HashMap<>();
		Map<String, Double> fromCosts = new HashMap<>();

		for (Transaction t : txs) {
			String from = t.getFromAddress();
			fromCounts.merge(from, 1, Integer::sum);
			fromCosts.merge(from, t.transactionCost(), Double::sum);
		}

		List<String> topFrom = new ArrayList<>(fromCosts.keySet());
		topFrom.sort(Comparator
			.<String>comparingDouble(fromCosts::get).reversed()
			.thenComparing(fromCounts::get, Comparator.reverseOrder())
			.thenComparing(a -> a));

		StringBuilder sb = new StringBuilder();
		sb.append("Focus block:\n");
		sb.append("- number: ").append(block.getNumber()).append("\n");
		sb.append("- miner: ").append(block.getMiner()).append("\n");
		sb.append("- timestamp: ").append(block.getDate()).append("\n");
		sb.append("- transaction count (metadata): ").append(block.getTransactionCount()).append("\n");
		sb.append("- transactions loaded: ").append(txs.size()).append("\n");
		sb.append("- avg transaction cost: ").append(String.format(Locale.US, "%.8f", block.avgTransactionCost())).append(" ETH\n");
		sb.append("- unique from addresses: ").append(fromCounts.size()).append("\n");

		int limit = Math.min(5, topFrom.size());
		if (limit > 0) {
			sb.append("- top from addresses (by total ETH cost):\n");
			for (int i = 0; i < limit; i++) {
				String addr = topFrom.get(i);
				sb.append("  - ").append(addr)
					.append(" (tx=").append(fromCounts.getOrDefault(addr, 0))
					.append(", total=").append(String.format(Locale.US, "%.8f", fromCosts.getOrDefault(addr, 0.0)))
					.append(" ETH)\n");
			}
		}

		return sb.toString().trim();
	}
}

