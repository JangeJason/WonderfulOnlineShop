import { del, get, post } from "./http";

export type AfterSaleItemSnapshot = {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
};

export type AfterSaleRecord = {
  id: number;
  orderId: number;
  requestType: string;
  detailText: string;
  status: string;
  adminRemark?: string | null;
  selectedItems: AfterSaleItemSnapshot[];
  orderStatus?: string | null;
  userNickname?: string | null;
  userPhone?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type AfterSaleDetail = {
  request: AfterSaleRecord;
  order: any;
  orderItems: any[];
  selectedItemIds: number[];
  imageUrls: string[];
};

export async function createAfterSale(payload: {
  orderId: number;
  itemIds: number[];
  requestType: string;
  detailText?: string;
  imageUrls?: string[];
}): Promise<AfterSaleRecord> {
  return await post<AfterSaleRecord>("/api/after-sales", payload);
}

export async function listAfterSales(): Promise<AfterSaleRecord[]> {
  return await get<AfterSaleRecord[]>("/api/after-sales/list");
}

export async function getAfterSaleDetail(id: number): Promise<AfterSaleDetail> {
  return await get<AfterSaleDetail>(`/api/after-sales/${id}`);
}

export async function deleteAfterSale(id: number): Promise<void> {
  return await del<void>(`/api/after-sales/${id}`);
}
