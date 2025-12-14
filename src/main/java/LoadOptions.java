public final class LoadOptions {
	public static final LoadOptions DEFAULT = new LoadOptions(true);
	public static final LoadOptions QUIET = new LoadOptions(false);

	private final boolean printWarnings;

	public LoadOptions(boolean printWarnings) {
		this.printWarnings = printWarnings;
	}

	public boolean printWarnings() {
		return printWarnings;
	}
}
