import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TestCsvReader {

    @Test
    void testParsesQuotedCommasAndEscapedQuotes() {
        String[] fields = CsvReader.parseRecord("alpha,\"bravo, charlie\",\"delta \"\"echo\"\"\",,");

        assertArrayEquals(
            new String[] {"alpha", "bravo, charlie", "delta \"echo\"", "", ""},
            fields
        );
    }

    @Test
    void testRejectsUnclosedQuotedField() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> CsvReader.parseRecord("alpha,\"broken")
        );

        assertTrue(error.getMessage().contains("Unclosed quoted CSV field"));
    }
}
