# Ethereum Block Explorer - Installation & Deployment Guide

## Table of Contents
- [Prerequisites](#prerequisites)
- [Installation Methods](#installation-methods)
- [Quick Start](#quick-start)
- [Development Setup](#development-setup)
- [Production Deployment](#production-deployment)
- [Docker Deployment](#docker-deployment)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Java Development Kit (JDK)**: Version 11 or higher
  - Download from: https://adoptium.net/
  - Verify: `java -version`

### Optional Software
- **Maven**: Version 3.6 or higher (for building from source)
  - Download from: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`

- **Docker**: Latest version (for containerized deployment)
  - Download from: https://www.docker.com/get-started
  - Verify: `docker --version`

### System Requirements
- **Memory**: Minimum 512MB RAM, recommended 1GB+
- **Disk Space**: ~100MB for application and data files
- **Operating System**: Windows, macOS, Linux

## Installation Methods

### Method 1: Pre-built JAR (Recommended for Users)

1. **Download the release**
   ```bash
   # Clone the repository
   git clone https://github.com/pdj555/ethereum-blocks.git
   cd ethereum-blocks
   ```

2. **Build the application**
   ```bash
   mvn clean package
   ```

3. **Run the application**
   ```bash
   java -jar target/ethereum-explorer.jar
   ```

### Method 2: From Source (Recommended for Developers)

1. **Clone the repository**
   ```bash
   git clone https://github.com/pdj555/ethereum-blocks.git
   cd ethereum-blocks
   ```

2. **Build with Maven**
   ```bash
   mvn clean install
   ```

3. **Run with Maven**
   ```bash
   mvn exec:java -Dexec.mainClass="EthereumBlockExplorer"
   ```

### Method 3: Direct Compilation (No Maven Required)

1. **Clone the repository**
   ```bash
   git clone https://github.com/pdj555/ethereum-blocks.git
   cd ethereum-blocks
   ```

2. **Compile the source files**
   ```bash
   mkdir -p bin
   javac -d bin src/*.java
   ```

3. **Run the application**
   ```bash
   java -cp bin EthereumBlockExplorer
   ```

## Quick Start

### Running the Application

1. **Ensure data files are present**
   - `ethereumP1data.csv` - Block data
   - `ethereumtransactions1.csv` - Transaction data

2. **Start the application**
   ```bash
   ./start.sh
   ```
   Or on Windows:
   ```cmd
   java -jar target\ethereum-explorer.jar
   ```

3. **Navigate the menu**
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

### Health Check

Run the health check script to verify your installation:
```bash
./health-check.sh
```

This will verify:
- Java installation
- Data files presence
- JAR file availability
- System resources

## Development Setup

### IDE Configuration

#### IntelliJ IDEA
1. Open IntelliJ IDEA
2. File → Open → Select `pom.xml`
3. Let IntelliJ import the Maven project
4. Wait for indexing to complete
5. Right-click on `EthereumBlockExplorer.java` → Run

#### Eclipse
1. Open Eclipse
2. File → Import → Existing Maven Projects
3. Select the project directory
4. Finish
5. Right-click on project → Run As → Java Application

#### VS Code
1. Install "Extension Pack for Java"
2. Open the project folder
3. VS Code will automatically detect the Maven project
4. Press F5 to run

### Building and Testing

```bash
# Clean build
mvn clean

# Compile only
mvn compile

# Run tests
mvn test

# Package without tests
mvn package -DskipTests

# Package with tests
mvn package

# Generate JavaDoc
mvn javadoc:javadoc

# Run code quality checks
mvn checkstyle:check
mvn spotbugs:check

# Generate site with reports
mvn site
```

## Production Deployment

### Step 1: Build Production JAR

```bash
mvn clean package
```

This creates:
- `target/ethereum-explorer.jar` - Executable uber JAR with all dependencies

### Step 2: Prepare Deployment Directory

```bash
mkdir -p /opt/ethereum-explorer
cp target/ethereum-explorer.jar /opt/ethereum-explorer/
cp ethereumP1data.csv /opt/ethereum-explorer/
cp ethereumtransactions1.csv /opt/ethereum-explorer/
cp start.sh /opt/ethereum-explorer/
```

### Step 3: Configure JVM Options

Create a configuration file `/opt/ethereum-explorer/config.sh`:
```bash
#!/bin/bash
export JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Step 4: Create Systemd Service (Linux)

Create `/etc/systemd/system/ethereum-explorer.service`:
```ini
[Unit]
Description=Ethereum Block Explorer
After=network.target

[Service]
Type=simple
User=ethereum
WorkingDirectory=/opt/ethereum-explorer
ExecStart=/usr/bin/java -Xmx1g -Xms512m -jar /opt/ethereum-explorer/ethereum-explorer.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:
```bash
sudo systemctl daemon-reload
sudo systemctl enable ethereum-explorer
sudo systemctl start ethereum-explorer
sudo systemctl status ethereum-explorer
```

### Step 5: Configure Logging

Logs are written to the `logs/` directory:
- `ethereum-explorer.log` - Application logs
- `ethereum-explorer-error.log` - Error logs

Configure log rotation in `/etc/logrotate.d/ethereum-explorer`:
```
/opt/ethereum-explorer/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    missingok
    create 0640 ethereum ethereum
}
```

## Docker Deployment

### Option 1: Using Docker

```bash
# Build the image
docker build -t ethereum-explorer:2.0.0 .

# Run the container
docker run -it --rm \
  --name ethereum-explorer \
  -v $(pwd)/logs:/app/logs \
  ethereum-explorer:2.0.0
```

### Option 2: Using Docker Compose

```bash
# Start the application
docker-compose up

# Run in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop the application
docker-compose down
```

### Docker Configuration

Customize the deployment in `docker-compose.yml`:
```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m
volumes:
  - ./logs:/app/logs
  - ./custom-data.csv:/app/ethereumP1data.csv
```

## Troubleshooting

### Common Issues

#### Issue: "Error: Data file not found"
**Solution:**
Ensure `ethereumP1data.csv` and `ethereumtransactions1.csv` are in the same directory as the JAR file.

#### Issue: "OutOfMemoryError"
**Solution:**
Increase JVM heap size:
```bash
java -Xmx1g -jar ethereum-explorer.jar
```

#### Issue: "UnsupportedClassVersionError"
**Solution:**
You're using an old Java version. Install Java 11 or higher.

#### Issue: Build fails with Maven
**Solution:**
```bash
# Clean Maven cache
mvn dependency:purge-local-repository

# Rebuild
mvn clean install -U
```

#### Issue: Port conflicts (if adding web interface later)
**Solution:**
```bash
# Check what's using the port
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Performance Tuning

#### JVM Tuning
```bash
# For large datasets
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=4"

# For low memory systems
export JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC"
```

#### Application Tuning
Edit `src/main/resources/application.properties`:
```properties
cache.enabled=true
cache.size.max=2000
validation.strict=false  # For better performance with trusted data
```

### Logs and Debugging

#### Enable Debug Logging
Edit `src/main/resources/logback.xml`:
```xml
<root level="DEBUG">
```

#### View Real-time Logs
```bash
tail -f logs/ethereum-explorer.log
```

#### View Errors Only
```bash
tail -f logs/ethereum-explorer-error.log
```

## Security Considerations

### Production Deployment Checklist
- [ ] Run application as non-root user
- [ ] Set appropriate file permissions (640 for logs, 644 for data)
- [ ] Configure firewall rules if adding network features
- [ ] Regularly update Java runtime
- [ ] Monitor logs for suspicious activity
- [ ] Implement log rotation to prevent disk filling
- [ ] Use strong validation settings in production

### File Permissions
```bash
# Set ownership
sudo chown -R ethereum:ethereum /opt/ethereum-explorer

# Set permissions
chmod 755 /opt/ethereum-explorer
chmod 644 /opt/ethereum-explorer/*.csv
chmod 755 /opt/ethereum-explorer/start.sh
chmod 640 /opt/ethereum-explorer/logs/*.log
```

## Support

For issues or questions:
- Check the [main README](README-PRODUCTION.md)
- Review [IMPROVEMENTS.md](IMPROVEMENTS.md)
- Open an issue on GitHub
- Check existing documentation in `doc/`

## Version History

- **2.0.0**: Production-ready release with Maven, Docker, CI/CD
- **1.0.0**: Initial academic implementation

---

**Last Updated**: October 2025  
**Maintained By**: Ethereum Block Explorer Team
