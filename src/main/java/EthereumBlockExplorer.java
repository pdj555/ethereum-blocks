import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Main application for exploring Ethereum blockchain data.
 * Provides a user-friendly command-line interface for various blockchain operations.
 */
public class EthereumBlockExplorer {
    private static final String APP_VERSION = "2.2";
    private static final Scanner scanner = new Scanner(System.in);
    private static ArrayList<Blocks> blocks = null;
    private static String blocksFile = Blocks.DEFAULT_BLOCKS_FILE;
    private static String transactionsFile = Blocks.DEFAULT_TRANSACTIONS_FILE;
    
    public static void main(String[] args) {
        System.out.println("===========================================");
        System.out.println("   Ethereum Block Explorer v" + APP_VERSION);
        System.out.println("===========================================\n");
        
        parseArgs(args);
        
        // Load data on startup
        loadData();
        
        boolean running = true;
        while (running) {
            displayMenu();
            int choice = getMenuChoice();
            
            switch (choice) {
                case 1:
                    viewBlockDetails();
                    break;
                case 2:
                    viewTransactionsByBlock();
                    break;
                case 3:
                    calculateAverageTransactionCost();
                    break;
                case 4:
                    viewUniqueMiners();
                    break;
                case 5:
                    compareBlocks();
                    break;
                case 6:
                    groupTransactionsByFromAddress();
                    break;
                case 7:
                    reloadData();
                    break;
                case 8:
                    askAi();
                    break;
                case 9:
                    showDatasetSummary();
                    break;
                case 10:
                    searchTransactionsByAddress();
                    break;
                case 0:
                    running = false;
                    System.out.println("\nThank you for using Ethereum Block Explorer!");
                    break;
                default:
                    System.out.println("\nInvalid choice. Please try again.");
            }
            
            if (running && choice >= 1 && choice <= 10) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        
        scanner.close();
    }
    
    private static void displayMenu() {
        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("1. View Block Details");
        System.out.println("2. View Transactions by Block");
        System.out.println("3. Calculate Average Transaction Cost");
        System.out.println("4. View Unique Miners");
        System.out.println("5. Compare Blocks");
        System.out.println("6. Group Block Transactions (by from address)");
        System.out.println("7. Reload Data");
        System.out.println("8. Ask AI (natural language)");
        System.out.println("9. Dataset Summary");
        System.out.println("10. Search Transactions by Address");
        System.out.println("0. Exit");
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

    private static void parseArgs(String[] args) {
        if (args == null || args.length == 0) {
            return;
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--help".equals(arg) || "-h".equals(arg)) {
                System.out.println("Usage: java -jar ethereum-blocks.jar [--blocks <file>] [--transactions <file>] [--version]");
                System.out.println("Defaults: --blocks " + Blocks.DEFAULT_BLOCKS_FILE + " --transactions " + Blocks.DEFAULT_TRANSACTIONS_FILE);
                System.exit(0);
            }
            if ("--version".equals(arg) || "-v".equals(arg)) {
                System.out.println("Ethereum Block Explorer v" + APP_VERSION);
                System.exit(0);
            }

            if ("--blocks".equals(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: missing value for --blocks");
                    System.err.println("Run with --help for usage.");
                    System.exit(2);
                }
                blocksFile = args[++i];
                continue;
            }

            if ("--transactions".equals(arg)) {
                if (i + 1 >= args.length) {
                    System.err.println("Error: missing value for --transactions");
                    System.err.println("Run with --help for usage.");
                    System.exit(2);
                }
                transactionsFile = args[++i];
                continue;
            }

            if (arg != null && arg.startsWith("-")) {
                System.err.println("Error: unknown option " + arg);
                System.err.println("Run with --help for usage.");
                System.exit(2);
            }
        }
    }
    
    private static void loadData() {
        System.out.println("Loading blockchain data...");
        try {
            Blocks.readFile(blocksFile, transactionsFile, LoadOptions.QUIET);
            Blocks.sortBlocksByNumber();
            blocks = Blocks.getBlocks();
            LoadReport report = Blocks.getLastLoadReport();
            if (report != null) {
                System.out.println(report.summary());
            }
            System.out.println();
        } catch (FileNotFoundException e) {
            System.err.println("Error loading data: " + e.getMessage());
            System.err.println("Blocks file: " + blocksFile);
            System.err.println("Transactions file: " + transactionsFile);
            System.err.println("Tip: run from the repository root so relative CSV paths resolve correctly.");
            System.err.println("Exiting application...");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading data: " + e.getMessage());
            System.err.println("Blocks file: " + blocksFile);
            System.err.println("Transactions file: " + transactionsFile);
            System.err.println("Tip: run from the repository root so relative CSV paths resolve correctly.");
            System.err.println("Exiting application...");
            System.exit(1);
        }
    }
    
    private static void reloadData() {
        System.out.println("\n===== RELOAD DATA =====");
        System.out.println("Tip: run from the repository root so relative CSV paths resolve correctly.");
        System.out.println("Current blocks file: " + blocksFile);
        System.out.print("New blocks file (press Enter to keep): ");
        String newBlocksFile = scanner.nextLine().trim();
        if (!newBlocksFile.isEmpty()) {
            blocksFile = newBlocksFile;
        }

        System.out.println("Current transactions file: " + transactionsFile);
        System.out.print("New transactions file (press Enter to keep): ");
        String newTransactionsFile = scanner.nextLine().trim();
        if (!newTransactionsFile.isEmpty()) {
            transactionsFile = newTransactionsFile;
        }

        loadData();
    }
    
    private static void viewBlockDetails() {
        try {
            Integer blockNum = promptInt("\nEnter block number (press Enter to cancel): ", true);
            if (blockNum == null) {
                return;
            }
            Blocks block = Blocks.getBlockByNumber(blockNum);
            
            if (block != null) {
                ArrayList<Transaction> txs = block.getTransactions();
                System.out.println("\n===== BLOCK DETAILS =====");
                System.out.println("Block Number: " + block.getNumber());
                System.out.println("Miner Address: " + block.getMiner());
                System.out.println("Timestamp (UTC): " + block.getDate());
                System.out.println("Transaction Count (metadata): " + block.getTransactionCount());
                System.out.println("Transactions Loaded: " + txs.size());
                System.out.printf(Locale.US, "Avg Transaction Cost: %.8f ETH\n", block.avgTransactionCost());
                System.out.println("=========================");
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }
    
    private static void viewTransactionsByBlock() {
        try {
            Integer blockNum = promptInt("\nEnter block number (press Enter to cancel): ", true);
            if (blockNum == null) {
                return;
            }
            Blocks block = Blocks.getBlockByNumber(blockNum);
            
            if (block != null) {
                ArrayList<Transaction> transactions = block.getTransactions();
                System.out.println("\n===== TRANSACTIONS FOR BLOCK " + blockNum + " =====");
                System.out.println("Transactions loaded: " + transactions.size() + " / " + block.getTransactionCount());
                
                if (transactions.size() > 0) {
                    boolean showAll = promptYesNo("Show all transactions? (y/n) [n]: ", false);
                    int limit = showAll ? transactions.size() : Math.min(10, transactions.size());
                    
                    if (!showAll) {
                        System.out.println("Showing first " + limit + " transactions:");
                    }

                    System.out.printf("%-6s %-16s %-16s %14s\n", "Index", "From", "To", "Cost (ETH)");
                    for (int i = 0; i < limit; i++) {
                        Transaction t = transactions.get(i);
                        System.out.printf(
                            Locale.US,
                            "%-6d %-16s %-16s %14.8f\n",
                            t.getIndex(),
                            shortAddress(t.getFromAddress()),
                            shortAddress(t.getToAddress()),
                            t.transactionCost()
                        );
                    }
                }
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }
    
    private static void calculateAverageTransactionCost() {
        try {
            Integer blockNum = promptInt("\nEnter block number (press Enter to cancel): ", true);
            if (blockNum == null) {
                return;
            }
            Blocks block = Blocks.getBlockByNumber(blockNum);
            
            if (block != null) {
                double avgCost = block.avgTransactionCost();
                System.out.printf("\nAverage transaction cost for Block %d: %.8f ETH\n", 
                                  blockNum, avgCost);
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }
    
    private static void viewUniqueMiners() {
        System.out.println("\n===== UNIQUE MINERS =====");
        if (blocks == null || blocks.isEmpty()) {
            System.out.println("No blocks loaded.");
            return;
        }

        Map<String, Integer> minerCounts = new HashMap<>();
        for (Blocks b : blocks) {
            minerCounts.merge(b.getMiner(), 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(minerCounts.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
            .thenComparing(Map.Entry::getKey));

        System.out.println("Unique miners: " + minerCounts.size());
        System.out.println("Top miners (by block count):");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            Map.Entry<String, Integer> e = sorted.get(i);
            System.out.println("- " + e.getKey() + " (" + e.getValue() + " blocks)");
        }

        if (promptYesNo("\nShow full list? (y/n) [n]: ", false)) {
            System.out.println();
            try {
                Blocks.calUniqMiners();
            } catch (FileNotFoundException | IOException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void compareBlocks() {
        try {
            Integer block1Num = promptInt("\nEnter first block number (press Enter to cancel): ", true);
            if (block1Num == null) {
                return;
            }
            Integer block2Num = promptInt("Enter second block number (press Enter to cancel): ", true);
            if (block2Num == null) {
                return;
            }
            
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
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }
    
    private static void groupTransactionsByFromAddress() {
        try {
            Integer blockNum = promptInt("\nEnter block number (press Enter to cancel): ", true);
            if (blockNum == null) {
                return;
            }
            Blocks block = Blocks.getBlockByNumber(blockNum);
            
            if (block != null) {
                System.out.println("\n===== GROUPED TRANSACTIONS (FROM ADDRESS) =====");
                block.uniqFromTo();
            } else {
                System.out.println("\nBlock not found.");
            }
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }
    
    private static void askAi() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("\nOPENAI_API_KEY is not set. Set it to enable AI queries.");
            return;
        }
        
        String model = System.getenv("OPENAI_MODEL");
        if (model == null || model.isBlank()) {
            model = "gpt-4.1-mini";
        }
        
        OpenAIClient client;
        String baseUrl = System.getenv("OPENAI_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            try {
                client = new OpenAIClient(apiKey, model, URI.create(baseUrl));
            } catch (Exception e) {
                System.out.println("\nInvalid OPENAI_BASE_URL; using default https://api.openai.com");
                client = new OpenAIClient(apiKey, model);
            }
        } else {
            client = new OpenAIClient(apiKey, model);
        }
        
        System.out.println("\n===== AI QUERY =====");
        System.out.println("Ask a question about the loaded block/transaction dataset.");
        System.out.print("Question: ");
        String question = scanner.nextLine().trim();
        
        if (question.isEmpty()) {
            System.out.println("\nNo question entered.");
            return;
        }
        
        Blocks focusBlock = null;
        Integer focusBlockNumber = promptInt("Optional focus block number (press Enter to skip): ", true);
        if (focusBlockNumber != null) {
            try {
                focusBlock = Blocks.getBlockByNumber(focusBlockNumber);
                if (focusBlock == null) {
                    System.out.println("Block not found; continuing without a focus block.");
                }
            } catch (Exception e) {
                System.err.println("Error loading focus block: " + e.getMessage());
            }
        }
        
        String context = DatasetInsights.buildContext(blocks, focusBlock);
        String systemPrompt =
            "You are an assistant for analyzing a locally loaded Ethereum CSV dataset. " +
            "Use only the provided context; if information is missing, say so. " +
            "Do not provide investment advice.";
        
        String userPrompt = context + "\n\nUser question:\n" + question;
        
        try {
            System.out.println("\nThinking...\n");
            String answer = client.chat(systemPrompt, userPrompt);
            System.out.println(answer);
        } catch (Exception e) {
            System.err.println("\nAI request failed: " + e.getMessage());
        }
    }

    private static void showDatasetSummary() {
        System.out.println("\n===== DATASET SUMMARY =====");
        System.out.println("Blocks file: " + blocksFile);
        System.out.println("Transactions file: " + transactionsFile);

        LoadReport report = Blocks.getLastLoadReport();
        if (report != null) {
            System.out.println(report.summary());
        }

        if (blocks == null || blocks.isEmpty()) {
            System.out.println("\nNo blocks loaded.");
            return;
        }

        int minBlock = Integer.MAX_VALUE;
        int maxBlock = Integer.MIN_VALUE;
        for (Blocks b : blocks) {
            minBlock = Math.min(minBlock, b.getNumber());
            maxBlock = Math.max(maxBlock, b.getNumber());
        }

        System.out.println("Block number range: " + minBlock + " .. " + maxBlock);
    }

    private static void searchTransactionsByAddress() {
        if (blocks == null || blocks.isEmpty()) {
            System.out.println("\nNo blocks loaded.");
            return;
        }

        System.out.println("\n===== ADDRESS SEARCH =====");
        String input = promptString("Enter address (0x...) or 'create' (press Enter to cancel): ", true);
        if (input == null) {
            return;
        }

        String query = input.trim();
        boolean queryContractCreation = query.equalsIgnoreCase("create");

        if (!queryContractCreation && !isValidHexAddress(query)) {
            System.out.println("Invalid address. Expected 0x + 40 hex chars, or 'create'.");
            return;
        }

        boolean defaultMatchFrom = !queryContractCreation;
        boolean matchFrom = promptYesNo(
            "Match from address? (y/n) [" + (defaultMatchFrom ? "y" : "n") + "]: ",
            defaultMatchFrom
        );
        boolean matchTo = promptYesNo("Match to address? (y/n) [y]: ", true);
        if (!matchFrom && !matchTo) {
            System.out.println("Nothing to search (both from/to disabled).");
            return;
        }

        ArrayList<TransactionMatch> matches = new ArrayList<>();
        for (Blocks b : blocks) {
            for (Transaction t : b.getTransactions()) {
                boolean fromMatches = matchFrom && t.getFromAddress().equalsIgnoreCase(query);
                boolean toMatches;
                if (!matchTo) {
                    toMatches = false;
                } else if (queryContractCreation) {
                    toMatches = t.isContractCreation();
                } else {
                    toMatches = t.getToAddress().equalsIgnoreCase(query);
                }

                if (fromMatches || toMatches) {
                    matches.add(new TransactionMatch(b.getNumber(), t, fromMatches, toMatches));
                }
            }
        }

        if (matches.isEmpty()) {
            System.out.println("No matches found.");
            return;
        }

        matches.sort(Comparator
            .comparingInt(TransactionMatch::blockNumber)
            .thenComparingInt(m -> m.transaction().getIndex()));

        System.out.println("Matches: " + matches.size());

        int defaultLimit = 25;
        int limit = Math.min(defaultLimit, matches.size());
        boolean showAll = (matches.size() <= defaultLimit) || promptYesNo("Show all matches? (y/n) [n]: ", false);
        if (showAll) {
            limit = matches.size();
        }

        System.out.printf("%-10s %-6s %-8s %-16s %-16s %14s\n", "Block", "Index", "Match", "From", "To", "Cost (ETH)");
        for (int i = 0; i < limit; i++) {
            TransactionMatch m = matches.get(i);
            Transaction t = m.transaction();
            System.out.printf(
                Locale.US,
                "%-10d %-6d %-8s %-16s %-16s %14.8f\n",
                m.blockNumber(),
                t.getIndex(),
                m.matchLabel(),
                shortAddress(t.getFromAddress()),
                shortAddress(t.getToAddress()),
                t.transactionCost()
            );
        }
    }

    private static String shortAddress(String address) {
        if (address == null) {
            return "";
        }
        if (address.isEmpty()) {
            return "(create)";
        }
        if (address.length() <= 12) {
            return address;
        }
        return address.substring(0, 6) + "…" + address.substring(address.length() - 4);
    }

    private record TransactionMatch(int blockNumber, Transaction transaction, boolean fromMatches, boolean toMatches) {
        private String matchLabel() {
            if (fromMatches && toMatches) {
                return "FROM+TO";
            }
            if (fromMatches) {
                return "FROM";
            }
            return "TO";
        }
    }

    private static String promptString(String prompt, boolean allowBlank) {
        System.out.print(prompt);
        String value = scanner.nextLine();
        String trimmed = (value == null) ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return allowBlank ? null : "";
        }
        return trimmed;
    }

    private static Integer promptInt(String prompt, boolean allowBlank) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                if (allowBlank) {
                    return null;
                }
                System.out.println("Please enter a number.");
                continue;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    private static boolean promptYesNo(String prompt, boolean defaultValue) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase(Locale.ROOT);
            if (input.isEmpty()) {
                return defaultValue;
            }
            if (input.equals("y") || input.equals("yes")) {
                return true;
            }
            if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("Please enter y or n.");
        }
    }

    private static boolean isValidHexAddress(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("0x") || trimmed.length() != 42) {
            return false;
        }
        for (int i = 2; i < trimmed.length(); i++) {
            if (Character.digit(trimmed.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
