import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;


public class Blocks implements Comparable<Blocks> {
	public static final String DEFAULT_BLOCKS_FILE = "ethereumP1data.csv";
	public static final String DEFAULT_TRANSACTIONS_FILE = "ethereumtransactions1.csv";
	
	private int number;				// Block number
	private String miner;			// Miner address
	private long timestamp; 		// Unix timestamp
	private int transactionCount;	// Transaction count
	private static ArrayList<Blocks> blocks = null;
	private static LoadReport lastLoadReport = null;
	private static Map<Integer, Blocks> blockMap = new HashMap<>();  // For O(1) lookups
	private static final Object TRANSACTIONS_CACHE_LOCK = new Object();
	private static String cachedTransactionsFile = null;
	private static Map<Integer, ArrayList<Transaction>> transactionsByBlockCache = null;
	private static TransactionsLoadStats cachedTransactionsStats = null;
	private static String transactionsSourceFile = DEFAULT_TRANSACTIONS_FILE;
	private StringBuilder returnString = new StringBuilder();
	private static final ZoneId DISPLAY_TIME_ZONE = ZoneId.of("UTC");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
		.ofPattern("E, dd MMMM yyyy HH:mm:ss z", Locale.US)
		.withZone(DISPLAY_TIME_ZONE);
	private ArrayList<Transaction> transactions = new ArrayList<>();
	
	
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
		readTransactions(transactionsSourceFile);
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
		if (transactions == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(transactions);
	}
	
	
	
	/**
	 * Creates and returns a copy of the ArrayList blocks
	 * @return A copy of blocks ArrayList
	 */
	public static ArrayList<Blocks> getBlocks() {
		if (blocks == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(blocks);
	}
	
	public static LoadReport getLastLoadReport() {
		return lastLoadReport;
	}

	
	/**
	 * Counts the number of unique miner addresses and prints the frequency that
	 * that each miner address occurs in the data file.
	 */
	public static void calUniqMiners() throws FileNotFoundException, IOException {	
		// if blocks ArrayList has not been read, do so now
		if (blocks == null)
		{
			readFile(DEFAULT_BLOCKS_FILE);
		}
		
		Map<String, Integer> minerFrequencies = new LinkedHashMap<>();
		for (Blocks block : blocks) {
			String miner = block.getMiner();
			minerFrequencies.merge(miner, 1, Integer::sum);
		}

		System.out.println("Number of unique Miners: " + minerFrequencies.size() + "\n");
		System.out.println("Each unique Miner and its frequency:");
		for (Map.Entry<String, Integer> entry : minerFrequencies.entrySet()) {
			System.out.println("Miner Address: " + entry.getKey() + "\nMiner Frequency: " + entry.getValue() + "\n");
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
			Blocks.readFile(DEFAULT_BLOCKS_FILE);
		}
		
		// Use HashMap for O(1) lookup
		return blockMap.get(num);
	}
	
	/**
	 * Outputs the information stored on a block.
	 */
	@Override
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
		return readFile(filename, DEFAULT_TRANSACTIONS_FILE, LoadOptions.DEFAULT);
	}
	
	/**
	 * Reads blocks from a CSV file and loads transactions from a separate CSV file.
	 * @param blocksFilename Block metadata CSV (e.g., {@value #DEFAULT_BLOCKS_FILE})
	 * @param transactionsFilename Transaction CSV (e.g., {@value #DEFAULT_TRANSACTIONS_FILE})
	 */
	public static ArrayList<Blocks> readFile(String blocksFilename, String transactionsFilename) throws FileNotFoundException, IOException, NullPointerException {
		return readFile(blocksFilename, transactionsFilename, LoadOptions.DEFAULT);
	}
	
	public static ArrayList<Blocks> readFile(String blocksFilename, String transactionsFilename, LoadOptions options) throws FileNotFoundException, IOException, NullPointerException {
		// Validate input
		if (blocksFilename == null || blocksFilename.trim().isEmpty()) {
			throw new IllegalArgumentException("Blocks filename cannot be null or empty");
		}
		if (transactionsFilename == null || transactionsFilename.trim().isEmpty()) {
			throw new IllegalArgumentException("Transactions filename cannot be null or empty");
		}
		
		// Fail fast if the transactions file is missing/unreadable.
		File transactionsFile = new File(transactionsFilename);
		if (!transactionsFile.exists()) {
			throw new FileNotFoundException("File not found: " + transactionsFilename);
		}
		if (!transactionsFile.canRead()) {
			throw new IOException("Cannot read file: " + transactionsFilename);
		}
		
		synchronized (TRANSACTIONS_CACHE_LOCK) {
				transactionsSourceFile = transactionsFilename;
				if (transactionsByBlockCache != null && !transactionsFilename.equals(cachedTransactionsFile)) {
					transactionsByBlockCache = null;
					cachedTransactionsFile = null;
					cachedTransactionsStats = null;
				}
			}
		
		LoadOptions effectiveOptions = (options == null) ? LoadOptions.DEFAULT : options;
		
		// Load transactions once so block construction is O(1) per block.
		getOrLoadTransactionsByBlock(transactionsFilename, effectiveOptions);
		
		// construct a file object for the file with the given name.
		File file = new File(blocksFilename);
		
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + blocksFilename);
		}
		
		if (!file.canRead()) {
			throw new IOException("Cannot read file: " + blocksFilename);
		}

		ArrayList<Blocks> loadedBlocks = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			int lineNumber = 0;
			int rowsRead = 0;
			int rowsSkipped = 0;

			// Store each line of the file into the ArrayList.
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				
				if (line.isBlank()) {
					continue;
				}
				rowsRead++;
				
				try {
					// split each line along the commas
					String[] fileData = line.trim().split(",");
					
					// Validate data
					if (fileData.length < 18) {
						rowsSkipped++;
						if (effectiveOptions.printWarnings()) {
							System.err.println("Warning: Line " + lineNumber + " has insufficient data, skipping");
						}
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
						rowsSkipped++;
						if (effectiveOptions.printWarnings()) {
							System.err.println("Warning: Invalid block number at line " + lineNumber + ", skipping");
						}
						continue;
					}
					
					if (timestamp < 0) {
						rowsSkipped++;
						if (effectiveOptions.printWarnings()) {
							System.err.println("Warning: Invalid timestamp at line " + lineNumber + ", skipping");
						}
						continue;
					}
					
					if (transactionCount < 0) {
						rowsSkipped++;
						if (effectiveOptions.printWarnings()) {
							System.err.println("Warning: Invalid transaction count at line " + lineNumber + ", skipping");
						}
						continue;
					}
					
					loadedBlocks.add(new Blocks(blockNumber, minerAddress, timestamp, transactionCount));
					
				} catch (NumberFormatException e) {
					rowsSkipped++;
					if (effectiveOptions.printWarnings()) {
						System.err.println("Warning: Invalid number format at line " + lineNumber + ": " + e.getMessage());
					}
				} catch (Exception e) {
					rowsSkipped++;
					if (effectiveOptions.printWarnings()) {
						System.err.println("Warning: Error processing line " + lineNumber + ": " + e.getMessage());
					}
				}
			}
			
			TransactionsLoadStats txStats = cachedTransactionsStats;
			if (txStats == null) {
				Map<Integer, ArrayList<Transaction>> byBlock = getOrLoadTransactionsByBlock(transactionsFilename, effectiveOptions);
				txStats = TransactionsLoadStats.fromLoaded(byBlock);
				cachedTransactionsStats = txStats;
			}
			
			lastLoadReport = new LoadReport(
				blocksFilename,
				transactionsFilename,
				rowsRead,
				loadedBlocks.size(),
				rowsSkipped,
				txStats.rowsRead(),
				txStats.transactionsLoaded(),
				txStats.rowsSkipped(),
				txStats.duplicateIndexes(),
				txStats.contractCreations(),
				txStats.blocksWithTransactions()
			);
		}

		blocks = new ArrayList<>(loadedBlocks);
		
		// Populate the HashMap for O(1) lookups
		blockMap.clear();
		for (Blocks block : blocks) {
			blockMap.put(block.getNumber(), block);
		}

		return loadedBlocks;
	}
	
	
	/**
	 * This sorts the Blocks by their Block number. 
	 */
	public static void sortBlocksByNumber() throws FileNotFoundException, IOException {
		if (blocks == null) {
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
		return DATE_FORMAT.format(Instant.ofEpochSecond(timestamp));
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
		
		int startIndex = Math.min(indexA, indexB);
		int endIndex = Math.max(indexA, indexB);
		if (startIndex == endIndex) {
			return 0;
		}
		
		// for loop to count the transactions
		for (int i = startIndex + 1; i < endIndex; ++i) {
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
	private static Map<Integer, ArrayList<Transaction>> getOrLoadTransactionsByBlock(String filename) throws IOException {
		return getOrLoadTransactionsByBlock(filename, LoadOptions.DEFAULT);
	}
	
	private static Map<Integer, ArrayList<Transaction>> getOrLoadTransactionsByBlock(String filename, LoadOptions options) throws IOException {
		synchronized (TRANSACTIONS_CACHE_LOCK) {
			if (transactionsByBlockCache != null && filename != null && filename.equals(cachedTransactionsFile)) {
				if (cachedTransactionsStats == null) {
					cachedTransactionsStats = TransactionsLoadStats.fromLoaded(transactionsByBlockCache);
				}
				return transactionsByBlockCache;
			}
			
			LoadOptions effectiveOptions = (options == null) ? LoadOptions.DEFAULT : options;
				TransactionsLoadResult loaded = loadTransactionsByBlock(filename, effectiveOptions);
				transactionsByBlockCache = loaded.transactionsByBlock();
				cachedTransactionsFile = filename;
				cachedTransactionsStats = loaded.stats();
				return transactionsByBlockCache;
			}
		}
	
	private static long parseGasPriceWei(String gasPrice) {
		if (gasPrice == null) {
			throw new NumberFormatException("Gas price is null");
		}
		
		String value = gasPrice.trim();
		if (value.isEmpty()) {
			throw new NumberFormatException("Gas price is empty");
		}
		
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			try {
				return new BigDecimal(value).longValueExact();
			} catch (ArithmeticException ex) {
				NumberFormatException wrapped = new NumberFormatException("Gas price is not an integer: " + value);
				wrapped.addSuppressed(ex);
				throw wrapped;
			}
		}
	}
	
	private static TransactionsLoadResult loadTransactionsByBlock(String filename, LoadOptions options) throws IOException {
		if (filename == null || filename.trim().isEmpty()) {
			throw new IllegalArgumentException("Transactions filename cannot be null or empty");
		}
		
		File file = new File(filename);
		
		if (!file.exists()) {
			throw new FileNotFoundException("File not found: " + filename);
		}
		
		if (!file.canRead()) {
			throw new IOException("Cannot read file: " + filename);
		}
		
		LoadOptions effectiveOptions = (options == null) ? LoadOptions.DEFAULT : options;
		Map<Integer, TreeSet<Transaction>> transactionsByBlock = new HashMap<>();
		int rowsRead = 0;
		int rowsSkipped = 0;
		int duplicateIndexes = 0;
		int contractCreations = 0;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			int lineNumber = 0;
			
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				
				if (line.isBlank()) {
					continue;
				}
				rowsRead++;
				
				String[] fileData = line.trim().split(",");
				
				// We read columns 3,4,5,6,8,9 (0-based indexing)
				if (fileData.length <= 9) {
					rowsSkipped++;
					if (effectiveOptions.printWarnings()) {
						System.err.println("Warning: Transaction line " + lineNumber + " has insufficient data, skipping");
					}
					continue;
				}
				
				try {
					int tranNumber = Integer.parseInt(fileData[3]);
					int tranIndex = Integer.parseInt(fileData[4]);
					int tranGasLimit = Integer.parseInt(fileData[8]);
					long tranGasPrice = parseGasPriceWei(fileData[9]);
					String tranFromAdr = fileData[5];
					String tranToAdr = fileData[6];
					
					Transaction transaction = new Transaction(tranNumber, tranIndex, tranGasLimit, tranGasPrice, tranFromAdr, tranToAdr);

					TreeSet<Transaction> blockTransactions = transactionsByBlock.computeIfAbsent(tranNumber, k -> new TreeSet<>());
					boolean added = blockTransactions.add(transaction);
					if (!added) {
						duplicateIndexes++;
					} else if (transaction.isContractCreation()) {
						contractCreations++;
					}
				} catch (IllegalArgumentException e) {
					rowsSkipped++;
					if (effectiveOptions.printWarnings()) {
						System.err.println("Warning: Skipping invalid transaction at line " + lineNumber + ": " + e.getMessage());
					}
				} catch (Exception e) {
					rowsSkipped++;
					if (effectiveOptions.printWarnings()) {
						System.err.println("Warning: Error processing transaction line " + lineNumber + ": " + e.getMessage());
					}
				}
			}
		}
		
		Map<Integer, ArrayList<Transaction>> result = new HashMap<>();
		for (Map.Entry<Integer, TreeSet<Transaction>> entry : transactionsByBlock.entrySet()) {
			result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}

		TransactionsLoadStats stats = new TransactionsLoadStats(
			rowsRead,
			loadedTransactionCount(result),
			rowsSkipped,
			duplicateIndexes,
			contractCreations,
			result.size()
		);
		
		return new TransactionsLoadResult(result, stats);
	}
	
	private void readTransactions(String filename) throws FileNotFoundException, IOException, NullPointerException {
		Map<Integer, ArrayList<Transaction>> byBlock = getOrLoadTransactionsByBlock(filename);
		ArrayList<Transaction> forBlock = byBlock.get(this.getNumber());
		transactions = (forBlock == null) ? new ArrayList<>() : new ArrayList<>(forBlock);
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
		
		for (Transaction t : transactions) {
			totalCost += t.transactionCost();
		}
		return totalCost / transactions.size();
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
			
			System.out.println("Total cost of transactions: " + String.format(Locale.US, "%.8f", totalCost) + " ETH");
			System.out.println();
		}
		
	}

	private static int loadedTransactionCount(Map<Integer, ArrayList<Transaction>> transactionsByBlock) {
		if (transactionsByBlock == null || transactionsByBlock.isEmpty()) {
			return 0;
		}
		int total = 0;
		for (ArrayList<Transaction> txs : transactionsByBlock.values()) {
			if (txs != null) {
				total += txs.size();
			}
		}
		return total;
	}
	
	private record TransactionsLoadResult(Map<Integer, ArrayList<Transaction>> transactionsByBlock, TransactionsLoadStats stats) {}
	
	private record TransactionsLoadStats(
		int rowsRead,
		int transactionsLoaded,
		int rowsSkipped,
		int duplicateIndexes,
		int contractCreations,
		int blocksWithTransactions
	) {
		private static TransactionsLoadStats fromLoaded(Map<Integer, ArrayList<Transaction>> transactionsByBlock) {
			int loaded = loadedTransactionCount(transactionsByBlock);
			int contractCreations = 0;
			int blocksWithTransactions = (transactionsByBlock == null) ? 0 : transactionsByBlock.size();
			if (transactionsByBlock != null) {
				for (ArrayList<Transaction> txs : transactionsByBlock.values()) {
					if (txs == null) {
						continue;
					}
					for (Transaction t : txs) {
						if (t != null && t.isContractCreation()) {
							contractCreations++;
						}
					}
				}
			}
			return new TransactionsLoadStats(0, loaded, 0, 0, contractCreations, blocksWithTransactions);
		}
	}

}
