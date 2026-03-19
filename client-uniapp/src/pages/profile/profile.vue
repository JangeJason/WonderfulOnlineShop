<template>
  <view class="profile-page">
    <OfficialHeader />
    <view class="page-container">
      <view class="content-wrapper">
        <!-- 头部用户卡片 -->
        <view class="user-card">
          <view class="user-card-main">
            <view class="avatar">
              <image v-if="isLoggedIn && userInfo.avatarUrl" :src="getImageUrl(userInfo.avatarUrl)" mode="aspectFill" class="avatar-img" />
              <text v-else class="avatar-text">{{ isLoggedIn ? (userInfo.nickname?.charAt(0) || "U") : "?" }}</text>
            </view>
            <view class="user-row user-row-top" v-if="isLoggedIn">
              <text class="user-nickname">{{ userInfo.nickname || "用户" }}</text>
              <text class="user-username">{{ userInfo.username ? `@${userInfo.username}` : "@-" }}</text>
            </view>
            <view class="user-row user-row-bottom" v-if="isLoggedIn">
              <text class="user-company-inline">{{ userInfo.companyName || "未填写公司" }}</text>
              <text class="user-email-inline">{{ userInfo.email || "未填写邮箱" }}</text>
            </view>
            <view class="user-row user-row-top user-row-guest-top" v-else>
              <text class="user-nickname">未登录</text>
            </view>
            <view class="user-row user-row-bottom user-row-guest-bottom" v-if="!isLoggedIn">
              <view class="login-btn" @click="goLogin">
                <text class="login-btn-text">登录账户</text>
              </view>
            </view>
            <view class="settings-btn" @click="goSettings" v-if="isLoggedIn">
              <image class="settings-icon" src="/static/icons/setting.svg" mode="aspectFit" />
            </view>
          </view>
        </view>

        <view class="feature-grid">
          <view class="feature-item" @click="goAddressManage">
            <view class="feature-texts">
              <text class="feature-title">地址管理</text>
              <text class="feature-subtitle">管理收货地址</text>
            </view>
            <image class="feature-arrow" src="/static/icons/right.svg" mode="aspectFit" />
          </view>
          <view class="feature-item" @click="goCertificateManage">
            <view class="feature-texts">
              <text class="feature-title">证书管理</text>
              <text class="feature-subtitle">常用证书维护</text>
            </view>
            <image class="feature-arrow" src="/static/icons/right.svg" mode="aspectFit" />
          </view>
          <view class="feature-item" @click="goFavorites">
            <view class="feature-texts">
              <text class="feature-title">产品收藏</text>
              <text class="feature-subtitle">查看收藏商品</text>
            </view>
            <image class="feature-arrow" src="/static/icons/right.svg" mode="aspectFit" />
          </view>
          <view class="feature-item" @click="goAfterSales">
            <view class="feature-texts">
              <text class="feature-title">售后记录</text>
              <text class="feature-subtitle">查看售后进度</text>
            </view>
            <image class="feature-arrow" src="/static/icons/right.svg" mode="aspectFit" />
          </view>
        </view>
      </view>
    </view>

    <view class="contact-title-wrap">
      <text class="contact-title">环地福联系方式</text>
    </view>
    <OfficialFooter />
    <!-- #ifdef MP-WEIXIN -->
    <CustomTabBar />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getMe } from "../../api/auth";
import { getToken } from "../../utils/storage";
import { toAbsoluteAssetUrl } from "../../utils/url";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import CustomTabBar from "../../components/CustomTabBar/CustomTabBar.vue";

const userInfo = reactive({
  username: "",
  nickname: "",
  companyName: "",
  avatarUrl: "",
  role: "",
  email: "",
  phone: "",
});
const isLoggedIn = ref(false);

function getImageUrl(url: string) {
  return toAbsoluteAssetUrl(url);
}

function goSettings() {
  if (!ensureLogin()) return;
  uni.navigateTo({ url: "/pages/profile-edit/profile-edit" });
}

function goAddressManage() {
  if (!ensureLogin()) return;
  uni.navigateTo({ url: "/pages/address/address-list?from=profile" });
}

function goCertificateManage() {
  if (!ensureLogin()) return;
  uni.navigateTo({ url: "/pages/profile-certificates/profile-certificates" });
}

function goFavorites() {
  if (!ensureLogin()) return;
  uni.navigateTo({ url: "/pages/profile-favorites/profile-favorites" });
}

function goAfterSales() {
  if (!ensureLogin()) return;
  uni.navigateTo({ url: "/pages/profile-after-sales/profile-after-sales" });
}

function goLogin() {
  uni.navigateTo({ url: "/pages/login/login" });
}

function ensureLogin(): boolean {
  if (isLoggedIn.value) return true;
  goLogin();
  return false;
}

async function loadUser() {
  try {
    const me = await getMe();
    if (me.isLogin) {
      userInfo.username = me.username || "";
      userInfo.nickname = me.nickname || "";
      userInfo.companyName = me.companyName || "";
      userInfo.avatarUrl = me.avatarUrl || "";
      userInfo.role = me.role || "";
      userInfo.email = me.email || "";
      userInfo.phone = me.phone || "";
    }
  } catch {
    isLoggedIn.value = false;
    userInfo.username = "";
    userInfo.nickname = "";
    userInfo.companyName = "";
    userInfo.avatarUrl = "";
    userInfo.role = "";
    userInfo.email = "";
    userInfo.phone = "";
  }
}


onShow(() => {
  const token = getToken();
  if (!token) {
    isLoggedIn.value = false;
    userInfo.username = "";
    userInfo.nickname = "";
    userInfo.companyName = "";
    userInfo.avatarUrl = "";
    userInfo.role = "";
    userInfo.email = "";
    userInfo.phone = "";
    return;
  }
  isLoggedIn.value = true;
  loadUser();
});
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: #FFFFFF;
  padding-bottom: 80rpx;
}
.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}

/* 用户卡片 */
.user-card {
  background: #FFFFFF;
  border: none;
  border-radius: 4rpx;
  padding: 28rpx 24rpx;
  margin-bottom: 24rpx;
}
.user-card-main {
  display: grid;
  grid-template-columns: 120rpx 1fr 64rpx;
  grid-template-rows: min-content min-content;
  align-items: center;
  column-gap: 20rpx;
  row-gap: 0;
}
.settings-btn {
  grid-column: 3;
  grid-row: 1 / 3;
  padding: 10rpx;
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.user-row-guest-top {
  align-items: flex-end;
}
.user-row-guest-bottom {
  margin-top: 6rpx;
}
.login-btn {
  height: 52rpx;
  padding: 0 20rpx;
  border-radius: 8rpx;
  background: #0F4C81;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.login-btn-text {
  color: #FFFFFF;
  font-size: 22rpx;
  font-weight: 600;
}
.settings-icon {
  width: 40rpx;
  height: 40rpx;
}
.settings-btn:active {
  opacity: 0.7;
}
.avatar {
  grid-column: 1;
  grid-row: 1 / 3;
  width: 120rpx;
  height: 120rpx;
  background: #004178;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-text {
  font-size: 48rpx;
  font-weight: 800;
  color: #fff;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  min-width: 0;
  line-height: 1;
}
.user-row-top {
  grid-column: 2;
  grid-row: 1;
}
.user-row-bottom {
  grid-column: 2;
  grid-row: 2;
  margin-top: -4rpx;
}
.user-nickname {
  font-size: 34rpx;
  line-height: 38rpx;
  font-weight: 700;
  color: #0F172A;
  max-width: 60%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.user-username {
  font-size: 24rpx;
  line-height: 30rpx;
  color: #64748B;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.user-company-inline,
.user-email-inline {
  font-size: 24rpx;
  line-height: 30rpx;
  color: #475569;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.user-company-inline {
  max-width: 48%;
}
.user-email-inline {
  max-width: 48%;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  padding: 24rpx;
}
.feature-item {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 24rpx 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.feature-item:active {
  opacity: 0.75;
}
.feature-texts {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}
.feature-title {
  color: #0F172A;
  font-size: 30rpx;
  font-weight: 700;
}
.feature-subtitle {
  color: #64748B;
  font-size: 22rpx;
}
.feature-arrow {
  width: 28rpx;
  height: 28rpx;
}

.contact-title-wrap {
  border-top: 1rpx solid #e2e8f0;
  padding: 18rpx 24rpx 18rpx;
  text-align: center;
}
.contact-title {
  color: #334155;
  font-size: 30rpx;
  font-weight: 600;
}

:deep(.mp-footer) {
  border-top: none;
}



/* 退出 */
.logout-section {
  margin-top: 24rpx;
}
.logout-btn {
  background: transparent;
  border: 1rpx solid #E2E8F0;
  height: 88rpx;
  border-radius: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.logout-btn:active {
  background: rgba(239, 68, 68, 0.1);
}
.logout-text {
  color: #EF4444;
  font-size: 28rpx;
  font-weight: 500;
}
</style>
