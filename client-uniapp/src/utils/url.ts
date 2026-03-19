function isH5Runtime(): boolean {
  return typeof window !== "undefined" && typeof document !== "undefined";
}
export function getApiBaseUrl(): string {
  if (isH5Runtime()) return "";
  return "https://6cb4605b.r39.cpolar.top";
}

export function toAbsoluteAssetUrl(url?: string): string {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) {
    return url;
  }
  return getApiBaseUrl() + url;
}
