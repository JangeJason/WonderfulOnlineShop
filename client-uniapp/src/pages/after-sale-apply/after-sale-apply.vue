<template>
  <view class="apply-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar">
        <view class="back-btn" @click="onBack">←</view>
        <text class="toolbar-title">申请售后</text>
      </view>

      <view v-if="detail" class="form-card">
        <view class="form-group">
          <text class="group-title">选择售后商品（可多选）</text>
          <view
            v-for="item in detail.items"
            :key="item.id"
            class="item-row"
            :class="{ active: selectedItemIds.includes(item.id) }"
            @click="toggleItem(item.id)"
          >
            <view class="item-check">{{ selectedItemIds.includes(item.id) ? "✓" : "" }}</view>
            <view class="item-main">
              <text class="item-name">{{ item.productName }}</text>
              <text class="item-sub">数量 x{{ item.quantity }}</text>
            </view>
          </view>
        </view>

        <view class="form-group">
          <text class="group-title">售后需求</text>
          <picker mode="selector" :range="reasonOptions.map(o => o.label)" :value="reasonIndex" @change="onReasonChange">
            <view class="picker-box">{{ reasonOptions[reasonIndex]?.label || "请选择售后需求" }}</view>
          </picker>
          <text v-if="selectedReason === 'RETURN_REFUND'" class="refund-tip">
            退货及退款需求请先与客服联系，不会无条件处理
          </text>
        </view>

        <view class="form-group">
          <text class="group-title">详细情况</text>
          <textarea
            class="detail-input"
            v-model="detailText"
            placeholder="请描述问题详情，方便客服与生产同事快速处理"
            maxlength="500"
          />
        </view>

        <view class="form-group">
          <text class="group-title">上传图片（可选）</text>
          <view class="upload-btn" @click="chooseImages">选择图片</view>
          <view class="image-list" v-if="imageList.length > 0">
            <view class="image-item" v-for="(url, idx) in imageList" :key="idx">
              <image class="image-preview" :src="resolveAsset(url)" mode="aspectFill" @click="previewImage(url)" />
              <view class="image-remove" @click="removeImage(idx)">×</view>
            </view>
          </view>
        </view>

        <view class="submit-btn" :class="{ disabled: submitting }" @click="submit">
          <text>{{ submitting ? "提交中..." : "提交售后申请" }}</text>
        </view>
      </view>
    </view>
    <!-- #ifdef H5 -->
    <OfficialFooter />
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import { getOrderDetail } from "../../api/order";
import { createAfterSale } from "../../api/after-sale";
import { getApiBaseUrl, toAbsoluteAssetUrl } from "../../utils/url";
import { getToken } from "../../utils/storage";

const detail = ref<any>(null);
const selectedItemIds = ref<number[]>([]);
const reasonOptions = [
  { value: "QUANTITY_SHORTAGE", label: "数量不足" },
  { value: "WRONG_PRODUCT", label: "产品有误" },
  { value: "PRINT_ISSUE", label: "印刷问题" },
  { value: "PROCESS_ISSUE", label: "工艺问题" },
  { value: "LOGISTICS_ISSUE", label: "物流问题" },
  { value: "RETURN_REFUND", label: "退货退款" },
];
const reasonIndex = ref(0);
const detailText = ref("");
const imageList = ref<string[]>([]);
const submitting = ref(false);
let orderId = 0;

const selectedReason = ref(reasonOptions[0].value);

onLoad(async (query: any) => {
  orderId = Number(query.orderId || 0);
  if (!orderId) {
    uni.showToast({ title: "订单参数无效", icon: "none" });
    return;
  }
  try {
    detail.value = await getOrderDetail(orderId);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载订单失败", icon: "none" });
  }
});

function onBack() {
  uni.navigateBack({ delta: 1 });
}

function onReasonChange(e: any) {
  const idx = Number(e?.detail?.value || 0);
  reasonIndex.value = idx;
  selectedReason.value = reasonOptions[idx]?.value || reasonOptions[0].value;
}

function toggleItem(itemId: number) {
  const current = [...selectedItemIds.value];
  const idx = current.indexOf(itemId);
  if (idx >= 0) current.splice(idx, 1);
  else current.push(itemId);
  selectedItemIds.value = current;
}

async function chooseImages() {
  try {
    const choose = await uni.chooseImage({ count: Math.max(1, 6 - imageList.value.length) });
    const files = choose.tempFilePaths || [];
    if (files.length === 0) return;
    uni.showLoading({ title: "上传中..." });
    for (const filePath of files) {
      const uploaded = await uploadSingleImage(filePath);
      imageList.value.push(uploaded);
    }
    uni.hideLoading();
  } catch (e: any) {
    uni.hideLoading();
    if (e?.errMsg && String(e.errMsg).includes("cancel")) return;
    uni.showToast({ title: e?.message || "图片上传失败", icon: "none" });
  }
}

function removeImage(idx: number) {
  imageList.value.splice(idx, 1);
}

function resolveAsset(url: string) {
  return toAbsoluteAssetUrl(url);
}

function previewImage(url: string) {
  uni.previewImage({ urls: imageList.value.map((u) => resolveAsset(u)), current: resolveAsset(url) });
}

async function uploadSingleImage(filePath: string): Promise<string> {
  const baseUrl = getApiBaseUrl();
  const uploadRes: any = await new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${baseUrl}/api/upload`,
      filePath,
      name: "file",
      header: { Authorization: getToken() },
      success: resolve,
      fail: reject,
    });
  });
  if (uploadRes.statusCode !== 200) throw new Error("上传失败");
  let data = uploadRes.data;
  if (typeof data === "string") data = JSON.parse(data);
  if (!data?.success) throw new Error(data?.message || "上传失败");
  return data.data.url;
}

async function submit() {
  if (!detail.value) return;
  if (selectedItemIds.value.length === 0) {
    uni.showToast({ title: "请至少选择一个商品", icon: "none" });
    return;
  }
  submitting.value = true;
  try {
    await createAfterSale({
      orderId,
      itemIds: selectedItemIds.value,
      requestType: selectedReason.value,
      detailText: detailText.value.trim(),
      imageUrls: imageList.value,
    });
    uni.showToast({ title: "售后申请已提交", icon: "success" });
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/order-detail/order-detail?id=${orderId}` });
    }, 500);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "提交失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.apply-page {
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
  gap: 16rpx;
  height: 80rpx;
}
.back-btn {
  width: 56rpx;
  text-align: center;
  font-size: 36rpx;
  color: #334155;
}
.toolbar-title {
  font-size: 32rpx;
  color: #0f172a;
  font-weight: 700;
}
.form-card {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 20rpx;
}
.form-group {
  margin-bottom: 24rpx;
}
.group-title {
  font-size: 28rpx;
  color: #0f172a;
  font-weight: 600;
  margin-bottom: 12rpx;
  display: block;
}
.item-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  border: 1rpx solid #e2e8f0;
  border-radius: 10rpx;
  padding: 14rpx;
  margin-bottom: 10rpx;
}
.item-row.active {
  border-color: #2563eb;
  background: #eff6ff;
}
.item-check {
  width: 34rpx;
  height: 34rpx;
  border: 1rpx solid #94a3b8;
  border-radius: 8rpx;
  color: #2563eb;
  font-weight: 700;
  text-align: center;
  line-height: 34rpx;
}
.item-main {
  display: flex;
  flex-direction: column;
}
.item-name {
  font-size: 28rpx;
  color: #0f172a;
}
.item-sub {
  font-size: 22rpx;
  color: #64748b;
}
.picker-box {
  border: 1rpx solid #e2e8f0;
  border-radius: 10rpx;
  padding: 16rpx;
  font-size: 26rpx;
  color: #334155;
}
.refund-tip {
  margin-top: 10rpx;
  color: #b45309;
  font-size: 24rpx;
  display: block;
}
.detail-input {
  width: 100%;
  min-height: 180rpx;
  border: 1rpx solid #e2e8f0;
  border-radius: 10rpx;
  padding: 16rpx;
  box-sizing: border-box;
  font-size: 26rpx;
}
.upload-btn {
  width: 180rpx;
  height: 64rpx;
  border-radius: 8rpx;
  background: #0f4c81;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-list {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 12rpx;
}
.image-item {
  position: relative;
  width: 140rpx;
  height: 140rpx;
}
.image-preview {
  width: 140rpx;
  height: 140rpx;
  border-radius: 8rpx;
}
.image-remove {
  position: absolute;
  right: -10rpx;
  top: -10rpx;
  width: 36rpx;
  height: 36rpx;
  border-radius: 18rpx;
  background: rgba(15, 23, 42, 0.75);
  color: #fff;
  font-size: 24rpx;
  text-align: center;
  line-height: 36rpx;
}
.submit-btn {
  height: 78rpx;
  border-radius: 10rpx;
  background: #16a34a;
  color: #fff;
  font-size: 28rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}
.submit-btn.disabled {
  opacity: 0.65;
}
</style>
