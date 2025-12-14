# Ethereum Blocks

This project is an Ethereum blockchain explorer and analyzer written in Java. It processes block and transaction data from CSV files and provides an interactive command-line interface (CLI) to explore the data.

## Project Overview

*   **Language:** Java
*   **Data Sources:**
    *   `ethereumP1data.csv`: Contains Ethereum block data (Block Number, Miner Address, Timestamp, Transaction Count).
    *   `ethereumtransactions1.csv`: Contains Ethereum transaction data (Block Number, Index, Gas Limit, Gas Price, From Address, To Address).
*   **Key Functionality:**
    *   Parsing block and transaction data.
    *   Calculating transaction costs in ETH.
    *   Analyzing unique miners.
    *   Comparing blocks (time difference, transaction count difference).
    *   Grouping transactions by address.
    *   Interactive CLI menu.

## Building and Running

The project relies on standard Java tools (`javac`, `java`).

### Compilation

To compile the source code, run the following command from the project root:

```bash
javac -cp src src/*.java
```

### Running the CLI

To start the interactive Ethereum Block Explorer:

```bash
java -cp src EthereumBlockExplorer
```

### Running the Driver

To run the `Driver` class, which demonstrates specific functionalities and outputs stats:

```bash
java -cp src Driver
```

## Key Files

*   **`src/EthereumBlockExplorer.java`**: The main entry point for the interactive CLI application. It handles the menu loop and user input.
*   **`src/Blocks.java`**: Represents an Ethereum block. It manages the collection of blocks, reads data from the CSV files, and performs calculations like finding unique miners or calculating time differences. It uses a `HashMap` for efficient block lookups and an `ArrayList` for ordered storage.
*   **`src/Transaction.java`**: Represents a single Ethereum transaction. It stores details like gas price, gas limit, and addresses, and calculates the transaction cost.
*   **`src/Driver.java`**: A driver class that demonstrates the usage of the `Blocks` and `Transaction` classes, likely used for testing or assignment verification.
*   **`ethereumP1data.csv`**: The dataset containing 100 Ethereum blocks.
*   **`ethereumtransactions1.csv`**: The dataset containing transactions for the first 15 blocks.

## Development Conventions

*   **Source Directory:** All Java source files are located in the `src/` directory.
*   **Data Loading:** The `Blocks` class is responsible for reading the CSV files. The `readTransactions` method is called within the `Blocks` constructor to load transactions for that specific block.
*   **Error Handling:** The CLI handles `NumberFormatException` and file I/O errors gracefully.
*   **Data Structures:**
    *   `HashMap` is used in `Blocks` for O(1) access to blocks by number.
    *   `TreeSet` is used to store unique transactions sorted by index.
