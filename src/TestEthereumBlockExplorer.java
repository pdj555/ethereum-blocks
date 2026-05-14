import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TestEthereumBlockExplorer {

    @Test
    void testHelpTextStaysMakeFirst() {
        String help = EthereumBlockExplorer.buildHelpText();
        assertTrue(help.contains("Run 'make help' for the full command guide."));
        assertTrue(help.contains("make dashboard"));
        assertTrue(help.contains("make help"));
        assertTrue(help.contains("make verify"));
        assertTrue(help.contains("Commands:"));
        assertTrue(help.contains("Serve the visual explorer at http://localhost:4173"));
        assertTrue(help.contains("make brief"));
        assertTrue(help.contains("make test"));
        assertTrue(help.contains("make cli-smoke"));
        assertTrue(help.contains("make ui-smoke"));
        assertTrue(help.contains("make snapshot"));
        assertTrue(help.contains("vendored JUnit runner"));
        assertFalse(help.contains("Open the visual explorer"));
        assertFalse(help.contains("java -cp"));
        assertFalse(help.contains("curl"));
    }

    @Test
    void testMissingDataErrorUsesActualFilename() {
        String json = EthereumBlockExplorer.buildMissingDataErrorJson("ethereumtransactions1.csv");
        assertEquals(
            "{\"error\": \"data_file_not_found\", \"file\": \"ethereumtransactions1.csv\"}",
            json
        );
        assertEquals(
            "{\"error\": \"data_file_not_found\", \"file\": \"ethereumP1data.csv\"}",
            EthereumBlockExplorer.buildMissingDataErrorJson("ethereumP1data.csv")
        );
        assertEquals(
            "Unexpected error while running 'dashboard'. Re-run the command or see 'make help'.",
            EthereumBlockExplorer.buildUnexpectedCommandError("dashboard")
        );
        assertTrue(EthereumBlockExplorer.isEthereumAddress("0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f"));
        assertFalse(EthereumBlockExplorer.isEthereumAddress("0xzzzzb1a1c67e984247a0c78f2875b0f9c781b64f"));
        assertFalse(EthereumBlockExplorer.isEthereumAddress("0x58a5b1a1c67e984247a0c78f2875b0f9c781b64"));
    }

    @Test
    void testMainMenuStaysUltraLean() {
        String menu = EthereumBlockExplorer.buildMainMenuText();
        assertTrue(menu.contains("dashboard"));
        assertTrue(menu.contains("block"));
        assertTrue(menu.contains("address"));
        assertTrue(menu.contains("network"));
        assertTrue(menu.contains("report"));
        assertTrue(menu.contains("help"));
        assertTrue(menu.contains("address profile"));
        assertFalse(menu.contains("more"));
        assertFalse(menu.contains("advanced"));
        assertFalse(menu.contains("intel"));
        assertFalse(menu.contains("miners"));
        assertFalse(menu.contains("brief"));
        assertFalse(menu.contains("anomalies"));
        assertFalse(menu.contains("compare"));
    }

    @Test
    void testFrontDoorSurfacesStayAligned() throws Exception {
        String cliHelp = normalize(EthereumBlockExplorer.buildHelpText());
        String makeHelp = normalize(runMakeHelp());
        String readme = Files.readString(Path.of("README.md"));
        String improvements = Files.readString(Path.of("IMPROVEMENTS.md"));

        assertEquals(cliHelp, makeHelp);
        assertEquals(extractHelpCommands(cliHelp), extractReadmeCommands(readme));
        assertFalse(readme.contains("java -cp src"));
        assertFalse(readme.contains("javac -cp src"));
        assertFalse(readme.contains("Driver.java"));
        assertFalse(improvements.contains("Driver.java"));
    }

    private static String runMakeHelp() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("make", "help")
            .redirectErrorStream(true)
            .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, output);
        return output;
    }

    private static List<String> extractHelpCommands(String helpText) {
        List<String> commands = new ArrayList<>();
        boolean inCommands = false;

        for (String line : normalize(helpText).split("\n")) {
            if (line.equals("Commands:")) {
                inCommands = true;
                continue;
            }
            if (line.equals("Requirements:")) {
                break;
            }
            if (inCommands && line.startsWith("  make ")) {
                commands.add(line.trim().split("\\s{2,}", 2)[0]);
            }
        }

        return commands;
    }

    private static List<String> extractReadmeCommands(String readmeText) {
        List<String> commands = new ArrayList<>();
        boolean inTable = false;

        for (String line : normalize(readmeText).split("\n")) {
            if (line.equals("## Supported Commands")) {
                inTable = true;
                continue;
            }
            if (inTable && line.startsWith("## ")) {
                break;
            }
            if (inTable && line.startsWith("| `make ")) {
                String[] cells = line.split("\\|");
                commands.add(cells[1].trim().replace("`", ""));
            }
        }

        return commands;
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").trim();
    }
}
