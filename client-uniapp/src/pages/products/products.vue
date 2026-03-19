<template>
  <view class="products-page">
    <OfficialHeader />
    <view class="page-container">
    <!-- 分类标签 -->
    <scroll-view scroll-x class="category-tabs">
      <view
        class="tab-item"
        :class="{ active: selectedCategory === null }"
        @click="selectCategory(null)"
      >
        <text class="tab-text">全部</text>
      </view>
      <view
        v-for="cat in categories"
        :key="cat.id"
        class="tab-item"
        :class="{ active: selectedCategory === cat.id }"
        @click="selectCategory(cat.id)"
      >
        <text class="tab-text">{{ cat.name }}</text>
      </view>
    </scroll-view>

    <!-- 商品列表 -->
    <view class="product-grid" v-if="products.length > 0">
      <view
        class="product-card"
        v-for="item in products"
        :key="item.id"
        @click="goDetail(item.id)"
      >
        <!-- 图片容器 -->
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
          <text class="product-desc" v-if="item.description">{{ item.description }}</text>
          <view class="product-bottom">
            <text class="product-price"><text class="price-symbol">¥</text>{{ item.basePrice }}</text>
            <view class="add-btn"><image class="add-icon" src="/static/icons/plus.svg" mode="aspectFit" /></view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <image class="empty-icon" src="/static/icons/empty-product.svg" mode="aspectFit" />
      <text class="empty-text">暂无商品</text>
    </view>

    <!-- 加载更多 -->
    <view v-if="hasMore" class="load-more" @click="loadMore">
      <text class="load-more-text">加载更多</text>
    </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { listPublicProducts, listPublicCategories, type Product, type Category } from "../../api/product";
import { toAbsoluteAssetUrl } from "../../utils/url";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const products = ref<Product[]>([]);
const categories = ref<Category[]>([]);
const keyword = ref("");
const selectedCategory = ref<number | null>(null);
const currentPage = ref(1);
const hasMore = ref(false);

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
  } catch {}
}

async function load(reset = true) {
  if (reset) {
    currentPage.value = 1;
    products.value = [];
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
  }
}

function loadMore() {
  currentPage.value++;
  load(false);
}

function onSearch() {
  load();
}

function selectCategory(id: number | null) {
  selectedCategory.value = id;
  load();
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/product-detail/product-detail?id=${id}` });
}

onShow(() => {
  loadCategories();
  load();
});
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding-bottom: 40rpx;
}

/* 分类标签 */
.category-tabs {
  white-space: nowrap;
  padding: 0 32rpx 16rpx;
}
.tab-item {
  display: inline-block;
  padding: 10rpx 28rpx;
  margin-right: 12rpx;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 32rpx;
}
.tab-item.active {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  border-color: #3B82F6;
}
.tab-text {
  font-size: 24rpx;
  color: #475569;
}
.tab-item.active .tab-text {
  color: #fff;
  font-weight: 600;
}

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24rpx;
  padding: 20rpx 32rpx;
}
@media screen and (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media screen and (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.product-card {
  width: 100%;
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  overflow: hidden;
  box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.15);
  display: flex;
  flex-direction: column;
}
.product-card:active {
  opacity: 0.8;
}
.img-container {
  width: 100%;
  aspect-ratio: 1 / 1;
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
  height: 78rpx;
}
.product-desc {
  font-size: 24rpx;
  color: #64748B;
  margin-bottom: 16rpx;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
  color: #F59E0B;
}
.price-symbol {
  font-size: 22rpx;
  margin-right: 4rpx;
}
.add-btn {
  width: 48rpx;
  height: 48rpx;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  border-radius: 12rpx;
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

/* 空状态 */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
}
.empty-icon { width: 82rpx; height: 82rpx; margin-bottom: 16rpx; opacity: 0.6; }
.empty-text { font-size: 28rpx; color: #475569; }

/* 加载更多 */
.load-more {
  display: flex;
  justify-content: center;
  padding: 24rpx 0 40rpx;
}
.load-more-text {
  font-size: 26rpx;
  color: #3B82F6;
  padding: 12rpx 48rpx;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 32rpx;
}
</style>
