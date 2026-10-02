export function formatTimestamp(unixSeconds: number): string {
  return new Date(unixSeconds * 1000).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

export function formatInteger(value: number): string {
  return new Intl.NumberFormat("en-US").format(value);
}

export function formatDecimal(value: number, digits: number): string {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(value);
}

export function formatPercent(value: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "percent",
    maximumFractionDigits: 1
  }).format(value);
}

export function formatEth(value: number): string {
  return `${formatDecimal(value, 6)} ETH`;
}

export function formatSignedEth(value: number): string {
  const prefix = value > 0 ? "+" : "";
  return prefix + formatEth(value);
}

export function formatRange(start: number, end: number): string {
  return `${start}-${end}`;
}

export function shorten(value: string): string {
  if (!value || value.length < 15) {
    return value || "";
  }
  return `${value.slice(0, 8)}...${value.slice(-6)}`;
}

export function looksLikeAddress(value: string): boolean {
  return typeof value === "string" && /^0x[0-9a-f]{40}$/i.test(value) && value.length === 42;
}

export function sparklinePath(values: number[], width: number, height: number): string {
  if (values.length < 2) {
    return "";
  }
  let min = values[0]!;
  let max = values[0]!;
  for (let index = 1; index < values.length; index += 1) {
    min = Math.min(min, values[index]!);
    max = Math.max(max, values[index]!);
  }
  const span = max - min || 1;
  const step = width / (values.length - 1);
  return values
    .map((value, index) => {
      const x = index * step;
      const y = height - ((value - min) / span) * (height - 4) - 2;
      return `${index === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");
}

export function sparklineAreaPath(values: number[], width: number, height: number): string {
  const line = sparklinePath(values, width, height);
  if (!line) {
    return "";
  }
  return `${line} L${width.toFixed(2)},${height} L0,${height} Z`;
}
