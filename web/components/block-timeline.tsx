"use client";

import { useCallback, useMemo, useRef } from "react";
import type { Dataset } from "@/lib/types";
import { formatEth, formatInteger } from "@/lib/utils";

type BlockTimelineProps = {
  dataset: Dataset;
  activeBlock: number | null;
  onSelect: (blockNumber: number) => void;
  onScrub: (blockNumber: number) => void;
  onScrubCommit: (blockNumber: number, startUrl: string) => void;
};

export function BlockTimeline({
  dataset,
  activeBlock,
  onSelect,
  onScrub,
  onScrubCommit
}: BlockTimelineProps) {
  const peak = useMemo(
    () => dataset.txSeries.reduce((highest, value) => Math.max(highest, value), 1),
    [dataset.txSeries]
  );
  const trackRef = useRef<HTMLDivElement>(null);
  const draggingRef = useRef(false);
  const scrubStartedRef = useRef(false);
  const suppressClickRef = useRef(false);
  const pointerStartXRef = useRef(0);
  const pointerStartedOnCellRef = useRef(false);
  const lastScrubbedBlockRef = useRef<number | null>(null);
  const scrubStartUrlRef = useRef("");
  const timelinePoints = useMemo(() => {
    const total = dataset.blockNumbers.length;
    if (total <= 400) {
      return dataset.blockNumbers.map((blockNumber, index) => ({ blockNumber, index }));
    }

    const stride = Math.ceil(total / 400);
    const indexes = new Set<number>();
    for (let index = 0; index < total; index += stride) {
      indexes.add(index);
    }
    indexes.add(total - 1);

    while (indexes.size > 400) {
      const removable = Array.from(indexes)
        .filter((index) => index !== 0 && index !== total - 1)
        .sort((left, right) => right - left)[0];
      if (removable === undefined) {
        break;
      }
      indexes.delete(removable);
    }

    const activeIndex = activeBlock === null ? -1 : dataset.blockNumbers.indexOf(activeBlock);
    if (activeIndex >= 0 && !indexes.has(activeIndex)) {
      indexes.add(activeIndex);
      if (indexes.size > 400) {
        const nearestNeighbor = Array.from(indexes)
          .filter((index) => index !== 0 && index !== total - 1 && index !== activeIndex)
          .sort(
            (left, right) =>
              Math.abs(left - activeIndex) - Math.abs(right - activeIndex) || left - right
          )[0];
        if (nearestNeighbor !== undefined) {
          indexes.delete(nearestNeighbor);
        }
      }
    }

    return Array.from(indexes)
      .sort((left, right) => left - right)
      .map((index) => ({ blockNumber: dataset.blockNumbers[index]!, index }));
  }, [activeBlock, dataset.blockNumbers]);

  const blockFromClientX = useCallback(
    (clientX: number) => {
      const track = trackRef.current;
      if (!track || dataset.blockNumbers.length === 0) {
        return null;
      }
      const rect = track.getBoundingClientRect();
      const ratio = Math.min(Math.max((clientX - rect.left) / rect.width, 0), 1);
      const index = Math.min(
        dataset.blockNumbers.length - 1,
        Math.round(ratio * (dataset.blockNumbers.length - 1))
      );
      return dataset.blockNumbers[index]!;
    },
    [dataset.blockNumbers]
  );

  const previewFromClientX = useCallback(
    (clientX: number) => {
      const blockNumber = blockFromClientX(clientX);
      if (blockNumber === null || blockNumber === lastScrubbedBlockRef.current) {
        return;
      }
      lastScrubbedBlockRef.current = blockNumber;
      onScrub(blockNumber);
    },
    [blockFromClientX, onScrub]
  );

  return (
    <div className="block-timeline">
      <div
        ref={trackRef}
        className="block-timeline__cells"
        role="list"
        aria-label="Block activity timeline"
        style={{ gridTemplateColumns: `repeat(${timelinePoints.length}, minmax(0, 1fr))` }}
        onPointerDown={(event) => {
          draggingRef.current = true;
          scrubStartedRef.current = false;
          suppressClickRef.current = false;
          pointerStartXRef.current = event.clientX;
          pointerStartedOnCellRef.current = event.target !== event.currentTarget;
          scrubStartUrlRef.current = window.location.href;
          lastScrubbedBlockRef.current = null;
        }}
        onPointerMove={(event) => {
          if (!draggingRef.current) {
            return;
          }
          if (!scrubStartedRef.current) {
            if (Math.abs(event.clientX - pointerStartXRef.current) < 4) {
              return;
            }
            scrubStartedRef.current = true;
            trackRef.current?.setPointerCapture(event.pointerId);
          }
          previewFromClientX(event.clientX);
        }}
        onPointerUp={(event) => {
          if (!draggingRef.current) {
            return;
          }
          draggingRef.current = false;
          if (scrubStartedRef.current) {
            previewFromClientX(event.clientX);
            if (trackRef.current?.hasPointerCapture(event.pointerId)) {
              trackRef.current.releasePointerCapture(event.pointerId);
            }
            suppressClickRef.current = pointerStartedOnCellRef.current;
            window.setTimeout(() => {
              suppressClickRef.current = false;
            }, 0);
            if (lastScrubbedBlockRef.current !== null) {
              onScrubCommit(lastScrubbedBlockRef.current, scrubStartUrlRef.current);
            }
          } else if (!pointerStartedOnCellRef.current) {
            const blockNumber = blockFromClientX(event.clientX);
            if (blockNumber !== null) {
              onSelect(blockNumber);
            }
          }
        }}
        onPointerCancel={() => {
          const finalBlock = lastScrubbedBlockRef.current;
          draggingRef.current = false;
          if (finalBlock !== null) {
            onScrubCommit(finalBlock, scrubStartUrlRef.current);
          }
        }}
      >
        {timelinePoints.map(({ blockNumber, index }) => {
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
              onClick={(event) => {
                if (suppressClickRef.current) {
                  suppressClickRef.current = false;
                  event.preventDefault();
                  return;
                }
                onSelect(blockNumber);
              }}
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
