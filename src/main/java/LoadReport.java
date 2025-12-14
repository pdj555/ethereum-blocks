import java.util.Locale;

public record LoadReport(
	String blocksFile,
	String transactionsFile,
	int blockRowsRead,
	int blocksLoaded,
	int blocksSkipped,
	int transactionRowsRead,
	int transactionsLoaded,
	int transactionsSkipped,
	int duplicateTransactionIndexes,
	int contractCreationsLoaded,
	int blocksWithTransactions
) {
	public String summary() {
		return String.format(
			Locale.US,
			"Blocks: %d loaded (%d skipped). Transactions: %d loaded across %d blocks (%d skipped, %d duplicate indexes, %d contract creations).",
			blocksLoaded,
			blocksSkipped,
			transactionsLoaded,
			blocksWithTransactions,
			transactionsSkipped,
			duplicateTransactionIndexes,
			contractCreationsLoaded
		);
	}
}
