import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Driver {
	public static void main (String[] args) throws FileNotFoundException, IOException {
		
		// read file and initialize transactions to each Block
		Blocks.readFile("ethereumP1data.csv");
		Blocks.sortBlocksByNumber();
		ArrayList<Blocks> blocks = Blocks.getBlocks();
		
		
		// print out each transaction for Block 15049311
		Iterator<Transaction> i = blocks.get(3).getTransactions().iterator();
		while (i.hasNext()) {
			System.out.println(i.next());
		}
		
		System.out.println();
		
		// print out the average transaction cost for Blocks
		Blocks b = Blocks.getBlockByNumber(15049311);
		Blocks c = Blocks.getBlockByNumber(15049321);

		System.out.printf("The average transaction cost for Block 15049311 is %.8f ETH\n", b.avgTransactionCost());
		System.out.printf("The average transaction cost for Block 15049321 is %.8f ETH\n", c.avgTransactionCost());

		System.out.println();
		
		// print out the transactions for Block 15049311
		// based on the given output on GitHub
		b.uniqFromTo();
	}
}
