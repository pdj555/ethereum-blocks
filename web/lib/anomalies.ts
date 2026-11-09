import type { BlockView } from "./types";

/**
 * Block anomalies in the browser, scored exactly as src/RobustStats.java scores them
 * for the CLI: the modified z-score (Iglewicz and Hoaglin), centred on the median and
 * scaled by the median absolute deviation, so one extreme block cannot hide another.
 * scripts/anomaly_parity.json holds the CLI's answer on the bundled sample, and the
 * Java and TypeScript test suites both check against it.
 */

export const ANOMALY_THRESHOLD = 3.5;

export type Anomaly = {
  blockNumber: number;
  value: number;
  zScore: number;
  direction: "high" | "low";
};

export type AnomalyReport = {
  threshold: number;
  cost: Anomaly[];
  volume: Anomaly[];
  costMedianEth: number | null;
  costScaleEth: number | null;
  volumeMedian: number | null;
  volumeScale: number | null;
};

export function median(values: number[]): number {
  if (values.length === 0) {
    throw new RangeError("no values");
  }
  const sorted = values.slice().sort((left, right) => left - right);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
}

/** 1.4826 * MAD, or 1.253314 * mean absolute deviation when more than half the values tie. */
export function robustScale(values: number[]): number {
  const center = median(values);
  const deviations = values.map((value) => Math.abs(value - center));
  const mad = median(deviations);
  if (mad > 0) {
    return mad / 0.6745;
  }
  return (1.253314 * deviations.reduce((sum, value) => sum + value, 0)) / values.length;
}

/** One score per value, in input order; all zero when every value is the same. */
export function modifiedZ(values: number[]): number[] {
  if (values.length === 0) {
    return [];
  }
  const center = median(values);
  const scale = robustScale(values);
  if (scale === 0) {
    return values.map(() => 0);
  }
  return values.map((value) => (value - center) / scale);
}

function flag(numbers: number[], values: number[], threshold: number): Anomaly[] {
  const scores = modifiedZ(values);
  const flagged: Anomaly[] = [];
  scores.forEach((zScore, index) => {
    if (Math.abs(zScore) >= threshold) {
      flagged.push({
        blockNumber: numbers[index],
        value: values[index],
        zScore,
        direction: zScore > 0 ? "high" : "low"
      });
    }
  });
  return flagged;
}

/**
 * Average cost per parsed transaction is scored over blocks that have parsed
 * transactions, and only once there are three of them. Transaction volume is scored
 * over every block, from the count the block export reports.
 */
export function detectAnomalies(blocks: BlockView[], threshold = ANOMALY_THRESHOLD): AnomalyReport {
  const priced = blocks.filter((block) => block.parsedTransactionCount > 0);
  const costs = priced.map((block) => block.avgCostEth);
  const volumes = blocks.map((block) => block.transactionCountMetadata);
  const scoreCosts = costs.length >= 3;
  return {
    threshold,
    cost: scoreCosts ? flag(priced.map((block) => block.number), costs, threshold) : [],
    volume: flag(blocks.map((block) => block.number), volumes, threshold),
    costMedianEth: scoreCosts ? median(costs) : null,
    costScaleEth: scoreCosts ? robustScale(costs) : null,
    volumeMedian: volumes.length ? median(volumes) : null,
    volumeScale: volumes.length ? robustScale(volumes) : null
  };
}
