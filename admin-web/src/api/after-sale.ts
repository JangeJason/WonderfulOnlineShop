import { get, put } from "./http";

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
  userCompanyName?: string | null;
  userPhone?: string | null;
  userEmail?: string | null;
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

export type PageResult<T> = {
  records: T[];
  total: number;
  size: number;
  current: number;
};

export async function listAdminAfterSales(page = 1, size = 15, filters?: {
  status?: string;
  orderId?: number;
  keyword?: string;
}) {
  return await get<PageResult<AfterSaleRecord>>("/admin/after-sales", { page, size, ...(filters || {}) });
}

export async function getAdminAfterSaleDetail(id: number) {
  return await get<AfterSaleDetail>(`/admin/after-sales/${id}`);
}

export async function updateAdminAfterSaleStatus(id: number, status: string, adminRemark?: string) {
  return await put<void>(`/admin/after-sales/${id}/status`, { status, adminRemark });
}
