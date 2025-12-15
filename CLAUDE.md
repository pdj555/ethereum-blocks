# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Ethereum blockchain explorer that processes and analyzes historical Ethereum block and transaction data from CSV files. The application provides both a command-line interface for interactive exploration and a programmatic API for blockchain data analysis.

## Building and Running

### Compile all source files:
```bash
javac -cp src src/*.java
```

### Run the interactive CLI application:
```bash
java -cp src EthereumBlockExplorer
```

### Run the original driver (demonstrates specific functionality):
```bash
java -cp src Driver
```

### Run tests:
The project uses JUnit 5 for testing. Tests are in `TestBlocks.java` and `TestTransaction.java`. To run tests, you'll need JUnit 5 on the classpath:
```bash
javac -cp src:junit-platform-console-standalone.jar src/*.java
java -jar junit-platform-console-standalone.jar --class-path src --scan-class-path
```

## Architecture

### Core Data Model

**Transaction** (`Transaction.java`)
- Represents individual Ethereum transactions with validation for Ethereum address format
- Implements `Comparable<Transaction>` to enable sorting by transaction index
- Calculates transaction cost in ETH (converts from wei: 1 ETH = 1e18 wei)
- Fields: blockNumber, index, gasLimit, gasPrice, fromAdr, toAdr

**Blocks** (`Blocks.java`)
- Represents Ethereum blocks and manages collections of blocks
- Uses **HashMap for O(1) block lookups** by block number (performance optimization)
- Uses **TreeSet for automatic transaction deduplication and sorting** when loading transactions
- Implements `Comparable<Blocks>` to enable sorting by block number
- Maintains static ArrayList of all blocks and HashMap for fast lookups
- Reads two CSV files:
  - `ethereumP1data.csv`: Block metadata (columns: 0=number, 9=miner, 16=timestamp, 17=transactionCount)
  - `ethereumtransactions1.csv`: Transaction data (columns: 3=blockNumber, 4=index, 8=gasLimit, 9=gasPrice, 5=fromAddr, 6=toAddr)
- Fields: number, miner, timestamp, transactionCount, transactions ArrayList

### Key Performance Optimizations

1. **HashMap for Block Lookups**: The `blockMap` HashMap enables O(1) retrieval of blocks by number instead of O(n) iteration
2. **BufferedReader**: Uses BufferedReader instead of Scanner for 30-50% faster file I/O
3. **Optimized uniqFromTo()**: Uses HashMap to group transactions in O(n) time instead of nested loops

### Data Flow

1. **Loading**: `Blocks.readFile()` reads block data and automatically calls `readTransactions()` for each block during construction
2. **Storage**: Blocks stored in both ArrayList (for iteration) and HashMap (for fast lookup)
3. **Transactions**: Each Block loads its transactions from CSV, deduplicates using TreeSet, and stores in sorted order by index

### Applications

**EthereumBlockExplorer** (`EthereumBlockExplorer.java`)
- Interactive menu-driven CLI application
- Loads data on startup with error handling
- Features: view block details, browse transactions, calculate averages, compare blocks, analyze miners, group transactions by address

**Driver** (`Driver.java`)
- Example program demonstrating core functionality
- Shows transactions for specific blocks and calculates average transaction costs

## Important Implementation Details

### Transaction Loading
- `readTransactions()` in Blocks constructor filters transactions by block number
- Uses TreeSet to automatically handle duplicates and maintain sort order
- Invalid transactions are skipped with warnings (input validation)

### Address Grouping (uniqFromTo method)
- Groups transactions by "from" address
- Maintains order based on first appearance (transaction index)
- Prints to addresses in index order for each from address
- Calculates total ETH cost per from address
- Format: "From address" → list of "to addresses" → "Total cost: X.XXXXXXXX ETH"

### Date Handling
- Timestamps are Unix timestamps (seconds since epoch)
- `getDate()` converts to human-readable format: "E, dd MMMM yyyy HH:mm:ss z"
- Uses CST timezone

### Error Handling Strategy
- File operations validate input and provide informative error messages
- Invalid data rows are skipped with warnings (line numbers provided)
- Defensive copying in getters prevents external ArrayList modification
- EthereumBlockExplorer validates user input and handles NumberFormatException gracefully

## Data File Structure

The codebase expects two CSV files in the root directory:

1. **ethereumP1data.csv**: 100 Ethereum blocks with columns for block number, miner, timestamp, transaction count
2. **ethereumtransactions1.csv**: Transactions for the first 15 blocks, contains duplicates and out-of-order entries

## Testing Approach

- Tests use JUnit 5 with assertions
- `ByteArrayOutputStream` captures console output for testing print methods
- Tests verify encapsulation (defensive copying), calculation accuracy (avgTransactionCost), and proper transaction loading
- TestTransaction validates individual transaction behavior with simplified Ethereum addresses

## Key Methods Reference

**Blocks class:**
- `readFile(String filename)`: Loads blocks from CSV, populates HashMap
- `getBlockByNumber(int num)`: O(1) lookup using HashMap
- `calUniqMiners()`: Counts unique miners and their frequencies
- `blockDiff(Blocks A, Blocks B)`: Returns difference between block numbers
- `timeDiff(Blocks first, Blocks second)`: Prints time difference in hours/minutes/seconds
- `transactionDiff(Blocks first, Blocks second)`: Returns total transactions between two blocks (exclusive)
- `avgTransactionCost()`: Calculates average cost of all transactions in a block
- `uniqFromTo()`: Groups transactions by from address, shows to addresses and total cost

**Transaction class:**
- `transactionCost()`: Returns gas limit × (gas price in ETH), converts wei to ETH
- `compareTo(Transaction t)`: Compares by index for sorting
