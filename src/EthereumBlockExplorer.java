import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Main application for exploring Ethereum blockchain data.
 * Supports interactive mode and direct command mode.
 */
public class EthereumBlockExplorer {
    private static Scanner scanner = new Scanner(System.in);
    private static ArrayList<Blocks> blocks = null;

    public static void main(String[] args) {
        loadData();

        if (args.length > 0) {
            runCommandMode(args);
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
                    Insights.printDashboard(blocks, 5);
                    break;
                case "block":
                    if (args.length < 2) {
                        System.out.println("Usage: java -cp src EthereumBlockExplorer block <blockNumber>");
                        return;
                    }
                    printBlockDetails(Integer.parseInt(args[1]));
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    System.out.println("Available commands: dashboard, block <blockNumber>");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric argument.");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void runInteractiveMode() {
        System.out.println("===========================================");
        System.out.println("   Ethereum Block Explorer v3.0");
        System.out.println("===========================================");

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
                    viewTransactionsByAddress();
                    break;
                case 7:
                    viewDashboard();
                    break;
                case 8:
                    reloadData();
                    break;
                case 0:
                    running = false;
                    System.out.println("\nThank you for using Ethereum Block Explorer!");
                    break;
                default:
                    System.out.println("\nInvalid choice. Please try again.");
            }

            if (running && choice >= 1 && choice <= 8) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    private static void displayMenu() {
        System.out.println("\n========== MAIN MENU ==========");
        System.out.println("1. View Block Details");
        System.out.println("2. View Transactions by Block");
        System.out.println("3. Calculate Average Transaction Cost");
        System.out.println("4. View Unique Miners");
        System.out.println("5. Compare Blocks");
        System.out.println("6. View Transactions by Address");
        System.out.println("7. View Analytics Dashboard");
        System.out.println("8. Reload Data");
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

    private static void loadData() {
        try {
            Blocks.readFile("ethereumP1data.csv");
            Blocks.sortBlocksByNumber();
            blocks = Blocks.getBlocks();
        } catch (FileNotFoundException e) {
            System.err.println("Error: Data file not found. Please ensure 'ethereumP1data.csv' exists.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error reading data file: " + e.getMessage());
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

                if (transactions.size() > 0) {
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
                double avgCost = block.avgTransactionCost();
                System.out.printf("\nAverage transaction cost for Block %d: %.8f ETH%n", blockNum, avgCost);
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
}
