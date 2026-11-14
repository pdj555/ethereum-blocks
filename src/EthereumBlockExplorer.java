import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main application for exploring Ethereum blockchain data.
 *
 * Supports four modes:
 *   1. Browser mode via `make ui` - static explorer built from the CSV sample
 *   2. Interactive (human) mode - menu-driven terminal UI via `make run`
 *   3. Command mode - single-shot CLI commands for scripts and make targets
 *   4. JSON mode (--json) - structured output for agents and pipelines
 *
 * The supported product surface is the browser explorer plus the make-first CLI.
 * JSON mode remains the machine-readable path for automation.
 */
public class EthereumBlockExplorer {
    private static Scanner scanner = new Scanner(System.in);
    private static ArrayList<Blocks> blocks = null;
    private static boolean jsonMode = false;

    /** The command succeeded. */
    static final int EXIT_OK = 0;
    /** The command ran but has no answer: unknown block, or a dataset with no blocks. */
    static final int EXIT_FAILED = 1;
    /** The command line or its inputs were rejected, including a dataset or report path that cannot be used. */
    static final int EXIT_USAGE = 2;
    /** A fault in this program. */
    static final int EXIT_INTERNAL = 3;
    /** Stdout could not be written. */
    static final int EXIT_OUTPUT = 4;

    /** Environment variable naming the directory that holds the two dataset CSVs. */
    static final String DATA_DIR_ENV = "ETHEREUM_BLOCKS_DATA";

    private static String currentCommand = null;

    public static void main(String[] args) {
        // Same bytes on every machine: numbers and dates never follow the host
        // locale, and text is UTF-8 whatever the platform default encoding is.
        Locale.setDefault(Locale.ROOT);
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        int exitCode = run(args);
        scanner.close();
        // PrintStream never throws; a closed pipe or a full device only shows up here.
        System.out.flush();
        if (System.out.checkError()) {
            System.err.println("Error: could not write to stdout.");
            exitCode = EXIT_OUTPUT;
        }
        System.exit(exitCode);
    }

    /** A command line split into its global options and the command with its arguments. */
    static final class CommandLine {
        final boolean json;
        final String dataDirectory;
        final String[] command;
        final String usageError;

        CommandLine(boolean json, String dataDirectory, String[] command, String usageError) {
            this.json = json;
            this.dataDirectory = dataDirectory;
            this.command = command;
            this.usageError = usageError;
        }
    }

    /**
     * Reads --json and --data wherever they are written. Any other word that
     * starts with "--" is rejected rather than passed on as an argument.
     */
    static CommandLine parseCommandLine(String[] args) {
        boolean json = false;
        String dataDirectory = null;
        String usageError = null;
        List<String> command = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--json")) {
                json = true;
            } else if (arg.equals("--data")) {
                if (i + 1 >= args.length) {
                    usageError = usageError != null ? usageError : "Expected a directory after --data.";
                } else {
                    dataDirectory = args[++i];
                }
            } else if (arg.startsWith("--data=")) {
                dataDirectory = arg.substring("--data=".length());
                if (dataDirectory.isBlank()) {
                    usageError = usageError != null ? usageError : "Expected a directory after --data=.";
                }
            } else if (arg.equals("--help") || arg.equals("-h")) {
                command.add(0, "help");
            } else if (arg.startsWith("--")) {
                usageError = usageError != null ? usageError : "Unknown option '" + arg + "'. The options are --json and --data <directory>.";
            } else {
                command.add(arg);
            }
        }
        return new CommandLine(json, dataDirectory, command.toArray(new String[0]), usageError);
    }

    /**
     * Where the dataset is read from: --data, then the ETHEREUM_BLOCKS_DATA
     * variable, then the working directory if the blocks file is there, then
     * the directory above the compiled classes (the repository root when run
     * as `java -cp bin`). Returns null to mean the working directory.
     */
    static File resolveDataDirectory(String option, String environment, File workingDirectory, File classLocation) {
        if (option != null && !option.isBlank()) {
            return new File(option);
        }
        if (environment != null && !environment.isBlank()) {
            return new File(environment);
        }
        if (new File(workingDirectory, Blocks.DEFAULT_BLOCKS_FILE).exists()) {
            return null;
        }
        File besideClasses = classLocation == null ? null : classLocation.getAbsoluteFile().getParentFile();
        if (besideClasses != null && new File(besideClasses, Blocks.DEFAULT_BLOCKS_FILE).exists()) {
            return besideClasses;
        }
        return null;
    }

    private static File classLocation() {
        try {
            return new File(EthereumBlockExplorer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            return null;
        }
    }

    private static int run(String[] args) {
        CommandLine line = parseCommandLine(args);
        jsonMode = line.json;
        String[] effectiveArgs = line.command;
        currentCommand = effectiveArgs.length > 0 ? effectiveArgs[0].toLowerCase(Locale.ROOT) : null;

        if (line.usageError != null) {
            return outputError("invalid_argument", line.usageError, EXIT_USAGE);
        }

        if ("help".equals(currentCommand)) {
            if (effectiveArgs.length > 1) {
                return outputError("invalid_argument", "help takes no argument.", EXIT_USAGE);
            }
            // Describes the CLI itself, so it must answer even when the dataset is missing.
            if (jsonMode) {
                System.out.println(JsonWriter.toJson(buildCommandGuide()));
            } else {
                printHelp();
            }
            return EXIT_OK;
        }

        if (effectiveArgs.length == 0 && !jsonMode && System.console() == null && !scanner.hasNextLine()) {
            // The menu needs someone to answer it; with stdin at its end there is no one.
            return outputError(
                "invalid_argument",
                "No command given and nothing to read on stdin. Name a command (see 'make help'), or run in a terminal for the menu.",
                EXIT_USAGE);
        }

        Blocks.setDataDirectory(resolveDataDirectory(
            line.dataDirectory, System.getenv(DATA_DIR_ENV), new File("").getAbsoluteFile(), classLocation()));
        int loadExit = loadData();
        if (loadExit != EXIT_OK) {
            return loadExit;
        }

        if (effectiveArgs.length > 0) {
            return runCommandMode(effectiveArgs);
        }

        if (jsonMode) {
            // No command + json mode = print overview
            currentCommand = "dashboard";
            return outputResult(AgentAPI.overview(blocks, 10));
        }

        runInteractiveMode();
        return EXIT_OK;
    }

    /** The command and its arguments, wherever on the line --json was written. */
    static String[] withoutJsonFlag(String[] args) {
        return parseCommandLine(args).command;
    }

    static boolean hasJsonFlag(String[] args) {
        return parseCommandLine(args).json;
    }

    /**
     * Exit code for a result document. The API reports failure with an "error" key:
     * a rejected argument is a usage error, anything else means the command ran
     * and found no answer.
     */
    static int exitCodeFor(Map<String, Object> result) {
        Object error = result == null ? null : result.get("error");
        if (error == null) {
            // The snapshot nests its failure: no top-level error, status "unavailable".
            return result != null && "unavailable".equals(result.get("status")) ? EXIT_FAILED : EXIT_OK;
        }
        if ("invalid_address_format".equals(error) || "address_required".equals(error)) {
            return EXIT_USAGE;
        }
        return EXIT_FAILED;
    }

    private static final java.util.regex.Pattern PLAIN_DECIMAL = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]+)?");
    private static final java.util.regex.Pattern PLAIN_INTEGER = java.util.regex.Pattern.compile("[0-9]+");

    /**
     * A z-score threshold is a plain decimal number above zero: digits with an
     * optional fraction. Java's own parser also takes "NaN", "0x1p3" and "3.5f",
     * none of which a caller means.
     */
    static Double parseThreshold(String raw) {
        if (raw == null || !PLAIN_DECIMAL.matcher(raw).matches()) {
            return null;
        }
        double value = Double.parseDouble(raw);
        return Double.isFinite(value) && value > 0 ? value : null;
    }

    /** A block number is plain digits that fit an int. */
    static Integer parseBlockNumber(String raw) {
        if (raw == null || !PLAIN_INTEGER.matcher(raw).matches()) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Most arguments each command takes after its name; a command given more is rejected. */
    static int maxArguments(String command) {
        switch (command) {
            case "block":
            case "address":
            case "anomalies":
            case "report":
                return 1;
            default:
                return 0;
        }
    }

    private static int runCommandMode(String[] args) {
        String command = args[0].toLowerCase(Locale.ROOT);

        try {
            switch (command) {
                case "dashboard":
                case "brief":
                case "snapshot":
                case "report":
                case "block":
                case "address":
                case "miners":
                case "network":
                case "anomalies":
                case "help":
                    break;
                default:
                    return outputError("unknown_command", "Unknown command '" + command + "'. Run 'make help' to see the supported explorer commands.", EXIT_USAGE);
            }
            if (args.length - 1 > maxArguments(command)) {
                return outputError("invalid_argument", "Too many arguments for '" + command + "': it takes at most " + maxArguments(command) + ".", EXIT_USAGE);
            }

            switch (command) {
                case "dashboard":
                    if (jsonMode) {
                        return outputResult(AgentAPI.overview(blocks, 10));
                    }
                    Insights.printDashboard(blocks, 5);
                    break;
                case "brief":
                    if (jsonMode) {
                        return outputResult(AgentAPI.overview(blocks, 5));
                    }
                    Insights.printActionBrief(blocks, 5);
                    break;
                case "snapshot":
                    return outputResult(AgentAPI.agentSnapshot(blocks));
                case "report":
                    return writeReport(args.length >= 2 ? args[1] : null);
                case "block":
                    Integer blockNumber = args.length < 2 ? null : parseBlockNumber(args[1]);
                    if (blockNumber == null) {
                        return outputError("invalid_argument", "Expected: block <blockNumber>. Try: make block N=15049311.", EXIT_USAGE);
                    }
                    Map<String, Object> blockDetail = AgentAPI.blockDetail(blocks, blockNumber);
                    if (jsonMode || exitCodeFor(blockDetail) != EXIT_OK) {
                        // An unknown block is the same failure, and the same exit code, in text.
                        return outputResult(blockDetail);
                    }
                    printBlockDetails(blockNumber);
                    break;
                case "address":
                    if (args.length < 2) {
                        return outputError("invalid_argument", "Expected: address <0xAddress>. Try: make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f.", EXIT_USAGE);
                    }
                    if (jsonMode || !isEthereumAddress(args[1].trim().toLowerCase(Locale.ROOT))) {
                        // A malformed address is rejected the same way, with the same exit code, in text.
                        return outputResult(AgentAPI.addressIntel(blocks, args[1], 10));
                    }
                    Insights.printAddressIntel(blocks, args[1], 5);
                    break;
                case "miners":
                    if (jsonMode) {
                        return outputResult(AgentAPI.minerAnalysis(blocks, 10));
                    }
                    System.out.println(Blocks.calUniqMiners());
                    break;
                case "network":
                    return outputResult(NetworkAnalyzer.analyzeNetwork(blocks, 10));
                case "anomalies":
                    double threshold = RobustStats.DEFAULT_THRESHOLD;
                    if (args.length >= 2) {
                        Double parsed = parseThreshold(args[1]);
                        if (parsed == null) {
                            return outputError("invalid_argument", "Expected: anomalies <zScoreThreshold>. Try: make anomalies THRESHOLD=3.5.", EXIT_USAGE);
                        }
                        threshold = parsed;
                    }
                    return outputResult(AgentAPI.detectAnomalies(blocks, threshold));
                default:
                    printHelp();
            }
        } catch (Exception e) {
            String cause = e.getMessage() == null || e.getMessage().isBlank() ? e.getClass().getSimpleName() : e.getClass().getSimpleName() + ": " + e.getMessage();
            return outputError("internal_error", buildUnexpectedCommandError(command) + " Cause: " + cause, EXIT_INTERNAL);
        }
        return EXIT_OK;
    }

    private static int outputResult(Map<String, Object> data) {
        int exitCode = exitCodeFor(data);
        if (exitCode != EXIT_OK) {
            Map<String, Object> failed = data;
            if (!(data.get("error") instanceof String) && data.get("overview") instanceof Map) {
                // An unavailable snapshot carries its error inside the overview.
                @SuppressWarnings("unchecked")
                Map<String, Object> overview = (Map<String, Object>) data.get("overview");
                failed = overview;
            }
            String code = String.valueOf(failed.get("error"));
            Map<String, Object> details = new LinkedHashMap<>(failed);
            details.remove("error");
            details.remove("status");
            return outputError(code, describeError(code, details), exitCode, details);
        }
        if (jsonMode) {
            System.out.println(JsonWriter.toJson(data));
        } else {
            // Pretty-print for humans
            printMap(data, 0);
        }
        return EXIT_OK;
    }

    static String describeError(String code, Map<String, Object> details) {
        switch (code) {
            case "block_not_found":
                return "No block numbered " + details.get("block_number") + " is in the loaded dataset.";
            case "invalid_address_format":
                return "Expected an address: 0x followed by 40 hexadecimal characters.";
            case "address_required":
                return "Expected: address <0xAddress>.";
            case "no_data":
                return "The dataset has no blocks.";
            default:
                return code;
        }
    }

    /**
     * The one shape every failed command prints under --json:
     * {"error": {"code", "message", "command", "exit_code", ...details}}.
     */
    static Map<String, Object> buildErrorDocument(String code, String message, String command, int exitCode, Map<String, Object> details) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("command", command);
        error.put("exit_code", exitCode);
        if (details != null) {
            error.putAll(details);
        }
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("error", error);
        return document;
    }

    private static int outputError(String code, String message, int exitCode) {
        return outputError(code, message, exitCode, null);
    }

    /** Reports a failed command in the caller's format and returns its exit code. */
    private static int outputError(String code, String message, int exitCode, Map<String, Object> details) {
        if (jsonMode) {
            System.out.println(JsonWriter.toJson(buildErrorDocument(code, message, currentCommand, exitCode, details)));
        } else {
            System.err.println("Error: " + message);
        }
        return exitCode;
    }

    private static void printMap(Map<String, Object> map, int indent) {
        String prefix = "  ".repeat(indent);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Map) {
                System.out.println(prefix + entry.getKey() + ":");
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) val;
                printMap(subMap, indent + 1);
            } else if (val instanceof java.util.List) {
                System.out.println(prefix + entry.getKey() + ":");
                for (Object item : (java.util.List<?>) val) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        printMap(itemMap, indent + 2);
                        System.out.println();
                    } else {
                        System.out.println(prefix + "  - " + item);
                    }
                }
            } else if (val instanceof Double) {
                System.out.printf(Locale.ROOT, "%s%s: %.8f%n", prefix, entry.getKey(), (Double) val);
            } else {
                System.out.println(prefix + entry.getKey() + ": " + val);
            }
        }
    }

    private static void printHelp() {
        System.out.print(buildHelpText());
    }

    private static void runInteractiveMode() {
        System.out.println("===========================================");
        System.out.println("   Ethereum Block Explorer v5.0");
        System.out.println("===========================================");

        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getMenuChoice();
            boolean pauseAfterAction = true;

            switch (choice) {
                case 1:  viewDashboard(); break;
                case 2:  viewBlockDetails(); break;
                case 3:  viewAddressIntel(); break;
                case 4:  viewNetworkAnalysis(); break;
                case 5:  exportReport(); break;
                case 6:  printHelp(); break;
                case 0:
                    running = false;
                    pauseAfterAction = false;
                    System.out.println("\nThank you for using Ethereum Block Explorer!");
                    break;
                default:
                    pauseAfterAction = false;
                    System.out.println("\nChoose a menu number from 0 to 6.");
            }

            if (running && pauseAfterAction) {
                System.out.println("\nPress Enter to continue...");
                readLine();
            }
        }
    }

    private static void displayMenu() {
        System.out.print(buildMainMenuText());
        System.out.print("Choose a menu item: ");
    }

    private static int getMenuChoice() {
        if (!hasInput()) {
            return 0;
        }
        try {
            return Integer.parseInt(readLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static boolean hasInput() {
        System.out.flush();
        return scanner.hasNextLine();
    }

    /** The next line typed at the menu; empty once stdin has ended. */
    private static String readLine() {
        return hasInput() ? scanner.nextLine() : "";
    }

    private static int loadData() {
        try {
            Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE);
            Blocks.sortBlocksByNumber();
            blocks = Blocks.getBlocks();
            emitLoadWarnings(Blocks.getLoadWarnings());
            return EXIT_OK;
        } catch (FileNotFoundException e) {
            String missingFile = missingDatasetFile(e);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("file", missingFile);
            details.put("directory", Blocks.dataDirectoryPath());
            return outputError("data_file_not_found", buildMissingDataMessage(missingFile, Blocks.dataDirectoryPath()), EXIT_USAGE, details);
        } catch (IOException e) {
            return outputError(
                "io_error",
                "Could not read the dataset in " + Blocks.dataDirectoryPath() + ": " + e.getMessage(),
                EXIT_USAGE);
        }
    }

    static String buildMissingDataMessage(String missingFile, String directory) {
        return "Missing dataset file '" + missingFile + "' in " + directory + ". Keep '" + Blocks.DEFAULT_BLOCKS_FILE + "' and '" + Blocks.DEFAULT_TRANSACTIONS_FILE
            + "' there, or name their directory with --data <directory> or the " + DATA_DIR_ENV + " variable.";
    }

    static String buildUnexpectedCommandError(String command) {
        return "Unexpected error while running '" + command + "'. Re-run the command or see 'make help'.";
    }

    static boolean isEthereumAddress(String value) {
        return EthereumAddressValidator.isValid(value);
    }

    private static String missingDatasetFile(FileNotFoundException exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return Blocks.DEFAULT_BLOCKS_FILE;
        }
        return message.trim();
    }

    private static void emitLoadWarnings(List<String> warnings) {
        for (String warning : warnings) {
            System.err.println(warning);
        }
    }

    private static void printInteractiveError(String failure, String nextStep, Exception e) {
        System.err.println("\nError: " + failure + ". " + nextStep);
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            System.err.println("Details: " + e.getMessage());
        }
    }

    private static void printBlockDetails(int blockNum) throws IOException {
        Blocks block = Blocks.getBlockByNumber(blockNum);
        if (block != null) {
            System.out.println("\n===== BLOCK DETAILS =====");
            System.out.println("Block Number: " + block.getNumber());
            System.out.println("Miner Address: " + block.getMiner());
            System.out.println("Timestamp: " + block.getDate());
            System.out.println("Transaction Count: " + block.getTransactionCount());
            System.out.printf(Locale.ROOT, "Average Transaction Cost: %.8f ETH%n", block.avgTransactionCost());
            System.out.println("=========================");
        } else {
            System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
        }
    }

    private static void printBlockTransactions(int blockNum) throws IOException {
        Blocks block = Blocks.getBlockByNumber(blockNum);
        if (block != null) {
            ArrayList<Transaction> txs = block.getTransactions();
            System.out.println("\n===== TRANSACTIONS FOR BLOCK " + blockNum + " =====");
            for (Transaction t : txs) {
                System.out.printf(Locale.ROOT, "  [%d] %s -> %s (%.8f ETH)%n",
                    t.getIndex(), t.getFromAddress(), t.getToAddress(), t.transactionCost());
            }
            System.out.println("Total: " + txs.size() + " transactions");
        } else {
            System.out.println("No block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
        }
    }

    private static void viewBlockDetails() {
        System.out.print("\nEnter a block number from the loaded dataset: ");
        try {
            printBlockDetails(Integer.parseInt(readLine()));
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            printInteractiveError("Could not load block details", "Try a known block such as 15049311 or rerun 'make help'", e);
        }
    }

    private static void viewTransactionsByBlock() {
        System.out.print("\nEnter a block number from the loaded dataset: ");
        try {
            int blockNum = Integer.parseInt(readLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                ArrayList<Transaction> transactions = block.getTransactions();
                System.out.println("\n===== TRANSACTIONS FOR BLOCK " + blockNum + " =====");
                System.out.println("Total transactions: " + transactions.size());
                if (!transactions.isEmpty()) {
                    int shown = Math.min(10, transactions.size());
                    if (transactions.size() > shown) {
                        System.out.println("Showing the first " + shown + " transactions.");
                    }
                    for (int i = 0; i < shown; i++) {
                        System.out.println(transactions.get(i));
                    }
                }
            } else {
                System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            printInteractiveError("Could not load block transactions", "Try a known block such as 15049311 or rerun 'make help'", e);
        }
    }

    private static void calculateAverageTransactionCost() {
        System.out.print("\nEnter a block number from the loaded dataset: ");
        try {
            int blockNum = Integer.parseInt(readLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.printf(Locale.ROOT, "\nAverage transaction cost for Block %d: %.8f ETH%n", blockNum, block.avgTransactionCost());
            } else {
                System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            printInteractiveError("Could not calculate the average transaction cost", "Try a known block such as 15049311 or rerun 'make help'", e);
        }
    }

    private static void viewUniqueMiners() {
        System.out.println("\n===== MINER BREAKDOWN =====");
        try {
            System.out.println(Blocks.calUniqMiners());
        } catch (Exception e) {
            printInteractiveError("Could not build the miner breakdown", "Rerun the explorer or see 'make help'", e);
        }
    }

    private static void compareBlocks() {
        try {
            System.out.print("\nEnter first block number: ");
            int block1Num = Integer.parseInt(readLine());
            System.out.print("Enter second block number: ");
            int block2Num = Integer.parseInt(readLine());

            Blocks block1 = Blocks.getBlockByNumber(block1Num);
            Blocks block2 = Blocks.getBlockByNumber(block2Num);

            if (block1 != null && block2 != null) {
                System.out.println("\n===== BLOCK COMPARISON =====");
                System.out.println("Block difference: " + Blocks.blockDiff(block1, block2));
                System.out.println("\nTime difference:");
                System.out.println(Blocks.timeDiff(block1, block2));
                int transDiff = Blocks.transactionDiff(block1, block2);
                if (transDiff >= 0) {
                    System.out.println("\nTransactions between blocks: " + transDiff);
                } else {
                    System.out.println("\nCannot calculate transactions between blocks.");
                }
            } else {
                System.out.println("\nOne or both block numbers were not found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try known blocks such as 15049311 and 15049321.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter numeric block numbers such as 15049311 and 15049321.");
        } catch (Exception e) {
            printInteractiveError("Could not compare those blocks", "Try known blocks such as 15049311 and 15049321", e);
        }
    }

    private static void viewTransactionsByAddress() {
        System.out.print("\nEnter a block number to group transactions by sender address: ");
        try {
            int blockNum = Integer.parseInt(readLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.println("\n===== GROUPED TRANSACTIONS BY ADDRESS =====");
                System.out.println(block.uniqFromTo());
            } else {
                System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            printInteractiveError("Could not group transactions for that block", "Try a known block such as 15049311 or rerun 'make help'", e);
        }
    }

    private static void viewDashboard() {
        Insights.printDashboard(blocks, 5);
    }

    private static void exportReport() {
        writeReport("ethereum-report.md");
    }

    private static void viewActionBrief() {
        Insights.printActionBrief(blocks, 5);
    }

    private static void viewAddressIntel() {
        System.out.print("\nEnter an Ethereum address such as 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f: ");
        String address = readLine().trim();
        if (!isEthereumAddress(address)) {
            System.out.println("\nEnter a 42-character Ethereum address that starts with 0x, for example 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f.");
            return;
        }
        Insights.printAddressIntel(blocks, address, 5);
    }

    private static void viewNetworkAnalysis() {
        System.out.println("\n===== NETWORK ANALYSIS =====");
        Map<String, Object> network = NetworkAnalyzer.analyzeNetwork(blocks, 10);
        printMap(network, 0);
    }

    private static void viewAnomalies() {
        System.out.print("\nModified z-score threshold [3.5]: ");
        try {
            String input = readLine().trim();
            double threshold = input.isEmpty() ? RobustStats.DEFAULT_THRESHOLD : Double.parseDouble(input);
            Map<String, Object> anomalies = AgentAPI.detectAnomalies(blocks, threshold);
            printMap(anomalies, 0);
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric threshold such as 3.5 or 5.");
        }
    }

    /**
     * Writes the Markdown report to the named file. With no file it goes to
     * stdout (inside the JSON document under --json), never silently into the
     * working directory.
     */
    private static int writeReport(String filePath) {
        String report = Insights.buildReport(blocks, 5);
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("status", "ok");
        document.put("file", filePath);
        if (filePath == null) {
            if (jsonMode) {
                document.put("markdown", report);
                System.out.println(JsonWriter.toJson(document));
            } else {
                System.out.print(report);
            }
            return EXIT_OK;
        }
        try {
            Files.writeString(Path.of(filePath), report, StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("file", filePath);
            return outputError("report_write_failed", buildReportWriteError(filePath, e), EXIT_USAGE, details);
        }
        if (jsonMode) {
            System.out.println(JsonWriter.toJson(document));
        } else {
            System.out.println("Saved report to: " + filePath);
        }
        return EXIT_OK;
    }

    static String buildReportWriteError(String filePath, Exception cause) {
        String detail = cause.getMessage() == null || cause.getMessage().isBlank() ? "" : " (" + cause.getMessage() + ")";
        return "Could not write the report to '" + filePath + "': " + cause.getClass().getSimpleName() + detail + ".";
    }

    static String buildHelpText() {
        return String.join(System.lineSeparator(),
            "Ethereum Block Explorer v5.0",
            "",
            "Start here:",
            "  Run 'make help' for the full command guide.",
            "  Run 'make dashboard' for the fastest dataset read.",
            "  Run 'make ui' to serve the visual explorer at http://localhost:4173.",
            "  Run 'make verify' to mirror the local health gate.",
            "",
            "Commands:",
            "  make help                 Show the command guide and runtime requirements",
            "  make dashboard            Print the human-readable dashboard",
            "  make ui                   Serve the visual explorer at http://localhost:4173",
            "  make block N=15049311     Inspect one block in JSON",
            "  make address ADDR=0x...   Inspect one address in JSON",
            "  make network              Print the network analysis in JSON",
            "  make report               Write ethereum-report.md",
            "  make run                  Open the small interactive menu",
            "  make brief                Print the action brief",
            "  make snapshot             Print the one-call agent snapshot in JSON",
            "  make anomalies THRESHOLD=3.5  Print anomaly analysis in JSON",
            "  make miners               Print the unique miner breakdown in JSON",
            "  make run-json             Print the JSON overview",
            "  make verify               Run the full local health gate",
            "  make test                 Run the existing JUnit suite",
            "  make cli-smoke            Smoke test the core explorer commands",
            "  make ui-build             Prepare the static web files in web/out/",
            "  make ui-contract          Test the browser CSV import/domain contract",
            "  make ui-typecheck         Check the complete browser TypeScript surface",
            "  make ui-audit             Audit the locked production web dependencies",
            "  make ui-smoke             Smoke test the browser explorer",
            "  make build                Compile the explorer into bin/",
            "  make clean                Remove compiled explorer artifacts",
            "  make ui-clean             Remove generated web preview files",
            "",
            "Requirements:",
            "  - A working Java runtime",
            "  - A JDK with javac",
            "  - The vendored JUnit runner in tools/",
            "  - Dataset files in the repo root: ethereumP1data.csv and ethereumtransactions1.csv",
            "  - Node.js for 'make cli-smoke', 'make ui-smoke', and 'make verify'",
            "  - Playwright browser binaries via 'npx playwright install --with-deps chromium'",
            "  - Python 3 to serve the browser UI with 'make ui'",
            "");
    }

    /** What `--json help` prints: the commands, their arguments, and the exit codes. */
    static Map<String, Object> buildCommandGuide() {
        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("usage", "java -cp bin EthereumBlockExplorer [--json] [--data <directory>] <command> [argument]");
        guide.put("schema", "schemas/cli-output.v1.schema.json");
        guide.put("data", "The two CSVs are read from --data <directory>, else the " + DATA_DIR_ENV
            + " variable, else the working directory, else the directory above the compiled classes.");

        List<Map<String, Object>> commands = new ArrayList<>();
        commands.add(guideEntry("snapshot", null, "snapshot", "Overview, network, and anomalies in one document"));
        commands.add(guideEntry("dashboard", null, "overview", "Dataset overview (also printed when no command is given)"));
        commands.add(guideEntry("brief", null, "overview", "Dataset overview limited to the top five entries"));
        commands.add(guideEntry("block", "<blockNumber>", "block", "One block"));
        commands.add(guideEntry("address", "<0xAddress>", "address", "One address across all loaded blocks"));
        commands.add(guideEntry("network", null, "network", "Address graph analysis"));
        commands.add(guideEntry("anomalies", "[zScoreThreshold]", "anomalies", "Blocks with outlying cost or volume (default threshold 3.5)"));
        commands.add(guideEntry("miners", null, "miners", "Miner breakdown and concentration"));
        commands.add(guideEntry("report", "[file]", "report", "Write the Markdown report to the file; with no file, return it in the document"));
        commands.add(guideEntry("help", null, "help", "This guide"));
        guide.put("commands", commands);

        Map<String, Object> exitCodes = new LinkedHashMap<>();
        exitCodes.put(String.valueOf(EXIT_OK), "ok");
        exitCodes.put(String.valueOf(EXIT_FAILED), "ran, but no answer: unknown block, or a dataset with no blocks");
        exitCodes.put(String.valueOf(EXIT_USAGE), "rejected command line or inputs: unknown command or option, missing, malformed or extra argument, dataset or report path that cannot be used");
        exitCodes.put(String.valueOf(EXIT_INTERNAL), "internal error");
        exitCodes.put(String.valueOf(EXIT_OUTPUT), "stdout could not be written");
        guide.put("exit_codes", exitCodes);
        return guide;
    }

    private static Map<String, Object> guideEntry(String name, String argument, String output, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("name", name);
        entry.put("argument", argument);
        entry.put("output", output);
        entry.put("description", description);
        return entry;
    }

    static String buildMainMenuText() {
        return String.join(System.lineSeparator(),
            "",
            "========== EXPLORER MENU ==========",
            "1.  dashboard     View the dashboard",
            "2.  block         View one block",
            "3.  address       View address profile",
            "4.  network       View network analysis",
            "5.  report        Write a markdown report",
            "6.  help          Show the command guide",
            "0.  Exit",
            "================================",
            "");
    }
}
