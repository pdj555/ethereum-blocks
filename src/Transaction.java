
/**
 * Represents a transaction in the Ethereum blockchain with comprehensive validation.
 * 
 * <p>This class models an Ethereum transaction with the following key features:
 * <ul>
 *   <li>Complete transaction metadata (block number, index, gas details, addresses)</li>
 *   <li>Strict input validation to ensure data integrity</li>
 *   <li>Ethereum address format validation (0x prefix, 42 characters)</li>
 *   <li>Gas cost calculation with automatic wei-to-ETH conversion</li>
 *   <li>Natural ordering by transaction index</li>
 * </ul>
 * 
 * <p>Gas Economics:
 * <ul>
 *   <li><b>Gas Limit:</b> Maximum computational work allocated for this transaction</li>
 *   <li><b>Gas Price:</b> Price per unit of computational work (in wei)</li>
 *   <li><b>Transaction Cost:</b> Gas Limit × Gas Price (converted to ETH)</li>
 *   <li><b>Conversion:</b> 1 ETH = 10^18 wei</li>
 * </ul>
 * 
 * <p>Validation Rules:
 * <ul>
 *   <li>Block number must be non-negative</li>
 *   <li>Transaction index must be non-negative</li>
 *   <li>Gas limit must be non-negative</li>
 *   <li>Gas price must be non-negative</li>
 *   <li>Addresses must be non-null, non-empty, start with "0x", and be 42 characters</li>
 * </ul>
 * 
 * <p>This class is immutable after construction and thread-safe.
 * 
 * @author Ethereum Block Explorer Team
 * @version 2.0.0
 * @since 1.0.0
 * @see Blocks
 * @see Comparable
 */
public class Transaction implements Comparable<Transaction>{
	private int blockNumber;
	private int index;
	private int gasLimit;
	private long gasPrice;
	private String fromAdr;
	private String toAdr;
	
	
	
	/**
	 * Constructs a Transaction object with complete validation of all parameters.
	 * 
	 * <p>This constructor performs comprehensive validation including:
	 * <ul>
	 *   <li>Non-negative validation for numeric fields</li>
	 *   <li>Non-null and non-empty validation for from address</li>
	 *   <li>Ethereum address format validation for from address (0x prefix, 42 characters)</li>
	 *   <li>Optional to address validation (can be empty for contract creation transactions)</li>
	 * </ul>
	 * 
	 * <p><b>Note on Contract Creation:</b> In Ethereum, contract creation transactions have
	 * an empty or null "to" address. This constructor accepts empty/null to addresses to
	 * support this use case.
	 * 
	 * <p>Example usage:
	 * <pre>
	 * // Regular transaction
	 * Transaction tx = new Transaction(
	 *     15049311,                                          // block number
	 *     0,                                                  // transaction index
	 *     21000,                                              // gas limit
	 *     50000000000L,                                       // gas price in wei
	 *     "0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f",      // from address
	 *     "0x0cec1a9154ff802e7934fc916ed7ca50bde6844e"       // to address
	 * );
	 * 
	 * // Contract creation transaction
	 * Transaction contractTx = new Transaction(
	 *     15049311,                                          // block number
	 *     5,                                                  // transaction index
	 *     500000,                                             // gas limit
	 *     50000000000L,                                       // gas price in wei
	 *     "0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f",      // from address
	 *     ""                                                  // empty for contract creation
	 * );
	 * </pre>
	 * 
	 * @param number The block number containing this transaction (must be &gt;= 0)
	 * @param index The position of this transaction within the block (must be &gt;= 0)
	 * @param gasLimit The maximum computational work allocated (must be &gt;= 0)
	 * @param gasPrice The price per unit of work in wei (must be &gt;= 0)
	 * @param fromAdr The sender's Ethereum address (42 chars, starts with "0x")
	 * @param toAdr The recipient's Ethereum address (42 chars starting with "0x", or empty for contract creation)
	 * @throws IllegalArgumentException if any parameter fails validation
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
		
		// Validate Ethereum address format (basic check)
		if (!fromAdr.startsWith("0x") || fromAdr.length() != 42) {
			throw new IllegalArgumentException("Invalid from address format. Ethereum addresses should start with '0x' and be 42 characters long");
		}
		
		// Note: toAdr can be null or empty for contract creation transactions
		// Only validate format if toAdr is provided
		if (toAdr != null && !toAdr.trim().isEmpty()) {
			if (!toAdr.startsWith("0x") || toAdr.length() != 42) {
				throw new IllegalArgumentException("Invalid to address format. Ethereum addresses should start with '0x' and be 42 characters long");
			}
		} else {
			// Set to empty string for consistency if null
			toAdr = "";
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
	 * Calculates the total cost of this transaction in ETH.
	 * 
	 * <p>The transaction cost is computed as: Gas Limit × Gas Price.
	 * The gas price is automatically converted from wei to ETH using the conversion rate:
	 * 1 ETH = 10^18 wei.
	 * 
	 * <p>Formula: cost (ETH) = gasLimit × (gasPrice / 10^18)
	 * 
	 * <p>Example:
	 * <pre>
	 * Transaction tx = new Transaction(15049311, 0, 21000, 50000000000L, "0x...", "0x...");
	 * double cost = tx.transactionCost();  // Returns: 0.00000105 ETH
	 * </pre>
	 * 
	 * @return The total transaction cost in ETH as a double
	 */
	public double transactionCost() {
		double ethGasPrice = gasPrice / 1e18;
		return gasLimit * ethGasPrice;
	}
	
	
	/**
	 * Returns a string representation of this transaction.
	 * 
	 * <p>Format: "Transaction {index} for Block {blockNumber}"
	 * 
	 * <p>Example output: "Transaction 0 for Block 15049311"
	 * 
	 * @return A formatted string identifying this transaction
	 */
	public String toString() {
		return "Transaction " + index + " for Block " + blockNumber;
	}
	
	
	/**
	 * Compares this transaction with another based on their indices.
	 * 
	 * <p>This method implements the natural ordering of transactions by their index
	 * within a block. This is required by the {@link Comparable} interface and allows
	 * transactions to be sorted automatically in collections.
	 * 
	 * <p>Comparison rules:
	 * <ul>
	 *   <li>Returns positive if this transaction's index &gt; other transaction's index</li>
	 *   <li>Returns zero if indices are equal</li>
	 *   <li>Returns negative if this transaction's index &lt; other transaction's index</li>
	 * </ul>
	 * 
	 * @param t The transaction to compare with
	 * @return A negative integer, zero, or positive integer as this transaction's
	 *         index is less than, equal to, or greater than the specified transaction's index
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