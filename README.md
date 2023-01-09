# Project 4: Ethereum Blocks

A blockchain is a database of transactions that is updated and shared across many computers in a network. Every time a new set of transactions is added, it’s called a “block” - hence the name blockchain. Most blockchains are public, and you can only add data, not remove. If someone wanted to alter any of the information or cheat the system, they’d need to do so on the majority of computers on the network. 

For this project, we will be using a dataset of 100 blocks in the Ethereum blockchain, as well as a new dataset of transactions corresponding to the first 15 blocks in the original dataset.

You will be updating your Blocks.java class as well as implementing the new Transaction.java class to accomplish the tasks laid out by this README and Driver.java.

There will be a template for Blocks.java provided in your repo that is caught up through project 2 if you were unable to complete it. 

## Transaction UML

<img src=./imgs/TransactionUML.PNG width=50% height=50%>

Feel free to add your own helper methods as needed.

## Transaction Methods

`Transaction(int number, int index, int gasLimit, long gasPrice, String fromAdr, String toAdr)`: Initialize the class fields `blockNumber`, `index`, `gasLimit`, `gasPrice`, `fromAdr`, `toAdr`. 

`getBlockNumber()`: Should return `blockNumber`.

`getIndex()`: Should return `index`.

`getGasLimit()`: Should return `gasLimit`.

`getGasPrice()`: Should return `gasPrice`.

`getFromAddress()`: Should return `fromAdr`.

`getToAddress()`: Should return `toAdr`.

`toString()`: Should return "Transaction `index` for Block `blockNumber`" exactly.

`compareTo(Transaction t)`: The Transaction class should implement the comparable interface to override this method. The comparison should be based on the `index` of the Transaction. 

`transactionCost()`: "Gas limit" is the maximum amount of work you're estimating a validator will do on a particular transaction. A higher gas limit usually means the user believes the transaction will require more work. "Gas price" is the price per unit of work done. So, a transaction cost is the gas limit multiplied by the gas price. In the new data file, gas price is given in wei, and we want to convert it to ETH for this method. The conversion rate is 1 ETH = 1e18 wei. You should return the cost of the transaction in ETH as a double. 

## Blocks UML 

<img src=./imgs/BlocksUML.PNG width=50% height=50%>

Feel free to add your own helper methods as needed.

## Blocks Methods

### Constructors

Same as Project 2:

`Blocks()`: Initialize no fields. When printed using the toString() method it should return "Empty Block" exactly.

`Blocks(int number)`: Initialize the Block number. When printed using the toString() method it should return "Block Number: `number`" exactly.

`Blocks(int number, String miner)`: Initialize the Block number and miner address. When printed using the toString() method it should return "Block Number: `number` Miner Address: `miner`" exactly. 

Updated for Project 4:

`Blocks(int number, String miner, long timestamp, int transactionCount)`: Initialize the Block number, miner address, timestamp, and transactionCount. When printed using the toString() method it should return "Block Number: `number` Miner Address: `miner`" exactly. This constructor should also initialize the transactions ArrayList by calling the new `readTransactions()` method for the new data file "ethereumtransactions1.csv".

### Getters

Same as Project 2:

`getNumber()`: Should return the Block number.

`getMiner()`: Should return the miner address.

`getBlocks()`: Should return a copy of the blocks ArrayList.

`getDate()`: Should return the String representation of the date / time of timestamp.

New / Updated for Project 4:

`getTransactionCount()`: Should return the transactionCount. This was previously getTransactions().

`getTransactions()`: Should return a copy of the transactions ArrayList. 

### Other Methods

Same as Project 2:

`calUniqMiners()`: Should print to output the number of unique miners in the data, and a pair of lines for each one giving its miner address and the frequency at which it appears.

`blockDiff(Blocks A, Blocks B)`: Should return the difference between A's and B's Block number. The result can be positive or negative depending on the order the Blocks are supplied.

`getBlockByNumber(int num)`: Should return the Blocks object you read from the file that corresponds to the given Block number.

`timeDiff(Blocks first, Blocks second)`: Should take two Blocks as input, and print to the console the difference in their times in hours, minutes, and seconds. The order the Blocks are given in should not matter, the resulting difference in time should be the same.

`transactionDiff(Blocks first, Blocks second)`: Should take two Blocks as input, and calculate the total transactions of the Blocks between those two Blocks (not inclusive).

`sortBlocksByNumber()`: Should sort your blocks ArrayList in ascending order based on Block number. This can be done by implementing the comparable interface and overriding the compareTo method. 

`readFile(String filename)`: Same as project 2.

New for Project 4:

`readTransactions(String filename)`: This method should read certain columns from the data file in order to fill the transactions ArrayList with Transaction objects by using the Transaction constructor. You should read the block number, transaction index, gas limit, gas price, from address, and to address. They are columns 4, 5, 9, 10, 6, and 7 respectively in the data file "ethereumtransactions1.csv". You can see more information about the data here: https://ethereum-etl.readthedocs.io/en/latest/schema/ under the section transactions.csv. Further, there are duplicate entries, out of order entries, and entries for other Blocks in the data. You must only read transactions that apply to the specific Block you are constructing, there should NOT be duplicates present in your ArrayList, and the ArrayList should be in sorted order based on the Transaction index. (Hint: use a Set)

`avgTransactionCost()`: This method should compute and return the average transaction cost of every transaction in a Block (every transaction in the transactions ArrayList). You can call the `transactionCost()` method from the Transaction class to help with this. It should return in ETH as well. 

`uniqFromTo()`: This method will look at all the transactions in a Block, and find every unique from address. Then it should keep track of every to address that corresponds to each unique from address. It will also keep track of the combined cost of each transaction associated with each unique from address. 

The method should print out to the console in the following format: 

<img src=./imgs/uniq1.PNG width=50% height=50%>

This example shows a portion of the print out for Block 15049314. The full sample output can be found [here](./imgs/sampleoutput).

Looking at the second entry printed out, the line "From 0xf1bb7079ce7002eef428e30124247b6b88080bdb" refers to one of the unique from addresses. 

The lines: 
* " -> 0x0cec1a9154ff802e7934fc916ed7ca50bde6844e"
* " -> 0x881d40237659c251811cec9c364ef91dc08d300c"

both correspond with to addresses that a transaction was sent to from the above from address. 

The line "Total cost of transactions: 0.01429558 ETH" means that both of the transactions from "0xf1bb7079ce7002eef428e30124247b6b88080bdb" totaled 0.01429558 ETH. Be sure when printing this out to round the number to 8 decimal points (there is an example of this in Driver.java).

Each entry should print in order of the lowest index that from address appears. For example, the from address "0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f" appears in the transaction with index 0. The entry after that has the from address "0xf1bb7079ce7002eef428e30124247b6b88080bdb" which corresponds to the transaction with index 1. They should appear in order of the first appearance of the from address. So if a from address appears in the transaction with index 2 and in the transaction with index 102, it should be listed as the third entry printed out by this method, and not the 103rd. This same logic applies to the order the to addresses should be listed in. As shown above, since the to address "0x0c..." has a lower index than "0x88...", so it is listed first. 

This method will be graded based on output, so be careful about capitalization and whitespace. Notice there is a space before and after the arrow when printing out to addresses. You can check this method yourself by editing Driver.java to call the method on Block 15049314 and comparing your output to the sample output. It will be graded based off the output for Block 15049311. 

## Grading

Plagiarism will not be tolerated under any circumstances. Participating students will be penalized depending on the degree of plagiarism. It includes “No-code” sharing among the students. It can lead to academic misconduct reporting to the authority if identical code is found among the students.

You will be graded on: 
* Zylabs Submission: 80 points possible
* at least 10 github commits: 10 points possible
* generated Javadocs: 10 points possible
* make sure Javadocs generates in the `doc` folder (this should be default)

Submit your project before the due date/time. No late submissions allowed.














