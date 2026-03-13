import { get, post, put, del } from './http';

export interface Address {
    id?: number;
    userId?: number;
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
    isDefault: boolean;
    createdAt?: string;
    updatedAt?: string;
}

export async function getAddresses(): Promise<Address[]> {
    return await get<Address[]>('/api/addresses');
}

export async function addAddress(address: Address): Promise<Address> {
    return await post<Address>('/api/addresses', address);
}

export async function updateAddress(id: number, address: Address): Promise<null> {
    return await put<null>(`/api/addresses/${id}`, address);
}

export async function deleteAddress(id: number): Promise<null> {
    return await del<null>(`/api/addresses/${id}`);
}

export async function setDefaultAddress(id: number): Promise<null> {
    return await put<null>(`/api/addresses/${id}/default`);
}
