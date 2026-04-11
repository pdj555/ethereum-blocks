import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestTransaction {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

	// Valid 42-char Ethereum addresses for testing
	private static final String TEST_FROM = "0x89abcdef0123456789abcdef0123456789abcdef";
	private static final String TEST_TO   = "0x1234567890abcdef1234567890abcdef12345678";

	@BeforeEach
	public void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
	}

	@Test
	void testTransactionCost() {
		int number = 1;
		int index = 0;
		int gas = 10000;
		long price = 10000000000L;
		double cost = 0.0001;
		Transaction t = new Transaction(number, index, gas, price, TEST_FROM, TEST_TO);
		assertEquals(cost, t.transactionCost());
	}

	@Test
	void testToString() {
		int number = 1234567;
		int index = 0;
		int gas = 5;
		long price = 100;

		Transaction t = new Transaction(number, index, gas, price, TEST_FROM, TEST_TO);
		System.out.println(t);
		assertTrue(outputStreamCaptor.toString().contains("Transaction 0 for Block 1234567"));
	}

	@AfterEach
	public void tearDown() {
		System.setOut(standardOut);
	}

	@Test
	void testConstructorAndGetters() {
		int number = 1234567;
		int index = 0;
		int gas = 5;
		long price = 100;

		Transaction t = new Transaction(number, index, gas, price, TEST_FROM, TEST_TO);
		assertEquals(number, t.getBlockNumber());
		assertEquals(index, t.getIndex());
		assertEquals(gas, t.getGasLimit());
		assertEquals(price, t.getGasPrice());
		assertEquals(TEST_FROM, t.getFromAddress());
		assertEquals(TEST_TO, t.getToAddress());
		assertFalse(t.isContractCreation());
	}

	@Test
	void testInvalidAddressThrows() {
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, 0, 100, 100L, "0x", "0x"));
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, 0, 100, 100L, "bad", TEST_TO));
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, 0, 100, 100L, TEST_FROM, "bad"));
	}

	@Test
	void testContractCreationTransactionAllowsBlankRecipient() {
		Transaction t = new Transaction(1, 0, 21000, 100L, TEST_FROM, "");
		assertTrue(t.isContractCreation());
		assertEquals(Transaction.CONTRACT_CREATION_RECIPIENT, t.getToAddress());
		assertEquals(true, t.toMap().get("contract_creation"));
	}

	@Test
	void testNegativeValuesThrow() {
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(-1, 0, 100, 100L, TEST_FROM, TEST_TO));
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, -1, 100, 100L, TEST_FROM, TEST_TO));
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, 0, -100, 100L, TEST_FROM, TEST_TO));
		assertThrows(IllegalArgumentException.class, () ->
			new Transaction(1, 0, 100, -100L, TEST_FROM, TEST_TO));
	}

	@Test
	void testCompareTo() {
		Transaction a = new Transaction(1, 5, 100, 100L, TEST_FROM, TEST_TO);
		Transaction b = new Transaction(1, 10, 100, 100L, TEST_FROM, TEST_TO);
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
		assertEquals(0, a.compareTo(a));
	}
}
