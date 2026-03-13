import axios from 'axios'

type ApiResponse<T> = {
  success: boolean
  message: string
  data: T
}

const client = axios.create({
  timeout: 10000
})

client.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('tokenValue')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers['Authorization'] = token
  }
  return config
})

client.interceptors.response.use(
  (resp: any) => {
    const body = resp?.data
    if (body && typeof body === 'object' && 'success' in body) {
      const r = body as ApiResponse<any>
      if (!r.success) {
        if (r.message === 'not login') {
          localStorage.removeItem('tokenValue')
          window.location.href = '/login'
        }
        throw new Error(r.message || 'request failed')
      }
      return r.data
    }
    return body
  },
  (err: any) => Promise.reject(err)
)

export async function get<T>(url: string, params?: Record<string, any>): Promise<T> {
  return await client.get(url, { params })
}

export async function post<T>(url: string, data?: any): Promise<T> {
  return await client.post(url, data)
}

export async function put<T>(url: string, data?: any): Promise<T> {
  return await client.put(url, data)
}

export async function del<T>(url: string): Promise<T> {
  return await client.delete(url)
}
