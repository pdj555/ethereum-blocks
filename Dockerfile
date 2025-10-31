# Multi-stage build for Ethereum Block Explorer
FROM maven:3.9-eclipse-temurin-11 AS builder

WORKDIR /app

# Copy POM and source code
COPY pom.xml .
COPY src ./src
COPY ethereumP1data.csv .
COPY ethereumtransactions1.csv .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/ethereum-explorer.jar /app/ethereum-explorer.jar
COPY --from=builder /app/ethereumP1data.csv /app/
COPY --from=builder /app/ethereumtransactions1.csv /app/

# Create directory for logs
RUN mkdir -p /app/logs

# Run the application
ENTRYPOINT ["java", "-jar", "/app/ethereum-explorer.jar"]

# Metadata
LABEL maintainer="Ethereum Block Explorer"
LABEL version="2.0.0"
LABEL description="Production-ready Ethereum blockchain explorer"
