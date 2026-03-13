function isH5Runtime(): boolean {
  return typeof window !== "undefined" && typeof document !== "undefined";
}

export function getApiBaseUrl(): string {
  if (isH5Runtime()) {
    // H5 uses Vite proxy in development.
    return "";
  }
  return (uni.getStorageSync("baseUrl") as string) || "http://localhost:8080";
}

export function toAbsoluteAssetUrl(url?: string): string {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) {
    return url;
  }
  return getApiBaseUrl() + url;
}
