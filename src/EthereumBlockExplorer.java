import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

/**
 * Main application for exploring Ethereum blockchain data.
 *
 * Supports three modes:
 *   1. Interactive (human) mode - menu-driven terminal UI
 *   2. Command mode - single-shot CLI commands for scripts
 *   3. Agent mode (--json) - structured JSON output for AI agents and pipelines
 *
 * Agent mode is the primary interface. Every command returns machine-parseable JSON
 * when --json is passed as the first argument.
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
                case "overview":
                    outputResult(AgentAPI.overview(blocks, 10));
                    break;
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
                        outputError("Usage: block <blockNumber>");
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
                        outputError("Usage: address <0xAddress>");
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
                        Blocks.calUniqMiners();
                    }
                    break;
                case "network":
                    outputResult(NetworkAnalyzer.analyzeNetwork(blocks, 10));
                    break;
                case "network-address":
                    if (args.length < 2) {
                        outputError("Usage: network-address <0xAddress>");
                        return;
                    }
                    outputResult(NetworkAnalyzer.addressNetworkProfile(blocks, args[1], 10));
                    break;
                case "anomalies":
                    double threshold = args.length >= 2 ? Double.parseDouble(args[1]) : 1.5;
                    outputResult(AgentAPI.detectAnomalies(blocks, threshold));
                    break;
                case "compare":
                    if (args.length < 3) {
                        outputError("Usage: compare <blockA> <blockB>");
                        return;
                    }
                    outputResult(AgentAPI.compareBlocks(blocks, Integer.parseInt(args[1]), Integer.parseInt(args[2])));
                    break;
                case "blocks":
                    if (jsonMode) {
                        System.out.println(JsonWriter.toJson(AgentAPI.listBlocks(blocks)));
                    } else {
                        for (Blocks b : blocks) {
                            System.out.printf("Block %d | Miner: %s | Tx: %d%n",
                                b.getNumber(), b.getMiner(), b.getTransactionCount());
                        }
                    }
                    break;
                case "transactions":
                    if (args.length < 2) {
                        outputError("Usage: transactions <blockNumber>");
                        return;
                    }
                    if (jsonMode) {
                        System.out.println(JsonWriter.toJson(AgentAPI.blockTransactions(blocks, Integer.parseInt(args[1]))));
                    } else {
                        printBlockTransactions(Integer.parseInt(args[1]));
                    }
                    break;
                case "help":
                    printHelp();
                    break;
                default:
                    outputError("Unknown command: " + command + ". Use 'help' for available commands.");
            }
        } catch (NumberFormatException e) {
            outputError("Invalid numeric argument: " + e.getMessage());
        } catch (Exception e) {
            outputError(e.getMessage());
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
            System.out.println("Error: " + msg);
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
        System.out.println("Ethereum Block Explorer v5.0 - Agent-Native Edition");
        System.out.println();
        System.out.println("Usage: java -cp bin EthereumBlockExplorer [--json] <command> [args]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  overview                   Full system overview (default in --json mode)");
        System.out.println("  dashboard                  Analytics dashboard");
        System.out.println("  brief                      Action brief with strategic signals");
        System.out.println("  block <number>             Block details");
        System.out.println("  blocks                     List all blocks");
        System.out.println("  transactions <number>      All transactions for a block");
        System.out.println("  address <0xAddr>           Wallet intelligence");
        System.out.println("  miners                     Miner concentration analysis");
        System.out.println("  network                    Full network graph analysis");
        System.out.println("  network-address <0xAddr>   Address network position");
        System.out.println("  anomalies [z-threshold]    Anomaly detection (default z=1.5)");
        System.out.println("  compare <blockA> <blockB>  Compare two blocks");
        System.out.println("  report [file]              Export markdown report");
        System.out.println("  help                       Show this message");
        System.out.println();
        System.out.println("Flags:");
        System.out.println("  --json    Output structured JSON (agent/pipeline mode)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -cp src EthereumBlockExplorer --json overview");
        System.out.println("  java -cp src EthereumBlockExplorer --json block 15049311");
        System.out.println("  java -cp src EthereumBlockExplorer --json network");
        System.out.println("  java -cp src EthereumBlockExplorer --json anomalies 2.0");
    }

    private static void runInteractiveMode() {
        System.out.println("===========================================");
        System.out.println("   Ethereum Block Explorer v5.0");
        System.out.println("   Agent-Native Edition");
        System.out.println("===========================================");

        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getMenuChoice();

            switch (choice) {
                case 1:  viewBlockDetails(); break;
                case 2:  viewTransactionsByBlock(); break;
                case 3:  calculateAverageTransactionCost(); break;
                case 4:  viewUniqueMiners(); break;
                case 5:  compareBlocks(); break;
                case 6:  viewTransactionsByAddress(); break;
                case 7:  viewDashboard(); break;
                case 8:  exportReport(); break;
                case 9:  viewActionBrief(); break;
                case 10: viewAddressIntel(); break;
                case 11: viewNetworkAnalysis(); break;
                case 12: viewAnomalies(); break;
                case 13: reloadData(); break;
                case 0:
                    running = false;
                    System.out.println("\nThank you for using Ethereum Block Explorer!");
                    break;
                default:
                    System.out.println("\nInvalid choice. Please try again.");
            }

            if (running && choice >= 1 && choice <= 13) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("1.  View Block Details");
        System.out.println("2.  View Transactions by Block");
        System.out.println("3.  Calculate Average Transaction Cost");
        System.out.println("4.  View Unique Miners");
        System.out.println("5.  Compare Blocks");
        System.out.println("6.  View Transactions by Address");
        System.out.println("7.  View Analytics Dashboard");
        System.out.println("8.  Export Lean Markdown Report");
        System.out.println("9.  View Action Brief");
        System.out.println("10. Address Intel");
        System.out.println("11. Network Graph Analysis");
        System.out.println("12. Anomaly Detection");
        System.out.println("13. Reload Data");
        System.out.println("0.  Exit");
        System.out.println("===============================");
        System.out.print("Enter your choice: ");
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
            Blocks.readFile("ethereumP1data.csv");
            Blocks.sortBlocksByNumber();
            blocks = Blocks.getBlocks();
        } catch (FileNotFoundException e) {
            if (jsonMode) {
                System.out.println("{\"error\": \"data_file_not_found\", \"file\": \"ethereumP1data.csv\"}");
            } else {
                System.err.println("Error: Data file not found. Please ensure 'ethereumP1data.csv' exists.");
            }
            System.exit(1);
        } catch (IOException e) {
            if (jsonMode) {
                System.out.println("{\"error\": \"io_error\", \"message\": \"" + e.getMessage() + "\"}");
            } else {
                System.err.println("Error reading data file: " + e.getMessage());
            }
            System.exit(1);
        }
    }

    private static void reloadData() {
        System.out.println("Reloading blockchain data...");
        loadData();
        System.out.println("Loaded " + blocks.size() + " blocks.");
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
            System.out.println("\nBlock not found.");
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
            System.out.println("Block not found.");
        }
    }

    private static void viewBlockDetails() {
        System.out.print("\nEnter block number: ");
        try {
            printBlockDetails(Integer.parseInt(scanner.nextLine()));
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid block number.");
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void viewTransactionsByBlock() {
        System.out.print("\nEnter block number: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                ArrayList<Transaction> transactions = block.getTransactions();
                System.out.println("\n===== TRANSACTIONS FOR BLOCK " + blockNum + " =====");
                System.out.println("Total transactions: " + transactions.size());
                if (!transactions.isEmpty()) {
                    System.out.print("Show all transactions? (y/n): ");
                    String showAll = scanner.nextLine().toLowerCase();
                    if (showAll.equals("y")) {
                        for (Transaction t : transactions) {
                            System.out.println(t);
                        }
                    } else {
                        System.out.println("Showing first 10 transactions:");
                        for (int i = 0; i < Math.min(10, transactions.size()); i++) {
                            System.out.println(transactions.get(i));
                        }
                    }
                }
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid block number.");
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void calculateAverageTransactionCost() {
        System.out.print("\nEnter block number: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.printf("\nAverage transaction cost for Block %d: %.8f ETH%n", blockNum, block.avgTransactionCost());
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid block number.");
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void viewUniqueMiners() {
        System.out.println("\n===== UNIQUE MINERS =====");
        try {
            Blocks.calUniqMiners();
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
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
                Blocks.timeDiff(block1, block2);
                int transDiff = Blocks.transactionDiff(block1, block2);
                if (transDiff >= 0) {
                    System.out.println("\nTransactions between blocks: " + transDiff);
                } else {
                    System.out.println("\nCannot calculate transactions between blocks.");
                }
            } else {
                System.out.println("\nOne or both blocks not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid block number.");
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void viewTransactionsByAddress() {
        System.out.print("\nEnter block number: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.println("\n===== TRANSACTIONS BY ADDRESS =====");
                block.uniqFromTo();
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nInvalid block number.");
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

    private static void viewDashboard() {
        Insights.printDashboard(blocks, 5);
    }

    private static void exportReport() {
        System.out.print("\nOutput file [ethereum-report.md]: ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) path = "ethereum-report.md";
        try {
            writeReport(path);
        } catch (IOException e) {
            System.err.println("Failed to write report: " + e.getMessage());
        }
    }

    private static void viewActionBrief() {
        Insights.printActionBrief(blocks, 5);
    }

    private static void viewAddressIntel() {
        System.out.print("\nEnter Ethereum address (0x...): ");
        String address = scanner.nextLine().trim();
        Insights.printAddressIntel(blocks, address, 5);
    }

    private static void viewNetworkAnalysis() {
        System.out.println("\n===== NETWORK GRAPH ANALYSIS =====");
        Map<String, Object> network = NetworkAnalyzer.analyzeNetwork(blocks, 10);
        printMap(network, 0);
    }

    private static void viewAnomalies() {
        System.out.print("\nZ-score threshold [1.5]: ");
        String input = scanner.nextLine().trim();
        double threshold = input.isEmpty() ? 1.5 : Double.parseDouble(input);
        Map<String, Object> anomalies = AgentAPI.detectAnomalies(blocks, threshold);
        printMap(anomalies, 0);
    }

    private static void writeReport(String filePath) throws IOException {
        String report = Insights.buildReport(blocks, 5);
        Files.writeString(Path.of(filePath), report, StandardCharsets.UTF_8);
        if (jsonMode) {
            System.out.println("{\"status\": \"ok\", \"file\": \"" + filePath + "\"}");
        } else {
            System.out.println("Report generated: " + filePath);
        }
    }
}
