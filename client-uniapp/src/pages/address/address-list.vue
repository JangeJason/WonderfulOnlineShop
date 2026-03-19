<template>
  <view class="address-container">
    <OfficialHeader />
      <view class="content-wrapper">
      <view class="page-header" :style="mpPageHeaderStyle">
        <view class="header-left" @click="goBackBySource">
          <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
        </view>
        <text class="page-title">收货地址管理</text>
        <button class="add-btn" @click="goToAdd">新增地址</button>
      </view>
      <view class="page-header-spacer" :style="mpPageHeaderSpacerStyle"></view>

      <view v-if="loading" class="loading-state">
        <uni-load-more status="loading" />
      </view>

      <view v-else-if="addresses.length === 0" class="empty-state">
        <text>您还没有添加过收货地址</text>
        <button class="add-btn-large" @click="goToAdd">立即添加</button>
      </view>

      <view v-else class="address-list">
        <view class="address-card" v-for="item in addresses" :key="item.id" @click="selectAddress(item)">
          <view class="card-top">
            <text class="name">{{ item.receiverName }}</text>
            <text class="phone">{{ item.phone }}</text>
            <text v-if="item.isDefault" class="default-tag">默认</text>
          </view>
          <view class="card-middle">
            <text class="full-address">{{ item.province }}{{ item.city }}{{ item.district }} {{ item.detailAddress }}</text>
          </view>
          <view class="card-bottom">
            <view class="default-action" @click.stop="setAsDefault(item)">
              <radio :checked="item.isDefault" color="#004178" style="transform:scale(0.8)" />
              <text :class="item.isDefault ? 'is-default-text' : 'set-default-text'">
                {{ item.isDefault ? '默认地址' : '设为默认' }}
              </text>
            </view>
            <view class="actions">
              <text class="action-btn edit-btn" @click.stop="goToEdit(item)">编辑</text>
              <text class="action-btn delete-btn" @click.stop="confirmDelete(item)">删除</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref } from 'vue';
import { onLoad, onUnload } from '@dcloudio/uni-app';
import type { Address } from '@/api/address';
import { getAddresses, deleteAddress, setDefaultAddress } from '@/api/address';
import OfficialHeader from '@/components/OfficialHeader/OfficialHeader.vue';
import OfficialFooter from '@/components/OfficialFooter/OfficialFooter.vue';

const addresses = ref<Address[]>([]);
const loading = ref(true);
const ADDRESS_UPDATED_EVENT = 'addressUpdated';
const source = ref<'cart' | 'profile' | ''>('');
const mpPageHeaderStyle = ref<Record<string, string>>({});
const mpPageHeaderSpacerStyle = ref<Record<string, string>>({});
const instance = getCurrentInstance();

// If opened from checkout, we return the selected address
const isSelectionMode = ref(false);

const loadAddresses = async () => {
  loading.value = true;
  try {
    addresses.value = await getAddresses();
  } catch (error) {
    uni.showToast({ title: '加载地址失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

onLoad((options: any) => {
  if (options && options.mode === 'select') {
    isSelectionMode.value = true;
  }
  if (options?.from === 'cart' || options?.from === 'profile') {
    source.value = options.from;
  } else if (isSelectionMode.value) {
    source.value = 'cart';
  }
  loadAddresses();
  // #ifndef H5
  nextTick(() => {
    syncMpPageHeaderLayout();
  });
  // #endif
});

function syncMpPageHeaderLayout() {
  // #ifdef H5
  return;
  // #endif
  const query = uni.createSelectorQuery().in(instance?.proxy as any);
  query.select(".mp-header-spacer").boundingClientRect();
  query.select(".page-header").boundingClientRect();
  query.exec((res: any[]) => {
    const headerSpacerHeight = Number(res?.[0]?.height || 0);
    const pageHeaderHeight = Number(res?.[1]?.height || 0);
    if (headerSpacerHeight > 0) {
      mpPageHeaderStyle.value = { top: `${Math.ceil(headerSpacerHeight)}px` };
    }
    if (pageHeaderHeight > 0) {
      mpPageHeaderSpacerStyle.value = { height: `${Math.ceil(pageHeaderHeight + 8)}px` };
    }
  });
}

function goBackBySource() {
  if (source.value === 'cart' || isSelectionMode.value) {
    uni.switchTab({ url: '/pages/cart/cart' });
    return;
  }
  if (source.value === 'profile') {
    uni.switchTab({ url: '/pages/profile/profile' });
    return;
  }
  uni.navigateBack({
    fail: () => {
      uni.switchTab({ url: '/pages/profile/profile' });
    }
  });
}

const goToAdd = () => {
  uni.navigateTo({
    url: '/pages/address/address-edit'
  });
};

const goToEdit = (item: Address) => {
  uni.navigateTo({
    url: `/pages/address/address-edit?id=${item.id}&data=${encodeURIComponent(JSON.stringify(item))}`
  });
};

const selectAddress = (item: Address) => {
  if (isSelectionMode.value) {
    uni.$emit('selectAddress', item);
    uni.navigateBack();
  }
};

const setAsDefault = async (item: Address) => {
  if (item.isDefault) return;
  
  try {
    uni.showLoading({ title: '正在设置...' });
    await setDefaultAddress(item.id!);
    uni.showToast({ title: '设置成功', icon: 'success' });
    loadAddresses();
  } catch (err) {
    uni.showToast({ title: '设置失败', icon: 'none' });
  } finally {
    uni.hideLoading();
  }
};

const confirmDelete = (item: Address) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个收货地址吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          uni.showLoading({ title: '正在删除...' });
          await deleteAddress(item.id!);
          uni.showToast({ title: '删除成功', icon: 'success' });
          loadAddresses();
        } catch (error) {
          uni.showToast({ title: '删除失败', icon: 'none' });
        } finally {
          uni.hideLoading();
        }
      }
    }
  });
};

function handleAddressUpdated() {
  loadAddresses();
}

// Listen for updates from edit page
// Need to re-load addresses when returning from edit page
uni.$on(ADDRESS_UPDATED_EVENT, handleAddressUpdated);

onUnload(() => {
  uni.$off(ADDRESS_UPDATED_EVENT, handleAddressUpdated);
});
</script>

<style scoped>
.address-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.content-wrapper {
  flex: 1;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #f5f7fa;
}

.header-left {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.back-arrow {
  width: 50rpx;
  height: 50rpx;
  transform: rotate(180deg);
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.add-btn {
  background-color: #004178;
  color: white;
  border-radius: 4px;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  margin: 0;
  border: none;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  color: #909399;
}

.add-btn-large {
  margin-top: 20px;
  background-color: #004178;
  color: white;
  border-radius: 4px;
  padding: 0 40px;
  height: 44px;
  line-height: 44px;
  font-size: 16px;
}

.address-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.address-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.05);
  transition: all 0.3s;
  cursor: pointer;
  border: 1px solid transparent;
}

.address-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0,0,0,0.08);
  border-color: #004178;
}

.card-top {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-right: 15px;
}

.phone {
  font-size: 16px;
  color: #666;
  flex: 1;
}

.default-tag {
  background-color: #e6f7ff;
  color: #004178;
  border: 1px solid #91d5ff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.card-middle {
  margin-bottom: 15px;
  min-height: 44px;
}

.full-address {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.default-action {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.is-default-text {
  color: #004178;
  font-size: 14px;
}

.set-default-text {
  color: #909399;
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 15px;
}

.action-btn {
  font-size: 14px;
  cursor: pointer;
}

.edit-btn {
  color: #409eff;
}

.delete-btn {
  color: #f56c6c;
}

/* #ifndef H5 */
.content-wrapper {
  padding: 0 16rpx;
}
.page-header {
  position: fixed;
  left: 0;
  right: 0;
  z-index: 40;
  margin-bottom: 0;
  background: #ffffff;
  padding: 8rpx 16rpx;
  border-bottom: 1rpx solid #e2e8f0;
}
.page-title {
  flex: 1;
  text-align: center;
  font-size: 34rpx;
  color: #0f172a;
}
.add-btn {
  height: 64rpx;
  line-height: 64rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
}
.page-header-spacer {
  width: 100%;
}
/* #endif */

@media (max-width: 768px) {
  .address-list {
    grid-template-columns: 1fr;
  }
}
</style>
