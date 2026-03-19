<template>
  <view class="config-code-page">
    <view class="custom-nav" :style="customNavStyle">
      <view class="back-btn" @click="onBack">
        <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
      </view>
      <text class="title">配置单详情</text>
      <view class="nav-right"></view>
    </view>

    <view class="content" v-if="loading">
      <text class="loading-text">解析配置码中...</text>
    </view>

    <view class="content" v-else-if="errorMessage">
      <view class="error-card">
        <text class="error-title">配置码不可用</text>
        <text class="error-msg">{{ errorMessage }}</text>
      </view>
    </view>

    <view class="content" v-else-if="resolved">
      <view class="summary-card">
        <view class="summary-row">
          <text class="summary-label">配置码</text>
          <text class="summary-value">{{ resolved.code }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">分享人</text>
          <text class="summary-value">{{ resolved.creatorName }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">产品</text>
          <text class="summary-value">{{ resolved.productName }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">有效期</text>
          <text class="summary-value">{{ resolved.expireAt ? formatDateTime(resolved.expireAt) : "永久" }}</text>
        </view>
      </view>

      <view class="params-card">
        <view class="params-head">
          <view class="params-bar"></view>
          <text class="params-title">参数配置明细</text>
        </view>
        <view v-if="previewRows.length === 0" class="empty-params">暂无参数明细</view>
        <view v-else class="param-row" v-for="(row, idx) in previewRows" :key="`row-${idx}`">
          <text class="param-name" :class="{ invalid: row.invalidParam }">{{ row.name }}</text>
          <view class="param-value-wrap">
            <text class="param-value" :class="{ invalid: row.invalidValue }">{{ row.value }}</text>
            <text v-if="row.invalidParam || row.invalidValue" class="invalid-tag">需重选</text>
          </view>
        </view>
      </view>
    </view>

    <view class="bottom-bar" v-if="resolved && !loading && !errorMessage">
      <view class="copy-btn" @click="copyCode">复制配置码</view>
      <view class="apply-btn" @click="applyConfigCode">应用配置</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { resolveProductConfigCode, type ProductConfigCodeResolveResponse } from "../../api/config-code";
import { getParameterOptions, getProductParameters, type ParameterOption, type ProductParameter } from "../../api/product";

const code = ref("");
const resolved = ref<ProductConfigCodeResolveResponse | null>(null);
const loading = ref(false);
const errorMessage = ref("");
const customNavStyle = ref<Record<string, string>>({});

type PreviewRow = {
  name: string;
  value: string;
  invalidParam: boolean;
  invalidValue: boolean;
};
type SnapshotParam = { parameterId?: number; name?: string; code?: string; value?: any };

const previewRows = computed<PreviewRow[]>(() => {
  if (!resolved.value?.paramsSnapshot) return [];
  try {
    const obj = JSON.parse(resolved.value.paramsSnapshot);
    const params: SnapshotParam[] = Array.isArray(obj?.params) ? obj.params : [];
    return params.map((p) => {
      const parameterId = Number(p?.parameterId || 0);
      const meta = parameterId > 0 ? parameterMap.value[parameterId] : undefined;
      const hasParamMeta = !!meta;
      const displayName = hasParamMeta
        ? meta!.paramName
        : String(p?.name || p?.code || "参数");
      const valueInfo = formatParamValueByMeta(p?.value, meta, parameterId);
      return {
        name: hasParamMeta ? displayName : `${displayName}（参数已失效）`,
        value: valueInfo.text,
        invalidParam: !hasParamMeta,
        invalidValue: valueInfo.invalid,
      };
    });
  } catch {
    return [];
  }
});

const parameterMap = ref<Record<number, ProductParameter>>({});
const optionMapByParam = ref<Record<number, Record<number, ParameterOption>>>({});
const optionNameSetByParam = ref<Record<number, Set<string>>>({});

function formatParamValueByMeta(raw: any, meta: ProductParameter | undefined, parameterId: number): { text: string; invalid: boolean } {
  if (raw === null || raw === undefined || raw === "") return { text: "未选择", invalid: false };
  if (!meta || meta.paramType !== "SELECT") {
    if (Array.isArray(raw)) {
      return { text: raw.length ? raw.map((v) => String(v)).join("、") : "未选择", invalid: false };
    }
    return { text: String(raw), invalid: false };
  }

  const optionMap = optionMapByParam.value[parameterId] || {};
  const optionNameSet = optionNameSetByParam.value[parameterId] || new Set<string>();
  let invalid = false;
  const values = Array.isArray(raw) ? raw : [raw];
  const texts = values.map((v) => {
    if (v === null || v === undefined || v === "") return "未选择";
    const n = Number(v);
    if (Number.isFinite(n) && String(v).trim() !== "") {
      const opt = optionMap[n];
      if (opt) return opt.optionName;
      invalid = true;
      return `选项#${n}（已失效）`;
    }
    const s = String(v);
    if (optionNameSet.has(s)) return s;
    invalid = true;
    return `${s}（已失效）`;
  });
  const filtered = texts.filter((t) => t !== "未选择");
  if (filtered.length === 0) {
    return { text: "未选择", invalid: false };
  }
  return { text: filtered.join("、"), invalid };
}

function formatDateTime(raw: string): string {
  return String(raw || "").replace("T", " ").slice(0, 16);
}

async function loadByCode() {
  if (!/^\d{8}$/.test(code.value)) {
    errorMessage.value = "配置码格式无效，请输入8位数字";
    return;
  }
  loading.value = true;
  errorMessage.value = "";
  try {
    resolved.value = await resolveProductConfigCode(code.value);
    await loadProductParamMeta(resolved.value.productId);
  } catch (e: any) {
    errorMessage.value = e?.message || "配置码不存在或已过期";
  } finally {
    loading.value = false;
  }
}

async function loadProductParamMeta(productId: number) {
  parameterMap.value = {};
  optionMapByParam.value = {};
  optionNameSetByParam.value = {};
  if (!productId) return;
  try {
    const list = await getProductParameters(productId);
    const map: Record<number, ProductParameter> = {};
    for (const p of list) {
      map[p.id] = p;
    }
    parameterMap.value = map;
    for (const p of list) {
      if (p.paramType !== "SELECT") continue;
      try {
        const options = await getParameterOptions(p.id);
        const byId: Record<number, ParameterOption> = {};
        const nameSet = new Set<string>();
        for (const opt of options) {
          byId[opt.id] = opt;
          nameSet.add(opt.optionName);
        }
        optionMapByParam.value[p.id] = byId;
        optionNameSetByParam.value[p.id] = nameSet;
      } catch {
        optionMapByParam.value[p.id] = {};
        optionNameSetByParam.value[p.id] = new Set<string>();
      }
    }
  } catch {
    // Keep preview available even when parameter metadata loading fails.
  }
}

function onBack() {
  uni.navigateBack({
    fail: () => uni.switchTab({ url: "/pages/index/index" }),
  });
}

function copyCode() {
  if (!resolved.value?.code) return;
  uni.setClipboardData({
    data: resolved.value.code,
    success: () => uni.showToast({ title: "已复制", icon: "success" }),
  });
}

function applyConfigCode() {
  if (!resolved.value) return;
  uni.redirectTo({
    url: `/pages/product-detail/product-detail?id=${resolved.value.productId}&source=home&configCode=${resolved.value.code}&autoApply=1`,
  });
}

// #ifdef MP-WEIXIN
try {
  const info = uni.getSystemInfoSync();
  const statusBarHeight = Number(info?.statusBarHeight || 0);
  customNavStyle.value = { paddingTop: `${Math.ceil(statusBarHeight + 6)}px` };
} catch {
  customNavStyle.value = { paddingTop: "24px" };
}
// #endif

onLoad((query: any) => {
  code.value = String(query?.code || "").trim();
  loadByCode();
});
</script>

<style scoped>
.config-code-page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: calc(130rpx + env(safe-area-inset-bottom));
}
.custom-nav {
  height: 88rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #e2e8f0;
}
.back-btn,
.nav-right {
  width: 64rpx;
  height: 64rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.back-arrow {
  width: 42rpx;
  height: 42rpx;
  transform: rotate(180deg);
}
.title {
  flex: 1;
  text-align: center;
  font-size: 32rpx;
  font-weight: 600;
  color: #0f172a;
}
.content {
  padding: 16rpx;
}
.loading-text {
  display: block;
  text-align: center;
  color: #64748b;
  font-size: 26rpx;
  padding: 40rpx 0;
}
.error-card,
.summary-card,
.params-card {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 20rpx;
}
.params-card {
  margin-top: 12rpx;
}
.error-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #b91c1c;
}
.error-msg {
  margin-top: 10rpx;
  display: block;
  font-size: 24rpx;
  color: #64748b;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 10rpx;
}
.summary-row:last-child {
  margin-bottom: 0;
}
.summary-label {
  color: #64748b;
  font-size: 24rpx;
}
.summary-value {
  color: #0f172a;
  font-size: 24rpx;
  font-weight: 600;
  text-align: right;
}
.params-head {
  display: flex;
  align-items: center;
  gap: 10rpx;
  margin-bottom: 12rpx;
}
.params-bar {
  width: 6rpx;
  height: 26rpx;
  border-radius: 3rpx;
  background: linear-gradient(180deg, #3b82f6, #8b5cf6);
}
.params-title {
  font-size: 28rpx;
  color: #1e293b;
  font-weight: 600;
}
.empty-params {
  color: #94a3b8;
  font-size: 24rpx;
}
.param-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20rpx;
  border-top: 1rpx solid #f1f5f9;
  padding-top: 10rpx;
  margin-top: 10rpx;
}
.param-name {
  color: #475569;
  font-size: 24rpx;
}
.param-name.invalid {
  color: #b91c1c;
}
.param-value-wrap {
  display: flex;
  align-items: center;
  gap: 10rpx;
  justify-content: flex-end;
  max-width: 70%;
}
.param-value {
  color: #0f172a;
  font-size: 24rpx;
  text-align: right;
  max-width: 100%;
}
.param-value.invalid {
  color: #b91c1c;
}
.invalid-tag {
  flex-shrink: 0;
  font-size: 18rpx;
  color: #b91c1c;
  background: #fee2e2;
  padding: 2rpx 8rpx;
  border-radius: 999rpx;
}
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 60;
  display: flex;
  gap: 12rpx;
  background: #fff;
  border-top: 1rpx solid #e2e8f0;
  padding: 16rpx 20rpx calc(16rpx + env(safe-area-inset-bottom));
}
.copy-btn,
.apply-btn {
  flex: 1;
  height: 84rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
}
.copy-btn {
  border: 1rpx solid #0f4c81;
  color: #0f4c81;
  background: #fff;
}
.apply-btn {
  background: #0f4c81;
  color: #fff;
}
</style>
