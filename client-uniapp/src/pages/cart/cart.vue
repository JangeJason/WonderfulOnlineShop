<template>
  <view class="cart-page">
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
      <!-- 空状态 -->
      <view v-if="items.length === 0" class="empty">
      <image class="empty-icon" src="/static/icons/empty-cart.svg" mode="aspectFit" />
      <text class="empty-text">购物车是空的</text>
      <view class="empty-btn" @click="goProducts">
        <text class="empty-btn-text">去逛逛</text>
      </view>
    </view>

      <view v-if="items.length > 0" class="address-selector" :style="mpAddressSelectorStyle" @click="goToSelectAddress">
        <view class="address-content" v-if="selectedAddress">
          <view class="address-main">
            <text class="address-name">{{ selectedAddress.receiverName }}</text>
            <text class="address-phone">{{ selectedAddress.phone }}</text>
            <text class="address-tag" v-if="selectedAddress.isDefault">默认</text>
          </view>
          <text class="address-detail">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }} {{ selectedAddress.detailAddress }}</text>
        </view>
        <view class="address-empty" v-else>
          <text>请选择收货地址</text>
        </view>
        <image class="address-arrow" src="/static/icons/right.svg" mode="aspectFit" />
      </view>
      <!-- #ifdef MP-WEIXIN -->
      <view v-if="items.length > 0" class="address-selector-spacer" :style="mpAddressSelectorSpacerStyle"></view>
      <!-- #endif -->

    <!-- 购物车列表 -->
    <view v-if="items.length > 0" class="cart-list">
      <view v-for="item in items" :key="item.id" class="cart-card" @click="goEditItem(item)">
        <!-- 单选框 -->
        <view class="check-box" @click.stop="toggleSelect(item)">
          <radio :checked="item.selected" color="#004178" style="transform:scale(0.8); pointer-events:none;" />
        </view>

        <view class="card-left">
          <text class="card-name">{{ item.productName || '商品#' + item.productId }}</text>
          <view class="file-links" v-if="item.printFileUrl || item.proofFileUrl">
            <text class="file-link" v-if="item.printFileUrl" @click.stop="openFile(item.printFileUrl)">印品文件：{{ getDisplayFileName(item, "print") }}</text>
            <text class="file-link" v-if="item.proofFileUrl" @click.stop="openFile(item.proofFileUrl)">工艺对稿图：{{ getDisplayFileName(item, "proof") }}</text>
          </view>
          
          <view class="copyright-warning" v-if="item.hasCopyright && !item.copyrightFileUrl">
            <image class="warning-icon" src="/static/icons/warning.svg" mode="aspectFit" />
            <text>请尽快上传版权授权书，无授权书不会进入生产流程</text>
          </view>
          
          <view class="card-param-chips" v-if="item.paramsSnapshot">
            <text class="param-chip" v-for="(chip, idx) in getSnapshotParamChips(item.paramsSnapshot)" :key="idx">
              {{ chip }}
            </text>
          </view>
          <text class="unit-price-line">产品单价：{{ Number(item.unitPrice).toFixed(2) }} 元</text>
          <text class="setup-fee-note" v-if="getAppliedSetupFee(item) > 0">
            起售价: +{{ getAppliedSetupFee(item).toFixed(2) }}
            <text v-if="getFreeSetupQuantityFromSnapshot(item.paramsSnapshot) > 0">
              （未满 {{ getFreeSetupQuantityFromSnapshot(item.paramsSnapshot) }} 个）
            </text>
          </text>
          <text class="card-price">¥{{ getLineTotal(item).toFixed(2) }}</text>
        </view>
        <view class="card-right">
          <view class="card-del" @click.stop="onRemove(item.id)">
            <image class="close-icon" src="/static/icons/close.svg" mode="aspectFit" />
          </view>
          <view class="qty-control" @click.stop>
            <view class="qty-btn" :class="{ disabled: item.quantity <= 1 }" @click="onChangeQty(item, -1)">
              <image class="qty-icon" src="/static/icons/minus.svg" mode="aspectFit" />
            </view>
            <input
              class="qty-num-input"
              type="number"
              :value="getQtyDisplay(item)"
              @input="(e: any) => onQtyInput(item, e.detail?.value)"
              @blur="() => onQtyBlur(item)"
              @confirm="() => onQtyBlur(item)"
            />
            <view class="qty-btn" @click="onChangeQty(item, 1)">
              <image class="qty-icon" src="/static/icons/plus.svg" mode="aspectFit" />
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部栏 (跟随在列表之后，不固定始终在底部) -->

    <!-- 底部栏 -->
    <view class="bottom-bar" v-if="items.length > 0">
      <view class="bar-left">
        <view class="select-all" @click="toggleSelectAll">
          <radio :checked="isAllSelected" color="#004178" style="transform:scale(0.8); pointer-events:none;" />
          <text class="select-all-text">全选</text>
        </view>
        <view class="delete-selected" @click="onBatchDelete" v-if="hasSelectedItems">
          <text class="delete-text">删除</text>
        </view>
      </view>
      <view class="bar-right">
        <view class="total-wrap">
          <text class="bar-label">合计:</text>
          <text class="bar-total">¥{{ total.toFixed(2) }}</text>
        </view>
        <view class="bar-action" :class="{ disabled: submitting || !hasSelectedItems }" @click="onSubmit">
          <text class="action-text">{{ submitting ? '提交中...' : `结算(${selectedCount})` }}</text>
        </view>
      </view>
    </view>
      </template>
      </view>
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
import { ref, computed, nextTick, getCurrentInstance } from "vue";
import { onLoad, onShow, onUnload } from "@dcloudio/uni-app";
import { listCartItems, updateCartItemQuantity, removeCartItem, batchDeleteItems, type CartItem } from "../../api/cart";
import { createOrder } from "../../api/order";
import { getToken } from "../../utils/storage";
import { toAbsoluteAssetUrl } from "../../utils/url";
import { getParameterOptions } from "../../api/product";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import CustomTabBar from "../../components/CustomTabBar/CustomTabBar.vue";

import { getAddresses, type Address } from "../../api/address";

const items = ref<CartItem[]>([]);
const submitting = ref(false);
const isLoggedIn = ref(false);

const selectedItems = computed(() => items.value.filter(i => i.selected));
const hasSelectedItems = computed(() => selectedItems.value.length > 0);
const selectedCount = computed(() => selectedItems.value.length);
const isAllSelected = computed(() => items.value.length > 0 && selectedCount.value === items.value.length);
const total = computed(() =>
  selectedItems.value.reduce((s, i) => s + Number(i.unitPrice) * i.quantity + getAppliedSetupFee(i), 0)
);

const selectedAddress = ref<Address | null>(null);
const shouldKeepManualSelectedAddressOnce = ref(false);
const SELECT_ADDRESS_EVENT = 'selectAddress';
const optionNameMap = ref<Record<string, string>>({});
const qtyDraftMap = ref<Record<number, string>>({});
const mpAddressSelectorStyle = ref<Record<string, string>>({});
const mpAddressSelectorSpacerStyle = ref<Record<string, string>>({});
const vm = getCurrentInstance();

function getSnapshotParamChips(json: string): string[] {
  try {
    const obj = JSON.parse(json);
    const parts: string[] = [];
    if (obj.params) {
      parts.push(
        ...obj.params.map((p: any) => {
          const mapped = mapParamValue(p);
          return `${p.name}: ${mapped}`;
        })
      );
    }
    return parts;
  } catch { return []; }
}

function mapParamValue(p: any): string {
  const raw = p?.value;
  const paramId = Number(p?.parameterId || 0);
  if (!paramId) {
    if (Array.isArray(raw)) {
      return raw.join("、");
    }
    return String(raw ?? "");
  }
  if (Array.isArray(raw)) {
    const names = raw.map((v: any) => {
      const key = `${paramId}:${v}`;
      return optionNameMap.value[key] ?? String(v ?? "");
    });
    return names.join("、");
  }
  const key = `${paramId}:${raw}`;
  return optionNameMap.value[key] ?? String(raw ?? "");
}

function getSetupFeeFromSnapshot(snapshot?: string | null): number {
  if (!snapshot) return 0;
  try {
    const obj = JSON.parse(snapshot);
    const fee = Number(obj?.setupFee || 0);
    return Number.isFinite(fee) ? fee : 0;
  } catch {
    return 0;
  }
}

function getFreeSetupQuantityFromSnapshot(snapshot?: string | null): number {
  if (!snapshot) return 0;
  try {
    const obj = JSON.parse(snapshot);
    const n = Number(obj?.freeSetupQuantity || 0);
    return Number.isFinite(n) ? n : 0;
  } catch {
    return 0;
  }
}

function getAppliedSetupFee(item: CartItem): number {
  const setupFee = getSetupFeeFromSnapshot(item.paramsSnapshot);
  if (setupFee <= 0) return 0;
  const freeSetupQuantity = getFreeSetupQuantityFromSnapshot(item.paramsSnapshot);
  if (freeSetupQuantity <= 0) return setupFee;
  return item.quantity < freeSetupQuantity ? setupFee : 0;
}

function getLineTotal(item: CartItem): number {
  return Number(item.unitPrice) * item.quantity + getAppliedSetupFee(item);
}

async function openFile(url: string) {
  if (!url) return;
  const absolute = toAbsoluteAssetUrl(url);
  const lower = absolute.toLowerCase();
  if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")) {
    uni.previewImage({ urls: [absolute] });
    return;
  }
  // #ifdef MP-WEIXIN
  try {
    const res: any = await new Promise((resolve, reject) => {
      uni.downloadFile({ url: absolute, success: resolve, fail: reject });
    });
    if (res?.tempFilePath) {
      uni.openDocument({ filePath: res.tempFilePath });
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || "文件打开失败", icon: "none" });
  }
  // #endif
  // #ifdef H5
  window.open(absolute, "_blank");
  // #endif
}

function getFilename(url: string): string {
  if (!url) return "";
  const clean = url.split("?")[0];
  return decodeURIComponent(clean.split("/").pop() || clean);
}

function getSnapshotFileName(snapshot?: string | null, kind: "print" | "proof"): string {
  if (!snapshot) return "";
  try {
    const obj = JSON.parse(snapshot);
    const key = kind === "print" ? "printFileName" : "proofFileName";
    return typeof obj?.[key] === "string" ? obj[key] : "";
  } catch {
    return "";
  }
}

function getDisplayFileName(item: CartItem, kind: "print" | "proof"): string {
  const fromSnapshot = getSnapshotFileName(item.paramsSnapshot, kind);
  if (fromSnapshot) return fromSnapshot;
  const url = kind === "print" ? item.printFileUrl : item.proofFileUrl;
  return url ? getFilename(url) : "";
}

function resolveAsset(url?: string) {
  return toAbsoluteAssetUrl(url || "");
}

async function load() {
  try { 
    const data = await listCartItems(); 
    items.value = data.map(i => ({ ...i, selected: true })); // default auto-select all
    await preloadOptionNames(items.value);

    const addresses = await getAddresses();
    if (addresses.length === 0) {
      selectedAddress.value = null;
    } else if (shouldKeepManualSelectedAddressOnce.value && selectedAddress.value?.id) {
      // Returning from address picker: keep the manually selected address once.
      selectedAddress.value =
        addresses.find(a => a.id === selectedAddress.value?.id) ||
        addresses.find(a => a.isDefault) ||
        addresses[0];
      shouldKeepManualSelectedAddressOnce.value = false;
    } else {
      // Normal cart entry: always sync to latest default address.
      selectedAddress.value = addresses.find(a => a.isDefault) || addresses[0];
    }
    await nextTick();
    syncAddressSelectorLayout();
  }
  catch (e: any) { uni.showToast({ title: e?.message || '加载失败', icon: 'none' }); }
}

function syncAddressSelectorLayout() {
  // #ifdef MP-WEIXIN
  const query = uni.createSelectorQuery().in(vm?.proxy as any);
  query.select(".mp-header-spacer").boundingClientRect();
  query.select(".address-selector").boundingClientRect();
  query.exec((rects: any[]) => {
    const headerHeight = Number(rects?.[0]?.height || 0);
    const selectorHeight = Number(rects?.[1]?.height || 0);
    if (headerHeight > 0) {
      mpAddressSelectorStyle.value = { top: `${Math.ceil(headerHeight)}px` };
    }
    if (selectorHeight > 0) {
      mpAddressSelectorSpacerStyle.value = { height: `${Math.ceil(selectorHeight + 8)}px` };
    }
  });
  // #endif
}

async function preloadOptionNames(cartItems: CartItem[]) {
  const targetParamIds = new Set<number>();
  for (const item of cartItems) {
    if (!item.paramsSnapshot) continue;
    try {
      const obj = JSON.parse(item.paramsSnapshot);
      const params = Array.isArray(obj?.params) ? obj.params : [];
      for (const p of params) {
        const parameterId = Number(p?.parameterId || 0);
        const value = p?.value;
        if (!parameterId) continue;
        if (Array.isArray(value)) {
          const hasNumeric = value.some((v: any) => typeof v === "number" || (typeof v === "string" && /^\d+$/.test(v)));
          if (hasNumeric) {
            targetParamIds.add(parameterId);
          }
          continue;
        }
        if (typeof value === "number" || (typeof value === "string" && /^\d+$/.test(value))) {
          targetParamIds.add(parameterId);
        }
      }
    } catch {
      // Ignore invalid legacy snapshots
    }
  }

  for (const parameterId of targetParamIds) {
    try {
      const options = await getParameterOptions(parameterId);
      for (const opt of options) {
        optionNameMap.value[`${parameterId}:${opt.id}`] = opt.optionName;
      }
    } catch {
      // Ignore option query failures and keep raw values as fallback
    }
  }
}

function toggleSelect(item: CartItem) {
  item.selected = !item.selected;
}

function toggleSelectAll() {
  const targetState = !isAllSelected.value;
  items.value.forEach(i => i.selected = targetState);
}

function goToSelectAddress() {
  uni.navigateTo({ url: '/pages/address/address-list?mode=select&from=cart' });
}

async function onChangeQty(item: CartItem, delta: number) {
  const baseQty = getQtyFromDraftOrItem(item);
  const newQty = baseQty + delta;
  if (newQty < 1) return;
  await persistQuantity(item, newQty);
}

function onQtyInput(item: CartItem, val: string) {
  const normalized = String(val ?? "").replace(/[^\d]/g, "");
  qtyDraftMap.value[item.id] = normalized;
}

async function onQtyBlur(item: CartItem) {
  const draft = qtyDraftMap.value[item.id];
  if (draft == null) return;
  let newQty = Number(draft);
  if (!Number.isFinite(newQty) || newQty < 1) {
    newQty = 1;
  }
  newQty = Math.floor(newQty);
  await persistQuantity(item, newQty);
}

function getQtyFromDraftOrItem(item: CartItem): number {
  const draft = qtyDraftMap.value[item.id];
  if (draft == null || draft === "") {
    return item.quantity;
  }
  const n = Number(draft);
  if (!Number.isFinite(n) || n < 1) {
    return item.quantity;
  }
  return Math.floor(n);
}

function getQtyDisplay(item: CartItem): string {
  const draft = qtyDraftMap.value[item.id];
  return draft == null ? String(item.quantity) : draft;
}

async function persistQuantity(item: CartItem, newQty: number) {
  const normalized = Math.max(1, Math.floor(newQty));
  if (normalized === item.quantity) {
    delete qtyDraftMap.value[item.id];
    return;
  }
  try {
    await updateCartItemQuantity(item.id, normalized);
    item.quantity = normalized;
    delete qtyDraftMap.value[item.id];
  } catch (e: any) { uni.showToast({ title: e?.message || '操作失败', icon: 'none' }); }
}

async function onRemove(id: number) {
  uni.showModal({
    title: '确认删除',
    content: '确定要将该商品移出购物车吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await removeCartItem(id);
          items.value = items.value.filter(i => i.id !== id);
          uni.showToast({ title: '已删除', icon: 'success' });
        } catch (e: any) { uni.showToast({ title: e?.message || '操作失败', icon: 'none' }); }
      }
    }
  });
}

async function onBatchDelete() {
  if (!hasSelectedItems.value) return;
  const ids = selectedItems.value.map(i => i.id);
  
  uni.showModal({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedCount.value} 件商品吗？`,
    success: async (res) => {
      if (res.confirm) {
        try {
          await batchDeleteItems(ids);
          items.value = items.value.filter(i => !ids.includes(i.id));
          uni.showToast({ title: '删除成功', icon: 'success' });
        } catch (e: any) { uni.showToast({ title: e?.message || '删除失败', icon: 'none' }); }
      }
    }
  });
}

async function onSubmit() {
  if (!hasSelectedItems.value) {
    uni.showToast({ title: '请至少选择一件商品', icon: 'none' });
    return;
  }
  if (!selectedAddress.value) {
    uni.showToast({ title: '请选择收货地址', icon: 'none' });
    return;
  }
  
  submitting.value = true;
  try {
    const ids = selectedItems.value.map(i => i.id);
    
    // Note: createOrder API requires cartItemIds so the backend only processes and removes those specific items
    const order = await createOrder('', ids, selectedAddress.value.id);
    
    // Remove only the items we placed an order for locally
    items.value = items.value.filter(i => !ids.includes(i.id));

    uni.showToast({ title: '下单成功', icon: 'success' });
    setTimeout(() => { uni.navigateTo({ url: `/pages/order-detail/order-detail?id=${order.id}` }); }, 800);
  } catch (e: any) {
    uni.showToast({ title: e?.message || '下单失败', icon: 'none' });
  } finally { submitting.value = false; }
}

function goProducts() {
  uni.switchTab({ url: '/pages/index/index' });
}

function goLogin() {
  uni.navigateTo({ url: "/pages/login/login" });
}

function goEditItem(item: CartItem) {
  uni.navigateTo({ url: `/pages/product-detail/product-detail?id=${item.productId}&cartItemId=${item.id}&source=cart` });
}

function handleSelectAddress(address: Address) {
  selectedAddress.value = address;
  shouldKeepManualSelectedAddressOnce.value = true;
}

onLoad(() => {
  uni.$on(SELECT_ADDRESS_EVENT, handleSelectAddress);
});

onShow(() => {
  const token = getToken();
  isLoggedIn.value = !!token;
  if (!token) {
    items.value = [];
    selectedAddress.value = null;
    return;
  }
  load();
});

onUnload(() => {
  uni.$off(SELECT_ADDRESS_EVENT, handleSelectAddress);
});
</script>

<style scoped>
.cart-page {
  min-height: 100vh;
  background: #FFFFFF;
  padding-bottom: 0;
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

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 0;
}
.empty-icon { width: 82rpx; height: 82rpx; margin-bottom: 16rpx; opacity: 0.6; }
.empty-text { font-size: 28rpx; color: #475569; margin-bottom: 32rpx; }
.empty-btn {
  background: #004178;
  padding: 16rpx 48rpx;
  border-radius: 4rpx;
}
.empty-btn-text { color: #fff; font-size: 28rpx; font-weight: 500; }

.address-selector {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.address-content {
  flex: 1;
  margin-right: 20rpx;
}
.address-empty {
  flex: 1;
  color: #64748B;
  font-size: 28rpx;
}
.address-main {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.address-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #0F172A;
  margin-right: 20rpx;
}
.address-phone {
  font-size: 28rpx;
  color: #475569;
  margin-right: 16rpx;
}
.address-tag {
  background-color: #e6f7ff;
  color: #004178;
  border: 1px solid #91d5ff;
  font-size: 20rpx;
  padding: 2rpx 8rpx;
  border-radius: 4rpx;
}
.address-detail {
  font-size: 26rpx;
  color: #64748B;
  display: block;
  line-height: 1.5;
}
.address-arrow {
  width: 52rpx;
  height: 52rpx;
  opacity: 0.55;
}

.cart-card {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  display: flex;
  align-items: stretch;
}
.check-box {
  display: flex;
  align-items: center;
  justify-content: center;
  padding-right: 20rpx;
}
.card-left { flex: 1; margin-right: 20rpx; overflow: hidden; }
.card-name {
  display: block;
  font-size: 28rpx; font-weight: 600; color: #0F172A;
  margin-bottom: 6rpx;
}
.file-links {
  margin-bottom: 10rpx;
}
.file-link {
  display: block;
  font-size: 22rpx;
  color: #0F4C81;
  text-decoration: underline;
  margin-bottom: 6rpx;
}
.copyright-warning {
  display: flex;
  align-items: flex-start;
  gap: 8rpx;
  margin-bottom: 10rpx;
  font-size: 22rpx;
  color: #DC2626;
  line-height: 1.5;
}
.warning-icon {
  width: 24rpx;
  height: 24rpx;
  margin-top: 2rpx;
  flex-shrink: 0;
}
.card-param-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
  margin-bottom: 12rpx;
}
.param-chip {
  display: inline-flex;
  align-items: center;
  background: #F1F5F9;
  border: 1rpx solid #E2E8F0;
  border-radius: 999rpx;
  padding: 6rpx 14rpx;
  font-size: 22rpx;
  color: #475569;
  line-height: 1.4;
}
.unit-price-line {
  display: block;
  font-size: 22rpx;
  color: #334155;
  margin-bottom: 10rpx;
}
.setup-fee-note {
  display: block;
  font-size: 22rpx;
  color: #DC2626;
  margin-bottom: 10rpx;
  line-height: 1.5;
}
.card-price {
  display: block;
  font-size: 32rpx; font-weight: 700; color: #004178;
}
.card-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
}
.card-del {
  padding: 4rpx 8rpx;
}
.close-icon {
  width: 28rpx;
  height: 28rpx;
  opacity: 0.8;
}
.qty-control {
  display: flex; align-items: center; gap: 16rpx;
}
.qty-btn {
  width: 52rpx; height: 52rpx;
  background: #004178; border-radius: 4rpx;
  display: flex; align-items: center; justify-content: center;
}
.qty-btn.disabled { background: #E2E8F0; }
.qty-icon {
  width: 24rpx;
  height: 24rpx;
  filter: brightness(0) invert(1);
}
.qty-num-input {
  width: 90rpx;
  height: 52rpx;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  background: #fff;
  font-size: 28rpx;
  font-weight: 600;
  color: #0F172A;
  text-align: center;
  padding: 0 8rpx;
}

.bottom-bar {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 4rpx;
  display: flex; 
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 32rpx;
  margin-bottom: 0;
}
.bar-left {
  display: flex;
  align-items: center;
  gap: 30rpx;
}

.select-all { display: flex; align-items: center; cursor: pointer; }
.select-all-text { font-size: 28rpx; color: #475569; margin-left: 8rpx; }

.delete-selected { padding: 8rpx 16rpx; border-radius: 4rpx; border: 1rpx solid #E2E8F0; cursor: pointer; }
.delete-text { font-size: 24rpx; color: #64748B; }

.bar-right {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.total-wrap { display: flex; align-items: baseline; }
.bar-label { font-size: 26rpx; color: #64748B; margin-right: 8rpx; }
.bar-total { font-size: 34rpx; font-weight: 700; color: #004178; }

.bar-action {
  background: #004178;
  padding: 16rpx 40rpx; 
  border-radius: 4rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bar-action:active { opacity: 0.85; }
.bar-action.disabled { opacity: 0.5; background: #94ADD7; }
.action-text { color: #fff; font-size: 30rpx; font-weight: 600; letter-spacing: 2rpx; }

@media screen and (min-width: 768px) {
}

/* #ifdef MP-WEIXIN */
.content-wrapper {
  padding: 0 8rpx;
  background: #F3F4F6;
}
.cart-list {
  padding-bottom: calc(240rpx + env(safe-area-inset-bottom));
}
.address-selector {
  position: fixed;
  width: calc(100% - 16rpx);
  z-index: 1100;
  border: none;
  margin-bottom: 0;
  border-bottom: 1rpx solid #E2E8F0;
}
.address-selector-spacer {
  width: 100%;
}
.cart-card {
  border: none;
  margin-bottom: 8rpx;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  z-index: 1190;
  border: none;
  border-radius: 0;
  box-shadow: 0 -4rpx 16rpx rgba(15, 23, 42, 0.08);
  padding-bottom: 24rpx;
}
/* #endif */
</style>
