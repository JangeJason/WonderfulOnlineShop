import { get, put } from './http'

export type OrderItem = {
    id: number
    orderId: number
    productId: number
    productName: string
    quantity: number
    unitPrice: string | number
    paramsSnapshot: string | null
    printFileUrl: string | null
    proofFileUrl: string | null
    hasCopyright?: boolean
    copyrightFileUrl?: string | null
}

export type Order = {
    id: number
    userId: number
    userNickname?: string
    userCompanyName?: string
    userPhone?: string
    userEmail?: string
    receiverName?: string
    receiverPhone?: string
    receiverAddress?: string
    totalAmount: string | number
    status: string
    productionStatus?: string | null
    shippingStatus?: string | null
    reviewStatus?: string | null
    reviewReason?: string | null
    reviewedBy?: number | null
    reviewedAt?: string | null
    remark: string | null
    hasCopyrightWarning?: boolean
    hasCopyrightIssue?: boolean
    urgent?: boolean
    previewProductNames?: string[]
    previewProductCount?: number
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

export type AdminOrderFilter = {
    orderId?: number
    status?: string
    customerKeyword?: string
    regionKeyword?: string
    hasCopyrightFile?: boolean
    urgent?: boolean
    createdFrom?: string
    createdTo?: string
}

export async function listAdminOrders(page = 1, size = 15, filters?: AdminOrderFilter) {
    return await get<PageResult<Order>>('/admin/orders', { page, size, ...(filters || {}) })
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

export async function updateShippingStatus(id: number, shippingStatus: string) {
    return await put<Order>(`/admin/orders/${id}/shipping-status`, { shippingStatus })
}

export async function updateProductionStatus(id: number, productionStatus: string) {
    return await put<Order>(`/admin/orders/${id}/production-status`, { productionStatus })
}

export async function approveOrderReview(id: number) {
    return await put<Order>(`/admin/orders/${id}/review/approve`)
}

export async function rejectOrderReview(id: number, reason: string) {
    return await put<Order>(`/admin/orders/${id}/review/reject`, { reason })
}

export async function advanceProductionStatus(id: number) {
    return await put<Order>(`/admin/orders/${id}/production/next`)
}
