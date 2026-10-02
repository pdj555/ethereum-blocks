"use client";

import type { Dataset } from "@/lib/types";
import { formatInteger, formatPercent, formatRange } from "@/lib/utils";

type LiveStatusPanelProps = {
  dataset: Dataset | null;
  loading: boolean;
  activeBlock: number | null;
};

export function LiveStatusPanel({ dataset, loading, activeBlock }: LiveStatusPanelProps) {
  if (loading || !dataset) {
    return (
      <div className="live-panel">
        <p className="live-panel__label">Slice status</p>
        <p className="live-panel__headline">Loading dataset</p>
        <div className="live-panel__bar">
          <div className="live-panel__bar-fill" style={{ width: "18%" }} />
        </div>
        <p className="live-panel__meta">Reading CSV files locally</p>
      </div>
    );
  }

  const { overview } = dataset;
  const txPct = overview.parsedTransactions / Math.max(overview.totalTransactionsMetadata, 1);
  const index = activeBlock ? dataset.blockNumbers.indexOf(activeBlock) : -1;
  const slicePct = index >= 0 ? (index + 1) / dataset.blockNumbers.length : 0;

  return (
    <div className="live-panel">
      <p className="live-panel__label">Live slice status</p>
      <p className="live-panel__headline">
        {activeBlock ? `Block ${activeBlock}` : "Dataset ready"}
      </p>

      <div className="live-panel__metrics">
        <div className="live-panel__metric">
          <span>Blocks loaded</span>
          <strong>{formatInteger(overview.blocksLoaded)}</strong>
        </div>
        <div className="live-panel__metric">
          <span>Parsed tx</span>
          <strong>{formatPercent(txPct)}</strong>
        </div>
        <div className="live-panel__metric">
          <span>Range</span>
          <strong>{formatRange(overview.blockRangeStart, overview.blockRangeEnd)}</strong>
        </div>
      </div>

      <div className="live-panel__bars">
        <div className="live-panel__bar-row">
          <span>Index</span>
          <div className="live-panel__bar">
            <div className="live-panel__bar-fill" style={{ width: "100%" }} />
          </div>
        </div>
        <div className="live-panel__bar-row">
          <span>Cursor</span>
          <div className="live-panel__bar">
            <div className="live-panel__bar-fill is-dim" style={{ width: `${slicePct * 100}%` }} />
          </div>
        </div>
      </div>

      <p className="live-panel__meta">
        {activeBlock && index >= 0
          ? `Position ${index + 1} of ${dataset.blockNumbers.length} in slice`
          : `${formatInteger(overview.activeAddresses)} active addresses indexed`}
      </p>
    </div>
  );
}
