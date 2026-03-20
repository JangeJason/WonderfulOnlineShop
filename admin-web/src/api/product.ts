import { del, get, post, put } from './http'

export type Product = {
  id: number
  name: string
  description: string | null
  imageUrl: string | null
  imageUrls?: string[] | null
  basePrice: string | number
  setupFee?: string | number | null
  freeSetupQuantity?: number | null
  pricingFormula: string | null
  categoryId: number | null
  status: number
}

export type PageResult<T> = {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export type ProductStats = {
  total: number
  online: number
  offline: number
}

export async function listAdminProducts(opts?: { page?: number; size?: number; keyword?: string; mainCategoryId?: number }) {
  return await get<PageResult<Product>>('/admin/products', opts)
}

export async function getAdminProductStats() {
  return await get<ProductStats>('/admin/products/stats')
}

export async function getAdminProduct(id: number) {
  return await get<Product>(`/admin/products/${id}`)
}

export async function listPublicProductsPaged(opts?: { keyword?: string; categoryId?: number; page?: number; size?: number }) {
  return await get<PageResult<Product>>('/api/products', opts)
}

export async function createProduct(payload: { name: string; basePrice: number; setupFee?: number; freeSetupQuantity?: number; status?: number; description?: string; imageUrl?: string; imageUrls?: string[]; categoryId?: number }) {
  return await post<Product>('/admin/products', payload)
}

export async function updateProduct(id: number, payload: { name?: string; basePrice?: number; setupFee?: number; freeSetupQuantity?: number; status?: number; description?: string; imageUrl?: string; imageUrls?: string[]; pricingFormula?: string; categoryId?: number }) {
  return await put<Product>(`/admin/products/${id}`, payload)
}

export async function deleteProduct(id: number) {
  return await del<null>(`/admin/products/${id}`)
}
