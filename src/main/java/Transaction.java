
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Transaction implements Comparable<Transaction>{
	public static final long WEI_PER_ETH = 1_000_000_000_000_000_000L;
	public static final String CONTRACT_CREATION_TO_ADDRESS = "";

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

		String normalizedFrom = fromAdr.trim();
		String normalizedTo = (toAdr == null) ? CONTRACT_CREATION_TO_ADDRESS : toAdr.trim();

		if (!isValidHexAddress(normalizedFrom)) {
			throw new IllegalArgumentException("Invalid from address format: " + normalizedFrom);
		}

		// Contract creation transactions have an empty to_address in this dataset.
		if (!normalizedTo.isEmpty() && !isValidHexAddress(normalizedTo)) {
			throw new IllegalArgumentException("Invalid to address format: " + normalizedTo);
		}
		
		this.blockNumber = number;
		this.index = index;
		this.gasLimit = gasLimit;
		this.gasPrice = gasPrice;
		this.fromAdr = normalizedFrom;
		this.toAdr = normalizedTo;
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

	public boolean isContractCreation() {
		return toAdr == null || toAdr.isEmpty();
	}

	public String getToAddressDisplay() {
		return isContractCreation() ? "(contract creation)" : toAdr;
	}
	
	
	/**
	 * The total cost of the transaction. The gas price multiplied by the gas limit.
	 * @return
	 */
	public double transactionCost() {
		BigDecimal weiCost = BigDecimal.valueOf(gasPrice).multiply(BigDecimal.valueOf(gasLimit));
		BigDecimal ethCost = weiCost.divide(BigDecimal.valueOf(WEI_PER_ETH), 18, RoundingMode.HALF_UP);
		return ethCost.doubleValue();
	}
	
	
	/**
	 * Prints the the transaction index for the associated block along with its block number.
	 */
	@Override
	public String toString() {
		return "Transaction " + index + " for Block " + blockNumber;
	}
	
	
	/**
	 * Compares the indices of two transactions and returns the difference between them.
	 * @param t A transaction outside of the class
	 * @return The difference between the indices of two transactions
	 */
	@Override
	public int compareTo(Transaction t) {
		return Integer.compare(this.getIndex(), t.getIndex());
	}

	private static boolean isValidHexAddress(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.trim();
		if (!trimmed.startsWith("0x") || trimmed.length() != 42) {
			return false;
		}
		for (int i = 2; i < trimmed.length(); i++) {
			if (Character.digit(trimmed.charAt(i), 16) < 0) {
				return false;
			}
		}
		return true;
	}
}
