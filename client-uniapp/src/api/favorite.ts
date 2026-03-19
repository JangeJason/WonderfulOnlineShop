import { del, get, post } from "./http";

export type ProductFavoriteStatus = {
  favorited: boolean;
  directFavorited?: boolean;
  favoriteConfigCodeCount?: number;
};

export type FavoriteProductItem = {
  productId: number;
  productName: string;
  productDescription?: string;
  productImageUrl?: string;
  productImageUrls?: string[];
  favoritedProduct: boolean;
  favoriteConfigCodeCount: number;
  lastFavoritedAt?: string;
};

export type FavoriteConfigCodeItem = {
  code: string;
  productId: number;
  creatorName: string;
  createdAt: string;
  expireAt?: string | null;
  expired: boolean;
};

export async function favoriteProduct(productId: number): Promise<void> {
  await post<void>(`/api/favorites/products/${productId}`);
}

export async function unfavoriteProduct(productId: number, removeConfigCodes = false): Promise<void> {
  await del<void>(`/api/favorites/products/${productId}?removeConfigCodes=${removeConfigCodes ? "true" : "false"}`);
}

export async function getProductFavoriteStatus(productId: number): Promise<ProductFavoriteStatus> {
  return await get<ProductFavoriteStatus>(`/api/favorites/products/${productId}/status`);
}

export async function listFavoriteProducts(): Promise<FavoriteProductItem[]> {
  return await get<FavoriteProductItem[]>("/api/favorites/products");
}

export async function favoriteConfigCode(code: string): Promise<{ code: string; productId: number }> {
  return await post<{ code: string; productId: number }>("/api/favorites/config-codes", { code });
}

export async function unfavoriteConfigCode(code: string): Promise<void> {
  await del<void>(`/api/favorites/config-codes/${encodeURIComponent(code)}`);
}

export async function listFavoriteConfigCodes(productId: number): Promise<FavoriteConfigCodeItem[]> {
  return await get<FavoriteConfigCodeItem[]>(`/api/favorites/products/${productId}/config-codes`);
}
