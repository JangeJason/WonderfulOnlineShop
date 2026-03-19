import { del, get, post, put } from "./http";

export type Order = {
    id: number;
    userId: number;
    totalAmount: number;
    status: string;
    productionStatus?: string | null;
    shippingStatus?: string | null;
    reviewStatus?: string | null;
    reviewReason?: string | null;
    reviewedBy?: number | null;
    reviewedAt?: string | null;
    customName?: string;
    remark: string | null;
    createdAt: string;
    updatedAt: string;
    previewItems?: OrderItem[];
    totalProductCount?: number;
    hasCopyrightWarning?: boolean;
    receiverName?: string;
    receiverPhone?: string;
    receiverAddress?: string;
};

export type OrderItem = {
    id: number;
    orderId: number;
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    paramsSnapshot: string | null;
    printFileUrl?: string;
    proofFileUrl?: string;
    hasCopyright?: boolean;
    copyrightFileUrl?: string;
};

export type OrderDetail = {
    order: Order;
    items: OrderItem[];
};

export type OrderPayCreateResponse = {
    channel: "JSAPI" | "NATIVE";
    outTradeNo: string;
    codeUrl?: string;
    jsapiParams?: {
        timeStamp: string;
        nonceStr: string;
        packageValue: string;
        signType: string;
        paySign: string;
    };
};

export async function createOrder(remark: string, cartItemIds: number[], addressId?: number): Promise<Order> {
    return await post<Order>("/api/orders", { remark, cartItemIds, addressId });
}

export async function listOrders(): Promise<Order[]> {
    return await get<Order[]>("/api/orders/list");
}

export async function getOrderDetail(id: number): Promise<OrderDetail> {
    return await get<OrderDetail>(`/api/orders/${id}`);
}

export async function payOrder(id: number): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/pay`);
}

export async function createOrderPayment(
    id: number,
    payload: { channel: "JSAPI" | "NATIVE"; jsCode?: string; clientIp?: string }
): Promise<OrderPayCreateResponse> {
    return await post<OrderPayCreateResponse>(`/api/orders/${id}/pay/create`, payload);
}

export async function syncOrderPayment(id: number): Promise<Order> {
    return await post<Order>(`/api/orders/${id}/pay/sync`);
}

export async function cancelOrder(id: number): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/cancel`);
}

export async function confirmOrder(id: number): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/confirm`);
}

export async function applyAfterSale(id: number): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/after-sale`);
}

export async function resubmitReview(id: number): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/resubmit-review`);
}

export async function updateOrderCustomName(id: number, customName: string): Promise<Order> {
    return await put<Order>(`/api/orders/${id}/custom-name`, { customName });
}

export async function reorderOrder(id: number): Promise<number> {
    return await post<number>(`/api/orders/${id}/reorder`);
}

export async function deleteCancelledOrder(id: number): Promise<void> {
    return await del<void>(`/api/orders/${id}`);
}

export async function updateRejectedOrderItem(
    orderId: number,
    itemId: number,
    payload: {
        quantity: number;
        paramsSnapshot: string;
        unitPrice: number;
        printFileUrl?: string;
        proofFileUrl?: string;
        hasCopyright?: boolean;
        copyrightFileUrl?: string;
    }
): Promise<OrderItem> {
    return await put<OrderItem>(`/api/orders/${orderId}/items/${itemId}`, payload);
}

export async function updateOrderItemFiles(
    orderId: number,
    itemId: number,
    payload: {
        printFileUrl?: string;
        printFileName?: string;
        proofFileUrl?: string;
        proofFileName?: string;
    }
): Promise<OrderItem> {
    return await put<OrderItem>(`/api/orders/${orderId}/items/${itemId}/files`, payload);
}
