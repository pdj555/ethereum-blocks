import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.TreeSet;
import java.text.SimpleDateFormat;


public class Blocks implements Comparable<Blocks> {
	private int number;				// Block number
	private String miner;			// Miner address
	private long timestamp; 		// Unix timestamp
	private int transactionCount;	// Transaction count
	private static ArrayList<Blocks> blocks = null;
	private static Map<Integer, Blocks> blockMap = new HashMap<>();  // For O(1) lookups
	private StringBuilder returnString = new StringBuilder();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMMM yyyy HH:mm:ss z");
	private Date date;				// date in the format of "dateFormat
	private ArrayList<Transaction> transactions = null;
	
	
	/**
	 * This constructs a Blocks object without initiating the number 
	 * and miner variables.
	 */
	public Blocks() {
		returnString.append("Empty Block");
	}
	
	
	/**
	 * This constructs a Blocks object. It initiates the number variable but
	 * does not initiate the miner variable
	 * @param number The number that identifies which block we are referring to
	 */
	public Blocks(int number) {
		this.number = number;
		returnString.append("Block Number: " + number);
	}
	
	
	/**
	 * This constructs a Blocks object and initiates the variables
	 * number and miner. 
	 * @param number The number that identifies which block we are referring to
	 * @param miner The address of the block
	 */
	public Blocks(int number, String miner) {
		this.number = number;
		this.miner = miner;
		returnString.append("Block Number: " + number + " Miner Address: " + miner);
	}
	
	
	/**
	 * This constructs a Block object and initiates the variables number, miner, timestamp, and transactionsCount.
	 * @param number The number that identifies which block we are referring to
	 * @param miner The address of the block 
	 * @param timestamp The time that the block was added to the chain
	 * @param transactionCount The number that identifies the number of transaction associated with the block
	 */
	public Blocks(int number, String miner, long timestamp, int transactionCount) throws NumberFormatException, IOException, NullPointerException{
		this.number = number;
		this.miner = miner;
		this.timestamp = timestamp;
		this.transactionCount = transactionCount;
		readTransactions("ethereumtransactions1.csv");
		returnString.append("Block Number: " + number + " Miner Address: " + miner);
	}
	
	
	/**
	 * Returns the number associated with a block object.
	 * @return The number used to identify a block
	 */
	public int getNumber() {
		return this.number;
	}
	
	
	/**
	 * Returns the miner associated with a block.
	 * @return The miner address
	 */
	public String getMiner() {
		return this.miner;
	}
	
	
	/**
	 * Returns the number of transactions associated with a block.
	 * @return The number of transactions
	 */
	public int getTransactionCount() {
		return this.transactionCount;
	}
	
	
	/**
	 * Returns a copy of the transactions ArrayList
	 * @return Copy of transactions ArrayList
	 */
	public ArrayList<Transaction> getTransactions() {
		return new ArrayList<>(transactions);
	}
	
	
	
	/**
	 * Creates and returns a copy of the ArrayList blocks
	 * @return A copy of blocks ArrayList
	 */
	public static ArrayList<Blocks> getBlocks() {
		return new ArrayList<>(blocks);
	}

	
	/**
	 * Counts the number of unique miner addresses and prints the frequency that
	 * that each miner address occurs in the data file.
	 */
	public static void calUniqMiners() throws FileNotFoundException, IOException {	
		// if blocks ArrayList has not been read, do so now
		if (blocks == null)
		{
			readFile("ethereumP1data.txt");
		}
		
		// initialize ArrayLists to store addresses and frequencies
		ArrayList<String> uniqMiners = new ArrayList<String>();
		ArrayList<Integer> uniqMinersFreq = new ArrayList<Integer>();
		// holds each miner address
		String miner;
		// loop through all Blocks
		for (int i = 0; i < blocks.size(); ++i)
		{
			miner = blocks.get(i).getMiner();
			// enter if the miner is new
			if (!(uniqMiners.contains(miner)))
			{
				// add the miner and add the frequency of 1
				uniqMiners.add(miner);
				uniqMinersFreq.add(1);
			}
			// otherwise increment the frequency of that miner
			else
			{
				for (int j = 0; j < uniqMiners.size(); ++j)
				{
					if (uniqMiners.get(j).equals(miner))
					{
						uniqMinersFreq.set(j, uniqMinersFreq.get(j) + 1);
					}
				}
			}
		}

		// print according to output
		System.out.println("Number of unique Miners: " + uniqMiners.size() + "\n");
		System.out.println("Each unique Miner and its frequency:");
		for (int i = 0; i < uniqMiners.size(); ++i)
		{
			System.out.println("Miner Address: " + uniqMiners.get(i) + "\nMiner Frequency: " + uniqMinersFreq.get(i) + "\n");
		}
	}
	
	
	/**
	 * Returns the difference between two block numbers.
	 * @param A Block A
	 * @param B Block B
	 * @return The int value of the difference between Block A's number and Block B's number
	 */
	public static int blockDiff(Blocks minuend, Blocks subtrahend) {
		int diff = minuend.getNumber() - subtrahend.getNumber();

		return diff;
	}
	
	
	/**
	 * Returns the block associated with the number used as a parameter.
	 * @param num Number of a block
	 * @return The block with the parameter number
	 */
	public static Blocks getBlockByNumber(int num) throws FileNotFoundException, IOException {
		
		if(blocks == null) {
			Blocks.readFile("ethereumP1data.txt");
		}
		
		// Use HashMap for O(1) lookup
		return blockMap.get(num);
	}
	
	/**
	 * Outputs the information stored on a block.
	 */
	public String toString() {
		return returnString.toString();
	}

	/**
	 * Reads and stores the 1st, 10th, 17th, and 18th columns of a file. 
	 * @param filename File that you want to be read
	 * @throws NumberFormatException 
	 * @throws IOException
	 */
	public static ArrayList<Blocks> readFile(String filename) throws FileNotFoundException, IOException, NullPointerException {
		// Validate input
		if (filename == null || filename.trim().isEmpty()) {
			throw new IllegalArgumentException("Filename cannot be null or empty");
		}
		
		// construct a file object for the file with the given name.
		File file = new File(filename);
		
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + filename);
		}
		
		if (!file.canRead()) {
			throw new IOException("Cannot read file: " + filename);
		}

		// Use BufferedReader for better performance
		BufferedReader reader = null;
		ArrayList<Blocks> b = new ArrayList<Blocks>();
		
		try {
			reader = new BufferedReader(new FileReader(file));

			// create the Array that will store each lines data so we can grab the required fields
			String[] fileData = null;
			String line;
			int lineNumber = 0;

			// Store each line of the file into the ArrayList.
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				
				try {
					// split each line along the commas
					fileData = line.trim().split(",");
					
					// Validate data
					if (fileData.length < 18) {
						System.err.println("Warning: Line " + lineNumber + " has insufficient data, skipping");
						continue;
					}

					// fileData[0] corresponds to block number, fileData[9] to miner address
					// fileData[16] corresponds to unix timestamp, fileData[17] corresponds to transaction count
					int blockNumber = Integer.parseInt(fileData[0]);
					String minerAddress = fileData[9];
					long timestamp = Long.parseLong(fileData[16]);
					int transactionCount = Integer.parseInt(fileData[17]);
					
					// Validate parsed data
					if (blockNumber < 0) {
						System.err.println("Warning: Invalid block number at line " + lineNumber + ", skipping");
						continue;
					}
					
					if (timestamp < 0) {
						System.err.println("Warning: Invalid timestamp at line " + lineNumber + ", skipping");
						continue;
					}
					
					if (transactionCount < 0) {
						System.err.println("Warning: Invalid transaction count at line " + lineNumber + ", skipping");
						continue;
					}
					
					b.add(new Blocks(blockNumber, minerAddress, timestamp, transactionCount));
					
				} catch (NumberFormatException e) {
					System.err.println("Warning: Invalid number format at line " + lineNumber + ": " + e.getMessage());
					continue;
				} catch (Exception e) {
					System.err.println("Warning: Error processing line " + lineNumber + ": " + e.getMessage());
					continue;
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					System.err.println("Warning: Error closing file: " + e.getMessage());
				}
			}
		}

		blocks = new ArrayList<>(b);
		
		// Populate the HashMap for O(1) lookups
		blockMap.clear();
		for (Blocks block : blocks) {
			blockMap.put(block.getNumber(), block);
		}

		return b;
	}
	
	
	/**
	 * This sorts the Blocks by their Block number. 
	 */
	public static void sortBlocksByNumber() throws FileNotFoundException, IOException {
		if (blocks==null) {
			readFile("ethereumP1.txt");
		}
		else {
			Collections.sort(blocks);
		}
	}
	
	
	/**
	 * This compares two blocks. Returns a positive number if this.block is larger than the parameter block.
	 * Returns a negative number if this.block is smaller than the parameter block. Returns a 0 if the two blocks
	 * are equal. 
	 */
	@Override
	public int compareTo(Blocks b) {
		Integer x = number;
		Integer y = b.getNumber();
		return x.compareTo(y);
	}
	
	/**
	 * Returns the data the block was added to the blockchain. The date is converted from 
	 * unix time to the desired format.
	 * @return The date block was added to chain.
	 */
	public String getDate() {
		// initialize date in milliseconds
		date = new Date(timestamp * 1000);
		dateFormat.setTimeZone(TimeZone.getTimeZone("CST"));
		return dateFormat.format(date);
	}
	
	
	/**
	 * This prints out the difference between the timestamps of the two blocks.
	 * @param first Represents one of the Blocks
	 * @param second Represents the other Block
	 */
	public static void timeDiff(Blocks first, Blocks second) {
		//make sure given Blocks aren't null
		if ((first == null) || (second == null)) {
			System.out.println("A given Block is null.");
		}
		else {
			String hours = " hours, ";
			String minutes = " minutes, and ";
			String seconds = " seconds.";
			// use timestamps to find hours, minutes, seconds
			int diffInSeconds = (int) Math.abs(first.timestamp - second.timestamp);
			int diffInMinutes = diffInSeconds / 60;
			int diffInHours = diffInMinutes / 60;
			diffInSeconds = diffInSeconds % 60;
			diffInMinutes = diffInMinutes % 60;
			
			if (diffInHours == 1) {
				hours = " hour, ";
			}
			if (diffInMinutes == 1) {
				minutes = " minute, and ";
			}
			if (diffInSeconds == 1) {
				seconds = " second.";
			}
			

			System.out.println("The difference in time between Block " + first.getNumber() + " and Block " + second.getNumber() + " is "
					+ diffInHours + hours + diffInMinutes + minutes + diffInSeconds + seconds);
		}
	}
	
	
	/**
	 * This returns the difference in transaction number between the two blocks given.
	 * @param first One of the blocks being compared. This will be printed first in the print statement.
	 * @param second The other block being compared. This will be printed second in the print statement.
	 */
	public static int transactionDiff(Blocks first, Blocks second) throws FileNotFoundException, IOException {
		
		// if blocks ArrayList has not been read, do so now and sort it
		if (blocks == null)
		{
			readFile("ethereumP1data.txt");
			sortBlocksByNumber();
		}
		
		// make sure given Blocks aren't null
		if ((first == null) || (second == null)) {
			return -1;
		}
		
		int indexA = -1;		// index of first in blocks ArrayList
		int indexB = -1;		// index of second in blocks ArrayList
		int count = 0;			// number of transactions between the two Blocks
		
		
		// for loop to find indexA and indexB
		for (int i = 0; i < blocks.size(); ++i) {
			if (first.getNumber() == blocks.get(i).getNumber()) {
				indexA = i;
			}
			if (second.getNumber() == blocks.get(i).getNumber()) {
				indexB = i;
			}
		}
		
		// make sure first and second are elements of blocks
		if ((indexA < 0) || (indexB < 0)) {
			return -1;
		}
		// make sure first comes before second
		if (indexA >= indexB) {
			return -1;
		}
		
		// for loop to count the transactions
		for (int i = indexA+1; i < indexB; ++i) {
			count += blocks.get(i).getTransactionCount();
		}
		
		return count;
	}
	
	
	/**
	 * Reads in information from a specifically formated text file. It corresponds the columbs of the text file
	 * to the information we need to interact with transactions. Regarding transactions it obtains the number, index,
	 * gas limit, gas price, from address, and to address.
	 * @param filename The name of the file that contains the specifically formated text with the information we need
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private void readTransactions(String filename) throws FileNotFoundException, IOException, NullPointerException {
		
		
		File file = new File(filename);
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		String[] fileData = null;
		String line;
		
		TreeSet<Transaction> tS = new TreeSet<Transaction>();
		
		while ((line = reader.readLine()) != null) {
			
			fileData = line.trim().split(",");
			
			int tranNumber = Integer.parseInt(fileData[3]);
			int tranIndex = Integer.parseInt(fileData[4]);
			int tranGasLimit = Integer.parseInt(fileData[8]);
			long tranGasPrice = (long)Double.parseDouble(fileData[9]);
			String tranFromAdr = fileData[5];
			String tranToAdr = fileData[6];
			
			try {
				Transaction nT = new Transaction(tranNumber, tranIndex, tranGasLimit, tranGasPrice, tranFromAdr, tranToAdr);
				
				if (nT.getBlockNumber() == this.getNumber()) {
					tS.add(nT);
				}
			} catch (IllegalArgumentException e) {
				// Skip invalid transactions
				System.err.println("Warning: Skipping invalid transaction: " + e.getMessage());
			}
		}
		
		transactions = new ArrayList<>(tS);
		
		reader.close();
		
	}
	
	
	/**
	 * Computes and returns the average transaction cost of all transactions
	 * @return The average transaction cost
	 */
	public double avgTransactionCost() {
		double totalCost = 0.0;
		int numTrans = transactions.size();
		
		for (Transaction t : transactions) {
			totalCost += t.transactionCost();
		}
		return totalCost/numTrans;
	}
	
	
	/**
	 * Finds every unique from address and keeps track of the Transaction involving that from address. It also
	 * outputs the transactions that regard the from address along with the total cost.
	 */
	public void uniqFromTo() {
		// Use LinkedHashMap to maintain insertion order (based on first appearance)
		Map<String, ArrayList<Transaction>> fromAddressMap = new HashMap<>();
		Map<String, Integer> firstAppearance = new HashMap<>();
		
		// Group transactions by from address in O(n) time
		for (int i = 0; i < transactions.size(); i++) {
			Transaction t = transactions.get(i);
			String fromAddr = t.getFromAddress();
			
			// Track first appearance index for ordering
			if (!firstAppearance.containsKey(fromAddr)) {
				firstAppearance.put(fromAddr, i);
			}
			
			// Add transaction to the list for this from address
			fromAddressMap.computeIfAbsent(fromAddr, k -> new ArrayList<>()).add(t);
		}
		
		// Sort from addresses by their first appearance
		ArrayList<String> sortedFromAddresses = new ArrayList<>(fromAddressMap.keySet());
		sortedFromAddresses.sort((a, b) -> firstAppearance.get(a).compareTo(firstAppearance.get(b)));
		
		System.out.println("Each transaction by from address for Block " + number);
		
		// Print transactions grouped by from address
		for (String fromAddr : sortedFromAddresses) {
			System.out.println("From " + fromAddr);
			
			double totalCost = 0.0;
			ArrayList<Transaction> transactionsForAddress = fromAddressMap.get(fromAddr);
			
			// Sort transactions by index to maintain order
			transactionsForAddress.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
			
			for (Transaction t : transactionsForAddress) {
				totalCost += t.transactionCost();
				System.out.println(" -> " + t.getToAddress());
			}
			
			System.out.println("Total cost of transactions: " + String.format("%.8f", totalCost) + " ETH");
			System.out.println();
		}
		
	}

}