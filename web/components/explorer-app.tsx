"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { buildDataset, parseBlocksToViews } from "@/lib/dataset";
import { parseBlocks, parseTransactions } from "@/lib/parser";
import type { Dataset, SearchMode } from "@/lib/types";
import { formatEth, formatInteger } from "@/lib/utils";
import { BlockTimeline } from "./block-timeline";
import { LiveStatusPanel } from "./live-status-panel";
import { BootLoader } from "./boot-loader";
import { CommandDock } from "./command-dock";
import { ResultPanel, SideRail, SummaryStrip } from "./explorer-panels";
import { SectionFrame } from "./section-frame";
import { SparkChart } from "./spark-chart";
import { ThemeToggle } from "./theme-toggle";

const DEFAULT_BLOCK = "15049311";

type MessageState = {
  kicker: string;
  title: string;
  body: string;
} | null;

export function ExplorerApp() {
  const [loading, setLoading] = useState(true);
  const [fatal, setFatal] = useState<string | null>(null);
  const [dataset, setDataset] = useState<Dataset | null>(null);
  const [mode, setMode] = useState<SearchMode>("block");
  const [query, setQuery] = useState(DEFAULT_BLOCK);
  const [message, setMessage] = useState<MessageState>(null);
  const [bootVisible, setBootVisible] = useState(true);
  const [bootStage, setBootStage] = useState("initializing");
  const [bootProgress, setBootProgress] = useState(0.08);
  const [dockSticky, setDockSticky] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const dockSentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;

    async function loadData() {
      try {
        setBootStage("reading ethereumP1data.csv");
        setBootProgress(0.18);
        const blocksCsv = await fetchText("/ethereumP1data.csv");

        if (!active) {
          return;
        }

        setBootStage("reading ethereumtransactions1.csv");
        setBootProgress(0.42);
        const transactionsCsv = await fetchText("/ethereumtransactions1.csv");

        if (!active) {
          return;
        }

        setBootStage("indexing blocks and addresses");
        setBootProgress(0.72);
        const blocks = parseBlocksToViews(parseBlocks(blocksCsv));
        const transactions = parseTransactions(transactionsCsv);
        const nextDataset = buildDataset(blocks, transactions);

        if (!active) {
          return;
        }

        setBootStage("ready");
        setBootProgress(1);
        setDataset(nextDataset);
        setLoading(false);
        window.setTimeout(() => setBootVisible(false), 320);
      } catch (error) {
        console.error(error);
        if (!active) {
          return;
        }
        setFatal("UI data is missing.");
        setLoading(false);
        setBootVisible(false);
      }
    }

    loadData();
    return () => {
      active = false;
    };
  }, []);

  const hydrateFromHash = useCallback(() => {
    const rawHash = window.location.hash.replace(/^#/, "");
    if (!rawHash) {
      setMode("block");
      setQuery(DEFAULT_BLOCK);
      return;
    }

    const parts = rawHash.split("/");
    const nextMode: SearchMode = parts[0] === "address" ? "address" : "block";
    const value = decodeURIComponent(parts.slice(1).join("/")) || DEFAULT_BLOCK;
    setMode(nextMode);
    setQuery(nextMode === "address" ? value.toLowerCase() : value);
  }, []);

  useEffect(() => {
    if (loading) {
      return;
    }
    hydrateFromHash();
  }, [hydrateFromHash, loading]);

  useEffect(() => {
    function onHashChange() {
      setMessage(null);
      hydrateFromHash();
    }
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, [hydrateFromHash]);

  useEffect(() => {
    const node = dockSentinelRef.current;
    if (!node) {
      return;
    }
    const observer = new IntersectionObserver(
      ([entry]) => setDockSticky(!entry.isIntersecting),
      { rootMargin: "-12px 0px 0px 0px", threshold: 0 }
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [loading]);

  const applyQuery = useCallback(
    (rawValue: string) => {
      const cleaned = (rawValue || "").trim();
      setMessage(null);

      if (mode === "block") {
        if (!/^\d+$/.test(cleaned)) {
          setMessage({
            kicker: "Block lookup",
            title: "Enter a block number like 15049311.",
            body: "The block search expects digits only."
          });
          return;
        }
        const nextHash = `block/${cleaned}`;
        if (window.location.hash.slice(1) !== nextHash) {
          window.location.hash = nextHash;
          return;
        }
        setQuery(cleaned);
        return;
      }

      const normalized = cleaned.toLowerCase();
      if (!/^0x[0-9a-f]{40}$/.test(normalized)) {
        setMessage({
          kicker: "Address profile",
          title: "Enter a full Ethereum address.",
          body: "Use 0x followed by 40 hex characters, for example 0x58a5b1a1c67e984247a0c78f2875b0f9c781b64f."
        });
        return;
      }

      const nextHash = `address/${normalized}`;
      if (window.location.hash.slice(1) !== nextHash) {
        window.location.hash = nextHash;
        return;
      }
      setQuery(normalized);
    },
    [mode]
  );

  const handleModeChange = useCallback(
    (nextMode: SearchMode) => {
      setMode(nextMode);
      setMessage(null);
      if (nextMode === "block") {
        if (/^0x[0-9a-f]{40}$/i.test(query)) {
          setQuery(DEFAULT_BLOCK);
        }
      } else if (!/^0x[0-9a-f]{40}$/i.test(query)) {
        setQuery("");
      }
    },
    [query]
  );

  const handleJump = useCallback((jumpMode: SearchMode, value: string) => {
    setMode(jumpMode);
    setQuery(value);
    setMessage(null);
    window.location.hash = `${jumpMode}/${encodeURIComponent(value)}`;
  }, []);

  useEffect(() => {
    if (!dataset || mode !== "block" || !/^\d+$/.test(query)) {
      return;
    }

    const blockNumbers = dataset.blockNumbers;
    const currentQuery = query;

    function onKeyDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      if (target?.tagName === "INPUT" || target?.tagName === "TEXTAREA") {
        return;
      }
      if (event.key !== "[" && event.key !== "]") {
        return;
      }

      const index = blockNumbers.indexOf(Number(currentQuery));
      if (index < 0) {
        return;
      }

      const nextIndex = event.key === "[" ? index - 1 : index + 1;
      const nextBlock = blockNumbers[nextIndex];
      if (nextBlock !== undefined) {
        event.preventDefault();
        handleJump("block", String(nextBlock));
      }
    }

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [dataset, mode, query, handleJump]);

  const handleCopy = useCallback(async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setToast("Address copied");
    } catch {
      setToast("Copy failed");
    }
    window.setTimeout(() => setToast(null), 1800);
  }, []);

  const chartValues = useMemo(() => {
    if (!dataset) {
      return null;
    }
    const txTotal = dataset.txSeries.reduce((sum, value) => sum + value, 0);
    const costTotal = dataset.costSeries.reduce((sum, value) => sum + value, 0);
    return {
      txTotal,
      costTotal,
      txPeak: Math.max(...dataset.txSeries),
      costPeak: Math.max(...dataset.costSeries)
    };
  }, [dataset]);

  const activeBlock = mode === "block" && /^\d+$/.test(query) ? Number(query) : null;

  return (
    <>
      <BootLoader visible={bootVisible} stage={bootStage} progress={bootProgress} />
      {toast ? (
        <div className="toast" role="status">
          {toast}
        </div>
      ) : null}

      <div className="page">
        <header className="top-bar">
          <div className="top-bar__left">
            <span className="top-bar__status">
              <span className={`live-dot${loading ? " is-loading" : ""}`} aria-hidden="true" />
              {dataset
                ? `${formatInteger(dataset.overview.blocksLoaded)} blocks · ${formatInteger(dataset.overview.parsedTransactions)} parsed tx`
                : "100 blocks · static browser explorer"}
            </span>
            <ThemeToggle />
          </div>
          <strong className="top-bar__brand">Ethereum Block Explorer</strong>
          <div className="top-bar__right">
            <a className="social-link" href="https://github.com/pdj555/ethereum-blocks" target="_blank" rel="noreferrer">
              github
            </a>
          </div>
        </header>

        <section className="hero">
          <div className="hero__grid">
            <div className="hero__main">
              <p className="hero__eyebrow">
                100blk curated slice · {dataset ? `${dataset.overview.blockRangeStart}–${dataset.overview.blockRangeEnd}` : "loading"}
              </p>
              <h1 className="hero__title">Ethereum Block Explorer</h1>
              <p className="hero__copy">
                One hundred blocks, parsed locally in your browser. Scrub the timeline, jump addresses, inspect every
                row the CSV can prove.
              </p>
              <div ref={dockSentinelRef} className="dock-sentinel" aria-hidden="true" />
              <CommandDock
                mode={mode}
                query={query}
                dataset={dataset}
                sticky={dockSticky}
                onModeChange={handleModeChange}
                onQueryChange={setQuery}
                onSubmit={applyQuery}
              />
            </div>
            <LiveStatusPanel dataset={dataset} loading={loading} activeBlock={activeBlock} />
          </div>
        </section>

        <div className="dashboard-deck">
          <SummaryStrip dataset={dataset} loading={loading} />

          {dataset ? (
            <SectionFrame title="Block timeline">
              <BlockTimeline
                dataset={dataset}
                activeBlock={activeBlock}
                onSelect={(blockNumber) => handleJump("block", String(blockNumber))}
              />
            </SectionFrame>
          ) : null}

          {dataset && chartValues ? (
            <SectionFrame title="Chain activity">
              <div className="chart-grid">
                <SparkChart
                  values={dataset.txSeries}
                  blockNumbers={dataset.blockNumbers}
                  label="Parsed transactions / block"
                  valueLabel={`${formatInteger(chartValues.txTotal)} total · peak ${formatInteger(chartValues.txPeak)}`}
                  formatHover={(value, blockNumber) =>
                    blockNumber ? `Block ${blockNumber} · ${formatInteger(value)} tx` : formatInteger(value)
                  }
                  onSelect={(blockNumber) => handleJump("block", String(blockNumber))}
                />
                <SparkChart
                  values={dataset.costSeries}
                  blockNumbers={dataset.blockNumbers}
                  label="Block cost / ETH"
                  valueLabel={`${formatEth(chartValues.costTotal)} total · peak ${formatEth(chartValues.costPeak)}`}
                  formatHover={(value, blockNumber) =>
                    blockNumber ? `Block ${blockNumber} · ${formatEth(value)}` : formatEth(value)
                  }
                  onSelect={(blockNumber) => handleJump("block", String(blockNumber))}
                />
              </div>
            </SectionFrame>
          ) : null}
        </div>

        <div className="workspace">
          <ResultPanel
            dataset={dataset}
            mode={mode}
            query={query}
            loading={loading}
            fatal={fatal}
            message={message}
            onJump={handleJump}
            onCopy={handleCopy}
          />
          {dataset ? <SideRail dataset={dataset} onJump={handleJump} /> : null}
        </div>

        <footer className="footer">
          <p>100 blocks · static browser explorer · no backend</p>
          <p>
            <a href="https://github.com/pdj555/ethereum-blocks">github.com/pdj555/ethereum-blocks</a>
          </p>
        </footer>
      </div>
    </>
  );
}

async function fetchText(url: string): Promise<string> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to load ${url}`);
  }
  return response.text();
}
