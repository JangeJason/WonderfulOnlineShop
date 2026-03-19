<template>
  <view class="custom-tabbar-spacer"></view>
  <view class="custom-tabbar">
    <view
      v-for="item in tabs"
      :key="item.pagePath"
      class="tab-item"
      :class="{ active: activePath === item.pagePath }"
      @click="switchTo(item.pagePath)"
    >
      <image
        class="tab-icon"
        :src="activePath === item.pagePath ? item.iconActive : item.iconInactive"
        mode="aspectFit"
      />
      <text class="tab-label">{{ item.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";

const tabs = [
  {
    pagePath: "pages/index/index",
    label: "首页",
    iconInactive: "/static/icons/shop-gray.svg",
    iconActive: "/static/icons/shop-blue.svg",
  },
  {
    pagePath: "pages/cart/cart",
    label: "购物车",
    iconInactive: "/static/icons/cart-gray.svg",
    iconActive: "/static/icons/cart-blue.svg",
  },
  {
    pagePath: "pages/orders/orders",
    label: "订单",
    iconInactive: "/static/icons/order-gray.svg",
    iconActive: "/static/icons/order-blue.svg",
  },
  {
    pagePath: "pages/profile/profile",
    label: "我的",
    iconInactive: "/static/icons/me-gray.svg",
    iconActive: "/static/icons/me-blue.svg",
  },
];

const activePath = ref("");

function syncActivePath() {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1];
  activePath.value = current?.route || "";
}

function hideNativeTabBar() {
  uni.hideTabBar({ animation: false });
}

function switchTo(pagePath: string) {
  if (activePath.value === pagePath) return;
  uni.switchTab({ url: `/${pagePath}` });
}

onShow(() => {
  syncActivePath();
  hideNativeTabBar();
});
</script>

<style scoped>
.custom-tabbar-spacer {
  height: calc(84rpx + env(safe-area-inset-bottom));
}

.custom-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 76rpx;
  padding: 15rpx 20rpx calc(15rpx + env(safe-area-inset-bottom));
  background: #ffffff;
  border-top: 1rpx solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 1200;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rpx;
}

.tab-icon {
  width: 50rpx;
  height: 50rpx;
}

.tab-label {
  font-size: 20rpx;
  color: #64748b;
  line-height: 1;
}
.tab-item.active .tab-label {
  color: #3b82f6;
}
</style>
