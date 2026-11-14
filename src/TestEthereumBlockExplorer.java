import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        String message = EthereumBlockExplorer.buildMissingDataMessage("ethereumtransactions1.csv", "/data");
        assertTrue(message.startsWith("Missing dataset file 'ethereumtransactions1.csv' in /data."), message);
        assertTrue(message.contains("--data <directory>"), message);
        assertTrue(message.contains("ETHEREUM_BLOCKS_DATA"), message);
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

        assertEquals(cliHelp, makeHelp);
        List<String> helpCommands = extractHelpCommands(cliHelp);
        List<String> readmeCommands = extractReadmeCommands(readme);
        assertFalse(readmeCommands.isEmpty());
        assertTrue(helpCommands.containsAll(readmeCommands), "README lists a command make help does not: " + readmeCommands);
        assertFalse(readme.contains("java -cp src"));
        assertFalse(readme.contains("javac -cp src"));
        assertFalse(readme.contains("Driver.java"));
    }

    @Test
    void testJsonFlagIsAcceptedAnywhereOnTheLine() {
        assertTrue(EthereumBlockExplorer.hasJsonFlag(new String[] {"anomalies", "5", "--json"}));
        assertFalse(EthereumBlockExplorer.hasJsonFlag(new String[] {"anomalies", "5"}));
        assertArrayEquals(
            new String[] {"anomalies", "5"},
            EthereumBlockExplorer.withoutJsonFlag(new String[] {"anomalies", "5", "--json"}));
        assertArrayEquals(
            new String[] {"anomalies", "5"},
            EthereumBlockExplorer.withoutJsonFlag(new String[] {"--json", "anomalies", "5"}));
        assertArrayEquals(
            new String[] {"anomalies", "5"},
            EthereumBlockExplorer.withoutJsonFlag(new String[] {"anomalies", "--json", "5"}));
    }

    @Test
    void testExitCodeSeparatesSuccessNoAnswerAndRejectedArguments() {
        assertEquals(0, EthereumBlockExplorer.exitCodeFor(Map.of("blocks_loaded", 100)));
        assertEquals(0, EthereumBlockExplorer.exitCodeFor(Map.of("address", "0x1", "status", "no_activity")));
        assertEquals(1, EthereumBlockExplorer.exitCodeFor(Map.of("error", "block_not_found", "block_number", 1)));
        assertEquals(1, EthereumBlockExplorer.exitCodeFor(Map.of("error", "no_data")));
        assertEquals(1, EthereumBlockExplorer.exitCodeFor(AgentAPI.agentSnapshot(new ArrayList<>())));
        assertEquals(2, EthereumBlockExplorer.exitCodeFor(Map.of("error", "invalid_address_format")));
        assertEquals(2, EthereumBlockExplorer.exitCodeFor(Map.of("error", "address_required")));
    }

    @Test
    void testThresholdMustBeAPlainPositiveDecimal() {
        assertEquals(3.5, EthereumBlockExplorer.parseThreshold("3.5"));
        assertEquals(5.0, EthereumBlockExplorer.parseThreshold("5"));
        for (String rejected : new String[] {
            "not-a-number", "NaN", "Infinity", "0", "0.0", "-1", "+3.5", "3.5f", "3.5d", "0x1p3", "1e2", " 5 ", ".5", "5.", ""}) {
            assertNull(EthereumBlockExplorer.parseThreshold(rejected), rejected);
        }
        assertNull(EthereumBlockExplorer.parseThreshold(null));
    }

    @Test
    void testBlockNumberMustBePlainDigits() {
        assertEquals(15049311, EthereumBlockExplorer.parseBlockNumber("15049311"));
        for (String rejected : new String[] {"+5", "-5", "5.0", "0x10", "99999999999", "five", ""}) {
            assertNull(EthereumBlockExplorer.parseBlockNumber(rejected), rejected);
        }
    }

    @Test
    void testCommandLineSeparatesOptionsFromTheCommand() {
        EthereumBlockExplorer.CommandLine line =
            EthereumBlockExplorer.parseCommandLine(new String[] {"--data", "/d", "block", "--json", "7"});
        assertTrue(line.json);
        assertEquals("/d", line.dataDirectory);
        assertArrayEquals(new String[] {"block", "7"}, line.command);
        assertNull(line.usageError);
        assertEquals("/e", EthereumBlockExplorer.parseCommandLine(new String[] {"--data=/e", "miners"}).dataDirectory);
        assertNotNull(EthereumBlockExplorer.parseCommandLine(new String[] {"miners", "--data"}).usageError);
        assertNotNull(EthereumBlockExplorer.parseCommandLine(new String[] {"--jso", "miners"}).usageError);
    }

    @Test
    void testDataDirectoryPrefersOptionThenEnvironmentThenWorkingDirectoryThenClasses(@org.junit.jupiter.api.io.TempDir Path temp) throws IOException {
        Path repo = Files.createDirectories(temp.resolve("repo"));
        Path classes = Files.createDirectories(repo.resolve("bin"));
        Path elsewhere = Files.createDirectories(temp.resolve("elsewhere"));
        Files.writeString(repo.resolve(Blocks.DEFAULT_BLOCKS_FILE), "");

        assertEquals(new java.io.File("/opt"), EthereumBlockExplorer.resolveDataDirectory("/opt", "/env", elsewhere.toFile(), classes.toFile()));
        assertEquals(new java.io.File("/env"), EthereumBlockExplorer.resolveDataDirectory(null, "/env", elsewhere.toFile(), classes.toFile()));
        assertNull(EthereumBlockExplorer.resolveDataDirectory(null, null, repo.toFile(), classes.toFile()));
        assertEquals(repo.toFile(), EthereumBlockExplorer.resolveDataDirectory(null, null, elsewhere.toFile(), classes.toFile()));
        assertNull(EthereumBlockExplorer.resolveDataDirectory(null, null, elsewhere.toFile(), elsewhere.resolve("bin").toFile()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorDocumentHasOneShape() {
        Map<String, Object> document = EthereumBlockExplorer.buildErrorDocument(
            "block_not_found", "No block numbered 1 is in the loaded dataset.", "block", 1, Map.of("block_number", 1));
        assertEquals(List.of("error"), new ArrayList<>(document.keySet()));
        Map<String, Object> error = (Map<String, Object>) document.get("error");
        assertEquals(List.of("code", "message", "command", "exit_code", "block_number"), new ArrayList<>(error.keySet()));
        assertEquals(1, error.get("exit_code"));
        String cause = EthereumBlockExplorer.buildReportWriteError("/x/r.md", new java.nio.file.NoSuchFileException("/x/r.md"));
        assertEquals("Could not write the report to '/x/r.md': NoSuchFileException (/x/r.md).", cause);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCommandGuideNamesEveryCommandAndExitCode() {
        Map<String, Object> guide = EthereumBlockExplorer.buildCommandGuide();
        List<String> names = new ArrayList<>();
        for (Map<String, Object> command : (List<Map<String, Object>>) guide.get("commands")) {
            names.add((String) command.get("name"));
        }
        assertEquals(
            List.of("snapshot", "dashboard", "brief", "block", "address", "network", "anomalies", "miners", "report", "help"),
            names);
        assertEquals(List.of("0", "1", "2", "3", "4"), new ArrayList<>(((Map<String, Object>) guide.get("exit_codes")).keySet()));
        assertTrue(Files.exists(Path.of((String) guide.get("schema"))), "schema file named by the guide is missing");
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
            if (line.equals("## Commands")) {
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
