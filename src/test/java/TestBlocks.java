import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestBlocks {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	
	@BeforeEach
	public void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
	}
	
	@Test
	void testAvgTransactionCost() throws FileNotFoundException, IOException {
		Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE, Blocks.DEFAULT_TRANSACTIONS_FILE, LoadOptions.QUIET);
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> b = Blocks.getBlocks();

		BigDecimal actual = new BigDecimal(Double.toString(b.get(0).avgTransactionCost()));
		actual = actual.setScale(8, RoundingMode.HALF_UP);
		double expected = 0.00804665;
		assertEquals(expected, actual.doubleValue());
	}
	
	@Test
	void testGetTransactionsEncapsulation() throws FileNotFoundException, IOException {
		Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE, Blocks.DEFAULT_TRANSACTIONS_FILE, LoadOptions.QUIET);
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> b = Blocks.getBlocks();
		ArrayList<Transaction> t = b.get(0).getTransactions();
		
		t.remove(0);
		
		b = Blocks.getBlocks();
		t = b.get(0).getTransactions();
		
		assertEquals(342, t.size());
		assertEquals(0, t.get(0).getIndex());
		
	}
	
	@Test
	void testConstructorAndReadTransactions() throws FileNotFoundException, IOException {
		Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE, Blocks.DEFAULT_TRANSACTIONS_FILE, LoadOptions.QUIET);
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> blocks = Blocks.getBlocks();
		
		int actual = blocks.get(3).getTransactions().size();
		int expected = blocks.get(3).getTransactionCount();
		assertEquals(expected, actual);
		actual = blocks.get(3).getTransactions().get(0).getIndex();
		expected = 0;
		assertEquals(expected, actual);
		actual = blocks.get(3).getTransactions().get(38).getIndex();
		expected = 38;
		assertEquals(expected, actual);
	}

	@Test
	void testContractCreationTransactionsAreLoaded() throws FileNotFoundException, IOException {
		Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE, Blocks.DEFAULT_TRANSACTIONS_FILE, LoadOptions.QUIET);
		Blocks.sortBlocksByNumber();

		Blocks block = Blocks.getBlockByNumber(15049315);
		assertNotNull(block);

		boolean found = false;
		for (Transaction t : block.getTransactions()) {
			if (t.getIndex() == 8) {
				found = true;
				assertTrue(t.isContractCreation());
				assertEquals("", t.getToAddress());
				break;
			}
		}
		assertTrue(found);

		LoadReport report = Blocks.getLastLoadReport();
		assertNotNull(report);
		assertEquals(4, report.contractCreationsLoaded());
	}

	@Test
	void testSortBlocksByNumberSortsLoadedData() throws IOException {
		Path temp = Files.createTempFile("blocks-unsorted", ".csv");
		try {
			List<String> lines = List.of(
				buildBlockLine(2, "0x2222222222222222222222222222222222222222", 200, 0),
				buildBlockLine(1, "0x1111111111111111111111111111111111111111", 100, 0)
			);
			Files.write(temp, lines);
	
			Blocks.readFile(temp.toString(), Blocks.DEFAULT_TRANSACTIONS_FILE, LoadOptions.QUIET);
			Blocks.sortBlocksByNumber();
	
			ArrayList<Blocks> sorted = Blocks.getBlocks();
			assertEquals(1, sorted.get(0).getNumber());
			assertEquals(2, sorted.get(1).getNumber());
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	private static String buildBlockLine(int number, String miner, long timestamp, int transactionCount) {
		String[] cols = new String[18];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = "0";
		}
		cols[0] = Integer.toString(number);
		cols[9] = miner;
		cols[16] = Long.toString(timestamp);
		cols[17] = Integer.toString(transactionCount);
		return String.join(",", cols);
	}
	
	@AfterEach
	public void tearDown() {
		System.setOut(standardOut);
	}

}
