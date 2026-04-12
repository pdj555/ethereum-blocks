import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestBlocks {

	private final PrintStream standardOut = System.out;
	private final PrintStream standardErr = System.err;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errorStreamCaptor = new ByteArrayOutputStream();
	
	@BeforeEach
	public void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
		System.setErr(new PrintStream(errorStreamCaptor));
		Blocks.resetState();
	}
	
	@Test
	void testAvgTransactionCost() throws FileNotFoundException, IOException {
		Blocks.readFile("ethereumP1data.csv");
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> b = Blocks.getBlocks();

		BigDecimal actual = new BigDecimal(Double.toString(b.get(0).avgTransactionCost()));
		actual = actual.setScale(8, RoundingMode.HALF_UP);
		double expected = 0.00804665;
		assertEquals(expected, actual.doubleValue());
	}
	
	@Test
	void testGetTransactionsEncapsulation() throws FileNotFoundException, IOException {
		Blocks.readFile("ethereumP1data.csv");
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
		Blocks.readFile("ethereumP1data.csv");
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
	void testContractCreationTransactionsArePreserved() throws FileNotFoundException, IOException {
		Blocks.readFile("ethereumP1data.csv");
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> blocks = Blocks.getBlocks();
		Set<Integer> contractCreationBlocks = new LinkedHashSet<>();
		int contractCreationCount = 0;

		for (Blocks block : blocks) {
			for (Transaction tx : block.getTransactions()) {
				if (tx.isContractCreation()) {
					contractCreationCount++;
					contractCreationBlocks.add(block.getNumber());
				}
			}
		}

		assertEquals(4, contractCreationCount);
		assertEquals(Set.of(15049315, 15049317, 15049318, 15049319), contractCreationBlocks);
	}

	@Test
	void testMalformedTransactionRowsAreReportedWithoutDroppingValidRows() throws IOException {
		String validFrom = "0x89abcdef0123456789abcdef0123456789abcdef";
		String validTo = "0x1234567890abcdef1234567890abcdef12345678";
		Path tempFile = Files.createTempFile("ethereumtransactions-malformed", ".csv");
		Files.write(tempFile, List.of(
			"hash1,1,parent,15049308,0," + validFrom + "," + validTo + ",0,21000,1000000000",
			"hash2,1,parent,15049308,1," + validFrom + ",bad,0,21000,1000000000"
		));

		try {
			Blocks.loadTransactionCacheForFile(tempFile.toString());
		} finally {
			Files.deleteIfExists(tempFile);
		}

		assertEquals(1, Blocks.getCachedTransactionsForBlock(15049308).size());
		assertEquals(1, Blocks.getSkippedTransactionRowCount());
		assertNotNull(Blocks.getTransactionLoadWarning());
		assertTrue(Blocks.getTransactionLoadWarning().contains("line 2"));
		assertTrue(errorStreamCaptor.toString().contains("Skipped 1 malformed transaction row"));
	}

	@Test
	void testReadFileFailsFastWhenTransactionDatasetIsMissing() throws IOException {
		Path transactionFile = Path.of(Blocks.DEFAULT_TRANSACTIONS_FILE);
		Path backupFile = Path.of(Blocks.DEFAULT_TRANSACTIONS_FILE + ".test-backup");
		Files.move(transactionFile, backupFile, StandardCopyOption.REPLACE_EXISTING);

		try {
			FileNotFoundException error = assertThrows(
				FileNotFoundException.class,
				() -> Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE)
			);
			assertEquals(Blocks.DEFAULT_TRANSACTIONS_FILE, error.getMessage());
		} finally {
			Blocks.resetState();
			Files.move(backupFile, transactionFile, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	@AfterEach
	public void tearDown() {
		System.setOut(standardOut);
		System.setErr(standardErr);
	}

}
