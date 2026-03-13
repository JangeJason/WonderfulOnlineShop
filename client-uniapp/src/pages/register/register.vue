<template>
  <view class="register-page">
    <view class="top-decor">
      <view class="decor-circle c1"></view>
      <view class="decor-circle c2"></view>
    </view>

    <view class="brand-area">
      <view class="brand-icon">W</view>
      <text class="brand-name">创建账号</text>
      <text class="brand-sub">加入 Wonderful Shop</text>
    </view>

    <view class="form-area">
      <view class="input-group">
        <text class="input-label">用户名</text>
        <view class="input-box">
          <input
            v-model="form.username"
            placeholder="3~20个字符"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">密码</text>
        <view class="input-box">
          <input
            v-model="form.password"
            password
            placeholder="至少4位"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">确认密码</text>
        <view class="input-box">
          <input
            v-model="confirmPassword"
            password
            placeholder="再次输入密码"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">并且请提供联系方式以便后续能更好地处理订单相关：</text>
      </view>
      <view class="input-group">
        <text class="input-label">手机号码</text>
        <view class="input-box">
          <input
            v-model="form.phone"
            placeholder="您的手机号"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">电子邮箱</text>
        <view class="input-box">
          <input
            v-model="form.email"
            placeholder="您的邮箱地址"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>
      <view class="input-group">
        <text class="input-label">昵称（可选）</text>
        <view class="input-box">
          <input
            v-model="form.nickname"
            placeholder="您的显示名称"
            placeholder-class="placeholder"
            :adjust-position="true"
          />
        </view>
      </view>

      <view class="submit-btn" :class="{ disabled: loading }" @click="onRegister">
        <text class="btn-text">{{ loading ? '注册中...' : '注 册' }}</text>
      </view>

      <view class="login-link" @click="goLogin">
        <text class="link-text">已有账号？</text>
        <text class="link-highlight">去登录</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { register } from "../../api/auth";
import { setToken } from "../../utils/storage";

const form = reactive({ username: "", password: "", nickname: "", phone: "", email: "" });
const confirmPassword = ref("");
const loading = ref(false);

async function onRegister() {
  if (!form.username || !form.password) {
    uni.showToast({ title: "请输入用户名和密码", icon: "none" });
    return;
  }
  if (!form.phone || !form.email) {
    uni.showToast({ title: "请填写手机号码和电子邮箱", icon: "none" });
    return;
  }
  if (form.username.length < 3) {
    uni.showToast({ title: "用户名至少3个字符", icon: "none" });
    return;
  }
  if (form.password.length < 4) {
    uni.showToast({ title: "密码至少4个字符", icon: "none" });
    return;
  }
  if (form.password !== confirmPassword.value) {
    uni.showToast({ title: "两次密码不一致", icon: "none" });
    return;
  }
  loading.value = true;
  try {
    const res = await register(form);
    setToken(res.tokenValue);
    uni.showToast({ title: "注册成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages/index/index" });
    }, 600);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "注册失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function goLogin() {
  uni.navigateBack();
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 0 48rpx;
  padding-bottom: 80rpx;
  position: relative;
}
.top-decor {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 500rpx;
  pointer-events: none;
}
.decor-circle { position: absolute; border-radius: 50%; }
.c1 {
  width: 400rpx; height: 400rpx;
  background: radial-gradient(circle, rgba(168, 85, 247, 0.15), transparent 70%);
  top: -100rpx; left: -100rpx;
}
.c2 {
  width: 300rpx; height: 300rpx;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.1), transparent 70%);
  top: 60rpx; right: -60rpx;
}

.brand-area {
  padding-top: 140rpx;
  padding-bottom: 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.brand-icon {
  width: 88rpx; height: 88rpx;
  background: linear-gradient(135deg, #8B5CF6, #3B82F6);
  border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 44rpx; font-weight: 800; color: #fff;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 32rpx rgba(139, 92, 246, 0.3);
}
.brand-name {
  font-size: 38rpx; font-weight: 700; color: #0F172A;
}
.brand-sub {
  font-size: 24rpx; color: #64748B; margin-top: 8rpx;
}

.form-area { position: relative; }
.input-group { margin-bottom: 28rpx; }
.input-label {
  display: block; font-size: 26rpx; color: #475569;
  margin-bottom: 12rpx; font-weight: 500;
}
.input-box {
  background: #FFFFFF;
  border: 2rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 0 24rpx;
  height: 92rpx;
  display: flex;
  align-items: center;
}
.input-box input {
  width: 100%;
  height: 92rpx;
  font-size: 30rpx;
  color: #0F172A;
}
.placeholder { color: #64748B; }

.submit-btn {
  margin-top: 16rpx;
  background: linear-gradient(135deg, #8B5CF6, #3B82F6);
  height: 96rpx;
  border-radius: 16rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(139, 92, 246, 0.25);
}
.submit-btn:active { opacity: 0.85; }
.submit-btn.disabled { opacity: 0.6; }
.btn-text {
  color: #fff; font-size: 32rpx; font-weight: 600; letter-spacing: 6rpx;
}

.login-link {
  display: flex; justify-content: center; align-items: center;
  margin-top: 36rpx; gap: 4rpx;
}
.link-text { font-size: 26rpx; color: #64748B; }
.link-highlight { font-size: 26rpx; color: #8B5CF6; font-weight: 600; }
</style>
