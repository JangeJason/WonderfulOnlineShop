<template>
  <view class="orders-page">
    <OfficialHeader />
    <view class="page-container">
      <view class="content-wrapper">
      <view v-if="!isLoggedIn" class="login-empty">
        <text class="login-empty-title">未登录</text>
        <view class="login-empty-btn" @click="goLogin">
          <text class="login-empty-btn-text">去登录</text>
        </view>
      </view>
      <template v-else>
      <!-- Tabs -->
      <view class="tabs-wrap tabs-mp-fixed" :style="mpTabsTopStyle">
        <view class="tabs">
          <view class="tabs-items-wrap">
            <view
              v-for="tab in tabs"
              :key="tab.value"
              class="tab-item"
              :class="{ active: currentTab === tab.value }"
              @click="onMainTabChange(tab.value)"
            >
              <text class="tab-text">{{ tab.label }}</text>
              <text class="tab-count">{{ getTabCount(tab.value) }}</text>
              <view v-if="currentTab === tab.value" class="tab-indicator"></view>
            </view>
          </view>
          <!-- #ifdef MP-WEIXIN -->
          <view class="tabs-search-trigger" @click="toggleMpSearch">
            <image class="tabs-search-icon" :src="showMpSearch ? '/static/icons/close.svg' : '/static/icons/search.svg'" mode="aspectFit" />
          </view>
          <!-- #endif -->
          <!-- #ifdef H5 -->
          <view class="tabs-search-box">
            <input
              class="tabs-search-input"
              type="text"
              v-model="orderSearchKeyword"
              placeholder="搜索订单/商品..."
              @confirm="onSearchConfirm"
            />
            <view class="tabs-search-btn" @click="onSearchConfirm">搜索</view>
          </view>
          <!-- #endif -->
        </view>
        <view v-if="activeSubTabs.length > 0" class="sub-tabs">
          <view
            v-for="sub in activeSubTabs"
            :key="sub.value"
            class="sub-tab-item"
            :class="{ active: currentSubTab === sub.value }"
            @click="currentSubTab = sub.value"
          >
            <text class="sub-tab-text">{{ sub.label }}</text>
            <text class="sub-tab-count">{{ getSubTabCount(sub.value) }}</text>
          </view>
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <view class="tabs-search-row" v-if="showMpSearch">
          <input
            class="tabs-search-input"
            type="text"
            v-model="orderSearchKeyword"
            placeholder="搜索订单/商品..."
            @confirm="onSearchConfirm"
          />
          <view class="tabs-search-btn" @click="onSearchConfirm">搜索</view>
        </view>
        <!-- #endif -->
      </view>
      <!-- #ifdef MP-WEIXIN -->
      <view class="tabs-spacer" :style="mpTabsSpacerStyle"></view>
      <!-- #endif -->

      <view v-if="filteredOrders.length === 0" class="empty">
      <text class="empty-icon">📋</text>
      <text class="empty-text">暂无订单</text>
    </view>

    <view v-else class="order-list">
      <view v-for="order in filteredOrders" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <view class="order-top">
          <text class="order-id">{{ order.customName || '订单' }} #{{ order.id }}</text>
          <view class="order-top-right">
            <text v-if="order.hasCopyrightWarning" class="copyright-warning-tag">缺授权书</text>
            <view :class="['status-tag', statusClass(order.status)]">
              <text class="status-text">{{ displayStatusLabel(order) }}</text>
            </view>
          </view>
        </view>
        <view class="order-mid">
          <view v-if="order.previewItems && order.previewItems.length > 0" class="preview-items">
            <text v-for="(item, idx) in order.previewItems" :key="idx" class="preview-item-name">{{ item.productName }}</text>
            <text v-if="order.totalProductCount && order.previewItems.length < order.totalProductCount" class="preview-item-more">...</text>
          </view>
          <view class="time-and-count">
            <text class="order-time">{{ formatTime(order.createdAt) }}</text>
            <text class="order-count">共 {{ order.totalProductCount || 0 }} 个产品</text>
          </view>
        </view>
        <view class="order-bottom">
          <text class="order-amount">¥{{ order.totalAmount }}</text>
          <view class="order-actions">
            <view v-if="order.status === 'PENDING'" class="act-btn act-pay" @click.stop="onPay(order.id)">
              <text class="act-text">支付</text>
            </view>
            <view v-if="order.status === 'SHIPPED' && order.shippingStatus === 'SIGNED'" class="act-btn act-confirm" @click.stop="onConfirm(order.id)">
              <text class="act-text">确认收货</text>
            </view>
            <view v-if="order.status === 'CANCELLED'" class="act-btn act-reorder" @click.stop="onReorder(order.id)">
              <text class="act-text">重新下单</text>
            </view>
            <view v-if="order.status === 'CANCELLED'" class="act-btn act-delete-order" @click.stop="onDeleteOrder(order.id)">
              <text class="act-text">删除订单</text>
            </view>
          </view>
        </view>
      </view>
      </view>
      </template>
      </view>
    </view>
    <view v-if="showBackTop" class="back-top-btn" @click="backToTop">
      <image class="back-top-icon" src="/static/icons/right.svg" mode="aspectFit" />
      <text class="back-top-text">顶部</text>
    </view>
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
    <!-- #ifdef MP-WEIXIN -->
    <CustomTabBar />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from "vue";
import { watch } from "vue";
import { onShow, onReady, onPageScroll } from "@dcloudio/uni-app";
import { listOrders, confirmOrder, deleteCancelledOrder, reorderOrder, type Order } from "../../api/order";
import { getToken } from "../../utils/storage";
import { triggerWechatPay } from "../../utils/pay";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import CustomTabBar from "../../components/CustomTabBar/CustomTabBar.vue";

const orders = ref<Order[]>([]);
const currentTab = ref('');
const mpTabsTopStyle = ref<Record<string, string>>({});
const mpTabsSpacerStyle = ref<Record<string, string>>({});
const showBackTop = ref(false);
const orderSearchKeyword = ref("");
const showMpSearch = ref(false);
const isLoggedIn = ref(false);
const currentSubTab = ref('');

const tabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'PENDING' },
  { label: '待修改', value: 'REJECTED' },
  { label: '待生产', value: 'WAIT_PRODUCTION' },
  { label: '生产中', value: 'IN_PRODUCTION' },
  { label: '待发货', value: 'WAIT_SHIPMENT' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '售后中', value: 'AFTER_SALE' },
];

const productionSubTabs = [
  { label: "全部环节", value: "" },
  { label: "印前检查", value: "PREPRESS_CHECK" },
  { label: "制版中", value: "PLATE_MAKING" },
  { label: "印刷中", value: "PRINTING" },
  { label: "印后加工", value: "POST_PROCESS" },
  { label: "质检打包", value: "QC_PACKING" },
];

const shippingSubTabs = [
  { label: "全部物流", value: "" },
  { label: "运输中", value: "IN_TRANSIT" },
  { label: "派送中", value: "OUT_FOR_DELIVERY" },
  { label: "已签收", value: "SIGNED" },
];

const activeSubTabs = computed(() => {
  if (currentTab.value === "IN_PRODUCTION") return productionSubTabs;
  if (currentTab.value === "SHIPPED") return shippingSubTabs;
  return [];
});

const filteredOrders = computed(() => {
  const tabFiltered = currentTab.value
    ? orders.value.filter((o) => o.status === currentTab.value)
    : orders.value;
  const subFiltered = currentSubTab.value
    ? tabFiltered.filter((o) => {
        if (currentTab.value === "IN_PRODUCTION") return o.productionStatus === currentSubTab.value;
        if (currentTab.value === "SHIPPED") return o.shippingStatus === currentSubTab.value;
        return true;
      })
    : tabFiltered;
  const kw = orderSearchKeyword.value.trim().toLowerCase();
  if (!kw) return subFiltered;
  return subFiltered.filter((order) => buildOrderSearchText(order).includes(kw));
});

function onMainTabChange(value: string) {
  currentTab.value = value;
  currentSubTab.value = "";
  nextTick(() => {
    syncMpTabsOffsets();
  });
}

function getTabCount(status: string): number {
  if (!status) return orders.value.length;
  return orders.value.filter((o) => o.status === status).length;
}

function getSubTabCount(subStatus: string): number {
  if (!currentTab.value) return 0;
  const main = orders.value.filter((o) => o.status === currentTab.value);
  if (!subStatus) return main.length;
  if (currentTab.value === "IN_PRODUCTION") {
    return main.filter((o) => o.productionStatus === subStatus).length;
  }
  if (currentTab.value === "SHIPPED") {
    return main.filter((o) => o.shippingStatus === subStatus).length;
  }
  return 0;
}

function buildOrderSearchText(order: Order): string {
  const parts: string[] = [];
  parts.push(String(order.id || ""));
  parts.push(String(order.customName || ""));
  parts.push(String(order.receiverName || ""));
  parts.push(String(order.receiverPhone || ""));
  parts.push(String(order.receiverAddress || ""));
  const previewNames = (order.previewItems || []).map((i) => i.productName || "");
  parts.push(...previewNames);
  return parts.join(" ").toLowerCase();
}

function statusClass(s: string) {
  const map: Record<string, string> = {
    PENDING: 'st-pending',
    WAIT_PRODUCTION: 'st-wait-production',
    IN_PRODUCTION: 'st-in-production',
    WAIT_SHIPMENT: 'st-wait-shipment',
    SHIPPED: 'st-shipped',
    COMPLETED: 'st-completed',
    AFTER_SALE: 'st-after-sale',
    REJECTED: 'st-rejected',
    CANCELLED: 'st-cancelled'
  };
  return map[s] || 'st-pending';
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    REJECTED: '待修改',
    WAIT_PRODUCTION: '待生产',
    IN_PRODUCTION: '生产中',
    WAIT_SHIPMENT: '待发货',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    AFTER_SALE: '售后中',
    CANCELLED: '已取消'
  };
  return map[s] || s;
}

function productionStatusLabel(s?: string | null) {
  const map: Record<string, string> = {
    PREPRESS_CHECK: "印前检查",
    PLATE_MAKING: "制版",
    PRINTING: "印刷中",
    POST_PROCESS: "印后加工",
    QC_PACKING: "质检打包",
  };
  if (!s) return "";
  return map[s] || s;
}

function shippingStatusLabel(s?: string | null) {
  const map: Record<string, string> = {
    IN_TRANSIT: '运输中',
    OUT_FOR_DELIVERY: '派送中',
    SIGNED: '已签收'
  };
  if (!s) return '';
  return map[s] || s;
}

function displayStatusLabel(order: Order) {
  const main = statusLabel(order.status);
  if (order.status === "IN_PRODUCTION" && order.productionStatus) {
    return `${main} > ${productionStatusLabel(order.productionStatus)}`;
  }
  if (order.status === "SHIPPED" && order.shippingStatus) {
    return `${main} > ${shippingStatusLabel(order.shippingStatus)}`;
  }
  return main;
}

watch(currentTab, () => {
  currentSubTab.value = "";
  nextTick(() => {
    syncMpTabsOffsets();
  });
});

watch(
  () => activeSubTabs.value.length,
  () => {
    nextTick(() => {
      syncMpTabsOffsets();
    });
  }
);

function formatTime(t: string) {
  if (!t) return '—';
  return t.replace('T', ' ').substring(0, 16);
}

async function load() {
  try { orders.value = await listOrders(); }
  catch (e: any) { uni.showToast({ title: e?.message || '加载失败', icon: 'none' }); }
}

async function onPay(id: number) {
  try {
    await triggerWechatPay(id);
    uni.showToast({ title: '支付成功', icon: 'success' });
    load();
  }
  catch (e: any) { uni.showToast({ title: e?.message || '操作失败', icon: 'none' }); }
}

async function onConfirm(id: number) {
  try { await confirmOrder(id); uni.showToast({ title: '已确认收货', icon: 'success' }); load(); }
  catch (e: any) { uni.showToast({ title: e?.message || '操作失败', icon: 'none' }); }
}

async function onReorder(id: number) {
  try {
    const count = await reorderOrder(id);
    uni.showToast({ title: `已加入购物车${count}项`, icon: "success" });
    setTimeout(() => {
      uni.switchTab({ url: "/pages/cart/cart" });
    }, 500);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "操作失败", icon: "none" });
  }
}

function onDeleteOrder(id: number) {
  uni.showModal({
    title: "删除订单",
    content: "确认删除该已取消订单吗？",
    confirmText: "删除",
    cancelText: "取消",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await deleteCancelledOrder(id);
        uni.showToast({ title: "已删除", icon: "success" });
        load();
      } catch (e: any) {
        uni.showToast({ title: e?.message || "操作失败", icon: "none" });
      }
    },
  });
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${id}` });
}

function goLogin() {
  uni.navigateTo({ url: "/pages/login/login" });
}

function backToTop() {
  uni.pageScrollTo({
    scrollTop: 0,
    duration: 260,
  });
}

function onSearchConfirm() {
  // Computed filtering reacts on input. Keep this handler for keyboard confirm / button click.
}

function toggleMpSearch() {
  showMpSearch.value = !showMpSearch.value;
  nextTick(() => {
    syncMpTabsOffsets();
  });
}

function syncMpTabsOffsets() {
  // #ifdef MP-WEIXIN
  const query = uni.createSelectorQuery();
  query.select(".mp-header-spacer").boundingClientRect();
  query.select(".tabs-mp-fixed").boundingClientRect();
  query.exec((rects: any[]) => {
    const headerHeight = Number(rects?.[0]?.height || 0);
    const tabsHeight = Number(rects?.[1]?.height || 0);
    if (headerHeight > 0) {
      mpTabsTopStyle.value = { top: `${Math.ceil(headerHeight)}px` };
    }
    if (tabsHeight > 0) {
      mpTabsSpacerStyle.value = { height: `${Math.ceil(tabsHeight)}px` };
    }
  });
  // #endif
}

onShow(() => {
  const token = getToken();
  isLoggedIn.value = !!token;
  if (!token) {
    orders.value = [];
    showBackTop.value = false;
    return;
  }
  load();
  syncMpTabsOffsets();
});

onReady(() => {
  syncMpTabsOffsets();
});

onPageScroll((e) => {
  showBackTop.value = Number(e.scrollTop || 0) > 360;
});
</script>

<style scoped>
.orders-page {
  min-height: 100vh;
  background: #FFFFFF;
  padding-bottom: 40rpx;
}
.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}
.login-empty {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.login-empty-title {
  font-size: 34rpx;
  color: #0F172A;
  font-weight: 700;
  margin-bottom: 24rpx;
}
.login-empty-btn {
  height: 72rpx;
  min-width: 180rpx;
  border-radius: 10rpx;
  background: #0F4C81;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24rpx;
}
.login-empty-btn-text {
  color: #FFFFFF;
  font-size: 28rpx;
  font-weight: 600;
}

.tabs {
  display: flex;
  background: #FFFFFF;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  border-bottom: 1rpx solid #E2E8F0;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.tabs-wrap {
  background: #FFFFFF;
}
.tabs-items-wrap {
  flex: 1;
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.tab-item {
  position: relative;
  padding: 24rpx 32rpx;
  color: #64748B;
  font-size: 28rpx;
  cursor: pointer;
  flex-shrink: 0;
}
.tab-count {
  margin-left: 8rpx;
  font-size: 20rpx;
  color: #94A3B8;
}
.tab-item.active .tab-count {
  color: #1D4ED8;
}
.tab-item.active {
  color: #004178;
  font-weight: 600;
}
.tab-indicator {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 40rpx;
  height: 6rpx;
  background: #004178;
  border-radius: 3rpx;
}
.tabs-search-box {
  margin-left: 16rpx;
  height: 72rpx;
  min-width: 360rpx;
  max-width: 420rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.tabs-search-row {
  padding: 0 24rpx 16rpx;
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.sub-tabs {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  gap: 12rpx;
  padding: 8rpx 24rpx 16rpx;
  border-bottom: 1rpx solid #E2E8F0;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.tabs::-webkit-scrollbar,
.tabs-items-wrap::-webkit-scrollbar,
.sub-tabs::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}
.sub-tab-item {
  flex-shrink: 0;
  padding: 10rpx 7rpx;
}
.sub-tab-item.active {
  background: transparent;
}
.sub-tab-text {
  font-size: 22rpx;
  color: #475569;
}
.sub-tab-count {
  margin-left: 2rpx;
  font-size: 20rpx;
  color: #94A3B8;
}
.sub-tab-item.active .sub-tab-text {
  color: #1D4ED8;
  font-weight: 600;
}
.sub-tab-item.active .sub-tab-count {
  color: #1D4ED8;
}
.tabs-search-input {
  flex: 1;
  height: 64rpx;
  border: 1rpx solid #E2E8F0;
  border-radius: 8rpx;
  background: #F8FAFC;
  font-size: 24rpx;
  color: #334155;
  padding: 0 18rpx;
}
.tabs-search-btn {
  height: 64rpx;
  min-width: 108rpx;
  border-radius: 8rpx;
  background: #0F4C81;
  color: #FFFFFF;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tabs-search-trigger {
  width: 72rpx;
  height: 72rpx;
  margin-left: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.tabs-search-icon {
  width: 34rpx;
  height: 34rpx;
}

.empty {
  display: flex; flex-direction: column; align-items: center; padding: 160rpx 0;
}
.empty-icon { font-size: 80rpx; margin-bottom: 16rpx; opacity: 0.5; }
.empty-text { font-size: 28rpx; color: #475569; }

.order-card {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
}
.order-card:active { border-color: #004178; }

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.order-id {
  font-size: 28rpx; font-weight: 600; color: #0F172A;
}
.order-top-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.copyright-warning-tag {
  font-size: 20rpx;
  color: #EF4444;
  background: #FEE2E2;
  padding: 4rpx 10rpx;
  border-radius: 6rpx;
  font-weight: 500;
}
.status-tag {
  padding: 4rpx 16rpx;
  border-radius: 8rpx;
}
.st-pending { background: rgba(0, 65, 120, 0.1); }
.st-wait-production { background: rgba(245, 158, 11, 0.15); }
.st-in-production { background: rgba(6, 182, 212, 0.15); }
.st-wait-shipment { background: rgba(59, 130, 246, 0.12); }
.st-shipped { background: rgba(59, 130, 246, 0.15); }
.st-completed { background: rgba(139, 92, 246, 0.15); }
.st-cancelled { background: rgba(100, 116, 139, 0.15); }
.st-after-sale { background: rgba(236, 72, 153, 0.14); }
.st-rejected { background: rgba(239, 68, 68, 0.12); }
.st-pending .status-text { color: #004178; }
.st-wait-production .status-text { color: #D97706; }
.st-in-production .status-text { color: #0891B2; }
.st-wait-shipment .status-text { color: #2563EB; }
.st-shipped .status-text { color: #3B82F6; }
.st-completed .status-text { color: #8B5CF6; }
.st-cancelled .status-text { color: #64748B; }
.st-after-sale .status-text { color: #DB2777; }
.st-rejected .status-text { color: #DC2626; }
.status-text { font-size: 22rpx; font-weight: 600; }

.order-mid {
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  background-color: #F8FAFC;
  gap: 8px;
}

.preview-items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-item-name {
  font-size: 13px;
  color: #334155;
  background-color: #ffffff;
  padding: 4px 8px;
  border-radius: 4px;
  border: 1px solid #E2E8F0;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-item-more {
  font-size: 13px;
  color: #94A3B8;
  padding: 4px;
}

.time-and-count {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-time {
  font-size: 12px;
  color: #94A3B8;
}

.order-count {
  font-size: 12px;
  color: #64748B;
}

.order-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-amount {
  font-size: 34rpx; font-weight: 700; color: #004178;
}
.order-actions {
  display: flex; gap: 12rpx;
}
.act-btn {
  padding: 8rpx 20rpx;
  border-radius: 4rpx;
}
.act-btn:active { opacity: 0.75; }
.act-pay { background: #004178; }
.act-confirm { background: #10B981; }
.act-reorder { background: #0F4C81; }
.act-delete-order { background: rgba(148, 163, 184, 0.18); }
.act-pay .act-text, .act-confirm .act-text, .act-reorder .act-text { color: #fff; font-size: 24rpx; font-weight: 500; }
.act-delete-order .act-text { color: #475569; font-size: 24rpx; font-weight: 500; }

.back-top-btn {
  position: fixed;
  right: 24rpx;
  bottom: 190rpx;
  width: 88rpx;
  height: 104rpx;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.95);
  border: 1rpx solid #D5DEE8;
  box-shadow: 0 8rpx 20rpx rgba(15, 76, 129, 0.16);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2rpx;
  z-index: 1800;
}
.back-top-icon {
  width: 50rpx;
  height: 50rpx;
  transform: rotate(-90deg);
}
.back-top-text {
  font-size: 16rpx;
  color: #475569;
  line-height: 1;
}

/* #ifdef MP-WEIXIN */
.orders-page {
  background: #F1F5F9;
}
.content-wrapper {
  max-width: none;
  padding: 0 8rpx;
  background: #F1F5F9;
}
.tabs-mp-fixed {
  position: fixed;
  left: 0;
  right: 0;
  z-index: 1300;
  margin-bottom: 8rpx;
  padding: 0 8rpx;
}
.tabs-mp-fixed .tabs {
  margin-bottom: 0;
  padding: 0 8rpx;
}
.tabs-mp-fixed .tab-item {
  padding: 24rpx 20rpx;
}
.tabs-mp-fixed .tabs-search-row {
  padding: 8rpx 8rpx 12rpx;
}
.tabs-spacer {
  width: 100%;
  margin-bottom: 8rpx;
}
.order-card {
  border: 0;
  margin-bottom: 8rpx;
}
.order-card:active {
  border-color: transparent;
}
.back-top-btn {
  bottom: calc(180rpx + env(safe-area-inset-bottom));
}
/* #endif */
</style>
