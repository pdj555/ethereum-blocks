import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestTransaction {

	private final PrintStream standardOut = System.out;
	private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
	
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
		String from = "0x0000000000000000000000000000000000000000";
		String to = "0x1111111111111111111111111111111111111111";
		double cost = 0.0001;
		Transaction t = new Transaction(number, index, gas, price, from, to);
		assertEquals(cost, t.transactionCost(), 1e-12);
	}
	
	@Test
	void testToString() {
		int number = 01234567;
		int index = 0;
		int gas = 5;
		long price = 100;
		String from = "0x89abcdef00000000000000000000000000000000";
		String to = "0xaabb000000000000000000000000000000000000";
		
		Transaction t = new Transaction(number, index, gas, price, from, to);
		System.out.println(t);
		assertTrue(outputStreamCaptor.toString().contains("Transaction " + 0 + " for Block " + 01234567));
	}

	@Test
	void testContractCreationTransaction() {
		Transaction t = new Transaction(
			1,
			0,
			21000,
			1L,
			"0x0000000000000000000000000000000000000000",
			""
		);

		assertTrue(t.isContractCreation());
		assertEquals("", t.getToAddress());
		assertEquals("(contract creation)", t.getToAddressDisplay());
	}
	
	@AfterEach
	public void tearDown() {
		System.setOut(standardOut);
	}
	
	@Test
	void testConstructorAndGetters() {
		int number = 01234567;
		int index = 0;
		int gas = 5;
		long price = 100;
		String from = "0x89abcdef00000000000000000000000000000000";
		String to = "0xaabb000000000000000000000000000000000000";
		
		Transaction t = new Transaction(number, index, gas, price, from, to);
		assertEquals(number, t.getBlockNumber());
		assertEquals(index, t.getIndex());
		assertEquals(gas, t.getGasLimit());
		assertEquals(price, t.getGasPrice());
		assertEquals(from, t.getFromAddress());
		assertEquals(to, t.getToAddress());
	}
	

}
