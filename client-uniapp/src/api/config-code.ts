import { get, post } from "./http";

export type ProductConfigCodeCreateResponse = {
  code: string;
  productId: number;
  productName: string;
  creatorName: string;
  createdAt: string;
  expireAt?: string | null;
  expireDays: number;
};

export type ProductConfigCodeResolveResponse = {
  code: string;
  productId: number;
  productName: string;
  creatorName: string;
  paramsSnapshot: string;
  createdAt: string;
  expireAt?: string | null;
  expired: boolean;
};

export async function createProductConfigCode(
  productId: number,
  paramsSnapshot: string,
  expireDays: number
): Promise<ProductConfigCodeCreateResponse> {
  return await post<ProductConfigCodeCreateResponse>("/api/config-codes", {
    productId,
    paramsSnapshot,
    expireDays,
  });
}

export async function resolveProductConfigCode(code: string): Promise<ProductConfigCodeResolveResponse> {
  return await get<ProductConfigCodeResolveResponse>(`/api/config-codes/${encodeURIComponent(code)}`);
}

