import java.util.regex.Pattern;

public final class EthereumAddressValidator {
	private static final Pattern ETHEREUM_ADDRESS = Pattern.compile("^0x[0-9a-fA-F]{40}$");

	private EthereumAddressValidator() {
	}

	public static boolean isValid(String value) {
		return value != null && ETHEREUM_ADDRESS.matcher(value).matches();
	}
}
