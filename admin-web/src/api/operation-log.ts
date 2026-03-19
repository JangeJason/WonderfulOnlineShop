import { get } from "./http";

export type OperationLogRecord = {
  id: number;
  adminUserId: number;
  adminNickname?: string | null;
  adminCompanyName?: string | null;
  moduleName: string;
  actionName: string;
  httpMethod: string;
  requestPath: string;
  targetId?: number | null;
  summary?: string | null;
  ipAddress?: string | null;
  userAgent?: string | null;
  statusCode?: number | null;
  createdAt: string;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  size: number;
  current: number;
};

export async function listOperationLogs(page = 1, size = 20, filters?: {
  adminUserId?: number;
  moduleName?: string;
  actionName?: string;
  keyword?: string;
  from?: string;
  to?: string;
}) {
  return await get<PageResult<OperationLogRecord>>("/admin/operation-logs", { page, size, ...(filters || {}) });
}
