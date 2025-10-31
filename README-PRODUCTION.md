# Ethereum Block Explorer - Production Edition

[![CI/CD Pipeline](https://github.com/pdj555/ethereum-blocks/workflows/CI%2FCD%20Pipeline/badge.svg)](https://github.com/pdj555/ethereum-blocks/actions)

A production-ready Ethereum blockchain explorer application with enterprise-grade features including comprehensive logging, containerization, CI/CD pipeline, and robust error handling.

## 🚀 Features

### Production-Ready Enhancements
- **Maven Build System**: Professional dependency management and build lifecycle
- **Logging Framework**: SLF4J + Logback for structured logging with file rotation
- **Docker Support**: Containerized deployment with multi-stage builds
- **CI/CD Pipeline**: Automated builds, tests, and quality checks via GitHub Actions
- **Configuration Management**: Externalized configuration via properties files
- **Error Handling**: Comprehensive exception handling and validation
- **Code Quality**: Checkstyle and SpotBugs integration
- **Testing Infrastructure**: JUnit 5 framework with proper test organization

### Core Features
- View detailed block information
- Browse transactions by block
- Calculate average transaction costs
- Compare multiple blocks
- Analyze unique miners and their frequencies
- Group transactions by sender address
- Interactive CLI interface

## 📋 Prerequisites

- **Java 11** or higher
- **Maven 3.6+** (for building from source)
- **Docker** (optional, for containerized deployment)

## 🔧 Building the Application

### Using Maven

```bash
# Clean and build
mvn clean package

# Run tests
mvn test

# Generate JavaDoc
mvn javadoc:javadoc

# Run code quality checks
mvn checkstyle:check
mvn spotbugs:check
```

### Using Docker

```bash
# Build Docker image
docker build -t ethereum-explorer:2.0.0 .

# Or use Docker Compose
docker-compose build
```

## 🏃 Running the Application

### From JAR File

```bash
# After building with Maven
java -jar target/ethereum-explorer.jar
```

### From Source

```bash
mvn exec:java -Dexec.mainClass="EthereumBlockExplorer"
```

### Using Docker

```bash
# Run with Docker
docker run -it --rm \
  -v $(pwd)/logs:/app/logs \
  ethereum-explorer:2.0.0

# Or use Docker Compose
docker-compose up
```

### Quick Start (No Build Required)

```bash
# Compile and run directly
javac -d bin src/*.java
java -cp bin EthereumBlockExplorer
```

## 📊 Data Files

The application requires two CSV data files:
- `ethereumP1data.csv` - Block information
- `ethereumtransactions1.csv` - Transaction details

These files should be in the same directory as the JAR file or in the current working directory.

## 🔍 Usage Examples

### Interactive Menu

The application provides an intuitive menu-driven interface:

```
========== MAIN MENU ==========
1. View Block Details
2. View Transactions by Block
3. Calculate Average Transaction Cost
4. View Unique Miners
5. Compare Blocks
6. View Transactions by Address
7. Reload Data
0. Exit
```

### Example Operations

**View Block Details:**
```
Enter block number: 15049311
Block Number: 15049311
Miner: 0x...
Date: Mon, 01 August 2022 12:34:56 CST
Transactions: 342
```

**Compare Blocks:**
```
Enter first block number: 15049311
Enter second block number: 15049321
Block difference: 10
Time difference: 2 minutes, 3 seconds
```

## 📁 Project Structure

```
ethereum-blocks/
├── src/
│   ├── main/
│   │   └── resources/
│   │       ├── logback.xml
│   │       └── application.properties
│   ├── test/
│   │   └── java/
│   │       ├── TestBlocks.java
│   │       └── TestTransaction.java
│   ├── Blocks.java
│   ├── Transaction.java
│   ├── Driver.java
│   └── EthereumBlockExplorer.java
├── .github/
│   └── workflows/
│       └── ci-cd.yml
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .gitignore
└── README-PRODUCTION.md
```

## 🔐 Security Features

- Input validation for all user inputs
- Ethereum address format validation
- Safe file handling with proper resource management
- No hardcoded credentials or sensitive data
- Secure exception handling without exposing internal details

## 📝 Configuration

Edit `src/main/resources/application.properties` to customize:

```properties
# Data Files
data.blocks.file=ethereumP1data.csv
data.transactions.file=ethereumtransactions1.csv

# Validation
validation.strict=true
validation.skip.invalid=true

# Output Formatting
output.decimal.places=8
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=TestBlocks

# Run with coverage
mvn test jacoco:report
```

## 📊 Logging

Logs are stored in the `logs/` directory:
- `ethereum-explorer.log` - All application logs
- `ethereum-explorer-error.log` - Error logs only

Log files are automatically rotated daily with 30-day retention.

## 🚢 Deployment

### Production Deployment

1. **Build the application:**
   ```bash
   mvn clean package
   ```

2. **Deploy the JAR:**
   ```bash
   java -jar target/ethereum-explorer.jar
   ```

3. **Or deploy with Docker:**
   ```bash
   docker-compose up -d
   ```

### Environment Variables

Configure JVM options for production:
```bash
export JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"
java $JAVA_OPTS -jar ethereum-explorer.jar
```

## 🔄 CI/CD Pipeline

The project includes a GitHub Actions workflow that:
- Builds the application on every push
- Runs all unit tests
- Performs code quality checks (Checkstyle, SpotBugs)
- Generates JavaDoc documentation
- Builds Docker images
- Uploads build artifacts

## 🛠️ Development

### Code Quality

```bash
# Run Checkstyle
mvn checkstyle:check

# Run SpotBugs
mvn spotbugs:check

# Generate reports
mvn site
```

### IDE Setup

Import the project as a Maven project in your favorite IDE:
- **IntelliJ IDEA**: File → Open → Select pom.xml
- **Eclipse**: File → Import → Existing Maven Projects
- **VS Code**: Install Java Extension Pack, open folder

## 📈 Performance Optimizations

- **O(1) Block Lookups**: HashMap-based indexing
- **O(n) Transaction Grouping**: Single-pass algorithm
- **BufferedReader**: Optimized file reading (30-50% faster)
- **Defensive Copying**: Prevents external modification
- **Resource Management**: Proper try-with-resources patterns

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and quality checks
5. Submit a pull request

## 📄 License

This project is for educational purposes.

## 🙏 Acknowledgments

- Built on academic foundations
- Enhanced for production use
- Optimized for performance and reliability

## 📞 Support

For issues or questions:
- Open an issue on GitHub
- Check existing documentation
- Review the IMPROVEMENTS.md file

---

**Version**: 2.0.0  
**Status**: Production Ready ✅
