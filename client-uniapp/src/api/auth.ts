import { post, get, put } from "./http";

export type LoginRequest = {
  phone?: string;
  email?: string;
  password: string;
};

export type RegisterRequest = {
  username: string;
  password: string;
  nickname?: string;
  phone: string;
  email: string;
};

export type AuthResponse = {
  tokenName: string;
  tokenValue: string;
  loginId: any;
  nickname?: string;
  companyName?: string;
  avatarUrl?: string;
  role?: string;
  phone?: string;
  email?: string;
  passwordSet?: boolean;
};

export type MeResponse = {
  isLogin: boolean;
  loginId?: any;
  nickname?: string;
  companyName?: string;
  avatarUrl?: string;
  role?: string;
  username?: string;
  phone?: string;
  email?: string;
  passwordSet?: boolean;
};

export async function loginByPhoneOrEmail(account: string, password: string): Promise<AuthResponse> {
  const payload: LoginRequest = { password };
  if (account.includes("@")) {
    payload.email = account.trim();
  } else {
    payload.phone = account.trim();
  }
  return await post<AuthResponse>("/api/auth/login", payload);
}

export async function loginByPhone(phone: string): Promise<AuthResponse> {
  return await post<AuthResponse>("/api/auth/mobile-login", { phone: phone.trim() });
}

export async function wechatPhoneLogin(payload: {
  code?: string;
  jsCode?: string;
  encryptedData?: string;
  iv?: string;
}): Promise<AuthResponse> {
  return await post<AuthResponse>("/api/auth/wechat/phone-login", payload);
}

export async function register(req: RegisterRequest): Promise<AuthResponse> {
  return await post<AuthResponse>("/api/auth/register", req);
}

export async function updateProfile(nickname: string, companyName?: string, avatarUrl?: string, phone?: string, email?: string): Promise<null> {
  return await put<null>("/api/auth/profile", { nickname, companyName, avatarUrl, phone, email });
}

export async function changePassword(oldPassword: string, newPassword: string): Promise<null> {
  return await put<null>("/api/auth/password", { oldPassword, newPassword });
}

export async function initPassword(newPassword: string): Promise<null> {
  return await put<null>("/api/auth/password/init", { newPassword });
}

export async function syncWechatProfile(nickname?: string, avatarUrl?: string): Promise<null> {
  return await put<null>("/api/auth/wechat/profile-sync", { nickname, avatarUrl });
}

export async function getMe(): Promise<MeResponse> {
  return await get<MeResponse>("/api/auth/me");
}

export async function logout(): Promise<null> {
  return await post<null>("/api/auth/logout");
}
