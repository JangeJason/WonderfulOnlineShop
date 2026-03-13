<template>
  <view class="home-page">
    <OfficialHeader />

    <view class="page-container">
      <view class="content-wrapper">
      <!-- 顶部搜素与导航容器 (保留仅用于分类标签) -->
    <view class="header">


      <!-- #ifdef MP-WEIXIN -->
      <view class="category-tabs-mp" :style="mpTabsTopStyle">
        <view class="tabs-fixed-left">
          <view class="menu-trigger" @click="toggleCategoryPanel">
            <image class="menu-icon" src="/static/icons/menu.svg" mode="aspectFit" />
          </view>
          <view class="all-tab" :class="{ active: selectedCategory === null }" @click="selectCategoryAndClose(null)">全部</view>
        </view>
        <scroll-view class="tabs-scroll" scroll-x :show-scrollbar="false" enhanced>
          <view class="tabs-scroll-inner">
            <view
              v-for="cat in categoryTree"
              :key="cat.id"
              class="tab-item-mp"
              :class="{ active: selectedCategory === cat.id || cat.children.some(sub => sub.id === selectedCategory) }"
              @click="selectCategory(cat.id)"
            >
              <text class="tab-text">{{ cat.name }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
      <view class="category-tabs-spacer" :style="mpTabsSpacerStyle"></view>
      <view class="category-panel-mask" :style="mpPanelMaskStyle" v-if="showCategoryPanel" @click="closeCategoryPanel">
        <view class="category-panel" @click.stop>
          <scroll-view class="panel-left" scroll-y>
            <view
              v-for="parent in categoryTree"
              :key="parent.id"
              class="panel-parent-item"
              :class="{ active: panelActiveParentId === parent.id }"
              @click="onPanelParentClick(parent.id)"
            >
              {{ parent.name }}
            </view>
          </scroll-view>
          <scroll-view class="panel-right" scroll-y>
            <view v-if="panelActiveParent" class="panel-section">
              <view class="panel-section-title">{{ panelActiveParent.name }}</view>
              <view class="panel-child-grid">
                <view class="panel-child-item" :class="{ active: selectedCategory === panelActiveParent.id }" @click="selectCategoryAndClose(panelActiveParent.id)">
                  全部{{ panelActiveParent.name }}
                </view>
                <view
                  v-for="child in panelActiveParent.children"
                  :key="child.id"
                  class="panel-child-item"
                  :class="{ active: selectedCategory === child.id }"
                  @click="selectCategoryAndClose(child.id)"
                >
                  {{ child.name }}
                </view>
              </view>
            </view>
          </scroll-view>
        </view>
      </view>
      <!-- #endif -->

      <!-- #ifndef MP-WEIXIN -->
      <view class="category-tabs">
        <view
          v-for="cat in categoryTree"
          :key="cat.id"
          class="tab-item has-dropdown"
          :class="{ active: selectedCategory === cat.id || activeHover === cat.id || cat.children.some(sub => sub.id === selectedCategory) }"
          @click="selectCategory(cat.id)"
          @mouseenter="activeHover = cat.id"
          @mouseleave="activeHover = null"
        >
          <view class="tab-text-wrap">
            <text class="tab-text">{{ cat.name }}</text>
            <image v-if="cat.children.length > 0" class="arrow-icon" src="/static/icons/chevron-down.svg" mode="aspectFit" />
          </view>
          <view class="dropdown-menu" v-show="activeHover === cat.id && cat.children.length > 0">
            <view class="dropdown-content">
              <view
                v-for="sub in cat.children"
                :key="sub.id"
                class="dropdown-item"
                :class="{ active: selectedCategory === sub.id }"
                @click.stop="selectCategory(sub.id); activeHover = null;"
              >
                {{ sub.name }}
              </view>
            </view>
          </view>
        </view>
      </view>
      <!-- #endif -->
    </view>

    <!-- 促销横幅占位 (Amazon-style hero) -->
    <view class="banner-area">
      <view class="banner">
        <text class="banner-text">🔥 发现当季热门商品</text>
      </view>
    </view>

    <!-- 面包屑导航 -->
    <view class="breadcrumb-container" v-if="currentCategoryPath.length > 0">
      <view class="breadcrumb">
        <text class="breadcrumb-item" @click="selectCategory(null)">全部商品</text>
        <template v-for="(node, index) in currentCategoryPath" :key="node.id">
          <image class="breadcrumb-separator" src="/static/icons/right.svg" mode="aspectFit" />
          <text 
            class="breadcrumb-item" 
            :class="{ active: index === currentCategoryPath.length - 1 }"
            @click="selectCategory(node.id)"
          >
            {{ node.name }}
          </text>
        </template>
      </view>
    </view>

    <!-- 分组展示 (当选中的是大分类且有子分类时) -->
    <view v-if="isParentCategorySelected && groupedProductsList.length > 0" class="grouped-container">
      <view v-for="group in groupedProductsList" :key="group.category.id" class="product-group">
        <view class="group-header">
          <text class="group-title">{{ group.category.name }}</text>
        </view>
        <view class="product-grid">
          <view class="product-card" v-for="item in group.products" :key="item.id" @click="goDetail(item.id)">
            <view class="img-container">
              <image v-if="getProductCover(item)" class="product-img" :src="getImageUrl(getProductCover(item)!)" mode="aspectFill" />
              <view v-else class="product-img-placeholder"><image class="placeholder-icon" src="/static/icons/empty-product.svg" mode="aspectFit" /></view>
            </view>
            <view class="product-info">
              <text class="product-name">{{ item.name }}</text>
              <view class="product-rating">
                <text class="stars">⭐⭐⭐⭐⭐</text>
                <text class="rating-count">99+</text>
              </view>
              <view class="product-bottom">
                <text class="product-price"><text class="price-symbol">¥</text>{{ item.basePrice }}</text>
                <view class="add-btn"><image class="add-icon" src="/static/icons/plus.svg" mode="aspectFit" /></view>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 普通单列表展示 -->
    <view v-else-if="!isParentCategorySelected && products.length > 0" class="product-grid">
      <view
        class="product-card"
        v-for="item in products"
        :key="item.id"
        @click="goDetail(item.id)"
      >
        <view class="img-container">
          <image
            v-if="getProductCover(item)"
            class="product-img"
            :src="getImageUrl(getProductCover(item)!)"
            mode="aspectFill"
          />
          <view v-else class="product-img-placeholder">
            <image class="placeholder-icon" src="/static/icons/empty-product.svg" mode="aspectFit" />
          </view>
        </view>
        <view class="product-info">
          <text class="product-name">{{ item.name }}</text>
          <view class="product-rating">
            <text class="stars">⭐⭐⭐⭐⭐</text>
            <text class="rating-count">99+</text>
          </view>
          <view class="product-bottom">
            <text class="product-price"><text class="price-symbol">¥</text>{{ item.basePrice }}</text>
            <view class="add-btn"><image class="add-icon" src="/static/icons/plus.svg" mode="aspectFit" /></view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <image class="empty-icon" src="/static/icons/empty-state.svg" mode="aspectFit" />
      <text class="empty-text">没找到相关商品</text>
    </view>

    <!-- 加载更多 -->
    <view v-if="!isParentCategorySelected && hasMore" class="load-more" @click="loadMore">
      <text class="load-more-text">加载更多</text>
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
import { ref, computed, onMounted, onUnmounted } from "vue";
import { onShow, onPullDownRefresh, onReachBottom, onReady, onPageScroll } from "@dcloudio/uni-app";
import { listPublicProducts, listPublicCategories, type Product, type Category } from "../../api/product";
import { getToken } from "../../utils/storage";
import { toAbsoluteAssetUrl } from "../../utils/url";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const keyword = ref("");
const selectedCategory = ref<number | null>(null);
const activeHover = ref<number | null>(null);
const showCategoryPanel = ref(false);
const panelActiveParentId = ref<number | null>(null);
const mpTabsTopStyle = ref<Record<string, string>>({});
const mpTabsSpacerStyle = ref<Record<string, string>>({});
const mpPanelMaskStyle = ref<Record<string, string>>({});
const showBackTop = ref(false);
const currentPage = ref(1);
const hasMore = ref(false);

type GroupedProducts = {
  category: Category;
  products: Product[];
};
const groupedProductsList = ref<GroupedProducts[]>([]);

type CategoryNode = Category & { children: Category[] };

const categoryTree = computed<CategoryNode[]>(() => {
  const map = new Map<number, CategoryNode>();
  const tree: CategoryNode[] = [];
  categories.value.forEach(c => {
    map.set(c.id, { ...c, children: [] });
  });
  categories.value.forEach(c => {
    if (c.parentId) {
      if (map.has(c.parentId)) {
        map.get(c.parentId)!.children.push(map.get(c.id)!);
      }
    } else {
      tree.push(map.get(c.id)!);
    }
  });
  return tree;
});

const currentCategoryPath = computed<Category[]>(() => {
  if (!selectedCategory.value) return [];
  
  // Find the selected category object
  const target = categories.value.find(c => c.id === selectedCategory.value);
  if (!target) return [];

  const path: Category[] = [];
  
  // If it's a child category (has a parent)
  if (target.parentId) {
    const parent = categories.value.find(c => c.id === target.parentId);
    if (parent) {
      path.push(parent);
    }
  }
  
  // Add the target itself
  path.push(target);
  
  return path;
});

const isParentCategorySelected = computed(() => {
  if (!selectedCategory.value) return false;
  const target = categoryTree.value.find(c => c.id === selectedCategory.value);
  return !!target && target.children.length > 0;
});

const panelActiveParent = computed(() => {
  if (!categoryTree.value.length) return null;
  return categoryTree.value.find((c) => c.id === panelActiveParentId.value) || categoryTree.value[0];
});

function resolveParentCategoryId(id: number | null): number | null {
  if (!categoryTree.value.length) return null;
  if (id == null) return categoryTree.value[0].id;
  const current = categories.value.find((c) => c.id === id);
  if (!current) return categoryTree.value[0].id;
  return current.parentId || current.id;
}

function getImageUrl(url: string) {
  return toAbsoluteAssetUrl(url);
}

function getProductCover(item: Product): string | undefined {
  if (Array.isArray(item.imageUrls) && item.imageUrls.length > 0) {
    return item.imageUrls[0];
  }
  return item.imageUrl;
}

async function loadCategories() {
  try {
    categories.value = await listPublicCategories();
    if (!panelActiveParentId.value && categoryTree.value.length > 0) {
      panelActiveParentId.value = categoryTree.value[0].id;
    }
  } catch {}
}

async function load(reset = true) {
  if (reset) {
    currentPage.value = 1;
    products.value = [];
    groupedProductsList.value = [];
  }

  // 大分类特殊逻辑：遍历查询它的所有子分类
  if (isParentCategorySelected.value && reset) {
    const targetParent = categoryTree.value.find(c => c.id === selectedCategory.value);
    if (targetParent && targetParent.children.length > 0) {
      try {
        const groups: GroupedProducts[] = [];
        for (const child of targetParent.children) {
          const res = await listPublicProducts(keyword.value || undefined, child.id, 1, 50);
          if (res.records && res.records.length > 0) {
            groups.push({ category: child, products: res.records });
          }
        }
        groupedProductsList.value = groups;
        hasMore.value = false; // 大分类分组展示不再支持列表平铺翻页
      } catch (e: any) {
        uni.showToast({ title: e?.message || "加载失败", icon: "none" });
      } finally {
        uni.stopPullDownRefresh();
      }
      return;
    }
  }

  try {
    const result = await listPublicProducts(
      keyword.value || undefined,
      selectedCategory.value || undefined,
      currentPage.value,
      10
    );
    if (reset) {
      products.value = result.records;
    } else {
      products.value = [...products.value, ...result.records];
    }
    hasMore.value = currentPage.value < result.pages;
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  } finally {
    uni.stopPullDownRefresh();
  }
}

function loadMore() {
  if (!hasMore.value) return;
  currentPage.value++;
  load(false);
}

function onSearch() {
  load();
}

function selectCategory(id: number | null) {
  selectedCategory.value = id;
  panelActiveParentId.value = resolveParentCategoryId(id);
  load();
}

function toggleCategoryPanel() {
  showCategoryPanel.value = !showCategoryPanel.value;
  if (showCategoryPanel.value) {
    panelActiveParentId.value = resolveParentCategoryId(selectedCategory.value);
  }
}

function closeCategoryPanel() {
  showCategoryPanel.value = false;
}

function onPanelParentClick(parentId: number) {
  panelActiveParentId.value = parentId;
}

function selectCategoryAndClose(id: number | null) {
  showCategoryPanel.value = false;
  selectCategory(id);
}

function syncMpFixedOffsets() {
  // #ifdef MP-WEIXIN
  const query = uni.createSelectorQuery();
  query.select(".mp-header-spacer").boundingClientRect();
  query.select(".category-tabs-mp").boundingClientRect();
  query.exec((rects: any[]) => {
    const headerHeight = Number(rects?.[0]?.height || 0);
    const tabsHeight = Number(rects?.[1]?.height || 0);
    if (headerHeight > 0) {
      mpTabsTopStyle.value = { top: `${Math.ceil(headerHeight)}px` };
    }
    if (tabsHeight > 0) {
      mpTabsSpacerStyle.value = { height: `${Math.ceil(tabsHeight)}px` };
    }
    if (headerHeight > 0 && tabsHeight > 0) {
      mpPanelMaskStyle.value = { top: `${Math.ceil(headerHeight + tabsHeight)}px` };
    }
  });
  // #endif
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/product-detail/product-detail?id=${id}&source=home` });
}

function goCart() {
  uni.switchTab({ url: "/pages/cart/cart" });
}

function backToTop() {
  uni.pageScrollTo({
    scrollTop: 0,
    duration: 260,
  });
}

// 支持页面底部的自动加载
onReachBottom(() => {
  loadMore();
});

onPageScroll((e) => {
  showBackTop.value = Number(e.scrollTop || 0) > 360;
});

// 支持下拉刷新
onPullDownRefresh(() => {
  loadCategories();
  load(true);
});

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: "/pages/login/login" });
    return;
  }
  loadCategories();
  load();
  syncMpFixedOffsets();
});

onReady(() => {
  syncMpFixedOffsets();
});

onMounted(() => {
  uni.$on("global-search", (kw: string) => {
    keyword.value = kw;
    onSearch();
  });
});

onUnmounted(() => {
  uni.$off("global-search");
});
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #FFFFFF; /* 白色背景 */
  padding-bottom: 20rpx;
}

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
.back-top-btn {
  bottom: calc(180rpx + env(safe-area-inset-bottom));
}
/* #endif */

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px; /* 与header/footer对齐 */
}

/* 顶部容器 */
.header {
  background: #FFFFFF;
  box-shadow: none;
  border-bottom: 1px solid #E2E8F0; /* 简约边框 */
  margin-bottom: 20rpx;
}

/* 分类标签 */
/* #ifdef MP-WEIXIN */
.category-tabs-mp {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 6rpx 0;
  position: fixed;
  left: 0;
  right: 0;
  z-index: 1400;
  background: #fff;
  border-bottom: 1rpx solid #E2E8F0;
}
.category-tabs-spacer {
  width: 100%;
}
.tabs-fixed-left {
  display: flex;
  align-items: center;
  gap: 10rpx;
  padding-left: 8rpx;
}
.menu-trigger {
  width: 58rpx;
  height: 58rpx;
  color: #334155;
  font-size: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.menu-icon {
  width: 30rpx;
  height: 30rpx;
}
.all-tab {
  min-width: 96rpx;
  height: 58rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24rpx;
  font-size: 26rpx;
  color: #475569;
}
.all-tab.active {
  color: #004178;
  font-weight: 600;
}
.tabs-scroll {
  flex: 1;
  white-space: nowrap;
}
.tabs-scroll-inner {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding-right: 12rpx;
}
.tab-item-mp {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 58rpx;
  padding: 0 26rpx;
}
.tab-item-mp.active {
  background: transparent;
}
.tab-item-mp .tab-text {
  font-size: 26rpx;
  color: #475569;
  white-space: nowrap;
}
.tab-item-mp.active .tab-text {
  color: #004178;
  font-weight: 600;
}
.category-panel-mask {
  position: fixed;
  left: 0;
  right: 0;
  top: 250rpx;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1500;
}
.category-panel {
  margin-top: 0;
  height: 62vh;
  background: #fff;
  display: flex;
  border-top-left-radius: 16rpx;
  border-top-right-radius: 16rpx;
  overflow: hidden;
}
.panel-left {
  width: 32%;
  background: #f7f8fa;
}
.panel-parent-item {
  padding: 24rpx 18rpx;
  font-size: 26rpx;
  color: #475569;
  border-left: 6rpx solid transparent;
}
.panel-parent-item.active {
  background: #fff;
  color: #004178;
  border-left-color: #004178;
  font-weight: 600;
}
.panel-right {
  flex: 1;
  background: #fff;
  padding: 22rpx 20rpx 30rpx;
}
.panel-section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 16rpx;
}
.panel-child-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
}
.panel-child-item {
  border: 1rpx solid #e2e8f0;
  background: #f8fafc;
  color: #475569;
  border-radius: 10rpx;
  text-align: center;
  font-size: 24rpx;
  padding: 16rpx 8rpx;
}
.panel-child-item.active {
  border-color: #004178;
  color: #004178;
  background: #eef5ff;
  font-weight: 600;
}
/* #endif */

/* #ifndef MP-WEIXIN */
.category-tabs {
  display: flex;
  flex-wrap: wrap;
  padding: 10rpx 32rpx 24rpx;
  gap: 16rpx;
}
.tab-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 12rpx 16rpx;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx; /* 方正边角 */
  transition: all 0.2s;
  cursor: pointer;
}
.tab-text-wrap {
  display: inline-flex;
  align-items: center;
}
.tab-item.active {
  background: #004178; /* 经典蓝色 */
  border-color: #004178;
  box-shadow: none;
}
.tab-text {
  font-size: 26rpx;
  color: #475569;
}
.arrow-icon {
  margin-left: 8rpx;
  width: 18rpx;
  height: 18rpx;
  opacity: 0.7;
}
.tab-item.active .tab-text,
.tab-item.active .arrow-icon {
  color: #FFFFFF;
  font-weight: 500;
}
.tab-item.active .arrow-icon {
  filter: brightness(0) invert(1);
  opacity: 1;
}

/* 下拉菜单 */
.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  padding-top: 8rpx; /* Invisible hover bridge */
  z-index: 999;
  min-width: 120px;
}
.dropdown-content {
  display: flex;
  flex-direction: column;
  padding: 8rpx 0;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
}
.dropdown-item {
  padding: 16rpx 32rpx;
  font-size: 26rpx;
  color: #475569;
  transition: background 0.2s;
  cursor: pointer;
  white-space: nowrap;
}
.dropdown-item:hover {
  background: #f1f5f9;
  color: #004178;
}
.dropdown-item.active {
  background: #e2e8f0;
  color: #004178;
  font-weight: bold;
}
/* #endif */

/* 面包屑导航 */
.breadcrumb-container {
  padding: 10rpx 32rpx 20rpx;
}
.breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-size: 26rpx;
}
.breadcrumb-item {
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}
.breadcrumb-item:hover {
  color: #004178;
}
.breadcrumb-item.active {
  color: #004178;
  font-weight: bold;
  cursor: default;
}
.breadcrumb-separator {
  width: 30rpx;
  height: 30rpx;
  margin: 0 12rpx;
  opacity: 0.52;
}

/* 横幅区域 */
.banner-area {
  padding: 24rpx 32rpx 10rpx;
}
.banner {
  background: #F0F5FA; /* 淡蓝色背景 */
  border: 1px solid #dce0e3;
  border-radius: 4rpx;
  padding: 30rpx 40rpx;
  display: flex;
  align-items: center;
  box-shadow: none;
}
.banner-text {
  color: #004178;
  font-weight: bold;
  font-size: 28rpx;
  letter-spacing: 1rpx;
}

/* 双列商品网格 -> 响应式多列网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
  padding: 20rpx 32rpx;
}
@media screen and (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr); /* 缩小卡片，放4列 */
  }
}
@media screen and (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(5, 1fr); /* 继续缩小卡片，放5列 */
  }
}
@media screen and (min-width: 1280px) {
  .product-grid {
    grid-template-columns: repeat(6, 1fr); /* 大屏放6列 */
  }
}

.product-card {
  width: 100%;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx; /* 方正边角 */
  overflow: hidden;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  transition: border-color 0.2s;
}
.product-card:hover {
  border-color: #004178;
}
.img-container {
  width: 100%;
  aspect-ratio: 1 / 1; /* 约束为正方形图片 */
}
.product-img {
  width: 100%;
  height: 100%;
}
.product-img-placeholder {
  width: 100%;
  height: 100%;
  background: #F8FAFC;
  display: flex;
  align-items: center;
  justify-content: center;
}
.placeholder-icon {
  width: 88rpx;
  height: 88rpx;
  opacity: 0.35;
}
.product-info {
  padding: 20rpx 16rpx;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.product-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #1E293B;
  line-height: 1.4;
  margin-bottom: 8rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 0;
}
.product-rating {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.stars {
  font-size: 22rpx;
  letter-spacing: 2rpx;
}
.rating-count {
  font-size: 22rpx;
  color: #64748B;
  margin-left: 8rpx;
}
.product-bottom {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.product-price {
  font-size: 32rpx;
  font-weight: 800;
  color: #004178; /* 经典蓝色 */
}
.price-symbol {
  font-size: 22rpx;
  margin-right: 4rpx;
}
.add-btn {
  width: 48rpx;
  height: 48rpx;
  background: #004178; /* 经典蓝色 */
  border-radius: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}
.add-icon {
  width: 24rpx;
  height: 24rpx;
  filter: brightness(0) invert(1);
}

/* 空状态与底部 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.empty-icon { width: 96rpx; height: 96rpx; margin-bottom: 20rpx; opacity: 0.65; }
.empty-text {
  color: #94A3B8;
  font-size: 28rpx;
  margin-top: 16rpx;
}
.load-more {
  text-align: center;
  padding: 40rpx 0;
}
.load-more-text {
  color: #3B82F6;
  font-size: 28rpx;
  font-weight: 500;
}

/* 分组样式 */
.grouped-container {
  display: flex;
  flex-direction: column;
  gap: 40rpx;
}
.product-group {
  display: flex;
  flex-direction: column;
}
.group-header {
  padding: 0 20rpx 20rpx;
  margin-top: 20rpx;
  margin-bottom: 20rpx;
  border-bottom: 2rpx solid #E2E8F0;
}
.group-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #0F172A;
  border-left: 8rpx solid #3B82F6;
  padding-left: 16rpx;
}
.no-more-text {
  font-size: 24rpx;
  color: #475569;
}

/* #ifdef MP-WEIXIN */
.content-wrapper {
  background: #F3F4F6;
}
.header {
  margin-bottom: 0;
}
.banner-area {
  padding: 8rpx 8rpx 8rpx;
}
.banner {
  border: none;
}
.product-grid {
  gap: 8rpx;
  padding: 8rpx 8rpx;
}
.product-card {
  border: none;
  box-shadow: 0 2rpx 10rpx rgba(15, 23, 42, 0.05);
  transition: transform 0.2s;
}
.product-card:hover {
  border-color: transparent;
  transform: translateY(-2rpx);
}
/* #endif */
</style>

