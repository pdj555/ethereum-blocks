SHELL := /bin/sh
.DEFAULT_GOAL := help
MAKEFLAGS += --no-print-directory

-include .env

# Ethereum Block Explorer v5.0

SRCDIR   = src
BINDIR   = bin
TOOLSDIR = tools
MAIN     = EthereumBlockExplorer
JAVA     = java -cp $(BINDIR)
JAVAC    = javac -d $(BINDIR) -sourcepath $(SRCDIR)
SOURCES  = $(shell find $(SRCDIR) -name '*.java' ! -name 'Test*.java')
TEST_SOURCES = $(shell find $(SRCDIR) -name 'Test*.java')
TEST_BINDIR = $(BINDIR)/test-classes
JUNIT_VERSION = 1.10.2
JUNIT_JAR = $(TOOLSDIR)/junit-platform-console-standalone-$(JUNIT_VERSION).jar
REPORT_FILE = ethereum-report.md
UI_PORT ?= 4173

.PHONY: help build compile run run-json dashboard block address brief snapshot network anomalies miners report clean check-java check-junit test verify ui ui-build ui-serve ui-clean ui-smoke cli-smoke check-python check-ui-data check-node

help:
	@echo "Ethereum Block Explorer v5.0"
	@echo ""
	@echo "Start here:"
	@echo "  Run 'make help' for the full command guide."
	@echo "  Run 'make dashboard' for the fastest dataset read."
	@echo "  Run 'make ui' to serve the visual explorer at http://localhost:4173."
	@echo "  Run 'make verify' to mirror the local health gate."
	@echo ""
	@echo "Commands:"
	@echo "  make help                 Show the command guide and runtime requirements"
	@echo "  make dashboard            Print the human-readable dashboard"
	@echo "  make ui                   Serve the visual explorer at http://localhost:4173"
	@echo "  make block N=15049311     Inspect one block in JSON"
	@echo "  make address ADDR=0x...   Inspect one address in JSON"
	@echo "  make network              Print the network analysis in JSON"
	@echo "  make report               Write ethereum-report.md"
	@echo "  make run                  Open the small interactive menu"
	@echo "  make brief                Print the action brief"
	@echo "  make snapshot             Print the one-call agent snapshot in JSON"
	@echo "  make anomalies THRESHOLD=1.5  Print anomaly analysis in JSON"
	@echo "  make miners               Print the unique miner breakdown in JSON"
	@echo "  make run-json             Print the JSON overview"
	@echo "  make verify               Run the full local health gate"
	@echo "  make test                 Run the existing JUnit suite"
	@echo "  make cli-smoke            Smoke test the core explorer commands"
	@echo "  make ui-build             Prepare the static web files in web/dist/"
	@echo "  make ui-smoke             Smoke test the browser explorer"
	@echo "  make build                Compile the explorer into $(BINDIR)/"
	@echo "  make clean                Remove compiled explorer artifacts"
	@echo "  make ui-clean             Remove generated web preview files"
	@echo ""
	@echo "Requirements:"
	@echo "  - A working Java runtime"
	@echo "  - A JDK with javac"
	@echo "  - The vendored JUnit runner in $(TOOLSDIR)/"
	@echo "  - Dataset files in the repo root: ethereumP1data.csv and ethereumtransactions1.csv"
	@echo "  - Node.js for 'make cli-smoke', 'make ui-smoke', and 'make verify'"
	@echo "  - Playwright browser binaries via 'npx playwright install --with-deps chromium'"
	@echo "  - Python 3 to serve the browser UI with 'make ui'"

build: compile

check-java:
	@if ! command -v java >/dev/null 2>&1 || ! java -version >/dev/null 2>&1; then \
		echo "Java runtime not available. Install a working Java runtime, then rerun your command."; \
		exit 1; \
	fi
	@if ! command -v javac >/dev/null 2>&1 || ! javac -version >/dev/null 2>&1; then \
		echo "Java compiler not available. Install a JDK, then rerun your command."; \
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

verify:
	@$(MAKE) --no-print-directory test cli-smoke ui-smoke

check-python:
	@if ! command -v python3 >/dev/null 2>&1; then \
		echo "Python 3 not available. Install Python 3, then rerun 'make ui'."; \
		exit 1; \
	fi

check-node:
	@if ! command -v node >/dev/null 2>&1; then \
		echo "Node.js not available. Install Node.js, then rerun 'make cli-smoke' or 'make ui-smoke'."; \
		exit 1; \
	fi

check-ui-data:
	@if [ ! -f "ethereumP1data.csv" ] || [ ! -f "ethereumtransactions1.csv" ]; then \
		echo "UI data files missing. Keep ethereumP1data.csv and ethereumtransactions1.csv in the repo root, then rerun 'make ui-build'."; \
		exit 1; \
	fi

ui: ui-serve

ui-build: check-ui-data
	@rm -rf web/dist
	@mkdir -p web/dist
	@cp web/index.html web/app.css web/app.js web/favicon.svg ethereumP1data.csv ethereumtransactions1.csv web/dist/

ui-serve: check-python ui-build
	@echo "Serving visual explorer at http://localhost:$(UI_PORT)"
	@python3 -m http.server $(UI_PORT) -d web/dist

ui-clean:
	@rm -rf web/dist

ui-smoke: ui-build check-node
	@if [ ! -d node_modules/playwright ]; then \
		echo "Browser smoke dependencies missing. Run 'npm ci' and 'npm run ui:install-browsers', then rerun 'make ui-smoke'."; \
		exit 1; \
	fi
	@node scripts/ui_smoke.mjs

cli-smoke: check-node
	@node scripts/cli_smoke.mjs

run: compile
	@$(JAVA) $(MAIN)

run-json: compile
	@$(JAVA) $(MAIN) --json

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

snapshot: compile
	@$(JAVA) $(MAIN) --json snapshot

network: compile
	@$(JAVA) $(MAIN) --json network

anomalies: compile
	@if [ -n "$(THRESHOLD)" ]; then \
		$(JAVA) $(MAIN) --json anomalies $(THRESHOLD); \
	else \
		$(JAVA) $(MAIN) --json anomalies; \
	fi

miners: compile
	@$(JAVA) $(MAIN) --json miners

report: compile
	@$(JAVA) $(MAIN) report ethereum-report.md

clean:
	@rm -rf $(BINDIR)
	@find $(SRCDIR) -maxdepth 1 -name '*.class' -delete
	@rm -f $(REPORT_FILE)
	@rm -rf web/dist
