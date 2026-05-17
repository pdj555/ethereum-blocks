export function formatTimestamp(unixSeconds) {
  return new Date(unixSeconds * 1000).toLocaleString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit"
  });
}

export function formatInteger(value) {
  return new Intl.NumberFormat("en-US").format(value);
}

export function formatDecimal(value, digits) {
  return new Intl.NumberFormat("en-US", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(value);
}

export function formatPercent(value) {
  return new Intl.NumberFormat("en-US", {
    style: "percent",
    maximumFractionDigits: 1
  }).format(value);
}

export function formatEth(value) {
  return formatDecimal(value, 6) + " ETH";
}

export function formatSignedEth(value) {
  const prefix = value > 0 ? "+" : "";
  return prefix + formatEth(value);
}

export function formatRange(start, end) {
  return String(start) + "-" + String(end);
}

export function shorten(value) {
  if (!value || value.length < 15) {
    return value || "";
  }
  return value.slice(0, 8) + "..." + value.slice(-6);
}

export function looksLikeAddress(value) {
  return typeof value === "string" && /^0x[0-9a-f]{40}$/i.test(value) && value.length === 42;
}

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
