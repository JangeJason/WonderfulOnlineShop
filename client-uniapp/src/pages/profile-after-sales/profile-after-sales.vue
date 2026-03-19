<template>
  <view class="after-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar" :style="mpToolbarStyle">
        <view class="toolbar-left" @click="goBack">
          <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <view class="toolbar-title">售后记录</view>
        <view class="toolbar-right">
          <view class="manage-btn" @click="toggleManage">{{ managing ? "完成" : "管理" }}</view>
        </view>
      </view>
      <view class="toolbar-spacer" :style="mpToolbarSpacerStyle"></view>

      <view v-if="records.length === 0" class="empty-card">
        <image class="empty-icon" src="/static/icons/empty-state.svg" mode="aspectFit" />
        <text class="empty-title">暂无售后记录</text>
        <text class="empty-desc">如发起退款、补印或工单，都会在这里展示。</text>
      </view>
      <view v-else class="record-list">
        <view class="record-card" v-for="row in records" :key="row.id" @click="onRecordTap(row)">
          <view class="record-top">
            <text class="record-id">售后单 #{{ row.id }}</text>
            <view class="record-top-right">
              <text class="record-status">{{ statusLabel(row.status) }}</text>
              <view
                v-if="managing && row.status === 'RESOLVED'"
                class="record-delete-btn"
                @click.stop="onDeleteResolved(row)"
              >
                删除
              </view>
            </view>
          </view>
          <text class="record-line">订单 #{{ row.orderId }}</text>
          <text class="record-line">需求：{{ typeLabel(row.requestType) }}</text>
          <text class="record-line">商品：{{ (row.selectedItems || []).map(i => i.productName).join("、") }}</text>
          <text class="record-time">{{ formatTime(row.createdAt) }}</text>
          <text v-if="row.adminRemark" class="record-remark">处理备注：{{ row.adminRemark }}</text>
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
import { deleteAfterSale, listAfterSales, type AfterSaleRecord } from "../../api/after-sale";

const records = ref<AfterSaleRecord[]>([]);
const managing = ref(false);
const mpToolbarStyle = ref<Record<string, string>>({});
const mpToolbarSpacerStyle = ref<Record<string, string>>({});
const instance = getCurrentInstance();

async function loadRecords() {
  try {
    records.value = await listAfterSales();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  }
}

function typeLabel(v: string) {
  const map: Record<string, string> = {
    QUANTITY_SHORTAGE: "数量不足",
    WRONG_PRODUCT: "产品有误",
    PRINT_ISSUE: "印刷问题",
    PROCESS_ISSUE: "工艺问题",
    LOGISTICS_ISSUE: "物流问题",
    RETURN_REFUND: "退货退款",
  };
  return map[v] || v;
}

function statusLabel(v: string) {
  const map: Record<string, string> = {
    PENDING: "待处理",
    PROCESSING: "处理中",
    RESOLVED: "已解决",
    REJECTED: "已驳回",
    CLOSED: "已关闭",
  };
  return map[v] || v;
}

function formatTime(v: string) {
  if (!v) return "";
  return v.replace("T", " ").substring(0, 16);
}

function goOrder(orderId: number) {
  uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${orderId}` });
}

function onRecordTap(row: AfterSaleRecord) {
  if (managing.value) return;
  goOrder(row.orderId);
}

function toggleManage() {
  managing.value = !managing.value;
}

async function onDeleteResolved(row: AfterSaleRecord) {
  if (row.status !== "RESOLVED") {
    uni.showToast({ title: "仅已解决记录可删除", icon: "none" });
    return;
  }
  const confirmed = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: "删除售后记录",
      content: "是否确认删除记录？删除后不可找回。",
      confirmText: "确认删除",
      cancelText: "取消",
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
  if (!confirmed) return;
  try {
    await deleteAfterSale(row.id);
    records.value = records.value.filter((r) => r.id !== row.id);
    uni.showToast({ title: "已删除", icon: "success" });
  } catch (e: any) {
    uni.showToast({ title: e?.message || "删除失败", icon: "none" });
  }
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
  loadRecords();
});

onShow(() => {
  loadRecords();
  // #ifndef H5
  nextTick(() => {
    syncToolbarLayout();
  });
  // #endif
});
</script>

<style scoped>
.after-page {
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
.manage-btn {
  min-width: 120rpx;
  height: 60rpx;
  border-radius: 10rpx;
  border: 1rpx solid #0f4c81;
  color: #0f4c81;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-card {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 48rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.empty-icon {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 20rpx;
}
.empty-title {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}
.empty-desc {
  color: #64748b;
  font-size: 24rpx;
  text-align: center;
}
.record-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
.record-card {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 18rpx;
}
.record-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.record-top-right {
  display: flex;
  align-items: center;
  gap: 10rpx;
}
.record-id {
  color: #0f172a;
  font-size: 28rpx;
  font-weight: 700;
}
.record-status {
  color: #2563eb;
  font-size: 22rpx;
}
.record-delete-btn {
  min-width: 92rpx;
  height: 46rpx;
  padding: 0 12rpx;
  border: 1rpx solid #ef4444;
  color: #ef4444;
  border-radius: 8rpx;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.record-line {
  display: block;
  color: #475569;
  font-size: 24rpx;
  margin-top: 6rpx;
}
.record-time {
  display: block;
  color: #94a3b8;
  font-size: 22rpx;
  margin-top: 8rpx;
}
.record-remark {
  display: block;
  color: #b45309;
  font-size: 23rpx;
  margin-top: 8rpx;
}
/* #ifndef H5 */
.content-wrapper {
  padding: 0 16rpx calc(110rpx + env(safe-area-inset-bottom));
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
