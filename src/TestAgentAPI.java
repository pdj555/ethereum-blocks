import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestAgentAPI {

    private static ArrayList<Blocks> blocks;

    @BeforeAll
    static void setUp() throws FileNotFoundException, IOException {
        Blocks.resetState();
        Blocks.readFile("ethereumP1data.csv");
        Blocks.sortBlocksByNumber();
        blocks = Blocks.getBlocks();
    }

    @Test
    void testOverviewReturnsStructuredData() {
        Map<String, Object> overview = AgentAPI.overview(blocks, 5);
        assertNotNull(overview);
        assertFalse(overview.containsKey("error"));
        assertEquals(100, overview.get("blocks_loaded"));
        assertTrue((int) overview.get("unique_miners") > 0);
        assertTrue((long) overview.get("total_transactions_metadata") > 0);
        assertTrue((int) overview.get("parsed_transactions") > 0);
        assertNotNull(overview.get("top_miners"));
        assertNotNull(overview.get("concentration_risk"));
        assertNotNull(overview.get("cost_outliers"));
    }

    @Test
    void testOverviewWithNullReturnsError() {
        Map<String, Object> result = AgentAPI.overview(null, 5);
        assertEquals("no_data", result.get("error"));
    }

    @Test
    void testBlockDetailReturnsStructuredData() {
        Map<String, Object> detail = AgentAPI.blockDetail(blocks, 15049308);
        assertNotNull(detail);
        assertFalse(detail.containsKey("error"));
        assertEquals(15049308, detail.get("block_number"));
        assertNotNull(detail.get("miner"));
        assertNotNull(detail.get("timestamp"));
    }

    @Test
    void testBlockDetailNotFoundReturnsError() {
        Map<String, Object> detail = AgentAPI.blockDetail(blocks, 99999999);
        assertEquals("block_not_found", detail.get("error"));
    }

    @Test
    void testAddressIntelValidAddress() {
        // Get a known address from the first block's transactions
        ArrayList<Transaction> txs = blocks.get(0).getTransactions();
        if (!txs.isEmpty()) {
            String testAddr = txs.get(0).getFromAddress();
            Map<String, Object> intel = AgentAPI.addressIntel(blocks, testAddr, 5);
            assertNotNull(intel);
            assertFalse(intel.containsKey("error"));
            assertTrue((int) intel.get("total_interactions") > 0);
            assertNotNull(intel.get("behavior_class"));
        }
    }

    @Test
    void testAddressIntelInvalidFormat() {
        Map<String, Object> result = AgentAPI.addressIntel(blocks, "bad_address", 5);
        assertEquals("invalid_address_format", result.get("error"));
    }

    @Test
    void testMinerAnalysis() {
        Map<String, Object> miners = AgentAPI.minerAnalysis(blocks, 5);
        assertFalse(miners.containsKey("error"));
        assertTrue((int) miners.get("unique_miners") > 0);
        assertNotNull(miners.get("hhi_index"));
        assertNotNull(miners.get("hhi_level"));
    }

    @Test
    void testDetectAnomalies() {
        Map<String, Object> anomalies = AgentAPI.detectAnomalies(blocks, 1.5);
        assertFalse(anomalies.containsKey("error"));
        assertEquals(1.5, anomalies.get("z_threshold"));
        assertNotNull(anomalies.get("cost_anomalies"));
        assertNotNull(anomalies.get("volume_anomalies"));
    }

    @Test
    void testListBlocks() {
        List<Map<String, Object>> list = AgentAPI.listBlocks(blocks);
        assertEquals(100, list.size());
        Map<String, Object> first = list.get(0);
        assertNotNull(first.get("number"));
        assertNotNull(first.get("miner"));
        assertNotNull(first.get("tx_count"));
    }

    @Test
    void testBlockTransactions() {
        // Block 15049308 is the first block with transactions
        List<Map<String, Object>> txs = AgentAPI.blockTransactions(blocks, 15049308);
        assertFalse(txs.isEmpty());
        Map<String, Object> first = txs.get(0);
        assertNotNull(first.get("from"));
        assertNotNull(first.get("to"));
        assertNotNull(first.get("cost_eth"));
    }
}
