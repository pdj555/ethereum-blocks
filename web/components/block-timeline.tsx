"use client";

import { useCallback, useRef } from "react";
import type { Dataset } from "@/lib/types";
import { formatEth, formatInteger } from "@/lib/utils";

type BlockTimelineProps = {
  dataset: Dataset;
  activeBlock: number | null;
  onSelect: (blockNumber: number) => void;
};

export function BlockTimeline({ dataset, activeBlock, onSelect }: BlockTimelineProps) {
  const peak = Math.max(...dataset.txSeries, 1);
  const trackRef = useRef<HTMLDivElement>(null);
  const draggingRef = useRef(false);

  const pickFromClientX = useCallback(
    (clientX: number) => {
      const track = trackRef.current;
      if (!track || dataset.blockNumbers.length === 0) {
        return;
      }
      const rect = track.getBoundingClientRect();
      const ratio = Math.min(Math.max((clientX - rect.left) / rect.width, 0), 1);
      const index = Math.min(
        dataset.blockNumbers.length - 1,
        Math.round(ratio * (dataset.blockNumbers.length - 1))
      );
      onSelect(dataset.blockNumbers[index]!);
    },
    [dataset.blockNumbers, onSelect]
  );

  return (
    <div className="block-timeline">
      <div
        ref={trackRef}
        className="block-timeline__cells"
        role="list"
        aria-label="Block activity timeline"
        onPointerDown={(event) => {
          draggingRef.current = true;
          trackRef.current?.setPointerCapture(event.pointerId);
          pickFromClientX(event.clientX);
        }}
        onPointerMove={(event) => {
          if (!draggingRef.current) {
            return;
          }
          pickFromClientX(event.clientX);
        }}
        onPointerUp={(event) => {
          draggingRef.current = false;
          trackRef.current?.releasePointerCapture(event.pointerId);
        }}
        onPointerLeave={() => {
          draggingRef.current = false;
        }}
      >
        {dataset.blockNumbers.map((blockNumber, index) => {
          const txCount = dataset.txSeries[index] ?? 0;
          const intensity = txCount / peak;
          const isActive = activeBlock === blockNumber;
          const title = `Block ${blockNumber} · ${formatInteger(txCount)} parsed tx · ${formatEth(dataset.costSeries[index] ?? 0)}`;

          return (
            <button
              key={blockNumber}
              type="button"
              role="listitem"
              className={`block-timeline__cell${isActive ? " is-active" : ""}`}
              style={{ "--heat": String(intensity) } as React.CSSProperties}
              title={title}
              aria-label={title}
              aria-current={isActive ? "true" : undefined}
              onClick={() => onSelect(blockNumber)}
            />
          );
        })}
      </div>
      <div className="block-timeline__axis">
        <span>{dataset.overview.blockRangeStart}</span>
        <span>scrub · click · [ ] step</span>
        <span>{dataset.overview.blockRangeEnd}</span>
      </div>
    </div>
  );
}
