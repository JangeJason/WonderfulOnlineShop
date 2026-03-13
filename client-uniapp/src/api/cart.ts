import { get, post, put, del } from "./http";

export type CartItem = {
    id: number;
    userId: number;
    productId: number;
    productName?: string;
    quantity: number;
    paramsSnapshot: string | null;
    unitPrice: number;
    createdAt: string;
    printFileUrl?: string;
    proofFileUrl?: string;
    hasCopyright?: boolean;
    copyrightFileUrl?: string;
    selected?: boolean;
};

export type AddCartRequest = {
    productId: number;
    quantity: number;
    paramsSnapshot: string | null;
    unitPrice: number;
    printFileUrl?: string;
    proofFileUrl?: string;
    hasCopyright?: boolean;
    copyrightFileUrl?: string;
};

export async function listCartItems(): Promise<CartItem[]> {
    return await get<CartItem[]>("/api/cart/items");
}

export async function addCartItem(req: AddCartRequest): Promise<CartItem> {
    return await post<CartItem>("/api/cart/items", req);
}

export async function updateCartItemQuantity(
    id: number,
    quantity: number
): Promise<CartItem> {
    return await put<CartItem>(`/api/cart/items/${id}`, { quantity });
}

export async function removeCartItem(id: number): Promise<void> {
    return await del<void>(`/api/cart/items/${id}`);
}

export async function clearCart(): Promise<void> {
    return await del<void>("/api/cart/clear");
}

export async function batchDeleteItems(ids: number[]): Promise<void> {
    return await post<void>("/api/cart/items/batch-delete", { ids });
}
