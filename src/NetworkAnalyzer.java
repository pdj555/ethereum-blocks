import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Graph-based network analytics for Ethereum transaction data.
 *
 * Treats addresses as nodes and transactions as directed edges.
 * Provides: connected component analysis, whale detection,
 * hub identification, flow concentration, and risk scoring.
 */
public final class NetworkAnalyzer {
    private NetworkAnalyzer() {}

    /**
     * Build the full transaction graph and return comprehensive network metrics.
     * This is the primary entry point for agent-driven network analysis.
     */
    public static Map<String, Object> analyzeNetwork(ArrayList<Blocks> blocks, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (blocks == null || blocks.isEmpty()) {
            result.put("error", "no_data");
            return result;
        }

        // Build adjacency structures
        Map<String, Map<String, EdgeStats>> graph = new HashMap<>();  // from -> to -> stats
        Map<String, AddressStats> addressStats = new HashMap<>();

        for (Blocks block : blocks) {
            for (Transaction tx : block.getTransactions()) {
                String from = tx.getFromAddress().toLowerCase();
                String to = tx.getToAddress().toLowerCase();
                double cost = tx.transactionCost();

                // Update edge
                graph.computeIfAbsent(from, k -> new HashMap<>())
                      .computeIfAbsent(to, k -> new EdgeStats())
                      .add(cost);

                // Update address stats
                addressStats.computeIfAbsent(from, k -> new AddressStats()).addOutbound(cost, to);
                addressStats.computeIfAbsent(to, k -> new AddressStats()).addInbound(cost, from);
            }
        }

        int totalAddresses = addressStats.size();
        int totalEdges = graph.values().stream().mapToInt(Map::size).sum();

        result.put("total_addresses", totalAddresses);
        result.put("total_unique_edges", totalEdges);
        result.put("graph_density", totalAddresses < 2 ? 0.0 :
            (double) totalEdges / (totalAddresses * (totalAddresses - 1)));

        // Connected components (undirected)
        result.put("connected_components", findComponents(addressStats));

        // Whale detection (top addresses by total volume)
        result.put("whales", detectWhales(addressStats, topN));

        // Hub detection (top addresses by unique counterparty count)
        result.put("hubs", detectHubs(addressStats, topN));

        // Flow concentration (how much volume flows through top addresses)
        result.put("flow_concentration", flowConcentration(addressStats, topN));

        // Strongest edges (highest volume pairs)
        result.put("strongest_connections", strongestEdges(graph, topN));

        // Risk signals
        result.put("risk_signals", riskSignals(addressStats, graph));

        return result;
    }

    /**
     * Analyze a specific address's position in the network graph.
     */
    public static Map<String, Object> addressNetworkProfile(ArrayList<Blocks> blocks, String rawAddress, int topN) {
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
        Map<String, Map<String, EdgeStats>> graph = new HashMap<>();
        Map<String, AddressStats> allStats = new HashMap<>();

        for (Blocks block : blocks) {
            for (Transaction tx : block.getTransactions()) {
                String from = tx.getFromAddress().toLowerCase();
                String to = tx.getToAddress().toLowerCase();
                double cost = tx.transactionCost();
                graph.computeIfAbsent(from, k -> new HashMap<>())
                      .computeIfAbsent(to, k -> new EdgeStats())
                      .add(cost);
                allStats.computeIfAbsent(from, k -> new AddressStats()).addOutbound(cost, to);
                allStats.computeIfAbsent(to, k -> new AddressStats()).addInbound(cost, from);
            }
        }

        AddressStats stats = allStats.get(address);
        if (stats == null) {
            result.put("address", address);
            result.put("status", "not_in_network");
            return result;
        }

        result.put("address", address);
        result.put("outbound_tx_count", stats.outCount);
        result.put("inbound_tx_count", stats.inCount);
        result.put("outbound_eth", stats.outVolume);
        result.put("inbound_eth", stats.inVolume);
        result.put("net_flow_eth", stats.inVolume - stats.outVolume);
        result.put("unique_counterparties", stats.uniqueCounterparties());
        result.put("unique_senders", stats.inboundPeers.size());
        result.put("unique_receivers", stats.outboundPeers.size());

        // Degree centrality (normalized)
        int totalAddresses = allStats.size();
        double degreeCentrality = totalAddresses < 2 ? 0.0 :
            (double) stats.uniqueCounterparties() / (totalAddresses - 1);
        result.put("degree_centrality", degreeCentrality);

        // Classify role
        result.put("network_role", classifyRole(stats));

        // Top peers by volume
        Map<String, Double> peerVolumes = new HashMap<>();
        Map<String, EdgeStats> outEdges = graph.getOrDefault(address, Collections.emptyMap());
        for (Map.Entry<String, EdgeStats> e : outEdges.entrySet()) {
            peerVolumes.merge(e.getKey(), e.getValue().totalVolume, Double::sum);
        }
        for (Map.Entry<String, Map<String, EdgeStats>> outer : graph.entrySet()) {
            EdgeStats inEdge = outer.getValue().get(address);
            if (inEdge != null) {
                peerVolumes.merge(outer.getKey(), inEdge.totalVolume, Double::sum);
            }
        }

        List<Map<String, Object>> topPeers = peerVolumes.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(topN)
            .map(e -> {
                Map<String, Object> peer = new LinkedHashMap<>();
                peer.put("address", e.getKey());
                peer.put("volume_eth", e.getValue());
                return peer;
            })
            .collect(Collectors.toList());
        result.put("top_peers_by_volume", topPeers);

        return result;
    }

    // ── Graph Algorithms ──

    private static Map<String, Object> findComponents(Map<String, AddressStats> stats) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        List<Integer> componentSizes = new ArrayList<>();

        // Build undirected adjacency
        Map<String, Set<String>> adj = new HashMap<>();
        for (Map.Entry<String, AddressStats> entry : stats.entrySet()) {
            String addr = entry.getKey();
            AddressStats s = entry.getValue();
            adj.computeIfAbsent(addr, k -> new HashSet<>()).addAll(s.outboundPeers);
            adj.computeIfAbsent(addr, k -> new HashSet<>()).addAll(s.inboundPeers);
            for (String peer : s.outboundPeers) {
                adj.computeIfAbsent(peer, k -> new HashSet<>()).add(addr);
            }
            for (String peer : s.inboundPeers) {
                adj.computeIfAbsent(peer, k -> new HashSet<>()).add(addr);
            }
        }

        // BFS to find components
        for (String addr : adj.keySet()) {
            if (visited.contains(addr)) continue;
            int size = 0;
            Queue<String> queue = new LinkedList<>();
            queue.add(addr);
            visited.add(addr);
            while (!queue.isEmpty()) {
                String current = queue.poll();
                size++;
                for (String neighbor : adj.getOrDefault(current, Collections.emptySet())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            componentSizes.add(size);
        }

        componentSizes.sort(Collections.reverseOrder());
        result.put("count", componentSizes.size());
        result.put("largest", componentSizes.isEmpty() ? 0 : componentSizes.get(0));
        result.put("sizes", componentSizes.size() > 20 ? componentSizes.subList(0, 20) : componentSizes);

        return result;
    }

    private static List<Map<String, Object>> detectWhales(Map<String, AddressStats> stats, int topN) {
        return stats.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().totalVolume(), a.getValue().totalVolume()))
            .limit(topN)
            .map(e -> {
                Map<String, Object> whale = new LinkedHashMap<>();
                whale.put("address", e.getKey());
                whale.put("total_volume_eth", e.getValue().totalVolume());
                whale.put("tx_count", e.getValue().totalTxCount());
                whale.put("role", classifyRole(e.getValue()));
                return whale;
            })
            .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> detectHubs(Map<String, AddressStats> stats, int topN) {
        return stats.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().uniqueCounterparties(), a.getValue().uniqueCounterparties()))
            .limit(topN)
            .map(e -> {
                Map<String, Object> hub = new LinkedHashMap<>();
                hub.put("address", e.getKey());
                hub.put("unique_counterparties", e.getValue().uniqueCounterparties());
                hub.put("total_volume_eth", e.getValue().totalVolume());
                hub.put("role", classifyRole(e.getValue()));
                return hub;
            })
            .collect(Collectors.toList());
    }

    private static Map<String, Object> flowConcentration(Map<String, AddressStats> stats, int topN) {
        Map<String, Object> result = new LinkedHashMap<>();
        double totalVolume = stats.values().stream().mapToDouble(AddressStats::totalVolume).sum();
        if (totalVolume == 0) {
            result.put("top_share", 0.0);
            return result;
        }

        double topVolume = stats.values().stream()
            .mapToDouble(AddressStats::totalVolume)
            .sorted()  // ascending
            .skip(Math.max(0, stats.size() - topN))
            .sum();

        result.put("top_n", topN);
        result.put("top_share", topVolume / totalVolume);
        result.put("total_network_volume_eth", totalVolume);
        result.put("gini_coefficient", computeGini(stats));

        return result;
    }

    private static List<Map<String, Object>> strongestEdges(Map<String, Map<String, EdgeStats>> graph, int topN) {
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Map.Entry<String, Map<String, EdgeStats>> outer : graph.entrySet()) {
            for (Map.Entry<String, EdgeStats> inner : outer.getValue().entrySet()) {
                Map<String, Object> edge = new LinkedHashMap<>();
                edge.put("from", outer.getKey());
                edge.put("to", inner.getKey());
                edge.put("tx_count", inner.getValue().count);
                edge.put("volume_eth", inner.getValue().totalVolume);
                edges.add(edge);
            }
        }
        edges.sort((a, b) -> Double.compare((double) b.get("volume_eth"), (double) a.get("volume_eth")));
        return edges.size() > topN ? edges.subList(0, topN) : edges;
    }

    private static Map<String, Object> riskSignals(Map<String, AddressStats> stats, Map<String, Map<String, EdgeStats>> graph) {
        Map<String, Object> risks = new LinkedHashMap<>();

        // Self-loops (address sending to itself)
        List<String> selfLoops = new ArrayList<>();
        for (Map.Entry<String, Map<String, EdgeStats>> outer : graph.entrySet()) {
            if (outer.getValue().containsKey(outer.getKey())) {
                selfLoops.add(outer.getKey());
            }
        }
        risks.put("self_loop_addresses", selfLoops);
        risks.put("self_loop_count", selfLoops.size());

        // One-shot addresses (single transaction, never seen again)
        long oneShots = stats.values().stream()
            .filter(s -> s.totalTxCount() == 1)
            .count();
        risks.put("one_shot_addresses", oneShots);
        risks.put("one_shot_ratio", stats.isEmpty() ? 0.0 : (double) oneShots / stats.size());

        // High fan-out (sending to many unique addresses — potential distribution)
        long highFanOut = stats.values().stream()
            .filter(s -> s.outboundPeers.size() >= 10)
            .count();
        risks.put("high_fan_out_addresses", highFanOut);

        return risks;
    }

    private static double computeGini(Map<String, AddressStats> stats) {
        List<Double> volumes = stats.values().stream()
            .mapToDouble(AddressStats::totalVolume)
            .sorted()
            .boxed()
            .collect(Collectors.toList());

        int n = volumes.size();
        if (n < 2) return 0.0;

        double sum = 0, cumulativeSum = 0;
        for (int i = 0; i < n; i++) {
            cumulativeSum += volumes.get(i);
            sum += (2.0 * (i + 1) - n - 1) * volumes.get(i);
        }
        return cumulativeSum == 0 ? 0.0 : sum / (n * cumulativeSum);
    }

    private static String classifyRole(AddressStats stats) {
        if (stats.outCount > 0 && stats.inCount == 0) return "distributor";
        if (stats.inCount > 0 && stats.outCount == 0) return "collector";
        if (stats.outboundPeers.size() >= 10 && stats.inboundPeers.size() <= 2) return "fan_out";
        if (stats.inboundPeers.size() >= 10 && stats.outboundPeers.size() <= 2) return "fan_in";
        if (stats.uniqueCounterparties() >= 15) return "hub";
        return "peer";
    }

    // ── Internal data structures ──

    private static class EdgeStats {
        int count = 0;
        double totalVolume = 0.0;

        void add(double cost) {
            count++;
            totalVolume += cost;
        }
    }

    private static class AddressStats {
        int inCount = 0, outCount = 0;
        double inVolume = 0.0, outVolume = 0.0;
        Set<String> inboundPeers = new HashSet<>();
        Set<String> outboundPeers = new HashSet<>();

        void addInbound(double cost, String peer) {
            inCount++;
            inVolume += cost;
            inboundPeers.add(peer);
        }

        void addOutbound(double cost, String peer) {
            outCount++;
            outVolume += cost;
            outboundPeers.add(peer);
        }

        double totalVolume() { return inVolume + outVolume; }
        int totalTxCount() { return inCount + outCount; }
        int uniqueCounterparties() {
            Set<String> all = new HashSet<>(inboundPeers);
            all.addAll(outboundPeers);
            return all.size();
        }
    }
}
