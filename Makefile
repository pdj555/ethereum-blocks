SHELL := /bin/sh
.DEFAULT_GOAL := help

# Ethereum Block Explorer v5.0

SRCDIR   = src
BINDIR   = bin
TOOLSDIR = tools
MAIN     = EthereumBlockExplorer
JAVA     = java -cp $(BINDIR)
JAVAC    = javac -d $(BINDIR) -sourcepath $(SRCDIR)
SOURCES  = $(shell find $(SRCDIR) -name '*.java' ! -name 'Test*.java' ! -name 'Driver.java')
TEST_SOURCES = $(shell find $(SRCDIR) -name 'Test*.java')
TEST_BINDIR = $(BINDIR)/test-classes
JUNIT_VERSION = 1.10.2
JUNIT_JAR = $(TOOLSDIR)/junit-platform-console-standalone-$(JUNIT_VERSION).jar
REPORT_FILE = ethereum-report.md

.PHONY: help build compile run run-json dashboard block address brief network anomalies miners report clean check-java check-junit test

help:
	@echo "Ethereum Block Explorer v5.0"
	@echo ""
	@echo "Start here:"
	@echo "  make help                 Show the supported explorer commands"
	@echo "  make dashboard            Fastest way to inspect the dataset"
	@echo ""
	@echo "Supported commands:"
	@echo "  make build                Compile the explorer into $(BINDIR)/"
	@echo "  make run                  Open the interactive menu (secondary surface)"
	@echo "  make run-json             Print the JSON overview"
	@echo "  make dashboard            Print the human-readable dashboard"
	@echo "  make block N=15049311     Inspect one block in JSON"
	@echo "  make address ADDR=0x...   Inspect one address in JSON"
	@echo "  make brief                Print the action brief"
	@echo "  make network              Print the network analysis in JSON"
	@echo "  make anomalies            Print anomaly analysis in JSON"
	@echo "  make miners               Print the unique miner breakdown in JSON"
	@echo "  make test                 Run the existing JUnit suite"
	@echo "  make report               Write ethereum-report.md"
	@echo "  make clean                Remove compiled explorer artifacts"
	@echo ""
	@echo "Requirements:"
	@echo "  - A working Java runtime"
	@echo "  - A JDK with javac"
	@echo "  - Dataset files in the repo root: ethereumP1data.csv and ethereumtransactions1.csv"

build: compile

check-java:
	@if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then \
		echo "Java runtime not available. Install a working Java runtime, then rerun 'make help'."; \
		exit 1; \
	fi
	@if ! command -v javac >/dev/null 2>&1 || ! javac -version >/dev/null 2>&1; then \
		echo "Java compiler not available. Install a JDK, then rerun 'make help'."; \
		exit 1; \
	fi

compile: check-java
	@mkdir -p $(BINDIR)
	@$(JAVAC) $(SOURCES)

check-junit:
	@if [ ! -f "$(JUNIT_JAR)" ]; then \
		echo "Vendored JUnit runner missing at $(JUNIT_JAR). Restore it from git, then rerun 'make test'."; \
		exit 1; \
	fi

test: compile check-junit
	@rm -rf $(TEST_BINDIR)
	@mkdir -p $(TEST_BINDIR)
	@javac -cp "$(JUNIT_JAR):$(BINDIR)" -d $(TEST_BINDIR) $(TEST_SOURCES)
	@java -jar $(JUNIT_JAR) --class-path "$(BINDIR):$(TEST_BINDIR)" --scan-class-path

run: compile
	@$(JAVA) $(MAIN)

run-json: compile
	@$(JAVA) $(MAIN) --json overview

dashboard: compile
	@$(JAVA) $(MAIN) dashboard

block: compile
	@if [ -z "$(N)" ]; then echo "Missing block number. Try: make block N=15049311"; exit 1; fi
	@$(JAVA) $(MAIN) --json block $(N)

address: compile
	@if [ -z "$(ADDR)" ]; then echo "Missing address. Try: make address ADDR=0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f"; exit 1; fi
	@$(JAVA) $(MAIN) --json address $(ADDR)

brief: compile
	@$(JAVA) $(MAIN) brief

network: compile
	@$(JAVA) $(MAIN) --json network

anomalies: compile
	@$(JAVA) $(MAIN) --json anomalies

miners: compile
	@$(JAVA) $(MAIN) --json miners

report: compile
	@$(JAVA) $(MAIN) report ethereum-report.md

clean:
	@rm -rf $(BINDIR)
	@find $(SRCDIR) -maxdepth 1 -name '*.class' -delete
	@rm -f $(REPORT_FILE)
