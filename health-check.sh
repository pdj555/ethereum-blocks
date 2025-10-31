#!/bin/bash
# Health Check Script for Ethereum Block Explorer

set -e

APP_NAME="Ethereum Block Explorer"
LOG_FILE="logs/ethereum-explorer.log"
ERROR_LOG_FILE="logs/ethereum-explorer-error.log"

echo "========================================="
echo "Health Check: $APP_NAME"
echo "========================================="
echo ""

# Check if Java is installed
echo "1. Checking Java installation..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "   ✓ Java found: $JAVA_VERSION"
else
    echo "   ✗ Java not found. Please install Java 11 or higher."
    exit 1
fi

# Check if data files exist
echo ""
echo "2. Checking data files..."
if [ -f "ethereumP1data.csv" ]; then
    SIZE=$(du -h ethereumP1data.csv | cut -f1)
    echo "   ✓ ethereumP1data.csv found ($SIZE)"
else
    echo "   ✗ ethereumP1data.csv not found"
    exit 1
fi

if [ -f "ethereumtransactions1.csv" ]; then
    SIZE=$(du -h ethereumtransactions1.csv | cut -f1)
    echo "   ✓ ethereumtransactions1.csv found ($SIZE)"
else
    echo "   ✗ ethereumtransactions1.csv not found"
    exit 1
fi

# Check if JAR file exists
echo ""
echo "3. Checking application JAR..."
if [ -f "target/ethereum-explorer.jar" ]; then
    SIZE=$(du -h target/ethereum-explorer.jar | cut -f1)
    echo "   ✓ ethereum-explorer.jar found ($SIZE)"
else
    echo "   ✗ ethereum-explorer.jar not found. Run: mvn clean package"
    exit 1
fi

# Check logs directory
echo ""
echo "4. Checking logs directory..."
if [ ! -d "logs" ]; then
    echo "   Creating logs directory..."
    mkdir -p logs
    echo "   ✓ Logs directory created"
else
    echo "   ✓ Logs directory exists"
    if [ -f "$LOG_FILE" ]; then
        SIZE=$(du -h "$LOG_FILE" | cut -f1)
        LINES=$(wc -l < "$LOG_FILE")
        echo "      - Application log: $SIZE ($LINES lines)"
    fi
    if [ -f "$ERROR_LOG_FILE" ]; then
        SIZE=$(du -h "$ERROR_LOG_FILE" | cut -f1)
        LINES=$(wc -l < "$ERROR_LOG_FILE")
        echo "      - Error log: $SIZE ($LINES lines)"
    fi
fi

# Check disk space
echo ""
echo "5. Checking disk space..."
DISK_USAGE=$(df -h . | tail -1 | awk '{print $5}')
echo "   Disk usage: $DISK_USAGE"

# Check memory
echo ""
echo "6. Checking system memory..."
if command -v free &> /dev/null; then
    free -h | grep -E "Mem|Swap"
elif command -v vm_stat &> /dev/null; then
    # macOS
    echo "   Using vm_stat (macOS)"
    vm_stat | head -4
fi

echo ""
echo "========================================="
echo "Health Check Complete: All systems OK ✓"
echo "========================================="
echo ""
echo "To run the application:"
echo "  java -jar target/ethereum-explorer.jar"
echo ""
