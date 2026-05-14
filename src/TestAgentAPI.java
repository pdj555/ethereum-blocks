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
        assertEquals(2735, overview.get("parsed_transactions"));
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
        Map<String, Object> nonHexResult = AgentAPI.addressIntel(blocks, "0x" + "z".repeat(40), 5);
        assertEquals("invalid_address_format", nonHexResult.get("error"));
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
    void testCompareBlocksIncludesRealTimeAndBetweenCounts() {
        Map<String, Object> result = AgentAPI.compareBlocks(blocks, 15049311, 15049321);
        assertFalse(result.containsKey("error"));
        assertEquals(10, result.get("block_diff"));
        assertEquals(182L, result.get("time_diff_seconds"));
        assertEquals(151, result.get("tx_count_diff"));
        assertEquals(1587, result.get("transactions_between"));
        assertEquals(false, result.get("same_miner"));
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
        assertEquals(false, first.get("contract_creation"));
    }

    @Test
    void testBlockTransactionsMarksContractCreationRows() {
        List<Map<String, Object>> txs = AgentAPI.blockTransactions(blocks, 15049315);
        Map<String, Object> contractCreationTx = txs.stream()
            .filter(tx -> Boolean.TRUE.equals(tx.get("contract_creation")))
            .findFirst()
            .orElse(null);

        assertNotNull(contractCreationTx);
        assertEquals(Transaction.CONTRACT_CREATION_RECIPIENT, contractCreationTx.get("to"));
    }

    @Test
    void testAgentSnapshotReturnsOneCallContext() {
        Map<String, Object> snapshot = AgentAPI.agentSnapshot(blocks);
        assertEquals("ethereum-block-explorer.agent-snapshot.v1", snapshot.get("schema"));
        assertEquals("ready", snapshot.get("status"));
        assertTrue(snapshot.containsKey("overview"));
        assertTrue(snapshot.containsKey("network"));
        assertTrue(snapshot.containsKey("anomalies"));

        @SuppressWarnings("unchecked")
        Map<String, Object> dataContract = (Map<String, Object>) snapshot.get("data_contract");
        assertEquals(Blocks.DEFAULT_BLOCKS_FILE, dataContract.get("blocks_file"));
        assertEquals(Blocks.DEFAULT_TRANSACTIONS_FILE, dataContract.get("transactions_file"));

        @SuppressWarnings("unchecked")
        List<String> nextActions = (List<String>) snapshot.get("recommended_next_actions");
        assertFalse(nextActions.isEmpty());
    }
}
