<template>
  <view class="fav-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar" :style="mpToolbarStyle">
        <view class="toolbar-left" @click="goBack">
          <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <view class="toolbar-title">产品收藏</view>
        <view class="toolbar-right">
          <view class="manage-btn" @click="toggleManage">{{ managing ? "完成" : "管理" }}</view>
        </view>
      </view>
      <view class="toolbar-spacer" :style="mpToolbarSpacerStyle"></view>

      <view class="empty-card" v-if="favoriteItems.length === 0">
        <image class="empty-icon" src="/static/icons/empty-state.svg" mode="aspectFit" />
        <text class="empty-title">暂无收藏产品</text>
        <text class="empty-desc">在商品详情页点击收藏后，会显示在这里。</text>
      </view>

      <view v-else class="fav-list">
        <view class="fav-item" v-for="item in favoriteItems" :key="item.productId" @click="onItemTap(item)">
          <view
            v-if="managing"
            class="select-dot"
            :class="{ active: selectedProductIds.includes(item.productId) }"
            @click.stop="toggleItemSelect(item.productId)"
          ></view>
          <image class="fav-thumb" :src="resolveAsset(item.productImageUrl)" mode="aspectFill" />
          <view class="fav-body">
            <view class="fav-top">
              <text class="fav-name">{{ item.productName }}</text>
              <view
                class="remove-btn"
                v-if="managing && item.favoritedProduct"
                @click.stop="onRemoveProduct(item)"
              >
                <text>取消收藏</text>
              </view>
            </view>
            <text class="fav-desc" v-if="item.productDescription">{{ item.productDescription }}</text>
            <view
              class="code-entry"
              v-if="item.favoriteConfigCodeCount > 0"
              @click.stop="onCodeEntryTap(item)"
            >
              <text class="code-entry-text">有 {{ item.favoriteConfigCodeCount }} 个配置码</text>
              <image class="code-entry-icon" src="/static/icons/chevron-down.svg" mode="aspectFit" />
            </view>
          </view>
        </view>
      </view>

      <view v-if="managing && favoriteItems.length > 0" class="manage-bottom-bar">
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
import { listFavoriteConfigCodes, listFavoriteProducts, unfavoriteConfigCode, unfavoriteProduct, type FavoriteProductItem } from "../../api/favorite";
import { toAbsoluteAssetUrl } from "../../utils/url";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const managing = ref(false);
const favoriteItems = ref<FavoriteProductItem[]>([]);
const selectedProductIds = ref<number[]>([]);
const mpToolbarStyle = ref<Record<string, string>>({});
const mpToolbarSpacerStyle = ref<Record<string, string>>({});
const instance = getCurrentInstance();

function toggleManage() {
  managing.value = !managing.value;
  if (!managing.value) {
    selectedProductIds.value = [];
  }
}

const allSelected = computed(() => {
  return favoriteItems.value.length > 0 && selectedProductIds.value.length === favoriteItems.value.length;
});

function goBack() {
  uni.navigateBack({
    fail: () => {
      uni.switchTab({ url: "/pages/profile/profile" });
    }
  });
}

function resolveAsset(url?: string) {
  if (!url) return "/static/icons/empty-product.svg";
  return toAbsoluteAssetUrl(url);
}

function goProduct(productId: number) {
  uni.navigateTo({ url: `/pages/product-detail/product-detail?id=${productId}&source=favorites` });
}

function goConfigCodeList(item: FavoriteProductItem) {
  const title = encodeURIComponent(item.productName || "配置码列表");
  uni.navigateTo({
    url: `/pages/profile-favorite-codes/profile-favorite-codes?productId=${item.productId}&productName=${title}`,
  });
}

function onCodeEntryTap(item: FavoriteProductItem) {
  if (managing.value) return;
  goConfigCodeList(item);
}

function onItemTap(item: FavoriteProductItem) {
  if (managing.value) {
    toggleItemSelect(item.productId);
    return;
  }
  goProduct(item.productId);
}

function toggleItemSelect(productId: number) {
  const idx = selectedProductIds.value.indexOf(productId);
  if (idx >= 0) {
    selectedProductIds.value.splice(idx, 1);
  } else {
    selectedProductIds.value.push(productId);
  }
}

function toggleSelectAll() {
  if (allSelected.value) {
    selectedProductIds.value = [];
    return;
  }
  selectedProductIds.value = favoriteItems.value.map((item) => item.productId);
}

async function onRemoveProduct(item: FavoriteProductItem) {
  if (item.favoriteConfigCodeCount > 0) {
    const shouldDeleteAll = await new Promise<boolean>((resolve) => {
      uni.showModal({
        title: "取消收藏",
        content: `此商品收藏有 ${item.favoriteConfigCodeCount} 个配置码，是否删除收藏？删除会同步删除配置码。`,
        confirmText: "不删除了",
        cancelText: "全部删除",
        success: (res) => resolve(!!res.cancel),
        fail: () => resolve(false),
      });
    });
    if (!shouldDeleteAll) return;
  }
  try {
    await unfavoriteProduct(item.productId, item.favoriteConfigCodeCount > 0);
    uni.showToast({ title: "已取消收藏", icon: "none" });
    await loadFavorites();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "操作失败", icon: "none" });
  }
}

async function loadFavorites() {
  try {
    favoriteItems.value = await listFavoriteProducts();
  } catch (e: any) {
    favoriteItems.value = [];
    uni.showToast({ title: e?.message || "加载收藏失败", icon: "none" });
  }
}

async function deleteSelected() {
  if (selectedProductIds.value.length === 0) {
    uni.showToast({ title: "请先选择收藏项", icon: "none" });
    return;
  }
  const selectedItems = favoriteItems.value.filter((item) => selectedProductIds.value.includes(item.productId));
  const totalConfigCodes = selectedItems.reduce((sum, item) => sum + Number(item.favoriteConfigCodeCount || 0), 0);

  let shouldDelete = false;
  if (totalConfigCodes > 0) {
    shouldDelete = await new Promise<boolean>((resolve) => {
      uni.showModal({
        title: "取消收藏",
        content: `将删除 ${selectedItems.length} 个商品、${totalConfigCodes} 个配置码，是否继续？`,
        confirmText: "不删除了",
        cancelText: "全部删除",
        success: (res) => resolve(!!res.cancel),
        fail: () => resolve(false),
      });
    });
  } else {
    shouldDelete = await new Promise<boolean>((resolve) => {
      uni.showModal({
        title: "确认删除",
        content: `将删除 ${selectedItems.length} 个商品，是否继续？`,
        confirmText: "确认",
        cancelText: "取消",
        success: (res) => resolve(!!res.confirm),
        fail: () => resolve(false),
      });
    });
  }
  if (!shouldDelete) return;

  try {
    for (const item of selectedItems) {
      if (item.favoriteConfigCodeCount > 0) {
        const codes = await listFavoriteConfigCodes(item.productId);
        for (const code of codes) {
          await unfavoriteConfigCode(code.code);
        }
      }
      await unfavoriteProduct(item.productId, item.favoriteConfigCodeCount > 0);
    }
    uni.showToast({ title: "删除成功", icon: "success" });
    selectedProductIds.value = [];
    await loadFavorites();
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

onLoad(() => {
  // #ifndef H5
  nextTick(() => {
    syncToolbarLayout();
  });
  // #endif
  loadFavorites();
});

onShow(() => {
  loadFavorites();
  // #ifndef H5
  nextTick(() => {
    syncToolbarLayout();
  });
  // #endif
});
</script>

<style scoped>
.fav-page {
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
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}
.fav-item {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 16rpx;
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
}
.select-dot {
  width: 36rpx;
  height: 36rpx;
  border-radius: 50%;
  border: 2rpx solid #cbd5e1;
  background: #fff;
  flex-shrink: 0;
  margin-top: 56rpx;
}
.select-dot.active {
  border-color: #0f4c81;
  background: #0f4c81;
}
.select-dot.small {
  width: 32rpx;
  height: 32rpx;
  margin-top: 0;
}
.fav-thumb {
  width: 150rpx;
  height: 150rpx;
  border-radius: 10rpx;
  background: #f1f5f9;
  flex-shrink: 0;
}
.fav-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  gap: 8rpx;
}
.fav-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.fav-name {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-desc {
  color: #64748b;
  font-size: 24rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.code-entry {
  margin-top: auto;
  align-self: flex-end;
  height: 42rpx;
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  color: #0f4c81;
  font-size: 24rpx;
  font-weight: 600;
}
.code-entry-text {
  line-height: 1;
}
.code-entry-icon {
  width: 20rpx;
  height: 20rpx;
  transform: rotate(-90deg);
}
.remove-btn {
  min-width: 130rpx;
  height: 52rpx;
  border-radius: 8rpx;
  border: 1rpx solid #f87171;
  color: #ef4444;
  font-size: 22rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
