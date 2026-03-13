import { post } from "./http";

export type QuoteRequestItem = {
  parameterId: number;
  paramCode?: string;
  valueNumber?: number;
  selectedOptionIds?: number[];
};

export type QuoteRequest = {
  productId: number;
  quantity: number;
  items: QuoteRequestItem[];
};

export type QuoteResponse = {
  productId: number;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  breakdown: Record<string, number>;
};

export async function quote(req: QuoteRequest): Promise<QuoteResponse> {
  return await post<QuoteResponse>("/api/quote", req);
}
