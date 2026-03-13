<template>
  <view class="orders-page">
    <OfficialHeader />
    <view class="page-container">
      <view class="content-wrapper">
        
      <!-- Tabs -->
      <view class="tabs-wrap tabs-mp-fixed" :style="mpTabsTopStyle">
        <view class="tabs">
          <view class="tabs-items-wrap">
            <view
              v-for="tab in tabs"
              :key="tab.value"
              class="tab-item"
              :class="{ active: currentTab === tab.value }"
              @click="currentTab = tab.value"
            >
              <text class="tab-text">{{ tab.label }}</text>
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
              <text class="status-text">{{ statusLabel(order.status) }}</text>
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
            <view v-if="order.status === 'SHIPPED'" class="act-btn act-confirm" @click.stop="onConfirm(order.id)">
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
      </view>
    </view>
    <view v-if="showBackTop" class="back-top-btn" @click="backToTop">
      <image class="back-top-icon" src="/static/icons/right.svg" mode="aspectFit" />
      <text class="back-top-text">顶部</text>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from "vue";
import { onShow, onReady, onPageScroll } from "@dcloudio/uni-app";
import { listOrders, payOrder, confirmOrder, deleteCancelledOrder, reorderOrder, type Order } from "../../api/order";
import { getToken } from "../../utils/storage";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const orders = ref<Order[]>([]);
const currentTab = ref('');
const mpTabsTopStyle = ref<Record<string, string>>({});
const mpTabsSpacerStyle = ref<Record<string, string>>({});
const showBackTop = ref(false);
const orderSearchKeyword = ref("");
const showMpSearch = ref(false);

const tabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: 'PENDING' },
  { label: '待发货', value: 'PAID' },
  { label: '待收货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' },
];

const filteredOrders = computed(() => {
  const tabFiltered = currentTab.value
    ? orders.value.filter((o) => o.status === currentTab.value)
    : orders.value;
  const kw = orderSearchKeyword.value.trim().toLowerCase();
  if (!kw) return tabFiltered;
  return tabFiltered.filter((order) => buildOrderSearchText(order).includes(kw));
});

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
    PENDING: 'st-pending', PAID: 'st-paid', SHIPPED: 'st-shipped',
    COMPLETED: 'st-completed', CANCELLED: 'st-cancelled'
  };
  return map[s] || 'st-pending';
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING: '待支付', PAID: '已支付', SHIPPED: '已发货',
    COMPLETED: '已完成', CANCELLED: '已取消'
  };
  return map[s] || s;
}

function formatTime(t: string) {
  if (!t) return '—';
  return t.replace('T', ' ').substring(0, 16);
}

async function load() {
  try { orders.value = await listOrders(); }
  catch (e: any) { uni.showToast({ title: e?.message || '加载失败', icon: 'none' }); }
}

async function onPay(id: number) {
  try { await payOrder(id); uni.showToast({ title: '支付成功', icon: 'success' }); load(); }
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
  if (!getToken()) { uni.reLaunch({ url: '/pages/login/login' }); return; }
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

.tabs {
  display: flex;
  background: #FFFFFF;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
  border-bottom: 1rpx solid #E2E8F0;
  overflow-x: auto;
  white-space: nowrap;
}
.tabs-wrap {
  background: #FFFFFF;
}
.tabs-items-wrap {
  flex: 1;
  display: flex;
  overflow-x: auto;
  white-space: nowrap;
}
.tab-item {
  position: relative;
  padding: 24rpx 32rpx;
  color: #64748B;
  font-size: 28rpx;
  cursor: pointer;
  flex-shrink: 0;
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
.st-paid { background: rgba(16, 185, 129, 0.15); }
.st-shipped { background: rgba(59, 130, 246, 0.15); }
.st-completed { background: rgba(139, 92, 246, 0.15); }
.st-cancelled { background: rgba(100, 116, 139, 0.15); }
.st-pending .status-text { color: #004178; }
.st-paid .status-text { color: #10B981; }
.st-shipped .status-text { color: #3B82F6; }
.st-completed .status-text { color: #8B5CF6; }
.st-cancelled .status-text { color: #64748B; }
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
