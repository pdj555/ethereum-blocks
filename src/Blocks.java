import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.text.SimpleDateFormat;


public class Blocks implements Comparable<Blocks> {
	static final String DEFAULT_BLOCKS_FILE = "ethereumP1data.csv";
	static final String DEFAULT_TRANSACTIONS_FILE = "ethereumtransactions1.csv";
	private int number;				// Block number
	private String miner;			// Miner address
	private long timestamp; 		// Unix timestamp
	private int transactionCount;	// Transaction count
	private static ArrayList<Blocks> blocks = null;
	private static Map<Integer, Blocks> blockMap = new HashMap<>();  // For O(1) lookups
	private static final Map<Integer, ArrayList<Transaction>> transactionsByBlock = new HashMap<>();
	private static final ArrayList<String> loadWarnings = new ArrayList<>();
	private static String cachedTransactionsFile = null;
	private static int skippedTransactionRows = 0;
	private static String transactionLoadWarning = null;
	private StringBuilder returnString = new StringBuilder();
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMMM yyyy HH:mm:ss z");
	private Date date;				// date in the format of "dateFormat
	private ArrayList<Transaction> transactions = null;
	
	
	/**
	 * Resets all static state. Required for test isolation and data reload.
	 */
	public static void resetState() {
		blocks = null;
		blockMap.clear();
		transactionsByBlock.clear();
		loadWarnings.clear();
		cachedTransactionsFile = null;
		skippedTransactionRows = 0;
		transactionLoadWarning = null;
	}

	static int getSkippedTransactionRowCount() {
		return skippedTransactionRows;
	}

	static String getTransactionLoadWarning() {
		return transactionLoadWarning;
	}

	static ArrayList<String> getLoadWarnings() {
		return new ArrayList<>(loadWarnings);
	}

	static ArrayList<Transaction> getCachedTransactionsForBlock(int blockNumber) {
		ArrayList<Transaction> cached = transactionsByBlock.get(blockNumber);
		return cached == null ? new ArrayList<>() : new ArrayList<>(cached);
	}

	static void loadTransactionCacheForFile(String filename) throws IOException {
		ensureTransactionCacheLoaded(filename);
	}

	/**
	 * Returns the unix timestamp.
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

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
		readTransactions(DEFAULT_TRANSACTIONS_FILE);
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
	 * Counts the number of unique miner addresses and returns a formatted
	 * frequency breakdown.
	 * @return Human-readable miner frequency report
	 */
	public static String calUniqMiners() throws FileNotFoundException, IOException {
		// if blocks ArrayList has not been read, do so now
		if (blocks == null)
		{
			readFile(DEFAULT_BLOCKS_FILE);
		}
		
		Map<String, Integer> uniqMinersFreq = new LinkedHashMap<>();
		for (Blocks block : blocks) {
			String miner = block.getMiner();
			uniqMinersFreq.put(miner, uniqMinersFreq.getOrDefault(miner, 0) + 1);
		}

		String lineSeparator = System.lineSeparator();
		StringBuilder report = new StringBuilder();
		report.append("Number of unique Miners: ")
			.append(uniqMinersFreq.size())
			.append(lineSeparator)
			.append(lineSeparator)
			.append("Each unique Miner and its frequency:");
		for (Map.Entry<String, Integer> entry : uniqMinersFreq.entrySet()) {
			report.append(lineSeparator)
				.append("Miner Address: ")
				.append(entry.getKey())
				.append(lineSeparator)
				.append("Miner Frequency: ")
				.append(entry.getValue())
				.append(lineSeparator);
		}
		return report.toString().trim();
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
			Blocks.readFile(DEFAULT_BLOCKS_FILE);
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
			throw new FileNotFoundException(filename);
		}
		
		if (!file.canRead()) {
			throw new IOException("Cannot read file: " + filename);
		}

		loadWarnings.clear();
		transactionLoadWarning = null;

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
					// parse each CSV record without letting quoted commas shift columns
					fileData = CsvReader.parseRecord(line.trim());
					
					// Validate data
					if (fileData.length < 18) {
						recordLoadWarning("Warning: Line " + lineNumber + " has insufficient data, skipping");
						continue;
					}

					// fileData[0] corresponds to block number, fileData[9] to miner address
					// fileData[16] corresponds to unix timestamp, fileData[17] corresponds to transaction count
					int blockNumber = Integer.parseInt(fileData[0].trim());
					String minerAddress = fileData[9].trim();
					long timestamp = Long.parseLong(fileData[16].trim());
					int transactionCount = Integer.parseInt(fileData[17].trim());
					
					// Validate parsed data
					if (blockNumber < 0) {
						recordLoadWarning("Warning: Invalid block number at line " + lineNumber + ", skipping");
						continue;
					}
					
					if (timestamp < 0) {
						recordLoadWarning("Warning: Invalid timestamp at line " + lineNumber + ", skipping");
						continue;
					}
					
					if (transactionCount < 0) {
						recordLoadWarning("Warning: Invalid transaction count at line " + lineNumber + ", skipping");
						continue;
					}
					
					b.add(new Blocks(blockNumber, minerAddress, timestamp, transactionCount));
					
				} catch (FileNotFoundException e) {
					throw e;
				} catch (IOException e) {
					throw e;
				} catch (NumberFormatException e) {
					recordLoadWarning("Warning: Invalid number format at line " + lineNumber + ": " + e.getMessage());
					continue;
				} catch (Exception e) {
					recordLoadWarning("Warning: Error processing line " + lineNumber + ": " + e.getMessage());
					continue;
				}
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					recordLoadWarning("Warning: Error closing file: " + e.getMessage());
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
			readFile(DEFAULT_BLOCKS_FILE);
		}
		Collections.sort(blocks);
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
	 * Builds a human-readable description of the time difference between two blocks.
	 * @param first Represents one of the Blocks
	 * @param second Represents the other Block
	 * @return Human-readable time difference
	 */
	public static String timeDiff(Blocks first, Blocks second) {
		//make sure given Blocks aren't null
		if ((first == null) || (second == null)) {
			return "A given Block is null.";
		}

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

		return "The difference in time between Block " + first.getNumber() + " and Block " + second.getNumber() + " is "
				+ diffInHours + hours + diffInMinutes + minutes + diffInSeconds + seconds;
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
			readFile(DEFAULT_BLOCKS_FILE);
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
		// normalize order to keep API easy to use
		if (indexA > indexB) {
			int temp = indexA;
			indexA = indexB;
			indexB = temp;
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
		ensureTransactionCacheLoaded(filename);
		ArrayList<Transaction> blockTransactions = transactionsByBlock.get(this.getNumber());
		if (blockTransactions == null) {
			transactions = new ArrayList<>();
			return;
		}
		transactions = new ArrayList<>(blockTransactions);
	}
	
	
	/**
	 * Computes and returns the average transaction cost of all transactions
	 * @return The average transaction cost
	 */
	public double avgTransactionCost() {
		if (transactions == null || transactions.isEmpty()) {
			return 0.0;
		}
		double totalCost = 0.0;
		int numTrans = transactions.size();
		
		for (Transaction t : transactions) {
			totalCost += t.transactionCost();
		}
		return totalCost/numTrans;
	}
	
	
	/**
	 * Finds every unique from address and keeps track of the Transaction involving that
	 * from address, then returns a formatted summary.
	 * @return Human-readable grouped transaction summary
	 */
	public String uniqFromTo() {
		// Use LinkedHashMap to maintain insertion order (based on first appearance)
		Map<String, ArrayList<Transaction>> fromAddressMap = new LinkedHashMap<>();
		Map<String, Integer> firstAppearance = new HashMap<>();
		String lineSeparator = System.lineSeparator();
		
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
		
		StringBuilder report = new StringBuilder();
		report.append("Each transaction by from address for Block ").append(number);
		
		// Print transactions grouped by from address
		for (String fromAddr : sortedFromAddresses) {
			report.append(lineSeparator).append("From ").append(fromAddr);
			
			double totalCost = 0.0;
			ArrayList<Transaction> transactionsForAddress = fromAddressMap.get(fromAddr);
			
			// Sort transactions by index to maintain order
			transactionsForAddress.sort((a, b) -> Integer.compare(a.getIndex(), b.getIndex()));
			
			for (Transaction t : transactionsForAddress) {
				totalCost += t.transactionCost();
				report.append(lineSeparator).append(" -> ").append(t.getToAddress());
			}
			
			report.append(lineSeparator)
				.append("Total cost of transactions: ")
				.append(String.format("%.8f", totalCost))
				.append(" ETH")
				.append(lineSeparator);
		}

		return report.toString().trim();
	}

	private static void ensureTransactionCacheLoaded(String filename) throws IOException {
		if (filename == null || filename.trim().isEmpty()) {
			throw new IllegalArgumentException("Transaction filename cannot be null or empty");
		}

		if (filename.equals(cachedTransactionsFile) && !transactionsByBlock.isEmpty()) {
			return;
		}

		File file = new File(filename);
		if (!file.exists()) {
			throw new FileNotFoundException(filename);
		}

		skippedTransactionRows = 0;
		transactionLoadWarning = null;
		Map<Integer, TreeMap<Integer, Transaction>> indexedTransactionsByBlock = new HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			int lineNumber = 0;
			int firstSkippedLine = -1;
			String firstSkippedReason = null;
			while ((line = reader.readLine()) != null) {
				lineNumber++;

				try {
					String[] fileData = CsvReader.parseRecord(line.trim());
					if (fileData.length < 10) {
						skippedTransactionRows++;
						if (firstSkippedLine < 0) {
							firstSkippedLine = lineNumber;
							firstSkippedReason = "Expected at least 10 columns";
						}
						continue;
					}

					int tranNumber = Integer.parseInt(fileData[3].trim());
					int tranIndex = Integer.parseInt(fileData[4].trim());
					int tranGasLimit = Integer.parseInt(fileData[8].trim());
					long tranGasPrice = (long) Double.parseDouble(fileData[9].trim());
					String tranFromAdr = fileData[5].trim();
					String tranToAdr = fileData[6].trim();

					Transaction nT = new Transaction(tranNumber, tranIndex, tranGasLimit, tranGasPrice, tranFromAdr, tranToAdr);
					indexedTransactionsByBlock
						.computeIfAbsent(tranNumber, k -> new TreeMap<>())
						.putIfAbsent(tranIndex, nT);
				} catch (RuntimeException e) {
					skippedTransactionRows++;
					if (firstSkippedLine < 0) {
						firstSkippedLine = lineNumber;
						firstSkippedReason = e.getMessage();
					}
				}
			}

			if (skippedTransactionRows > 0) {
				String rowLabel = skippedTransactionRows == 1 ? "row" : "rows";
				transactionLoadWarning = "Warning: Skipped " + skippedTransactionRows + " malformed transaction " + rowLabel
					+ " while loading " + filename + ". First issue at line " + firstSkippedLine + ": " + firstSkippedReason + ".";
				recordLoadWarning(transactionLoadWarning);
			}
		}

		transactionsByBlock.clear();
		for (Map.Entry<Integer, TreeMap<Integer, Transaction>> entry : indexedTransactionsByBlock.entrySet()) {
			transactionsByBlock.put(entry.getKey(), new ArrayList<>(entry.getValue().values()));
		}
		cachedTransactionsFile = filename;
	}

	private static void recordLoadWarning(String warning) {
		loadWarnings.add(warning);
	}

}
