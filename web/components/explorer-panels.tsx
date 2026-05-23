"use client";

import { useEffect, useState } from "react";
import type { Dataset, SearchMode } from "@/lib/types";
import {
  formatDecimal,
  formatEth,
  formatInteger,
  formatPercent,
  formatRange,
  formatSignedEth,
  looksLikeAddress,
  shorten
} from "@/lib/utils";
import { SectionFrame } from "./section-frame";

type JumpHandler = (mode: SearchMode, value: string) => void;

type SummaryStripProps = {
  dataset: Dataset | null;
  loading: boolean;
};

export function SummaryStrip({ dataset, loading }: SummaryStripProps) {
  const items = loading || !dataset
    ? [
        ["Blocks", "Loading", "Reading CSV data"],
        ["Miners", "-", "Preparing counts"],
        ["Transactions", "-", "Preparing activity"],
        ["Addresses", "-", "Preparing search"]
      ]
    : [
        [
          "Loaded",
          `${formatInteger(dataset.overview.blocksLoaded)} blocks`,
          formatRange(dataset.overview.blockRangeStart, dataset.overview.blockRangeEnd)
        ],
        [
          "Miners",
          formatInteger(dataset.overview.uniqueMiners),
          `${formatPercent(dataset.overview.topMinerShare)} top share`
        ],
        [
          "Transactions",
          formatInteger(dataset.overview.parsedTransactions),
          `${formatInteger(dataset.overview.totalTransactionsMetadata)} in metadata`
        ],
        [
          "Addresses",
          formatInteger(dataset.overview.activeAddresses),
          `${formatInteger(dataset.overview.contractCreations)} contract creations`
        ]
      ];

  return (
    <div className="metrics-grid" aria-live="polite">
      {items.map(([label, value, note]) => (
        <article key={label} className="metric-card">
          <span className="metric-card__label">{label}</span>
          <strong className="metric-card__value">{value}</strong>
          <span className="metric-card__note">{note}</span>
        </article>
      ))}
    </div>
  );
}

type SideRailProps = {
  dataset: Dataset;
  onJump: JumpHandler;
};

export function SideRail({ dataset, onJump }: SideRailProps) {
  const { overview } = dataset;

  return (
    <div className="rail-stack">
      <SectionFrame title="Snapshot">
        <div className="snapshot-meta">
          <SnapshotRow
            label="Blocks"
            value={formatRange(overview.blockRangeStart, overview.blockRangeEnd)}
          />
          <SnapshotRow label="Avg tx / block" value={formatDecimal(overview.avgTransactionsPerBlock, 2)} />
          <SnapshotRow label="Avg cost / parsed tx" value={formatEth(overview.avgCostPerTxEth)} />
          <SnapshotRow label="Parsed blocks" value={formatInteger(overview.blocksWithParsedTransactions)} />
        </div>
      </SectionFrame>
      <SectionFrame title="Top miners">
        <div className="action-list">
          {dataset.topMiners.map((item) => (
            <JumpButton
              key={item.address}
              label={shorten(item.address)}
              meta={`${item.count} blocks | latest ${item.featuredBlock}`}
              mode="block"
              value={String(item.featuredBlock)}
              onJump={onJump}
            />
          ))}
        </div>
      </SectionFrame>
      <SectionFrame title="Hot blocks">
        <div className="action-list">
          {dataset.hotBlocks.map((item) => (
            <JumpButton
              key={item.number}
              label={`Block ${item.number}`}
              meta={`${item.transactionCountMetadata} tx metadata`}
              mode="block"
              value={String(item.number)}
              onJump={onJump}
            />
          ))}
        </div>
      </SectionFrame>
      <SectionFrame title="Active addresses">
        <div className="action-list">
          {dataset.activeAddresses.map((item) => (
            <JumpButton
              key={item.address}
              label={shorten(item.address)}
              meta={`${item.totalInteractions} touches`}
              mode="address"
              value={item.address}
              onJump={onJump}
            />
          ))}
        </div>
      </SectionFrame>
      <SectionFrame title="Network glance">
        <div className="glance-list">
          <GlanceRow label="Largest tx" value={`Block ${dataset.largestTransaction.blockNumber}`} />
          <GlanceRow label="Cost" value={formatEth(dataset.largestTransaction.costEth)} />
          <GlanceRow label="Heaviest sender" value={shorten(dataset.heaviestSender.address)} />
          <GlanceRow label="Heaviest receiver" value={shorten(dataset.heaviestReceiver.address)} />
          <GlanceRow label="Avg tx / block" value={formatDecimal(overview.avgTransactionsPerBlock, 2)} />
        </div>
      </SectionFrame>
    </div>
  );
}

type ResultPanelProps = {
  dataset: Dataset | null;
  mode: SearchMode;
  query: string;
  loading: boolean;
  fatal: string | null;
  message: { kicker: string; title: string; body: string } | null;
  onJump: JumpHandler;
  onCopy?: (value: string) => void;
};

export function ResultPanel({
  dataset,
  mode,
  query,
  loading,
  fatal,
  message,
  onJump,
  onCopy
}: ResultPanelProps) {
  const [swapping, setSwapping] = useState(false);

  useEffect(() => {
    setSwapping(true);
    const timer = window.setTimeout(() => setSwapping(false), 90);
    return () => window.clearTimeout(timer);
  }, [mode, query, loading, fatal, message, dataset]);

  if (loading) {
    return (
      <SectionFrame title="Block lookup">
        <PanelHeader kicker="Loading" title="Preparing the explorer" meta="Reading block and transaction CSV data." />
        <div className="empty-state">
          <p>The browser UI parses the dataset locally so you can search without a backend.</p>
        </div>
      </SectionFrame>
    );
  }

  if (fatal) {
    return (
      <SectionFrame title="Setup">
        <PanelHeader kicker="Setup" title={fatal} meta="" />
        <div className="empty-state">
          <p>
            Run make ui-build, then reopen make ui and refresh the page. The browser surface expects
            ethereumP1data.csv and ethereumtransactions1.csv beside the app.
          </p>
        </div>
      </SectionFrame>
    );
  }

  if (message) {
    return (
      <SectionFrame title={message.kicker}>
        <PanelHeader kicker={message.kicker} title={message.title} meta="" />
        <div className="empty-state">
          <p>{message.body}</p>
        </div>
      </SectionFrame>
    );
  }

  if (!dataset) {
    return null;
  }

  if (mode === "block") {
    return <BlockResult query={query} dataset={dataset} swapping={swapping} onJump={onJump} />;
  }

  return <AddressResult query={query} dataset={dataset} swapping={swapping} onJump={onJump} onCopy={onCopy} />;
}

function BlockResult({
  query,
  dataset,
  swapping,
  onJump
}: {
  query: string;
  dataset: Dataset;
  swapping: boolean;
  onJump: JumpHandler;
}) {
  const blockNumber = Number(query);
  const block = dataset.blockMap.get(blockNumber);

  if (!block) {
    return (
      <SectionFrame title="Block lookup">
        <PanelHeader
          kicker="Block lookup"
          title={`Block ${query} is not in the loaded dataset.`}
          meta={`Choose a block between ${dataset.overview.blockRangeStart} and ${dataset.overview.blockRangeEnd}.`}
        />
        <div className="empty-state">
          <p>
            Choose a block between {dataset.overview.blockRangeStart} and {dataset.overview.blockRangeEnd}.
          </p>
        </div>
      </SectionFrame>
    );
  }

  return (
    <SectionFrame title="Block lookup">
      <PanelHeader
        kicker="Block"
        title={`Block ${block.number}`}
        meta={`${block.displayTimestamp} · Miner ${shorten(block.miner)}`}
      />
      <BlockNav dataset={dataset} current={block.number} onJump={onJump} />
      <div className={`result-body${swapping ? " is-swapping" : ""}`}>
        <div className="stat-grid">
          <Stat
            label="Miner"
            value={<JumpLink mode="address" value={block.miner} label={shorten(block.miner)} onJump={onJump} />}
          />
          <Stat label="Parsed tx" value={formatInteger(block.parsedTransactionCount)} />
          <Stat label="Metadata tx" value={formatInteger(block.transactionCountMetadata)} />
          <Stat label="Avg cost" value={formatEth(block.avgCostEth)} />
        </div>
        <div className="detail-grid">
          <DetailBlock
            title="Block view"
            rows={[
              ["Timestamp", block.displayTimestamp],
              ["Total cost", formatEth(block.totalCostEth)],
              ["Unique senders", formatInteger(block.uniqueSenders)],
              ["Unique receivers", formatInteger(block.uniqueReceivers)]
            ]}
          />
          <DetailBlock
            title="Top senders"
            rows={
              block.topSenders.length
                ? block.topSenders.map((item) => [
                    <JumpLink
                      key={item.address}
                      mode="address"
                      value={item.address}
                      label={shorten(item.address)}
                      onJump={onJump}
                    />,
                    `${formatInteger(item.count)} tx`
                  ])
                : [["No parsed senders for this block.", ""]]
            }
          />
        </div>
        <div className="transactions-wrap">
          <h3>Transactions</h3>
          {block.transactions.length ? (
            <TransactionTable transactions={block.transactions} onJump={onJump} />
          ) : (
            <div className="empty-state">
              <p>
                No parsed transactions were loaded for this block. The block metadata is still available above.
              </p>
            </div>
          )}
        </div>
      </div>
    </SectionFrame>
  );
}

function AddressResult({
  query,
  dataset,
  swapping,
  onJump,
  onCopy
}: {
  query: string;
  dataset: Dataset;
  swapping: boolean;
  onJump: JumpHandler;
  onCopy?: (value: string) => void;
}) {
  const address = (query || "").toLowerCase();
  const profile = dataset.addressMap.get(address);

  if (!profile) {
    return (
      <SectionFrame title="Address profile">
        <PanelHeader
          kicker="Address profile"
          title={`No parsed activity was loaded for ${address}.`}
          meta="Paste another address or open one from the block and miner lists."
        />
        <div className="empty-state">
          <p>Paste another address or open one from the block and miner lists.</p>
        </div>
      </SectionFrame>
    );
  }

  return (
    <SectionFrame title="Address profile">
      <PanelHeader
        kicker="Address"
        title={shorten(profile.address)}
        meta={`Active from block ${profile.firstBlock} to ${profile.lastBlock} · ${profile.behaviorClass.replace("_", " ")}`}
        action={
          onCopy ? (
            <button type="button" className="copy-btn" onClick={() => onCopy(profile.address)}>
              copy full address
            </button>
          ) : null
        }
      />
      <div className={`result-body${swapping ? " is-swapping" : ""}`}>
        <div className="stat-grid">
          <Stat label="Touches" value={formatInteger(profile.totalInteractions)} />
          <Stat label="Inbound" value={formatEth(profile.inboundEth)} />
          <Stat label="Outbound" value={formatEth(profile.outboundEth)} />
          <Stat label="Net flow" value={formatSignedEth(profile.netFlowEth)} />
        </div>
        <div className="detail-grid">
          <DetailBlock
            title="Profile"
            rows={[
              ["Inbound tx", formatInteger(profile.inboundCount)],
              ["Outbound tx", formatInteger(profile.outboundCount)],
              ["Active blocks", formatInteger(profile.activeBlocks)],
              ["Counterparties", formatInteger(profile.uniqueCounterparties)]
            ]}
          />
          <DetailBlock
            title="Top counterparties"
            rows={profile.topCounterparties.map((item) => [
              looksLikeAddress(item.address) ? (
                <JumpLink
                  key={item.address}
                  mode="address"
                  value={item.address}
                  label={shorten(item.address)}
                  onJump={onJump}
                />
              ) : (
                item.address.replace("_", " ")
              ),
              `${formatInteger(item.count)} touches`
            ])}
          />
        </div>
        {profile.busiestBlocks.length ? (
          <div className="detail-block">
            <h3>Busiest blocks</h3>
            <div className="detail-list">
              {profile.busiestBlocks.map((item) => (
                <div key={item.blockNumber} className="detail-row">
                  <JumpLink
                    mode="block"
                    value={String(item.blockNumber)}
                    label={`Block ${item.blockNumber}`}
                    onJump={onJump}
                  />
                  <strong>{formatInteger(item.count)} tx</strong>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>
    </SectionFrame>
  );
}

function BlockNav({
  dataset,
  current,
  onJump
}: {
  dataset: Dataset;
  current: number;
  onJump: JumpHandler;
}) {
  const index = dataset.blockNumbers.indexOf(current);
  const prev = index > 0 ? dataset.blockNumbers[index - 1] : null;
  const next = index >= 0 && index < dataset.blockNumbers.length - 1 ? dataset.blockNumbers[index + 1] : null;

  return (
    <div className="block-nav">
      <button
        type="button"
        className="block-nav__btn"
        disabled={prev === null}
        onClick={() => prev && onJump("block", String(prev))}
      >
        ← Block {prev ?? "—"}
      </button>
      <span className="block-nav__position">
        {index + 1} / {dataset.blockNumbers.length}
      </span>
      <button
        type="button"
        className="block-nav__btn"
        disabled={next === null}
        onClick={() => next && onJump("block", String(next))}
      >
        Block {next ?? "—"} →
      </button>
    </div>
  );
}

function PanelHeader({
  kicker,
  title,
  meta,
  action
}: {
  kicker: string;
  title: string;
  meta: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="panel-header">
      <div>
        <p className="panel-kicker">{kicker}</p>
        <h2 className="panel-title">{title}</h2>
      </div>
      <div className="panel-header__side">
        {action}
        {meta ? <p className="panel-meta">{meta}</p> : null}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="stat">
      <span className="stat__label">{label}</span>
      <span className="stat__value">{value}</span>
    </div>
  );
}

function DetailBlock({ title, rows }: { title: string; rows: Array<[React.ReactNode, React.ReactNode]> }) {
  return (
    <div className="detail-block">
      <h3>{title}</h3>
      <div className="detail-list">
        {rows.map(([label, value], index) => (
          <div key={`${title}-${index}`} className="detail-row">
            <span>{label}</span>
            <strong>{value}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

function SnapshotRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="snapshot-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function GlanceRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="glance-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function JumpButton({
  label,
  meta,
  mode,
  value,
  onJump
}: {
  label: string;
  meta: string;
  mode: SearchMode;
  value: string;
  onJump: JumpHandler;
}) {
  return (
    <button type="button" className="action-row" onClick={() => onJump(mode, value)}>
      <span className="action-row__label">{label}</span>
      <span className="action-row__meta">{meta}</span>
    </button>
  );
}

function JumpLink({
  mode,
  value,
  label,
  onJump,
  className = "jump-link"
}: {
  mode: SearchMode;
  value: string;
  label: string;
  onJump: JumpHandler;
  className?: string;
}) {
  return (
    <button type="button" className={className} onClick={() => onJump(mode, value)}>
      {label}
    </button>
  );
}

function TransactionTable({
  transactions,
  onJump
}: {
  transactions: Array<{
    index: number;
    from: string;
    to: string;
    contractCreation: boolean;
    costEth: number;
  }>;
  onJump: JumpHandler;
}) {
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Index</th>
            <th>From</th>
            <th>To</th>
            <th>Type</th>
            <th>Cost</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => (
            <tr key={`${tx.index}-${tx.from}`}>
              <td>{tx.index}</td>
              <td>
                <JumpLink mode="address" value={tx.from} label={shorten(tx.from)} onJump={onJump} className="address-chip" />
              </td>
              <td>
                {tx.contractCreation ? (
                  <span className="pill alert">Contract creation</span>
                ) : (
                  <JumpLink mode="address" value={tx.to} label={shorten(tx.to)} onJump={onJump} className="address-chip" />
                )}
              </td>
              <td>{tx.contractCreation ? <span className="pill alert">Create</span> : <span className="pill">Transfer</span>}</td>
              <td>{formatEth(tx.costEth)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
