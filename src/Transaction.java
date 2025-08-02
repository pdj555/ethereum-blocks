
public class Transaction implements Comparable<Transaction>{
	private int blockNumber;
	private int index;
	private int gasLimit;
	private long gasPrice;
	private String fromAdr;
	private String toAdr;
	
	
	
	/**
	 * Constructs a Transaction object and initializes its number, index, gasLimit, gasPrice, fromAdr, and toAdr.
	 * @param number The number that identifies the transaction
	 * @param index The index of the transaction
	 * @param gasLimit The maximum amount of work you're estimating a validator will do on a particular transaction.
	 * @param gasPrice The price per unit of work done
	 * @param fromAdr The original location of the transaction
	 * @param toAdr Where the transaction went from the original location
	 * @throws IllegalArgumentException if any parameter is invalid
	 */
	public Transaction(int number, int index, int gasLimit, long gasPrice, String fromAdr, String toAdr) {
		// Validate inputs
		if (number < 0) {
			throw new IllegalArgumentException("Block number cannot be negative");
		}
		if (index < 0) {
			throw new IllegalArgumentException("Transaction index cannot be negative");
		}
		if (gasLimit < 0) {
			throw new IllegalArgumentException("Gas limit cannot be negative");
		}
		if (gasPrice < 0) {
			throw new IllegalArgumentException("Gas price cannot be negative");
		}
		if (fromAdr == null || fromAdr.trim().isEmpty()) {
			throw new IllegalArgumentException("From address cannot be null or empty");
		}
		if (toAdr == null || toAdr.trim().isEmpty()) {
			throw new IllegalArgumentException("To address cannot be null or empty");
		}
		
		// Validate Ethereum address format (basic check)
		if (!fromAdr.startsWith("0x") || fromAdr.length() != 42) {
			throw new IllegalArgumentException("Invalid from address format. Ethereum addresses should start with '0x' and be 42 characters long");
		}
		if (!toAdr.startsWith("0x") || toAdr.length() != 42) {
			throw new IllegalArgumentException("Invalid to address format. Ethereum addresses should start with '0x' and be 42 characters long");
		}
		
		this.blockNumber = number;
		this.index = index;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.fromAdr = fromAdr;
		this.toAdr = toAdr;
	}
	
	
	/**
	 * Returns the block's number
	 * @return The block number
	 */
	public int getBlockNumber() {
		return blockNumber;
	}
	
	
	/**
	 * Returns the index of the transaction
	 * @return Index of the transaction
	 */
	public int getIndex() {
		return index;
	}
	
	
	/**
	 * Returns the gas limit of the transaction. The gas limit is the maximum amount of work 
	 * you're estimating a validator will do on a particular transaction.
	 * @return The gas limit of the transaction
	 */
	public int getGasLimit() {
		return gasLimit;
	}
	
	
	/**
	 * Returns the gas price of the transaction. The gas price is the price per unit of work done.
	 * @return The gas price of the transaction
	 */
	public long getGasPrice() {
		return gasPrice;
	}
	
	/**
	 * Returns the original address of the transaction.
	 * @return The original address of the transaction.
	 */
	public String getFromAddress() {
		return fromAdr;
	}
	
	/**
	 * Returns the address where the transaction went.
	 * @return The address where the transaction went
	 */
	public String getToAddress() {
		return toAdr;
	}
	
	
	/**
	 * The total cost of the transaction. The gas price multiplied by the gas limit.
	 * @return
	 */
	public double transactionCost() {
		double ethGasPrice = gasPrice / 1e18;
		return gasLimit * ethGasPrice;
	}
	
	
	/**
	 * Prints the the transaction index for the associated block along with its block number.
	 */
	public String toString() {
		return "Transaction " + index + " for Block " + blockNumber;
	}
	
	
	/**
	 * Compares the indices of two transactions and returns the difference between them.
	 * @param t A transaction outside of the class
	 * @return The difference between the indices of two transactions
	 */
	public int compareTo(Transaction t) {
		if (this.getIndex() > t.getIndex()) {
			return 1;
		}
		else if (this.getIndex() == t.getIndex()) {
			return 0;
		}
		else {
			return -1;
		}
	}
}