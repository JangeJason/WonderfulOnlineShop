import { del, get, post, put } from './http'

export type Product = {
  id: number
  name: string
  basePrice: string | number
  status: number
}

export async function listAdminProducts() {
  return await get<Product[]>('/admin/products')
}

export async function createProduct(payload: { name: string; basePrice: number; status?: number }) {
  return await post<Product>('/admin/products', payload)
}

export async function updateProduct(id: number, payload: { name: string; basePrice: number; status?: number }) {
  return await put<Product>(`/admin/products/${id}`, payload)
}

export async function deleteProduct(id: number) {
  return await del<null>(`/admin/products/${id}`)
}

export async function listPublicProducts() {
  return await get<Product[]>('/api/products')
}
