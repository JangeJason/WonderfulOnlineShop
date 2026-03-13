<template>
  <view class="profile-edit-page">
    <OfficialHeader />
    <view class="page-container">
      <view class="content-wrapper">
      <view class="custom-nav">
        <view class="back-btn" @click="onBack">
          <text class="back-icon">←</text>
        </view>
        <view class="page-title">个人设置</view>
      </view>

      <!-- 头像和基本信息修改 -->
      <view class="section">
        <view class="section-head">
          <view class="section-bar"></view>
          <text class="section-title">基本信息</text>
        </view>

        <view class="avatar-edit" @click="onChooseAvatar">
          <image v-if="formData.avatarUrl" :src="getImageUrl(formData.avatarUrl)" mode="aspectFill" class="avatar-img" />
          <view v-else class="avatar-placeholder">
            <text class="avatar-text">{{ formData.nickname?.charAt(0) || 'U' }}</text>
          </view>
          <view class="edit-overlay"><text>更换头像</text></view>
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <view class="action-btn sync-wx-btn" @click="onSyncWechatProfile">
          <text class="action-text">同步微信头像昵称</text>
        </view>
        <!-- #endif -->

        <view class="form-item">
          <text class="form-label">昵称</text>
          <view class="input-box">
            <input
              v-model="formData.nickname"
              placeholder="请输入昵称"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">公司名称</text>
          <view class="input-box">
            <input
              v-model="formData.companyName"
              placeholder="请输入公司名称 (选填)"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">手机号码</text>
          <view class="input-box">
            <input
              v-model="formData.phone"
              placeholder="请输入手机号码"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">电子邮箱</text>
          <view class="input-box">
            <input
              v-model="formData.email"
              placeholder="请输入邮箱地址"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="action-btn" @click="onSaveProfile">
          <text class="action-text">保存基本信息</text>
        </view>
      </view>

      <!-- 修改密码 -->
      <view class="section">
        <view class="section-head">
          <view class="section-bar"></view>
          <text class="section-title">{{ passwordSet ? '修改密码' : '设置登录密码' }}</text>
        </view>

        <view class="form-item" v-if="passwordSet">
          <text class="form-label">当前密码</text>
          <view class="input-box">
            <input
              v-model="pwdData.oldPassword"
              password
              placeholder="输入当前密码"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="form-item">
          <text class="form-label">新密码</text>
          <view class="input-box">
            <input
              v-model="pwdData.newPassword"
              password
              placeholder="输入新密码（至少4位）"
              placeholder-class="placeholder"
            />
          </view>
        </view>

        <view class="action-btn" @click="onChangePassword">
          <text class="action-text">{{ passwordSet ? '修改密码' : '设置密码' }}</text>
        </view>
      </view>

      </view>

      <!-- 退出登录 -->
      <view class="logout-section">
        <view class="logout-btn" @click="onLogout">
          <text class="logout-text">退出登录</text>
        </view>
      </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getMe, updateProfile, changePassword, initPassword, syncWechatProfile, logout } from "../../api/auth";
import { getToken, clearToken } from "../../utils/storage";
import { getApiBaseUrl, toAbsoluteAssetUrl } from "../../utils/url";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const formData = reactive({
  nickname: "",
  companyName: "",
  avatarUrl: "",
  phone: "",
  email: "",
});

const pwdData = reactive({
  oldPassword: "",
  newPassword: "",
});
const passwordSet = ref(true);

function getImageUrl(url: string) {
  return toAbsoluteAssetUrl(url);
}

function onBack() {
  uni.navigateBack();
}

async function onLogout() {
  try { await logout(); } catch {}
  clearToken();
  uni.reLaunch({ url: "/pages/login/login" });
}

async function loadUser() {
  try {
    const me = await getMe();
    if (me.isLogin) {
      formData.nickname = me.nickname || "";
      formData.companyName = me.companyName || "";
      formData.avatarUrl = me.avatarUrl || "";
      formData.phone = me.phone || "";
      formData.email = me.email || "";
      passwordSet.value = me.passwordSet !== false;
    }
  } catch {}
}

function onChooseAvatar() {
  uni.chooseImage({
    count: 1,
    success: (res) => {
      const tempFilePath = res.tempFilePaths[0];
      uploadAvatar(tempFilePath);
    }
  });
}

function uploadAvatar(filePath: string) {
  uni.showLoading({ title: "上传中..." });
  const token = getToken();
  const baseUrl = getApiBaseUrl();
  
  uni.uploadFile({
    url: `${baseUrl}/api/upload`,
    filePath,
    name: "file",
    header: {
      "Authorization": token
    },
    success: (uploadRes) => {
      uni.hideLoading();
      try {
        const data = JSON.parse(uploadRes.data);
        if (data.code === 200 || data.success) {
          formData.avatarUrl = typeof data.data === "string" ? data.data : (data.data?.url || "");
          uni.showToast({ title: "上传成功", icon: "success" });
        } else {
          uni.showToast({ title: data.message || "上传失败", icon: "none" });
        }
      } catch (e) {
        uni.showToast({ title: "上传异常", icon: "none" });
      }
    },
    fail: () => {
      uni.hideLoading();
      uni.showToast({ title: "网络错误", icon: "none" });
    }
  });
}

async function onSaveProfile() {
  if (!formData.nickname.trim()) {
    uni.showToast({ title: "请输入昵称", icon: "none" });
    return;
  }
  try {
    await updateProfile(formData.nickname.trim(), formData.companyName.trim(), formData.avatarUrl, formData.phone.trim(), formData.email.trim());
    uni.showToast({ title: "基本信息已保存", icon: "success" });
    setTimeout(() => {
      uni.navigateBack();
    }, 1500);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "更新失败", icon: "none" });
  }
}

async function onSyncWechatProfile() {
  // #ifndef MP-WEIXIN
  return;
  // #endif
  try {
    const profile: any = await uni.getUserProfile({ desc: "用于同步你的微信昵称与头像" });
    const nickname = profile?.userInfo?.nickName || "";
    const avatarUrl = profile?.userInfo?.avatarUrl || "";
    if (!nickname && !avatarUrl) {
      uni.showToast({ title: "未获取到微信资料", icon: "none" });
      return;
    }
    await syncWechatProfile(nickname, avatarUrl);
    if (nickname) formData.nickname = nickname;
    if (avatarUrl) formData.avatarUrl = avatarUrl;
    uni.showToast({ title: "微信资料已同步", icon: "success" });
  } catch (e: any) {
    uni.showToast({ title: e?.message || "同步失败", icon: "none" });
  }
}

async function onChangePassword() {
  if (pwdData.newPassword.length < 4) {
    uni.showToast({ title: "新密码至少4位", icon: "none" });
    return;
  }
  const wasPasswordSet = passwordSet.value;
  try {
    if (passwordSet.value) {
      if (!pwdData.oldPassword) {
        uni.showToast({ title: "请输入当前密码", icon: "none" });
        return;
      }
      await changePassword(pwdData.oldPassword, pwdData.newPassword);
      pwdData.oldPassword = "";
    } else {
      await initPassword(pwdData.newPassword);
      passwordSet.value = true;
    }
    pwdData.newPassword = "";
    uni.showToast({ title: wasPasswordSet ? "密码已修改" : "密码已设置", icon: "success" });
  } catch (e: any) {
    uni.showToast({ title: e?.message || "修改失败", icon: "none" });
  }
}

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: "/pages/login/login" });
    return;
  }
  loadUser();
});
</script>

<style scoped>
.profile-edit-page {
  min-height: 100vh;
  background: #F8FAFC;
}
.page-container {
  padding: 24rpx 32rpx;
  padding-bottom: 80rpx;
}
.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}
.custom-nav {
  display: flex;
  align-items: center;
  margin-bottom: 32rpx;
  position: relative;
}
.back-btn {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
  border-radius: 50%;
  background: #FFFFFF;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05);
}
.back-icon {
  font-size: 36rpx;
  color: #0F172A;
  font-weight: 600;
}
.page-title {
  font-size: 40rpx;
  font-weight: 700;
  color: #0F172A;
}

/* 退出登录 */
.logout-section {
  margin-top: 40rpx;
}
.logout-btn {
  background: #FFFFFF;
  border: 1rpx solid #ef4444;
  height: 88rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logout-btn:active {
  background: #fef2f2;
}
.logout-text {
  color: #ef4444;
  font-size: 30rpx;
  font-weight: 600;
}

/* 头像上传区 */
.avatar-edit {
  width: 160rpx;
  height: 160rpx;
  margin: 0 auto 40rpx;
  border-radius: 50%;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #3B82F6, #8B5CF6);
  border: 4rpx solid #fff;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 64rpx;
  font-weight: 800;
  color: #fff;
}
.edit-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  background: rgba(0,0,0,0.5);
  padding: 4rpx 0;
  text-align: center;
}
.edit-overlay text {
  color: #fff;
  font-size: 20rpx;
}

/* 分区 */
.section {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 32rpx 24rpx;
  margin-bottom: 24rpx;
}
.section-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 32rpx;
}
.section-bar {
  width: 6rpx;
  height: 28rpx;
  background: linear-gradient(180deg, #3B82F6, #8B5CF6);
  border-radius: 3rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 600;
  color: #1E293B;
}

.form-item {
  margin-bottom: 24rpx;
}
.form-label {
  display: block;
  font-size: 26rpx;
  color: #475569;
  margin-bottom: 12rpx;
}
.input-box {
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 0 24rpx;
  height: 88rpx;
  display: flex;
  align-items: center;
}
.input-box input {
  width: 100%;
  height: 88rpx;
  font-size: 28rpx;
  color: #0F172A;
}
.placeholder {
  color: #94A3B8;
}

.action-btn {
  margin-top: 32rpx;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  height: 88rpx;
  border-radius: 14rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(59, 130, 246, 0.25);
}
.action-btn:active {
  opacity: 0.85;
}
.sync-wx-btn {
  margin-top: 20rpx;
  background: linear-gradient(135deg, #07C160, #10B981);
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.25);
}
.action-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
