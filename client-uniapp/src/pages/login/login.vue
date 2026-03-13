<template>
  <view class="login-page">
    <!-- 顶部装饰 -->
    <view class="top-decor">
      <view class="decor-circle c1"></view>
      <view class="decor-circle c2"></view>
    </view>

    <!-- 品牌区域 -->
    <view class="brand-area">
      <view class="brand-icon">W</view>
      <text class="brand-name">Wonderful Shop</text>
      <text class="brand-sub">欢迎回来</text>
    </view>

    <!-- 表单区域 -->
    <view class="form-area">
      <!-- #ifndef MP-WEIXIN -->
      <view class="input-group">
        <text class="input-label">手机号 / 邮箱</text>
        <view class="input-box">
          <input
            v-model="account"
            placeholder="请输入手机号或邮箱"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">密码</text>
        <view class="input-box">
          <input
            v-model="password"
            password
            placeholder="请输入密码"
            placeholder-class="placeholder"
            :adjust-position="true"
            @confirm="onLogin"
          />
        </view>
      </view>

      <view class="login-btn" :class="{ disabled: loading }" @click="onLogin">
        <text class="btn-text">{{ loading ? '登录中...' : '登 录' }}</text>
      </view>
      <!-- #endif -->

      <!-- #ifdef MP-WEIXIN -->
      <view class="wx-login-wrap">
        <button
          class="wx-phone-btn"
          open-type="getPhoneNumber"
          :disabled="loading"
          @getphonenumber="onWechatPhoneLogin"
        >
          {{ loading ? "登录中..." : "微信手机号一键登录" }}
        </button>
      </view>
      <!-- #endif -->

      <view class="register-link" @click="goRegister">
        <text class="link-text">没有账号？</text>
        <text class="link-highlight">立即注册</text>
      </view>
    </view>

    <!-- 底部提示 -->
    <view class="footer-hint">
      <!-- #ifndef MP-WEIXIN -->
      <text class="hint-text">请使用手机号或邮箱登录</text>
      <!-- #endif -->
      <!-- #ifdef MP-WEIXIN -->
      <text class="hint-text">微信端使用手机号快速登录</text>
      <!-- #endif -->
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { loginByPhoneOrEmail, wechatPhoneLogin } from "../../api/auth";
import { setToken } from "../../utils/storage";

const account = ref("");
const password = ref("");
const loading = ref(false);

async function onLogin() {
  // #ifdef MP-WEIXIN
  return;
  // #endif

  loading.value = true;
  try {
    if (!account.value || !password.value) {
      uni.showToast({ title: "请输入手机号/邮箱和密码", icon: "none" });
      loading.value = false;
      return;
    }
    if (!account.value.includes("@") && !/^1[3-9]\d{9}$/.test(account.value)) {
      uni.showToast({ title: "请输入正确的手机号或邮箱", icon: "none" });
      loading.value = false;
      return;
    }
    const res = await loginByPhoneOrEmail(account.value, password.value);

    setToken(res.tokenValue);
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages/index/index" });
    }, 600);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "登录失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

async function onWechatPhoneLogin(e: any) {
  // #ifndef MP-WEIXIN
  return;
  // #endif
  const code = e?.detail?.code;
  const encryptedData = e?.detail?.encryptedData;
  const iv = e?.detail?.iv;
  if (!code && (!encryptedData || !iv)) {
    uni.showToast({ title: "你已取消授权或授权失败", icon: "none" });
    return;
  }

  loading.value = true;
  try {
    let jsCode = "";
    try {
      const loginRes: any = await uni.login({ provider: "weixin" });
      jsCode = loginRes?.code || "";
    } catch {}

    const res = await wechatPhoneLogin({
      code,
      jsCode,
      encryptedData,
      iv
    });
    setToken(res.tokenValue);
    if (res.passwordSet === false) {
      uni.showToast({ title: "请先设置登录密码（用于网页端登录）", icon: "none" });
      setTimeout(() => {
        uni.reLaunch({ url: "/pages/set-password/set-password?force=1" });
      }, 500);
      return;
    }
    uni.showToast({ title: "登录成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages/index/index" });
    }, 500);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "登录失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function goRegister() {
  uni.navigateTo({ url: "/pages/register/register" });
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 0 48rpx;
  position: relative;
}

/* 装饰圆 */
.top-decor {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 500rpx;
  pointer-events: none;
}
.decor-circle {
  position: absolute;
  border-radius: 50%;
}
.c1 {
  width: 400rpx;
  height: 400rpx;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.15), transparent 70%);
  top: -100rpx;
  left: -100rpx;
}
.c2 {
  width: 300rpx;
  height: 300rpx;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.1), transparent 70%);
  top: 60rpx;
  right: -60rpx;
}

/* 品牌 */
.brand-area {
  padding-top: 180rpx;
  padding-bottom: 80rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand-icon {
  width: 100rpx;
  height: 100rpx;
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  font-weight: 800;
  color: #fff;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(59, 130, 246, 0.3);
}
.brand-name {
  font-size: 42rpx;
  font-weight: 700;
  color: #0F172A;
  letter-spacing: 2rpx;
}
.brand-sub {
  font-size: 26rpx;
  color: #64748B;
  margin-top: 8rpx;
}

/* 表单 */
.form-area {
  position: relative;
}
.input-group {
  margin-bottom: 32rpx;
}
.input-label {
  display: block;
  font-size: 26rpx;
  color: #475569;
  margin-bottom: 12rpx;
  font-weight: 500;
}
.input-box {
  background: #FFFFFF;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 96rpx;
  display: flex;
  align-items: center;
}
.input-box input {
  width: 100%;
  height: 96rpx;
  font-size: 30rpx;
  color: #0F172A;
}
.placeholder {
  color: #64748B;
}

/* 按钮 */
.login-btn {
  margin-top: 16rpx;
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  height: 96rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(59, 130, 246, 0.25);
}
.login-btn:active {
  opacity: 0.85;
  transform: scale(0.98);
}
.login-btn.disabled {
  opacity: 0.6;
}
.wx-login-wrap {
  margin-top: 20rpx;
}
.wx-phone-btn {
  width: 100%;
  margin: 0;
  border: none;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #07C160, #13CE66);
}
.wx-phone-btn::after {
  border: none;
}
.btn-text {
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  letter-spacing: 6rpx;
}

/* 注册链接 */
.register-link {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 40rpx;
  gap: 4rpx;
}
.link-text {
  font-size: 26rpx;
  color: #64748B;
}
.link-highlight {
  font-size: 26rpx;
  color: #3B82F6;
  font-weight: 600;
}

/* 底部 */
.footer-hint {
  position: fixed;
  bottom: 60rpx;
  left: 0;
  right: 0;
  text-align: center;
}
.hint-text {
  font-size: 22rpx;
  color: #E2E8F0;
}

@media screen and (min-width: 768px) {
  .footer-hint {
    max-width: 1200px;
    margin: 0 auto;
    left: 0;
    right: 0;
  }
}
</style>
