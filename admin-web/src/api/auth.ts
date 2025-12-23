import { get, post } from './http'

export type LoginResp = {
  tokenName: string
  tokenValue: string
  loginId: any
}

export async function login(username: string, password: string) {
  return await post<LoginResp>('/api/auth/login', { username, password })
}

export async function logout() {
  return await post<null>('/api/auth/logout')
}

export async function me() {
  return await get<{ isLogin: boolean; loginId: any }>('/api/auth/me')
}
