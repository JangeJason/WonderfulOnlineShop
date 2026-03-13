import { get, post, put } from "./http";

export type UserCertificate = {
  id: number;
  certificateType: string;
  trademarkType?: string;
  trademarkContent: string;
  principal: string;
  endDate: string;
  fileUrls: string[];
  fileNames?: string[];
};

export type CreateCertificateRequest = {
  certificateType: string;
  trademarkType?: string;
  trademarkContent: string;
  principal: string;
  endDate: string;
  fileUrls: string[];
  fileNames?: string[];
};

export async function listCommonCertificates(): Promise<UserCertificate[]> {
  return await get<UserCertificate[]>("/api/certificates/common");
}

export async function createCommonCertificate(req: CreateCertificateRequest): Promise<UserCertificate> {
  return await post<UserCertificate>("/api/certificates/common", req);
}

export async function updateCommonCertificate(id: number, req: CreateCertificateRequest): Promise<UserCertificate> {
  return await put<UserCertificate>(`/api/certificates/common/${id}`, req);
}
