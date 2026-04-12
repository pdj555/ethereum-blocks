import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEthereumBlockExplorer {

    @Test
    void testHelpTextStaysMakeFirst() {
        String help = EthereumBlockExplorer.buildHelpText();
        assertTrue(help.contains("Run 'make help' for the full command guide."));
        assertTrue(help.contains("make dashboard"));
        assertTrue(help.contains("make help"));
        assertTrue(help.contains("make verify"));
        assertTrue(help.contains("Commands:"));
        assertTrue(help.contains("Serve the visual explorer at http://localhost:4173"));
        assertTrue(help.contains("make brief"));
        assertTrue(help.contains("make test"));
        assertTrue(help.contains("make cli-smoke"));
        assertTrue(help.contains("make ui-smoke"));
        assertTrue(help.contains("vendored JUnit runner"));
        assertFalse(help.contains("Open the visual explorer"));
        assertFalse(help.contains("java -cp"));
        assertFalse(help.contains("curl"));
    }

    @Test
    void testMissingDataErrorUsesActualFilename() {
        String json = EthereumBlockExplorer.buildMissingDataErrorJson("ethereumtransactions1.csv");
        assertEquals(
            "{\"error\": \"data_file_not_found\", \"file\": \"ethereumtransactions1.csv\"}",
            json
        );
        assertEquals(
            "{\"error\": \"data_file_not_found\", \"file\": \"ethereumP1data.csv\"}",
            EthereumBlockExplorer.buildMissingDataErrorJson("ethereumP1data.csv")
        );
        assertEquals(
            "Unexpected error while running 'dashboard'. Re-run the command or see 'make help'.",
            EthereumBlockExplorer.buildUnexpectedCommandError("dashboard")
        );
    }

    @Test
    void testMainMenuStaysUltraLean() {
        String menu = EthereumBlockExplorer.buildMainMenuText();
        assertTrue(menu.contains("dashboard"));
        assertTrue(menu.contains("block"));
        assertTrue(menu.contains("address"));
        assertTrue(menu.contains("network"));
        assertTrue(menu.contains("report"));
        assertTrue(menu.contains("help"));
        assertTrue(menu.contains("address profile"));
        assertFalse(menu.contains("more"));
        assertFalse(menu.contains("advanced"));
        assertFalse(menu.contains("intel"));
        assertFalse(menu.contains("miners"));
        assertFalse(menu.contains("brief"));
        assertFalse(menu.contains("anomalies"));
        assertFalse(menu.contains("compare"));
    }
}
