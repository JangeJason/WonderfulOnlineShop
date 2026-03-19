<template>
  <view class="codes-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar" :style="mpToolbarStyle">
        <view class="toolbar-left" @click="goBack">
          <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <view class="toolbar-title">{{ pageTitle }}</view>
        <view class="toolbar-right">
          <view class="manage-btn" @click="toggleManage">{{ managing ? "完成" : "管理" }}</view>
        </view>
      </view>
      <view class="toolbar-spacer" :style="mpToolbarSpacerStyle"></view>

      <view class="empty-card" v-if="codeItems.length === 0">
        <image class="empty-icon" src="/static/icons/empty-state.svg" mode="aspectFit" />
        <text class="empty-title">暂无收藏配置码</text>
      </view>

      <view v-else class="code-list">
        <view class="code-item" :class="{ managing }" v-for="item in codeItems" :key="item.code" @click="onCodeItemTap(item.code)">
          <view
            v-if="managing"
            class="select-dot"
            :class="{ active: selectedCodes.includes(item.code) }"
            @click.stop="toggleCodeSelect(item.code)"
          ></view>
          <view class="code-row">
            <text class="label">配置码</text>
            <text class="value">{{ item.code }}</text>
          </view>
          <view class="code-row">
            <text class="label">分享人</text>
            <text class="value">{{ item.creatorName }}</text>
          </view>
          <view class="code-row">
            <text class="label">有效期</text>
            <text class="value" :class="{ expired: item.expired }">
              {{ item.expireAt ? formatDateTime(item.expireAt) : "永久" }}
            </text>
          </view>
          <view class="status-row">
            <text class="status-tag" :class="{ expired: item.expired, active: !item.expired }">
              {{ item.expired ? "已过期" : "生效中" }}
            </text>
          </view>
        </view>
      </view>

      <view v-if="managing && codeItems.length > 0" class="manage-bottom-bar">
        <view class="manage-all" @click="toggleSelectAll">
          <view class="select-dot small" :class="{ active: allSelected }"></view>
          <text>全选</text>
        </view>
        <view class="manage-delete-btn" @click="deleteSelected">删除</view>
      </view>
    </view>
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, nextTick, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { listFavoriteConfigCodes, unfavoriteConfigCode, type FavoriteConfigCodeItem } from "../../api/favorite";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const instance = getCurrentInstance();
const productId = ref(0);
const pageTitle = ref("配置码列表");
const codeItems = ref<FavoriteConfigCodeItem[]>([]);
const managing = ref(false);
const selectedCodes = ref<string[]>([]);
const mpToolbarStyle = ref<Record<string, string>>({});
const mpToolbarSpacerStyle = ref<Record<string, string>>({});

const allSelected = computed(() => codeItems.value.length > 0 && selectedCodes.value.length === codeItems.value.length);

function formatDateTime(raw: string) {
  return String(raw || "").replace("T", " ").slice(0, 16);
}

function goBack() {
  uni.navigateBack({
    fail: () => {
      uni.navigateTo({ url: "/pages/profile-favorites/profile-favorites" });
    },
  });
}

function openCode(code: string) {
  uni.navigateTo({ url: `/pages/config-code-preview/config-code-preview?code=${code}` });
}

function toggleManage() {
  managing.value = !managing.value;
  if (!managing.value) {
    selectedCodes.value = [];
  }
}

function onCodeItemTap(code: string) {
  if (managing.value) {
    toggleCodeSelect(code);
    return;
  }
  openCode(code);
}

function toggleCodeSelect(code: string) {
  const idx = selectedCodes.value.indexOf(code);
  if (idx >= 0) {
    selectedCodes.value.splice(idx, 1);
  } else {
    selectedCodes.value.push(code);
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedCodes.value = [];
    return;
  }
  selectedCodes.value = codeItems.value.map((item) => item.code);
}

async function loadList() {
  if (!productId.value) return;
  try {
    codeItems.value = await listFavoriteConfigCodes(productId.value);
  } catch (e: any) {
    codeItems.value = [];
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  }
}

async function deleteSelected() {
  if (selectedCodes.value.length === 0) {
    uni.showToast({ title: "请先选择配置码", icon: "none" });
    return;
  }
  const confirmDelete = await new Promise<boolean>((resolve) => {
    uni.showModal({
      title: "确认删除",
      content: "是否确认删除？",
      confirmText: "确认",
      cancelText: "取消",
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false),
    });
  });
  if (!confirmDelete) return;

  try {
    for (const code of selectedCodes.value) {
      await unfavoriteConfigCode(code);
    }
    uni.showToast({ title: "删除成功", icon: "success" });
    selectedCodes.value = [];
    await loadList();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "删除失败", icon: "none" });
  }
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

onLoad((query: any) => {
  const id = Number(query?.productId || 0);
  productId.value = Number.isFinite(id) ? id : 0;
  const rawTitle = String(query?.productName || "").trim();
  let decodedTitle = rawTitle;
  if (rawTitle) {
    try {
      decodedTitle = decodeURIComponent(rawTitle);
    } catch {
      decodedTitle = rawTitle;
    }
  }
  pageTitle.value = decodedTitle || "配置码列表";
  loadList();
  // #ifndef H5
  nextTick(() => syncToolbarLayout());
  // #endif
});

onShow(() => {
  loadList();
  // #ifndef H5
  nextTick(() => syncToolbarLayout());
  // #endif
});
</script>

<style scoped>
.codes-page {
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
.back-arrow {
  width: 50rpx;
  height: 50rpx;
  transform: rotate(180deg);
}
.toolbar-title {
  flex: 1;
  text-align: center;
  color: #0f172a;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.toolbar-right {
  min-width: 120rpx;
  height: 56rpx;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
}
.manage-btn {
  min-width: 110rpx;
  height: 56rpx;
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
}
.code-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.code-item {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 18rpx;
  position: relative;
}
.select-dot {
  position: absolute;
  left: 16rpx;
  top: 22rpx;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  border: 2rpx solid #cbd5e1;
  background: #fff;
}
.select-dot.active {
  border-color: #0f4c81;
  background: #0f4c81;
}
.select-dot.small {
  position: static;
  width: 32rpx;
  height: 32rpx;
}
.code-item.managing .code-row,
.code-item.managing .status-row {
  padding-left: 52rpx;
}
.code-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}
.code-row + .code-row {
  margin-top: 8rpx;
}
.label {
  color: #64748b;
  font-size: 24rpx;
}
.value {
  color: #0f172a;
  font-size: 26rpx;
  font-weight: 600;
}
.expired {
  color: #ef4444;
}
.status-row {
  margin-top: 12rpx;
  display: flex;
  justify-content: flex-end;
}
.status-tag {
  min-width: 98rpx;
  height: 40rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.status-tag.active {
  color: #10b981;
  background: #ecfdf5;
}
.status-tag.expired {
  color: #ef4444;
  background: #fef2f2;
}
.manage-bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: calc(92rpx + env(safe-area-inset-bottom));
  padding: 0 20rpx env(safe-area-inset-bottom);
  background: #ffffff;
  border-top: 1rpx solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 60;
}
.manage-all {
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  color: #334155;
  font-size: 28rpx;
}
.manage-delete-btn {
  min-width: 180rpx;
  height: 64rpx;
  border-radius: 10rpx;
  background: #ef4444;
  color: #fff;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
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
