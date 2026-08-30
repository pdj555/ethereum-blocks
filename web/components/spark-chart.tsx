"use client";

import { useId, useMemo, useState } from "react";
import { downsampleSeries } from "@/lib/series";
import { sparklineAreaPath, sparklinePath } from "@/lib/utils";

type SparkChartProps = {
  values: number[];
  blockNumbers?: number[];
  label: string;
  valueLabel: string;
  formatHover?: (value: number, blockNumber: number | null) => string;
  onSelect?: (blockNumber: number) => void;
};

export function SparkChart({
  values,
  blockNumbers,
  label,
  valueLabel,
  formatHover,
  onSelect
}: SparkChartProps) {
  const gradientId = useId().replace(/:/g, "");
  const width = 320;
  const height = 96;
  const sampled = useMemo(
    () => downsampleSeries(values, blockNumbers),
    [blockNumbers, values]
  );
  const path = useMemo(
    () => sparklinePath(sampled.values, width, height),
    [sampled.values]
  );
  const areaPath = useMemo(
    () => sparklineAreaPath(sampled.values, width, height),
    [sampled.values]
  );
  const gridLines = [0.25, 0.5, 0.75].map((ratio) => height * ratio);
  const [hoverIndex, setHoverIndex] = useState<number | null>(null);

  const hoverLabel = useMemo(() => {
    if (hoverIndex === null || sampled.values[hoverIndex] === undefined) {
      return null;
    }
    const blockNumber = sampled.blockNumbers[hoverIndex] ?? null;
    const value = sampled.values[hoverIndex];
    if (formatHover) {
      return formatHover(value, blockNumber);
    }
    return blockNumber ? `Block ${blockNumber} · ${value}` : String(value);
  }, [formatHover, hoverIndex, sampled.blockNumbers, sampled.values]);

  function indexFromEvent(event: React.MouseEvent<SVGSVGElement>) {
    if (sampled.values.length < 2) {
      return null;
    }
    const rect = event.currentTarget.getBoundingClientRect();
    const ratio = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1);
    return Math.round(ratio * (sampled.values.length - 1));
  }

  function handleMove(event: React.MouseEvent<SVGSVGElement>) {
    const index = indexFromEvent(event);
    if (index !== null) {
      setHoverIndex(index);
    }
  }

  function handleClick(event: React.MouseEvent<SVGSVGElement>) {
    const index = indexFromEvent(event);
    const blockNumber = index !== null ? sampled.blockNumbers[index] : undefined;
    if (blockNumber !== undefined && onSelect) {
      onSelect(blockNumber);
    }
  }

  return (
    <div className="chart-card">
      <div className="chart-card__head">
        <span>{label}</span>
        <span className="chart-card__value">{hoverLabel ?? valueLabel}</span>
      </div>
      <svg
        className={`spark-chart${onSelect ? " is-interactive" : ""}`}
        viewBox={`0 0 ${width} ${height}`}
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIndex(null)}
        onClick={handleClick}
      >
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="var(--accent)" stopOpacity="0.22" />
            <stop offset="100%" stopColor="var(--accent)" stopOpacity="0" />
          </linearGradient>
        </defs>
        <g className="spark-chart__grid">
          {gridLines.map((y) => (
            <line key={y} x1="0" y1={y} x2={width} y2={y} />
          ))}
        </g>
        {areaPath ? <path className="spark-chart__area" d={areaPath} fill={`url(#${gradientId})`} /> : null}
        {path ? <path className="spark-chart__line" d={path} /> : null}
        {hoverIndex !== null ? (
          <line
            className="spark-chart__cursor"
            x1={(hoverIndex / Math.max(sampled.values.length - 1, 1)) * width}
            y1="0"
            x2={(hoverIndex / Math.max(sampled.values.length - 1, 1)) * width}
            y2={height}
          />
        ) : null}
      </svg>
    </div>
  );
}
