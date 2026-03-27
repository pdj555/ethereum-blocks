import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestNetworkAnalyzer {

    private static ArrayList<Blocks> blocks;

    @BeforeAll
    static void setUp() throws FileNotFoundException, IOException {
        Blocks.resetState();
        Blocks.readFile("ethereumP1data.csv");
        Blocks.sortBlocksByNumber();
        blocks = Blocks.getBlocks();
    }

    @Test
    void testNetworkAnalysisReturnsStructuredData() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        assertNotNull(result);
        assertFalse(result.containsKey("error"));
        assertTrue((int) result.get("total_addresses") > 0);
        assertTrue((int) result.get("total_unique_edges") > 0);
        assertNotNull(result.get("graph_density"));
    }

    @Test
    void testConnectedComponents() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) result.get("connected_components");
        assertNotNull(components);
        assertTrue((int) components.get("count") >= 1);
        assertTrue((int) components.get("largest") > 0);
    }

    @Test
    void testWhaleDetection() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> whales = (List<Map<String, Object>>) result.get("whales");
        assertNotNull(whales);
        assertFalse(whales.isEmpty());
        // Whales should have address, volume, role
        Map<String, Object> topWhale = whales.get(0);
        assertNotNull(topWhale.get("address"));
        assertTrue((double) topWhale.get("total_volume_eth") > 0);
        assertNotNull(topWhale.get("role"));
    }

    @Test
    void testHubDetection() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> hubs = (List<Map<String, Object>>) result.get("hubs");
        assertNotNull(hubs);
        assertFalse(hubs.isEmpty());
        assertTrue((int) hubs.get(0).get("unique_counterparties") > 0);
    }

    @Test
    void testFlowConcentration() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        @SuppressWarnings("unchecked")
        Map<String, Object> flow = (Map<String, Object>) result.get("flow_concentration");
        assertNotNull(flow);
        double topShare = (double) flow.get("top_share");
        assertTrue(topShare > 0.0 && topShare <= 1.0);
        assertNotNull(flow.get("gini_coefficient"));
    }

    @Test
    void testRiskSignals() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(blocks, 5);
        @SuppressWarnings("unchecked")
        Map<String, Object> risks = (Map<String, Object>) result.get("risk_signals");
        assertNotNull(risks);
        assertNotNull(risks.get("self_loop_count"));
        assertNotNull(risks.get("one_shot_addresses"));
        assertNotNull(risks.get("one_shot_ratio"));
    }

    @Test
    void testAddressNetworkProfile() {
        // Get a known address
        ArrayList<Transaction> txs = blocks.get(0).getTransactions();
        if (!txs.isEmpty()) {
            String addr = txs.get(0).getFromAddress();
            Map<String, Object> profile = NetworkAnalyzer.addressNetworkProfile(blocks, addr, 5);
            assertNotNull(profile);
            assertFalse(profile.containsKey("error"));
            assertTrue((int) profile.get("unique_counterparties") > 0);
            assertNotNull(profile.get("network_role"));
            assertNotNull(profile.get("degree_centrality"));
        }
    }

    @Test
    void testAddressNotInNetwork() {
        Map<String, Object> result = NetworkAnalyzer.addressNetworkProfile(blocks,
            "0x0000000000000000000000000000000000000000", 5);
        assertEquals("not_in_network", result.get("status"));
    }

    @Test
    void testNullBlocksReturnsError() {
        Map<String, Object> result = NetworkAnalyzer.analyzeNetwork(null, 5);
        assertEquals("no_data", result.get("error"));
    }
}
