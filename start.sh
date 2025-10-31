#!/bin/bash
# Startup script for Ethereum Block Explorer

APP_NAME="Ethereum Block Explorer"
JAR_FILE="target/ethereum-explorer.jar"

# Default JVM options
DEFAULT_JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Override with environment variable if set
JAVA_OPTS="${JAVA_OPTS:-$DEFAULT_JAVA_OPTS}"

echo "========================================="
echo "Starting: $APP_NAME"
echo "========================================="
echo ""

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run: mvn clean package"
    exit 1
fi

# Check if data files exist
if [ ! -f "ethereumP1data.csv" ] || [ ! -f "ethereumtransactions1.csv" ]; then
    echo "Error: Data files not found in current directory"
    echo "Please ensure ethereumP1data.csv and ethereumtransactions1.csv are present"
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

echo "JVM Options: $JAVA_OPTS"
echo "Starting application..."
echo ""

# Run the application
exec java $JAVA_OPTS -jar "$JAR_FILE"
