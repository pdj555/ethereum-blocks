import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestBlocks {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	
	@BeforeEach
	public void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
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
	
	@AfterEach
	public void tearDown() {
		System.setOut(standardOut);
	}

}
