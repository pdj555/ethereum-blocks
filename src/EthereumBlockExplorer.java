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
                        Blocks.calUniqMiners();
                    }
                    break;
                case "network":
                    outputResult(NetworkAnalyzer.analyzeNetwork(blocks, 10));
                    break;
                case "network-address":
                    if (args.length < 2) {
                        outputError("Expected: network-address <0xAddress>. Try the address command first, then rerun with --json if you need the network profile.");
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
                        outputError("Expected: compare <blockA> <blockB>. Run 'make help' to see the supported explorer commands.");
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
                        outputError("Expected: transactions <blockNumber>. Run 'make help' to see the supported explorer commands.");
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
                    outputError("Unknown command '" + command + "'. Run 'make help' to see the supported explorer commands.");
            }
        } catch (NumberFormatException e) {
            outputError("Expected a numeric value such as 15049311. Run 'make help' to see command examples.");
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
        System.out.println("Ethereum Block Explorer v5.0");
        System.out.println();
        System.out.println("Start with:");
        System.out.println("  make help");
        System.out.println("  make dashboard");
        System.out.println();
        System.out.println("Common commands:");
        System.out.println("  dashboard                  Human-readable dashboard");
        System.out.println("  block <number>             Block details");
        System.out.println("  address <0xAddr>           Address intel");
        System.out.println("  brief                      Action brief");
        System.out.println("  miners                     Unique miner breakdown");
        System.out.println("  network                    Network analysis");
        System.out.println("  anomalies [z-threshold]    Anomaly analysis (default z=1.5)");
        System.out.println("  report [file]              Save a markdown report");
        System.out.println("  help                       Show this message");
        System.out.println();
        System.out.println("Advanced explorer commands:");
        System.out.println("  overview                   Full system overview (default in --json mode)");
        System.out.println("  blocks                     Block list");
        System.out.println("  transactions <number>      Transactions for one block");
        System.out.println("  network-address <0xAddr>   Network profile for one address");
        System.out.println("  compare <blockA> <blockB>  Compare two blocks");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  make dashboard");
        System.out.println("  make block N=15049311");
        System.out.println("  make run-json");
        System.out.println("  make run");
    }

    private static void runInteractiveMode() {
        System.out.println("===========================================");
        System.out.println("   Ethereum Block Explorer v5.0");
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
                    System.out.println("\nChoose a menu number from 0 to 13.");
            }

            if (running && choice >= 1 && choice <= 13) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n========== EXPLORER MENU ==========");
        System.out.println("1.  block         View one block");
        System.out.println("2.  transactions  View one block's transactions");
        System.out.println("3.  avg cost      View average transaction cost");
        System.out.println("4.  miners        View the unique miner breakdown");
        System.out.println("5.  compare       Compare two blocks");
        System.out.println("6.  sender groups Group transactions by sender");
        System.out.println("7.  dashboard     View the dashboard");
        System.out.println("8.  report        Write a markdown report");
        System.out.println("9.  brief         View the action brief");
        System.out.println("10. address       View address intel");
        System.out.println("11. network       View network analysis");
        System.out.println("12. anomalies     View anomaly analysis");
        System.out.println("13. reload        Reload the dataset");
        System.out.println("0.  Exit");
        System.out.println("================================");
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
        } catch (FileNotFoundException e) {
            if (jsonMode) {
                System.out.println("{\"error\": \"data_file_not_found\", \"file\": \"" + Blocks.DEFAULT_BLOCKS_FILE + "\"}");
            } else {
                System.err.println("Error: Missing dataset files. Keep '" + Blocks.DEFAULT_BLOCKS_FILE + "' and '" + Blocks.DEFAULT_TRANSACTIONS_FILE + "' in the repo root, then rerun 'make help' or your command.");
            }
            System.exit(1);
        } catch (IOException e) {
            if (jsonMode) {
                System.out.println("{\"error\": \"io_error\", \"message\": \"" + e.getMessage() + "\"}");
            } else {
                System.err.println("Error: Could not read the dataset files. Check '" + Blocks.DEFAULT_BLOCKS_FILE + "' and '" + Blocks.DEFAULT_TRANSACTIONS_FILE + "', then rerun 'make help' or your command.");
            }
            System.exit(1);
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
            System.err.println("\nError: Could not load block details. " + e.getMessage());
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
                    System.out.print("Show every transaction? [y/N]: ");
                    String showAll = scanner.nextLine().toLowerCase();
                    if (showAll.equals("y")) {
                        for (Transaction t : transactions) {
                            System.out.println(t);
                        }
                    } else {
                        System.out.println("Showing the first 10 transactions. Enter 'y' next time to print the full block.");
                        for (int i = 0; i < Math.min(10, transactions.size()); i++) {
                            System.out.println(transactions.get(i));
                        }
                    }
                }
            } else {
                System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            System.err.println("\nError: Could not load block transactions. " + e.getMessage());
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
            System.err.println("\nError: Could not calculate the average transaction cost. " + e.getMessage());
        }
    }

    private static void viewUniqueMiners() {
        System.out.println("\n===== MINER BREAKDOWN =====");
        try {
            Blocks.calUniqMiners();
        } catch (Exception e) {
            System.err.println("\nError: Could not build the miner breakdown. " + e.getMessage());
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
                System.out.println("\nOne or both block numbers were not found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try known blocks such as 15049311 and 15049321.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter numeric block numbers such as 15049311 and 15049321.");
        } catch (Exception e) {
            System.err.println("\nError: Could not compare those blocks. " + e.getMessage());
        }
    }

    private static void viewTransactionsByAddress() {
        System.out.print("\nEnter a block number to group transactions by sender address: ");
        try {
            int blockNum = Integer.parseInt(scanner.nextLine());
            Blocks block = Blocks.getBlockByNumber(blockNum);
            if (block != null) {
                System.out.println("\n===== GROUPED TRANSACTIONS BY ADDRESS =====");
                block.uniqFromTo();
            } else {
                System.out.println("\nNo block numbered " + blockNum + " was found in " + Blocks.DEFAULT_BLOCKS_FILE + ". Try a known block such as 15049311.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\nEnter a numeric block number such as 15049311.");
        } catch (Exception e) {
            System.err.println("\nError: Could not group transactions for that block. " + e.getMessage());
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
            System.err.println("Error: Could not write the report to '" + path + "'. " + e.getMessage());
        }
    }

    private static void viewActionBrief() {
        Insights.printActionBrief(blocks, 5);
    }

    private static void viewAddressIntel() {
        System.out.print("\nEnter an Ethereum address such as 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f: ");
        String address = scanner.nextLine().trim();
        if (!isProbablyEthereumAddress(address)) {
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

    private static boolean isProbablyEthereumAddress(String value) {
        return value != null && value.startsWith("0x") && value.length() == 42;
    }
}
