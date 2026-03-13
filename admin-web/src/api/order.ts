import { get, put } from './http'

export type OrderItem = {
    id: number
    orderId: number
    productId: number
    productName: string
    quantity: number
    unitPrice: string | number
    paramsSnapshot: string | null
    designFileUrl: string | null
}

export type Order = {
    id: number
    userId: number
    userNickname?: string
    userCompanyName?: string
    totalAmount: string | number
    status: string
    remark: string | null
    createdAt: string
    updatedAt: string
}

export type OrderDetail = {
    order: Order
    items: OrderItem[]
}

export type PageResult<T> = {
    records: T[]
    total: number
    size: number
    current: number
}

export async function listAdminOrders(page = 1, size = 15) {
    return await get<PageResult<Order>>('/admin/orders', { page, size })
}

export async function getAdminOrderDetail(id: number) {
    return await get<OrderDetail>(`/admin/orders/${id}`)
}

export async function updateOrderStatus(id: number, status: string) {
    return await put<Order>(`/admin/orders/${id}/status`, { status })
}

export async function shipOrder(id: number) {
    return await put<Order>(`/admin/orders/${id}/ship`)
}
