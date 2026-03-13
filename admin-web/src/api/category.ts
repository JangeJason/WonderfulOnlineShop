import { get, post, put, del } from './http'

export type Category = {
    id: number
    name: string
    parentId?: number | null
    sortOrder: number
}

export async function listCategories() {
    return await get<Category[]>('/admin/categories')
}

export async function listPublicCategories() {
    return await get<Category[]>('/api/products/categories')
}

export async function createCategory(payload: { name: string; sortOrder?: number; parentId?: number | null }) {
    return await post<Category>('/admin/categories', payload)
}

export async function updateCategory(id: number, payload: { name?: string; sortOrder?: number; parentId?: number | null }) {
    return await put<Category>(`/admin/categories/${id}`, payload)
}

export async function deleteCategory(id: number) {
    return await del<null>(`/admin/categories/${id}`)
}
