import { get } from "./http";

export type Product = {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  imageUrls?: string[];
  basePrice: number;
  status: number;
  pricingFormula?: string;
  categoryId?: number;
  setupFee?: number;
  freeSetupQuantity?: number;
};

export type Category = {
  id: number;
  name: string;
  parentId?: number | null;
  sortOrder: number;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
};

export type ProductParameter = {
  id: number;
  productId: number;
  paramName: string;
  paramType: string; // INPUT or SELECT
  isRequired: boolean;
  isMultiple?: boolean;
  isDynamicByMaterial?: boolean;
  unit?: string | null;
  sortOrder: number;
  validationRules?: Record<string, any>;
};

export type ParameterOption = {
  id: number;
  parameterId: number;
  optionName: string;
  priceAdjustment: number;
  imageUrl?: string;
  sortOrder: number;
};

export type MaterialConfig = {
  materialParameterId?: number | null;
  materialOptionId?: number | null;
  enabledDynamicParamIds: number[];
  enabledOptionIds: Record<string, number[]>;
};

export async function listPublicProducts(keyword?: string, categoryId?: number, page = 1, size = 10): Promise<PageResult<Product>> {
  const params: Record<string, any> = { page, size };
  if (keyword) params.keyword = keyword;
  if (categoryId) params.categoryId = categoryId;
  return await get<PageResult<Product>>("/api/products", params);
}

export async function listPublicCategories(): Promise<Category[]> {
  return await get<Category[]>("/api/products/categories");
}

export async function getPublicProduct(id: number): Promise<Product> {
  return await get<Product>(`/api/products/${id}`);
}

export async function getProductParameters(
  productId: number
): Promise<ProductParameter[]> {
  return await get<ProductParameter[]>(`/api/products/${productId}/parameters`);
}

export async function getParameterOptions(
  parameterId: number
): Promise<ParameterOption[]> {
  return await get<ParameterOption[]>(
    `/api/products/parameters/${parameterId}/options`
  );
}

export async function getMaterialConfig(productId: number, materialOptionId?: number): Promise<MaterialConfig> {
  const params: Record<string, any> = {};
  if (materialOptionId != null) {
    params.materialOptionId = materialOptionId;
  }
  return await get<MaterialConfig>(`/api/products/${productId}/material-config`, params);
}
