import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestEthereumBlockExplorer {

    @Test
    void testHelpTextStaysMakeFirst() {
        String help = EthereumBlockExplorer.buildHelpText();
        assertTrue(help.contains("make help"));
        assertTrue(help.contains("make dashboard"));
        assertTrue(help.contains("make run-json"));
        assertFalse(help.contains("java -cp"));
        assertFalse(help.contains("network-address <0xAddr>"));
    }

    @Test
    void testMainMenuUsesCanonicalVocabulary() {
        String menu = EthereumBlockExplorer.buildMainMenuText();
        assertTrue(menu.contains("dashboard"));
        assertTrue(menu.contains("address"));
        assertTrue(menu.contains("more"));
        assertFalse(menu.contains("avg cost"));
        assertFalse(menu.contains("sender groups"));
        assertFalse(menu.contains("reload"));
    }

    @Test
    void testAdvancedMenuHoldsLegacyTools() {
        String menu = EthereumBlockExplorer.buildAdvancedMenuText();
        assertTrue(menu.contains("average cost"));
        assertTrue(menu.contains("transactions by sender"));
        assertTrue(menu.contains("reload dataset"));
    }
}
