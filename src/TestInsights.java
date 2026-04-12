import static org.junit.jupiter.api.Assertions.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TestInsights {

	private static ArrayList<Blocks> blocks;

	@BeforeAll
	static void setUp() throws FileNotFoundException, IOException {
		Blocks.resetState();
		Blocks.readFile("ethereumP1data.csv");
		Blocks.sortBlocksByNumber();
		blocks = Blocks.getBlocks();
	}

	@Test
	void testBuildAddressIntelRejectsNonHexAddress() {
		String result = Insights.buildAddressIntel(blocks, "0x" + "z".repeat(40), 5);
		assertTrue(result.contains("Invalid Ethereum address."));
	}
}
