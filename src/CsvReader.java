import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC 4180-style CSV record parser used by the Java CLI.
 *
 * The Ethereum exports can contain long calldata and metadata fields. Those
 * fields are data, not delimiters, so plain String.split(",") is not safe for
 * production-grade ingestion. This parser handles quoted fields, escaped quotes,
 * empty cells, and trailing empty cells without pulling in a runtime dependency.
 */
public final class CsvReader {
    private CsvReader() {
        // Utility class.
    }

    public static String[] parseRecord(String record) {
        if (record == null) {
            throw new IllegalArgumentException("CSV record cannot be null");
        }

        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean quotedField = false;

        for (int i = 0; i < record.length(); i++) {
            char current = record.charAt(i);

            if (current == '"') {
                if (inQuotes && i + 1 < record.length() && record.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                    continue;
                }

                if (!inQuotes && field.length() == 0) {
                    quotedField = true;
                    inQuotes = true;
                    continue;
                }

                if (inQuotes) {
                    inQuotes = false;
                    continue;
                }
            }

            if (current == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
                quotedField = false;
                continue;
            }

            if (quotedField || current != '\r') {
                field.append(current);
            }
        }

        if (inQuotes) {
            throw new IllegalArgumentException("Unclosed quoted CSV field");
        }

        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }
}
