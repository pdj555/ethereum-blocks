import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent-native API for Ethereum block analytics.
 *
 * Every method returns structured data as a Map that serializes cleanly to JSON.
 * This is the primary interface for AI agents, pipelines, and programmatic consumers.
 * No System.out. No formatting. Pure structured data.
 */
public final class AgentAPI {
    private AgentAPI() {}

    /**
     * Full system overview — the single call an agent makes first.
     * Returns everything needed to decide what to investigate next.
     */
    public static Map<String, Object> overview(ArrayList<Blocks> blocks, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            result.put("error", "no_data");
            return result;
        }

        long totalTx = 0;
        int parsedTx = 0;
        int blocksWithTx = 0;
        double totalCostEth = 0.0;
        Map<String, Integer> minerFreq = new HashMap<>();

        for (Blocks block : blocks) {
            totalTx += block.getTransactionCount();
            minerFreq.merge(block.getMiner(), 1, Integer::sum);
            ArrayList<Transaction> txs = block.getTransactions();
            if (!txs.isEmpty()) {
                blocksWithTx++;
                for (Transaction tx : txs) {
                    totalCostEth += tx.transactionCost();
                    parsedTx++;
                }
            }
        }

        result.put("blocks_loaded", blocks.size());
        result.put("unique_miners", minerFreq.size());
        result.put("total_transactions_metadata", totalTx);
        result.put("parsed_transactions", parsedTx);
        result.put("blocks_with_parsed_tx", blocksWithTx);
        result.put("total_cost_eth", totalCostEth);
        result.put("avg_tx_per_block", (double) totalTx / blocks.size());
        result.put("avg_cost_per_tx_eth", parsedTx == 0 ? 0.0 : totalCostEth / parsedTx);

        // Block range
        int minBlock = Integer.MAX_VALUE, maxBlock = Integer.MIN_VALUE;
        for (Blocks b : blocks) {
            minBlock = Math.min(minBlock, b.getNumber());
            maxBlock = Math.max(maxBlock, b.getNumber());
        }
        result.put("block_range_start", minBlock);
        result.put("block_range_end", maxBlock);

        result.put("top_miners", topEntries(minerFreq, topN));
        result.put("concentration_risk", concentrationRisk(minerFreq, blocks.size()));
        result.put("cost_outliers", costOutliersData(blocks, topN));

        return result;
    }

    /**
     * Deep intel on a single block — everything an agent needs to reason about it.
     */
    public static Map<String, Object> blockDetail(ArrayList<Blocks> blocks, int blockNumber) {
        Map<String, Object> result = new LinkedHashMap<>();
        Blocks block = findBlock(blocks, blockNumber);
        if (block == null) {
            result.put("error", "block_not_found");
            result.put("block_number", blockNumber);
            return result;
        }

        result.put("block_number", block.getNumber());
        result.put("miner", block.getMiner());
        result.put("timestamp", block.getDate());
        result.put("transaction_count_metadata", block.getTransactionCount());

        ArrayList<Transaction> txs = block.getTransactions();
        result.put("parsed_transaction_count", txs.size());
        result.put("avg_cost_eth", block.avgTransactionCost());

        if (!txs.isEmpty()) {
            double totalCost = 0, maxCost = 0, minCost = Double.MAX_VALUE;
            Map<String, Integer> senderCounts = new HashMap<>();
            Map<String, Integer> receiverCounts = new HashMap<>();

            for (Transaction tx : txs) {
                double cost = tx.transactionCost();
                totalCost += cost;
                maxCost = Math.max(maxCost, cost);
                minCost = Math.min(minCost, cost);
                senderCounts.merge(tx.getFromAddress(), 1, Integer::sum);
                receiverCounts.merge(tx.getToAddress(), 1, Integer::sum);
            }

            result.put("total_cost_eth", totalCost);
            result.put("max_tx_cost_eth", maxCost);
            result.put("min_tx_cost_eth", minCost);
            result.put("unique_senders", senderCounts.size());
            result.put("unique_receivers", receiverCounts.size());
            result.put("top_senders", topEntries(senderCounts, 5));
            result.put("top_receivers", topEntries(receiverCounts, 5));
        }

        return result;
    }

    /**
     * Address intelligence — full wallet profile across all loaded blocks.
     */
    public static Map<String, Object> addressIntel(ArrayList<Blocks> blocks, String rawAddress, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            result.put("error", "no_data");
            return result;
        }
        if (rawAddress == null || rawAddress.trim().isEmpty()) {
            result.put("error", "address_required");
            return result;
        }

        String address = rawAddress.trim().toLowerCase();
        if (!EthereumAddressValidator.isValid(address)) {
            result.put("error", "invalid_address_format");
            return result;
        }

        int inCount = 0, outCount = 0;
        double inEth = 0, outEth = 0;
        int firstBlock = Integer.MAX_VALUE, lastBlock = Integer.MIN_VALUE;
        Map<String, Integer> counterparties = new HashMap<>();
        Map<Integer, Integer> activityByBlock = new HashMap<>();
        Map<String, Double> counterpartyVolume = new HashMap<>();

        for (Blocks block : blocks) {
            for (Transaction tx : block.getTransactions()) {
                String from = tx.getFromAddress().toLowerCase();
                String to = tx.getToAddress().toLowerCase();
                boolean isOut = from.equals(address);
                boolean isIn = to.equals(address);
                if (!isIn && !isOut) continue;

                firstBlock = Math.min(firstBlock, block.getNumber());
                lastBlock = Math.max(lastBlock, block.getNumber());
                activityByBlock.merge(block.getNumber(), 1, Integer::sum);

                double cost = tx.transactionCost();
                if (isOut) {
                    outCount++;
                    outEth += cost;
                    counterparties.merge(to, 1, Integer::sum);
                    counterpartyVolume.merge(to, cost, Double::sum);
                }
                if (isIn) {
                    inCount++;
                    inEth += cost;
                    counterparties.merge(from, 1, Integer::sum);
                    counterpartyVolume.merge(from, cost, Double::sum);
                }
            }
        }

        int total = inCount + outCount;
        if (total == 0) {
            result.put("address", address);
            result.put("status", "no_activity");
            return result;
        }

        result.put("address", address);
        result.put("total_interactions", total);
        result.put("inbound_count", inCount);
        result.put("outbound_count", outCount);
        result.put("inbound_eth", inEth);
        result.put("outbound_eth", outEth);
        result.put("net_flow_eth", inEth - outEth);
        result.put("first_block", firstBlock);
        result.put("last_block", lastBlock);
        result.put("active_blocks", activityByBlock.size());
        result.put("top_counterparties", topEntries(counterparties, topN));
        result.put("busiest_blocks", topIntEntries(activityByBlock, topN));

        // Behavioral classification
        String behavior;
        if (outCount > 0 && inCount == 0) behavior = "pure_sender";
        else if (inCount > 0 && outCount == 0) behavior = "pure_receiver";
        else if (outCount > inCount * 3) behavior = "heavy_sender";
        else if (inCount > outCount * 3) behavior = "heavy_receiver";
        else behavior = "balanced";
        result.put("behavior_class", behavior);

        return result;
    }

    /**
     * Compare two blocks — structured diff for agent reasoning.
     */
    public static Map<String, Object> compareBlocks(ArrayList<Blocks> blocks, int blockA, int blockB) {
        Map<String, Object> result = new LinkedHashMap<>();
        Blocks a = findBlock(blocks, blockA);
        Blocks b = findBlock(blocks, blockB);

        if (a == null || b == null) {
            result.put("error", "block_not_found");
            if (a == null) result.put("missing", blockA);
            if (b == null) result.put("missing", blockB);
            return result;
        }

        result.put("block_a", blockA);
        result.put("block_b", blockB);
        result.put("block_diff", Math.abs(a.getNumber() - b.getNumber()));
        result.put("time_diff_seconds", Math.abs(a.getTimestamp() - b.getTimestamp()));
        result.put("tx_count_a", a.getTransactionCount());
        result.put("tx_count_b", b.getTransactionCount());
        result.put("tx_count_diff", Math.abs(a.getTransactionCount() - b.getTransactionCount()));
        try {
            result.put("transactions_between", Blocks.transactionDiff(a, b));
        } catch (Exception e) {
            result.put("transactions_between", -1);
        }
        result.put("avg_cost_a_eth", a.avgTransactionCost());
        result.put("avg_cost_b_eth", b.avgTransactionCost());
        result.put("cost_diff_eth", Math.abs(a.avgTransactionCost() - b.avgTransactionCost()));
        result.put("same_miner", a.getMiner().equals(b.getMiner()));

        return result;
    }

    /**
     * Miner concentration analysis — governance risk detection.
     */
    public static Map<String, Object> minerAnalysis(ArrayList<Blocks> blocks, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            result.put("error", "no_data");
            return result;
        }

        Map<String, Integer> freq = new HashMap<>();
        for (Blocks block : blocks) {
            freq.merge(block.getMiner(), 1, Integer::sum);
        }

        result.put("unique_miners", freq.size());
        result.put("total_blocks", blocks.size());
        result.put("miners", topEntries(freq, topN));
        result.put("concentration_risk", concentrationRisk(freq, blocks.size()));

        // Herfindahl-Hirschman Index (HHI) for market concentration
        double hhi = 0.0;
        for (int count : freq.values()) {
            double share = (double) count / blocks.size();
            hhi += share * share;
        }
        result.put("hhi_index", hhi);
        String hhi_level;
        if (hhi > 0.25) hhi_level = "highly_concentrated";
        else if (hhi > 0.15) hhi_level = "moderately_concentrated";
        else hhi_level = "competitive";
        result.put("hhi_level", hhi_level);

        return result;
    }

    /**
     * List all blocks with key metrics — bulk data for agent pipelines.
     */
    public static List<Map<String, Object>> listBlocks(ArrayList<Blocks> blocks) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (blocks == null) return list;

        for (Blocks block : blocks) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("number", block.getNumber());
            entry.put("miner", block.getMiner());
            entry.put("timestamp", block.getDate());
            entry.put("tx_count", block.getTransactionCount());
            entry.put("parsed_tx", block.getTransactions().size());
            entry.put("avg_cost_eth", block.avgTransactionCost());
            list.add(entry);
        }
        return list;
    }

    /**
     * Transaction-level data for a block — full detail for agent processing.
     */
    public static List<Map<String, Object>> blockTransactions(ArrayList<Blocks> blocks, int blockNumber) {
        List<Map<String, Object>> list = new ArrayList<>();
        Blocks block = findBlock(blocks, blockNumber);
        if (block == null) return list;

        for (Transaction tx : block.getTransactions()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("index", tx.getIndex());
            entry.put("from", tx.getFromAddress());
            entry.put("to", tx.getToAddress());
            entry.put("contract_creation", tx.isContractCreation());
            entry.put("gas_limit", tx.getGasLimit());
            entry.put("gas_price", tx.getGasPrice());
            entry.put("cost_eth", tx.transactionCost());
            list.add(entry);
        }
        return list;
    }

    /**
     * Anomaly detection — find blocks that deviate from normal patterns.
     */
    public static Map<String, Object> detectAnomalies(ArrayList<Blocks> blocks, double zThreshold) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            result.put("error", "no_data");
            return result;
        }

        // Cost anomalies
        List<Map<String, Object>> costAnomalies = new ArrayList<>();
        List<Double> costs = new ArrayList<>();
        Map<Integer, Double> blockCosts = new LinkedHashMap<>();

        for (Blocks block : blocks) {
            ArrayList<Transaction> txs = block.getTransactions();
            if (!txs.isEmpty()) {
                double avg = block.avgTransactionCost();
                costs.add(avg);
                blockCosts.put(block.getNumber(), avg);
            }
        }

        if (costs.size() >= 3) {
            double mean = costs.stream().mapToDouble(d -> d).average().orElse(0.0);
            double variance = costs.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0.0);
            double std = Math.sqrt(variance);

            if (std > 0) {
                for (Map.Entry<Integer, Double> entry : blockCosts.entrySet()) {
                    double z = (entry.getValue() - mean) / std;
                    if (Math.abs(z) >= zThreshold) {
                        Map<String, Object> anomaly = new LinkedHashMap<>();
                        anomaly.put("block", entry.getKey());
                        anomaly.put("avg_cost_eth", entry.getValue());
                        anomaly.put("z_score", z);
                        anomaly.put("direction", z > 0 ? "high" : "low");
                        costAnomalies.add(anomaly);
                    }
                }
            }
            result.put("cost_mean_eth", mean);
            result.put("cost_std_eth", std);
        }

        // Transaction volume anomalies
        List<Map<String, Object>> volumeAnomalies = new ArrayList<>();
        List<Integer> volumes = blocks.stream()
            .map(Blocks::getTransactionCount)
            .collect(Collectors.toList());

        double volMean = volumes.stream().mapToInt(i -> i).average().orElse(0.0);
        double volVar = volumes.stream().mapToDouble(v -> (v - volMean) * (v - volMean)).average().orElse(0.0);
        double volStd = Math.sqrt(volVar);

        if (volStd > 0) {
            for (Blocks block : blocks) {
                double z = (block.getTransactionCount() - volMean) / volStd;
                if (Math.abs(z) >= zThreshold) {
                    Map<String, Object> anomaly = new LinkedHashMap<>();
                    anomaly.put("block", block.getNumber());
                    anomaly.put("tx_count", block.getTransactionCount());
                    anomaly.put("z_score", z);
                    anomaly.put("direction", z > 0 ? "high" : "low");
                    volumeAnomalies.add(anomaly);
                }
            }
        }

        result.put("z_threshold", zThreshold);
        result.put("cost_anomalies", costAnomalies);
        result.put("volume_anomalies", volumeAnomalies);
        result.put("volume_mean", volMean);
        result.put("volume_std", volStd);

        return result;
    }

    // ── Internal helpers ──

    private static Blocks findBlock(ArrayList<Blocks> blocks, int number) {
        if (blocks == null) return null;
        for (Blocks b : blocks) {
            if (b.getNumber() == number) return b;
        }
        return null;
    }

    private static Map<String, Object> concentrationRisk(Map<String, Integer> freq, int totalBlocks) {
        Map<String, Object> risk = new LinkedHashMap<>();
        if (freq.isEmpty()) {
            risk.put("level", "unknown");
            return risk;
        }

        int maxCount = freq.values().stream().mapToInt(i -> i).max().orElse(0);
        double topShare = (double) maxCount / totalBlocks;
        risk.put("top_miner_share", topShare);
        risk.put("top_miner_blocks", maxCount);

        if (topShare >= 0.50) risk.put("level", "critical");
        else if (topShare >= 0.25) risk.put("level", "high");
        else if (topShare >= 0.15) risk.put("level", "moderate");
        else risk.put("level", "healthy");

        return risk;
    }

    private static List<Map<String, Object>> costOutliersData(ArrayList<Blocks> blocks, int limit) {
        List<Map<String, Object>> outliers = new ArrayList<>();
        List<Double> costs = new ArrayList<>();
        Map<Integer, double[]> blockData = new LinkedHashMap<>();

        for (Blocks block : blocks) {
            ArrayList<Transaction> txs = block.getTransactions();
            if (!txs.isEmpty()) {
                double avg = block.avgTransactionCost();
                costs.add(avg);
                blockData.put(block.getNumber(), new double[]{avg, txs.size()});
            }
        }

        if (costs.size() < 3) return outliers;

        double mean = costs.stream().mapToDouble(d -> d).average().orElse(0.0);
        double variance = costs.stream().mapToDouble(d -> (d - mean) * (d - mean)).average().orElse(0.0);
        double std = Math.sqrt(variance);
        if (std == 0) return outliers;

        for (Map.Entry<Integer, double[]> entry : blockData.entrySet()) {
            double z = (entry.getValue()[0] - mean) / std;
            if (z >= 1.5) {
                Map<String, Object> o = new LinkedHashMap<>();
                o.put("block", entry.getKey());
                o.put("avg_cost_eth", entry.getValue()[0]);
                o.put("tx_count", (int) entry.getValue()[1]);
                o.put("z_score", z);
                outliers.add(o);
            }
        }

        outliers.sort((a, b) -> Double.compare((double) b.get("z_score"), (double) a.get("z_score")));
        if (outliers.size() > limit) outliers = outliers.subList(0, limit);
        return outliers;
    }

    @SuppressWarnings("unchecked")
    private static <V extends Comparable<V>> List<Map<String, Object>> topEntries(Map<String, ? extends Number> map, int n) {
        List<Map<String, Object>> top = new ArrayList<>();
        map.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue()))
            .limit(n)
            .forEach(e -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", e.getKey());
                entry.put("value", e.getValue());
                top.add(entry);
            });
        return top;
    }

    private static List<Map<String, Object>> topIntEntries(Map<Integer, Integer> map, int n) {
        List<Map<String, Object>> top = new ArrayList<>();
        map.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(n)
            .forEach(e -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("key", e.getKey());
                entry.put("value", e.getValue());
                top.add(entry);
            });
        return top;
    }
}
