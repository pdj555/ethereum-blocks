# Ethereum Block Explorer v5.0 — Agent-Native Edition
# Build system for blockchain analytics engine

SRCDIR   = src
BINDIR   = bin
MAIN     = EthereumBlockExplorer
JAVA     = java -cp $(BINDIR)
JAVAC    = javac -d $(BINDIR) -sourcepath $(SRCDIR)

.PHONY: all clean run run-json dashboard network anomalies help

all: compile

compile:
	@mkdir -p $(BINDIR)
	$(JAVAC) $(shell find $(SRCDIR) -name '*.java' ! -name 'Test*.java') 2>&1 | grep -v "^Note:" || true

run: compile
	$(JAVA) $(MAIN)

# ── Agent-Native Commands (JSON output) ──

run-json: compile
	$(JAVA) $(MAIN) --json overview

dashboard: compile
	$(JAVA) $(MAIN) dashboard

network: compile
	$(JAVA) $(MAIN) --json network

anomalies: compile
	$(JAVA) $(MAIN) --json anomalies

block: compile
	@if [ -z "$(N)" ]; then echo "Usage: make block N=15049311"; exit 1; fi
	$(JAVA) $(MAIN) --json block $(N)

address: compile
	@if [ -z "$(ADDR)" ]; then echo "Usage: make address ADDR=0x..."; exit 1; fi
	$(JAVA) $(MAIN) --json address $(ADDR)

miners: compile
	$(JAVA) $(MAIN) --json miners

report: compile
	$(JAVA) $(MAIN) report ethereum-report.md

# ── Cleanup ──

clean:
	rm -rf $(BINDIR)/*.class

help:
	@echo "Ethereum Block Explorer v5.0 — Agent-Native Edition"
	@echo ""
	@echo "Build & Run:"
	@echo "  make              Build all sources"
	@echo "  make run          Interactive mode"
	@echo "  make run-json     JSON overview (agent mode)"
	@echo "  make dashboard    Human-readable dashboard"
	@echo "  make network      Network graph analysis (JSON)"
	@echo "  make anomalies    Anomaly detection (JSON)"
	@echo "  make miners       Miner concentration analysis (JSON)"
	@echo "  make block N=NUM  Block details (JSON)"
	@echo "  make address ADDR=0x...  Address intel (JSON)"
	@echo "  make report       Export markdown report"
	@echo "  make clean        Remove compiled classes"
