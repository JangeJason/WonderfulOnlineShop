<template>
  <view class="quote-page">
    <OfficialHeader />
    <view class="page-container">
    <!-- 表单 -->
    <view class="section">
      <view class="section-head">
        <view class="section-bar"></view>
        <text class="section-title">报价参数</text>
      </view>

      <view class="form-row">
        <text class="form-label">商品</text>
        <picker :range="productNames" :value="productIndex" @change="onPickProduct">
          <view class="picker-box">
            <text class="picker-text">{{ productNames[productIndex] || '请选择' }}</text>
            <image class="picker-arrow" src="/static/icons/chevron-down.svg" mode="aspectFit" />
          </view>
        </picker>
      </view>

      <view class="form-row">
        <text class="form-label">数量</text>
        <view class="input-box">
          <input type="number" v-model="quantity" :adjust-position="true" placeholder="1" placeholder-class="placeholder" />
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">L (mm)</text>
        <view class="input-box">
          <input type="number" v-model="L" :adjust-position="true" placeholder="1000" placeholder-class="placeholder" />
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">W (mm)</text>
        <view class="input-box">
          <input type="number" v-model="W" :adjust-position="true" placeholder="800" placeholder-class="placeholder" />
        </view>
      </view>

      <view class="form-row">
        <text class="form-label">OptionCount</text>
        <view class="input-box">
          <input type="number" v-model="optionCount" :adjust-position="true" placeholder="0" placeholder-class="placeholder" />
        </view>
      </view>

      <view class="submit-btn" :class="{ disabled: loading }" @click="onQuote">
        <text class="btn-text">{{ loading ? '计算中...' : '计算报价' }}</text>
      </view>
    </view>

    <!-- 结果 -->
    <view v-if="result" class="result-section">
      <view class="result-grid">
        <view class="result-item">
          <text class="result-label">单价</text>
          <text class="result-val">¥{{ result.unitPrice }}</text>
        </view>
        <view class="result-item">
          <text class="result-label">总价</text>
          <text class="result-val result-total">¥{{ result.totalPrice }}</text>
        </view>
      </view>
      <view v-if="result.breakdown" class="breakdown">
        <text class="breakdown-title">价格明细</text>
        <text class="breakdown-text">{{ typeof result.breakdown === 'string' ? result.breakdown : JSON.stringify(result.breakdown, null, 2) }}</text>
      </view>
    </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { listPublicProducts, type Product } from "../../api/product";
import { quote, type QuoteRequest, type QuoteResponse } from "../../api/quote";
import { getToken } from "../../utils/storage";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";

const products = ref<Product[]>([]);
const productIndex = ref(0);
const quantity = ref(1);
const L = ref(1000);
const W = ref(800);
const optionCount = ref(0);
const loading = ref(false);
const result = ref<QuoteResponse | null>(null);

const productNames = computed(() => products.value.map((p) => `${p.id} - ${p.name}`));

function onPickProduct(e: any) {
  productIndex.value = Number(e?.detail?.value || 0);
}

function buildOptionIds(n: number) {
  const ids: number[] = [];
  for (let i = 1; i <= n; i++) ids.push(i);
  return ids;
}

async function loadProducts() {
  const result = await listPublicProducts();
  products.value = result.records;
  if (productIndex.value >= products.value.length) productIndex.value = 0;
}

async function onQuote() {
  if (products.value.length === 0) {
    uni.showToast({ title: "请先加载商品", icon: "none" });
    return;
  }
  loading.value = true;
  try {
    const p = products.value[productIndex.value];
    const req: QuoteRequest = {
      productId: p.id,
      quantity: Number(quantity.value),
      items: [
        { parameterId: 1, paramCode: "L", valueNumber: Number(L.value), selectedOptionIds: [] },
        { parameterId: 2, paramCode: "W", valueNumber: Number(W.value), selectedOptionIds: [] },
        { parameterId: 3, paramCode: "OPT", valueNumber: undefined, selectedOptionIds: buildOptionIds(Number(optionCount.value)) },
      ],
    };
    result.value = await quote(req);
  } catch (e: any) {
    uni.showToast({ title: e?.message || "报价失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

onShow(async () => {
  if (!getToken()) { uni.reLaunch({ url: "/pages/login/login" }); return; }
  try { await loadProducts(); } catch (e: any) {
    uni.showToast({ title: e?.message || "加载商品失败", icon: "none" });
  }
});
</script>

<style scoped>
.quote-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding: 24rpx 32rpx;
}

.section {
  background: #FFFFFF;
  border: 1rpx solid #E2E8F0;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
}
.section-head {
  display: flex; align-items: center; gap: 12rpx; margin-bottom: 24rpx;
}
.section-bar {
  width: 6rpx; height: 28rpx;
  background: linear-gradient(180deg, #3B82F6, #8B5CF6);
  border-radius: 3rpx;
}
.section-title { font-size: 28rpx; font-weight: 600; color: #E2E8F0; }

.form-row {
  display: flex; align-items: center; margin-bottom: 20rpx;
}
.form-label {
  width: 180rpx; flex-shrink: 0;
  font-size: 26rpx; color: #475569; font-weight: 500;
}
.input-box {
  flex: 1;
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 0 20rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
}
.input-box input {
  width: 100%;
  height: 80rpx;
  font-size: 28rpx;
  color: #0F172A;
}
.placeholder { color: #475569; }

.picker-box {
  flex: 1;
  background: #F8FAFC;
  border: 2rpx solid #E2E8F0;
  border-radius: 12rpx;
  padding: 0 20rpx;
  height: 80rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.picker-text { font-size: 28rpx; color: #0F172A; }
.picker-arrow { width: 24rpx; height: 24rpx; opacity: 0.7; }

.submit-btn {
  margin-top: 12rpx;
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  height: 88rpx;
  border-radius: 14rpx;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4rpx 16rpx rgba(59, 130, 246, 0.25);
}
.submit-btn:active { opacity: 0.85; }
.submit-btn.disabled { opacity: 0.6; }
.btn-text { color: #fff; font-size: 30rpx; font-weight: 600; letter-spacing: 4rpx; }

/* 结果 */
.result-section {
  background: rgba(245, 158, 11, 0.06);
  border: 1rpx solid rgba(245, 158, 11, 0.2);
  border-radius: 16rpx;
  padding: 24rpx;
}
.result-grid {
  display: flex; justify-content: space-around; margin-bottom: 16rpx;
}
.result-item { text-align: center; }
.result-label { display: block; font-size: 24rpx; color: #64748B; margin-bottom: 4rpx; }
.result-val { display: block; font-size: 36rpx; font-weight: 700; color: #F59E0B; }
.result-total { font-size: 44rpx; }
.breakdown {
  border-top: 1rpx solid rgba(245, 158, 11, 0.15);
  padding-top: 16rpx;
}
.breakdown-title { display: block; font-size: 24rpx; font-weight: 600; color: #64748B; margin-bottom: 8rpx; }
.breakdown-text { display: block; font-size: 24rpx; color: #475569; white-space: pre-wrap; }
</style>
