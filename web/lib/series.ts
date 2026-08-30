export const MAX_SPARKLINE_POINTS = 240;

export function downsampleSeries(
  values: number[],
  blockNumbers: number[] = [],
  limit = MAX_SPARKLINE_POINTS
): { values: number[]; blockNumbers: number[] } {
  if (limit < 2) {
    throw new Error("Series display limit must be at least 2.");
  }
  if (values.length <= limit) {
    return { values, blockNumbers };
  }

  const sampledValues: number[] = [];
  const sampledBlockNumbers: number[] = [];
  const finalIndex = values.length - 1;
  for (let point = 0; point < limit; point += 1) {
    const index = Math.round((point * finalIndex) / (limit - 1));
    sampledValues.push(values[index]!);
    if (blockNumbers[index] !== undefined) {
      sampledBlockNumbers.push(blockNumbers[index]!);
    }
  }
  return { values: sampledValues, blockNumbers: sampledBlockNumbers };
}
