import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

    public static void main(String[] args) {
        // Parse global flags
        int argStart = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--json")) {
                jsonMode = true;
                argStart = i + 1;
                break;
            }
        }

        loadData();

        // Build effective args (without --json flag)
        String[] effectiveArgs = new String[args.length - argStart];
        System.arraycopy(args, argStart, effectiveArgs, 0, effectiveArgs.length);

        if (effectiveArgs.length > 0) {
            runCommandMode(effectiveArgs);
            scanner.close();
            return;
        }

        if (jsonMode) {
            // No command + json mode = print overview
            System.out.println(JsonWriter.toJson(AgentAPI.overview(blocks, 10)));
            scanner.close();
            return;
        }

        runInteractiveMode();
        scanner.close();
    }

    private static void runCommandMode(String[] args) {
        String command = args[0].toLowerCase();

        try {
            switch (command) {
                case "dashboard":
                    if (jsonMode) {
                        outputResult(AgentAPI.overview(blocks, 10));
                    } else {
                        Insights.printDashboard(blocks, 5);
                    }
                    break;
                case "brief":
                    if (jsonMode) {
                        outputResult(AgentAPI.overview(blocks, 5));
                    } else {
                        Insights.printActionBrief(blocks, 5);
                    }
                    break;
                case "report":
                    writeReport(args.length >= 2 ? args[1] : "ethereum-report.md");
                    break;
                case "block":
                    if (args.length < 2) {
                        outputError("Expected: block <blockNumber>. Try: make block N=15049311.");
                        return;
                    }
                    if (jsonMode) {
                        outputResult(AgentAPI.blockDetail(blocks, Integer.parseInt(args[1])));
                    } else {
                        printBlockDetails(Integer.parseInt(args[1]));
                    }
                    break;
                case "address":
                    if (args.length < 2) {
                        outputError("Expected: address <0xAddress>. Try: make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f.");
                        return;
                    }
                    if (jsonMode) {
                        outputResult(AgentAPI.addressIntel(blocks, args[1], 10));
                    } else {
                        Insights.printAddressIntel(blocks, args[1], 5);
                    }
                    break;
                case "miners":
                    if (jsonMode) {
                        outputResult(AgentAPI.minerAnalysis(blocks, 10));
                    } else {
                        System.out.println(Blocks.calUniqMiners());
                    }
                    break;
                case "network":
                    outputResult(NetworkAnalyzer.analyzeNetwork(blocks, 10));
                    break;
                case "anomalies":
                    double threshold = args.length >= 2 ? Double.parseDouble(args[1]) : 1.5;
                    outputResult(AgentAPI.detectAnomalies(blocks, threshold));
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    outputError("Unknown command '" + command + "'. Run 'make help' to see the supported explorer commands.");
            }
        } catch (NumberFormatException e) {
            outputError("Expected a numeric value such as 15049311. Run 'make help' to see command examples.");
        } catch (Exception e) {
            if (!jsonMode && e.getMessage() != null && !e.getMessage().isBlank()) {
                System.err.println("Details: " + e.getMessage());
            }
            outputError(buildUnexpectedCommandError(command));
        }
    }

    private static void outputResult(Map<String, Object> data) {
        if (jsonMode) {
            System.out.println(JsonWriter.toJson(data));
        } else {
            // Pretty-print for humans
            printMap(data, 0);
        }
    }

    private static void outputError(String msg) {
        if (jsonMode) {
            System.out.println("{\"error\": \"" + msg.replace("\"", "\\\"") + "\"}");
        } else {
            System.err.println("Error: " + msg);
        }
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
                System.out.printf("%s%s: %.8f%n", prefix, entry.getKey(), (Double) val);
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
                scanner.nextLine();
            }
        }
    }

    private static void displayMenu() {
        System.out.print(buildMainMenuText());
        System.out.print("Choose a menu item: ");
    }

    private static int getMenuChoice() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void loadData() {
        try {
            Blocks.readFile(Blocks.DEFAULT_BLOCKS_FILE);
            Blocks.sortBlocksByNumber();
            blocks = Blocks.getBlocks();
            emitLoadWarnings(Blocks.getLoadWarnings());
        } catch (FileNotFoundException e) {
            String missingFile = missingDatasetFile(e);
            if (jsonMode) {
                System.out.println(buildMissingDataErrorJson(missingFile));
            } else {
                System.err.println("Error: Missing dataset file '" + missingFile + "'. Keep '" + Blocks.DEFAULT_BLOCKS_FILE + "' and '" + Blocks.DEFAULT_TRANSACTIONS_FILE + "' in the repo root, then rerun your command.");
            }
            System.exit(1);
        } catch (IOException e) {
            if (jsonMode) {
                System.out.println("{\"error\": \"io_error\", \"message\": \"" + e.getMessage() + "\"}");
            } else {
                System.err.println("Error: Could not read the dataset files. Check '" + Blocks.DEFAULT_BLOCKS_FILE + "' and '" + Blocks.DEFAULT_TRANSACTIONS_FILE + "', then rerun your command.");
            }
            System.exit(1);
        }
    }

    static String buildMissingDataErrorJson(String missingFile) {
        return "{\"error\": \"data_file_not_found\", \"file\": \"" + missingFile + "\"}";
    }

    static String buildUnexpectedCommandError(String command) {
        return "Unexpected error while running '" + command + "'. Re-run the command or see 'make help'.";
    }

    static boolean isEthereumAddress(String value) {
        return value != null && value.matches("^0x[0-9a-fA-F]{40}$");
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

    private static void reloadData() {
        System.out.println("Reloading " + Blocks.DEFAULT_BLOCKS_FILE + "...");
        loadData();
        System.out.println("Loaded " + blocks.size() + " blocks from " + Blocks.DEFAULT_BLOCKS_FILE + ".");
    }

    private static void printBlockDetails(int blockNum) throws IOException {
        Blocks block = Blocks.getBlockByNumber(blockNum);
        if (block != null) {
            System.out.println("\n===== BLOCK DETAILS =====");
            System.out.println("Block Number: " + block.getNumber());
            System.out.println("Miner Address: " + block.getMiner());
            System.out.println("Timestamp: " + block.getDate());
            System.out.println("Transaction Count: " + block.getTransactionCount());
            System.out.printf("Average Transaction Cost: %.8f ETH%n", block.avgTransactionCost());
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
                System.out.printf("  [%d] %s -> %s (%.8f ETH)%n",
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
            printBlockDetails(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            printInteractiveError("Could not load block details", "Try a known block such as 15049311 or rerun 'make help'", e);
        }
    }

    private static void viewTransactionsByBlock() {
        System.out.print("\nEnter a block number from the loaded dataset: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine());
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
            int blockNum = Integer.parseInt(scanner.nextLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.printf("\nAverage transaction cost for Block %d: %.8f ETH%n", blockNum, block.avgTransactionCost());
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
            int block1Num = Integer.parseInt(scanner.nextLine());
            System.out.print("Enter second block number: ");
            int block2Num = Integer.parseInt(scanner.nextLine());

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
            int blockNum = Integer.parseInt(scanner.nextLine());
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
        String path = "ethereum-report.md";
        try {
            writeReport(path);
        } catch (IOException e) {
            printInteractiveError("Could not write the report to '" + path + "'", "Check file permissions, then rerun 'make report'", e);
        }
    }

    private static void viewActionBrief() {
        Insights.printActionBrief(blocks, 5);
    }

    private static void viewAddressIntel() {
        System.out.print("\nEnter an Ethereum address such as 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f: ");
        String address = scanner.nextLine().trim();
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
        System.out.print("\nZ-score threshold [1.5]: ");
        try {
            String input = scanner.nextLine().trim();
            double threshold = input.isEmpty() ? 1.5 : Double.parseDouble(input);
            Map<String, Object> anomalies = AgentAPI.detectAnomalies(blocks, threshold);
            printMap(anomalies, 0);
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric threshold such as 1.5 or 2.0.");
        }
    }

    private static void writeReport(String filePath) throws IOException {
        String report = Insights.buildReport(blocks, 5);
        Files.writeString(Path.of(filePath), report, StandardCharsets.UTF_8);
        if (jsonMode) {
            System.out.println("{\"status\": \"ok\", \"file\": \"" + filePath + "\"}");
        } else {
            System.out.println("Saved report to: " + filePath);
        }
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
            "  make anomalies            Print anomaly analysis in JSON",
            "  make miners               Print the unique miner breakdown in JSON",
            "  make run-json             Print the JSON overview",
            "  make verify               Run the full local health gate",
            "  make test                 Run the existing JUnit suite",
            "  make cli-smoke            Smoke test the core explorer commands",
            "  make ui-build             Prepare the static web files in web/dist/",
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
            "  - Python 3 to serve the browser UI with 'make ui'",
            "");
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
