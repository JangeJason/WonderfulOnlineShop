import { clearToken, getToken } from "../utils/storage";
import { getApiBaseUrl } from "../utils/url";

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
};

export function request<T>(method: HttpMethod, url: string, data?: any): Promise<T> {
  const token = getToken();
  const baseUrl = getApiBaseUrl();

  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: baseUrl + url,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: token } : {}),
      },
      success: (res) => {
        if (res.statusCode && res.statusCode >= 400) {
          reject(new Error(`HTTP ${res.statusCode}`));
          return;
        }

        const body = res.data as any;
        if (!body || typeof body !== "object") {
          reject(new Error("Invalid response"));
          return;
        }

        const api = body as ApiResponse<T>;
        if (api.success) {
          resolve(api.data);
          return;
        }

        if (api.message === "not login") {
          clearToken();
          uni.reLaunch({ url: "/pages/login/login" });
        }

        reject(new Error(api.message || "Request failed"));
      },
      fail: (err) => {
        reject(new Error((err as any)?.errMsg || "Network error"));
      },
    });
  });
}

export function get<T>(url: string, params?: any) {
  return request<T>("GET", url, params);
}

export function del<T>(url: string): Promise<T> {
  return request<T>("DELETE", url);
}

export function put<T>(url: string, data?: any): Promise<T> {
  return request<T>("PUT", url, data);
}

export function post<T>(url: string, data?: any) {
  return request<T>("POST", url, data);
}
