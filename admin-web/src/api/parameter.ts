import { del, get, post, put } from './http'

export interface ProductParameter {
    id: number
    productId: number
    paramName: string
    paramType: 'INPUT' | 'SELECT'
    isRequired: boolean
    isMultiple?: boolean
    isDynamicByMaterial?: boolean
    unit: string | null
    pricingRule: Record<string, any> | null
    validationRules: Record<string, any> | null
    sortOrder: number
}

export interface MaterialRuleItem {
    paramId: number
    enabled: boolean
    optionIds: number[]
}

export interface MaterialRulesResponse {
    materialParameterId: number | null
    materialOptionId: number | null
    rules: MaterialRuleItem[]
}

export interface ParameterOption {
    id: number
    parameterId: number
    optionName: string
    priceAdjustment: number
    imageUrl?: string
    sortOrder: number
}

// Parameters
export async function listParameters(productId: number) {
    return await get<ProductParameter[]>(`/api/admin/product-parameters?productId=${productId}`)
}

export async function createParameter(data: Partial<ProductParameter>) {
    return await post<ProductParameter>('/api/admin/product-parameters', data)
}

export async function updateParameter(id: number, data: Partial<ProductParameter>) {
    return await put<ProductParameter>(`/api/admin/product-parameters/${id}`, data)
}

export async function deleteParameter(id: number) {
    return await del<null>(`/api/admin/product-parameters/${id}`)
}

// Options
export async function listOptions(parameterId: number) {
    return await get<ParameterOption[]>(`/api/admin/parameter-options?parameterId=${parameterId}`)
}

export async function createOption(data: Partial<ParameterOption>) {
    return await post<ParameterOption>('/api/admin/parameter-options', data)
}

export async function updateOption(id: number, data: Partial<ParameterOption>) {
    return await put<ParameterOption>(`/api/admin/parameter-options/${id}`, data)
}

export async function deleteOption(id: number) {
    return await del<null>(`/api/admin/parameter-options/${id}`)
}

export async function getMaterialRules(productId: number, materialOptionId?: number) {
    return await get<MaterialRulesResponse>(`/api/admin/products/${productId}/material-rules`, materialOptionId ? { materialOptionId } : undefined)
}

export async function saveMaterialRules(productId: number, payload: { materialOptionId: number; rules: MaterialRuleItem[] }) {
    return await put<MaterialRulesResponse>(`/api/admin/products/${productId}/material-rules`, payload)
}
