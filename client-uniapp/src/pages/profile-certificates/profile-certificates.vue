<template>
  <view class="cert-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar" :style="mpToolbarStyle">
        <view class="toolbar-left" @click="goBack">
          <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <view class="toolbar-title">证书管理</view>
        <view class="toolbar-right">
          <view class="add-btn" @click="goAdd">新增证书</view>
        </view>
      </view>
      <view class="toolbar-spacer" :style="mpToolbarSpacerStyle"></view>

      <view v-if="loading" class="loading">加载中...</view>
      <view v-else-if="certs.length === 0" class="empty">暂无常用证书</view>
      <view v-else class="cert-list">
        <view class="cert-item" v-for="cert in certs" :key="cert.id" @click="goDetail(cert)">
          <view class="cert-top">
            <text class="cert-type">{{ cert.certificateType }}</text>
            <view class="cert-top-right">
              <text class="cert-date">截止 {{ cert.endDate }}</text>
              <text class="cert-status" :class="isExpired(cert.endDate) ? 'expired' : 'valid'">
                {{ isExpired(cert.endDate) ? "已过期" : "有效" }}
              </text>
            </view>
          </view>
          <text class="cert-line">{{ cert.trademarkContent }}</text>
          <text class="cert-line">{{ cert.principal }}</text>
        </view>
      </view>
    </view>
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import { listCommonCertificates, type UserCertificate } from "../../api/certificate";

const loading = ref(false);
const certs = ref<UserCertificate[]>([]);
const mpToolbarStyle = ref<Record<string, string>>({});
const mpToolbarSpacerStyle = ref<Record<string, string>>({});
const instance = getCurrentInstance();

async function loadList() {
  loading.value = true;
  try {
    certs.value = await listCommonCertificates();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function goAdd() {
  uni.navigateTo({ url: "/pages/certificate-add/certificate-add" });
}

function isExpired(endDate: string): boolean {
  if (!endDate) return false;
  const target = new Date(`${endDate}T23:59:59`).getTime();
  if (!Number.isFinite(target)) return false;
  return Date.now() > target;
}

function goDetail(cert: UserCertificate) {
  uni.setStorageSync("profile_certificate_detail", cert);
  uni.navigateTo({ url: "/pages/profile-certificate-detail/profile-certificate-detail" });
}

function goBack() {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({ url: "/pages/profile/profile" });
    }
  });
}

function syncToolbarLayout() {
  // #ifdef H5
  return;
  // #endif
  const query = uni.createSelectorQuery().in(instance?.proxy as any);
  query.select(".mp-header-spacer").boundingClientRect();
  query.select(".toolbar").boundingClientRect();
  query.exec((res: any[]) => {
    const headerSpacerHeight = Number(res?.[0]?.height || 0);
    const toolbarHeight = Number(res?.[1]?.height || 0);
    if (headerSpacerHeight > 0) {
      mpToolbarStyle.value = { top: `${Math.ceil(headerSpacerHeight)}px` };
    }
    if (toolbarHeight > 0) {
      mpToolbarSpacerStyle.value = { height: `${Math.ceil(toolbarHeight + 8)}px` };
    }
  });
}

onLoad(() => {
  // #ifndef H5
  nextTick(() => {
    syncToolbarLayout();
  });
  // #endif
});

onShow(() => {
  loadList();
  // #ifndef H5
  nextTick(() => {
    syncToolbarLayout();
  });
  // #endif
});
</script>

<style scoped>
.cert-page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 80rpx;
}
.content-wrapper {
  padding: 20rpx;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}
.toolbar-left {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.toolbar-title {
  flex: 1;
  text-align: center;
  color: #0f172a;
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1;
}
.toolbar-right {
  min-width: 160rpx;
  display: flex;
  justify-content: flex-end;
  flex-shrink: 0;
}
.back-arrow {
  width: 50rpx;
  height: 50rpx;
  transform: rotate(180deg);
}
.add-btn {
  background: #004178;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  padding: 14rpx 24rpx;
  border-radius: 8rpx;
  line-height: 1;
}
.loading,
.empty {
  text-align: center;
  color: #64748b;
  font-size: 26rpx;
  padding: 40rpx 0;
}
.cert-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.cert-item {
  background: #fff;
  border-radius: 10rpx;
  padding: 20rpx;
  border: 1rpx solid #e2e8f0;
}
.cert-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}
.cert-top-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.cert-type {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}
.cert-date {
  color: #64748b;
  font-size: 22rpx;
}
.cert-line {
  display: block;
  color: #475569;
  font-size: 24rpx;
  line-height: 1.6;
}
.cert-status {
  font-size: 20rpx;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  font-weight: 600;
}
.cert-status.valid {
  color: #15803d;
  background: #dcfce7;
}
.cert-status.expired {
  color: #b91c1c;
  background: #fee2e2;
}

/* #ifndef H5 */
.content-wrapper {
  padding: 0 16rpx;
}
.toolbar {
  position: fixed;
  left: 0;
  right: 0;
  z-index: 40;
  margin-bottom: 0;
  padding: 8rpx 16rpx;
  border-bottom: 1rpx solid #e2e8f0;
  background: #ffffff;
}
.toolbar-spacer {
  width: 100%;
}
/* #endif */
</style>
