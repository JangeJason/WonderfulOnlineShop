<template>
  <view class="set-password-page">
    <view class="card">
      <text class="title">设置登录密码</text>
      <text class="desc">为了后续在网页端使用手机号登录，请先设置密码并牢记。</text>

      <view class="input-group">
        <text class="label">新密码</text>
        <view class="input-box">
          <input v-model="newPassword" password placeholder="至少 4 位" placeholder-class="placeholder" />
        </view>
      </view>

      <view class="input-group">
        <text class="label">确认密码</text>
        <view class="input-box">
          <input v-model="confirmPassword" password placeholder="请再次输入密码" placeholder-class="placeholder" @confirm="onSubmit" />
        </view>
      </view>

      <view class="submit-btn" :class="{ disabled: loading }" @click="onSubmit">
        <text class="submit-text">{{ loading ? "提交中..." : "确认设置" }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getToken } from "../../utils/storage";
import { initPassword } from "../../api/auth";

const loading = ref(false);
const newPassword = ref("");
const confirmPassword = ref("");

onLoad(() => {
  if (!getToken()) {
    uni.reLaunch({ url: "/pages/login/login" });
  }
});

async function onSubmit() {
  if (newPassword.value.length < 4) {
    uni.showToast({ title: "密码至少 4 位", icon: "none" });
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    uni.showToast({ title: "两次输入密码不一致", icon: "none" });
    return;
  }

  loading.value = true;
  try {
    await initPassword(newPassword.value);
    uni.showToast({ title: "密码设置成功", icon: "success" });
    setTimeout(() => {
      uni.reLaunch({ url: "/pages/index/index" });
    }, 500);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "设置失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.set-password-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 120rpx 40rpx;
}
.card {
  background: #FFFFFF;
  border-radius: 20rpx;
  padding: 40rpx 32rpx;
  box-shadow: 0 8rpx 32rpx rgba(15, 23, 42, 0.08);
}
.title {
  display: block;
  font-size: 42rpx;
  font-weight: 700;
  color: #0F172A;
}
.desc {
  display: block;
  margin-top: 16rpx;
  margin-bottom: 36rpx;
  font-size: 26rpx;
  color: #64748B;
  line-height: 1.6;
}
.input-group {
  margin-bottom: 24rpx;
}
.label {
  display: block;
  margin-bottom: 10rpx;
  color: #334155;
  font-size: 26rpx;
}
.input-box {
  border: 2rpx solid #E2E8F0;
  border-radius: 14rpx;
  height: 90rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
}
.input-box input {
  width: 100%;
  font-size: 30rpx;
}
.placeholder {
  color: #94A3B8;
}
.submit-btn {
  margin-top: 20rpx;
  height: 90rpx;
  border-radius: 14rpx;
  background: #0F4C81;
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-btn.disabled {
  opacity: 0.6;
}
.submit-text {
  color: #fff;
  font-size: 30rpx;
  font-weight: 600;
}
</style>
