<template>
  <view class="cert-add-page">
    <view class="custom-nav" :style="customNavStyle">
      <view class="back-btn" @click="onBack"><text>←</text></view>
      <text class="title">新增证书</text>
    </view>

    <view class="form-card">
      <view class="form-item">
        <text class="label">证书类型 *</text>
        <picker mode="selector" :range="certificateTypes" :value="certificateTypeIndex" @change="onCertificateTypeChange">
          <view class="picker-box picker-select">
            <text class="picker-text">{{ form.certificateType || "请选择证书类型" }}</text>
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">商标类型</text>
        <picker mode="selector" :range="trademarkTypes" :value="trademarkTypeIndex" @change="onTrademarkTypeChange">
          <view class="picker-box picker-select">
            <text class="picker-text">{{ form.trademarkType || "未知" }}</text>
            <text class="picker-arrow">▼</text>
          </view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">商标/授权内容 *</text>
        <input class="input-box" v-model="form.trademarkContent" placeholder="请输入商标或授权内容" />
      </view>

      <view class="form-item">
        <text class="label">委托方 *</text>
        <input class="input-box" v-model="form.principal" placeholder="请输入委托方（盖章单位）名称" />
      </view>

      <view class="form-item">
        <text class="label">结束日期 *</text>
        <picker mode="date" :value="form.endDate" @change="onEndDateChange">
          <view class="picker-box">{{ form.endDate || "请选择结束日期" }}</view>
        </picker>
      </view>

      <view class="form-item">
        <text class="label">上传证书 *</text>
        <view class="upload-btn" @click="chooseCertificateFiles">
          <text>选择文件（{{ fileList.length }}/5）</text>
        </view>
        <text class="hint">证书信息仅供审核使用，最多上传5张图片，每张大小不超过10M，图片格式:jpg/png/gif/pdf</text>
        <view class="file-list" v-if="fileList.length > 0">
          <view class="file-row" v-for="(f, idx) in fileList" :key="idx">
            <text class="file-name">{{ f.name }}</text>
            <text class="remove-btn" @click="removeFile(idx)">删除</text>
          </view>
        </view>
      </view>
    </view>

    <view class="submit-btn" :class="{ disabled: submitting }" @click="onSubmit">
      <text>{{ submitting ? "提交中..." : "提交并保存" }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { createCommonCertificate } from "../../api/certificate";
import { getApiBaseUrl } from "../../utils/url";
import { getToken } from "../../utils/storage";

const certificateTypes = ["商标注册证", "营业执照", "合同/授权证明", "印刷委托书"];
const trademarkTypes = ["未知", "普通商标", "知名商标", "著名商标", "驰名商标", "地理标志商标", "无商标"];
const allowedExt = ["jpg", "png", "gif", "pdf"];

const certificateTypeIndex = ref(0);
const trademarkTypeIndex = ref(0);
const submitting = ref(false);
const customNavStyle = ref<Record<string, string>>({});

const form = reactive({
  certificateType: "",
  trademarkType: "未知",
  trademarkContent: "",
  principal: "",
  endDate: "",
});

type LocalFile = { path: string; name: string; size?: number };
const fileList = ref<LocalFile[]>([]);

function onBack() {
  uni.navigateBack();
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

function onCertificateTypeChange(e: any) {
  const idx = Number(e.detail.value || 0);
  certificateTypeIndex.value = idx;
  form.certificateType = certificateTypes[idx] || "";
}

function onTrademarkTypeChange(e: any) {
  const idx = Number(e.detail.value || 0);
  trademarkTypeIndex.value = idx;
  form.trademarkType = trademarkTypes[idx] || "未知";
}

function onEndDateChange(e: any) {
  form.endDate = e.detail.value;
}

function getExt(name: string): string {
  if (!name || !name.includes(".")) return "";
  return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
}

async function chooseCertificateFiles() {
  const remain = 5 - fileList.value.length;
  if (remain <= 0) {
    uni.showToast({ title: "最多上传5个文件", icon: "none" });
    return;
  }
  try {
    // #ifdef MP-WEIXIN
    const res: any = await new Promise((resolve, reject) => {
      uni.chooseMessageFile({
        count: remain,
        type: "file",
        extension: allowedExt,
        success: resolve,
        fail: reject,
      });
    });
    const files = (res?.tempFiles || []) as any[];
    for (const f of files) {
      appendLocalFile({ path: f.path || f.tempFilePath, name: f.name || "", size: Number(f.size || 0) });
    }
    // #endif

    // #ifdef H5
    const res: any = await new Promise((resolve, reject) => {
      uni.chooseFile({
        count: remain,
        extension: allowedExt,
        success: resolve,
        fail: reject,
      });
    });
    const files = (res?.tempFiles || []) as any[];
    for (const f of files) {
      appendLocalFile({ path: f.path || f.tempFilePath, name: f.name || "", size: Number(f.size || 0) });
    }
    // #endif
  } catch {}
}

function appendLocalFile(file: LocalFile) {
  const ext = getExt(file.name || file.path);
  if (!allowedExt.includes(ext)) {
    uni.showToast({ title: "仅支持 jpg/png/gif/pdf", icon: "none" });
    return;
  }
  if ((file.size || 0) > 10 * 1024 * 1024) {
    uni.showToast({ title: "单个文件不能超过10M", icon: "none" });
    return;
  }
  if (fileList.value.length >= 5) return;
  fileList.value.push(file);
}

function removeFile(idx: number) {
  fileList.value.splice(idx, 1);
}

function validateForm(): boolean {
  if (!form.certificateType) {
    uni.showToast({ title: "请选择证书类型", icon: "none" });
    return false;
  }
  if (!form.trademarkContent.trim()) {
    uni.showToast({ title: "请输入商标/授权内容", icon: "none" });
    return false;
  }
  if (!form.principal.trim()) {
    uni.showToast({ title: "请输入委托方", icon: "none" });
    return false;
  }
  if (!form.endDate) {
    uni.showToast({ title: "请选择结束日期", icon: "none" });
    return false;
  }
  if (fileList.value.length === 0) {
    uni.showToast({ title: "请上传证书文件", icon: "none" });
    return false;
  }
  return true;
}

async function uploadSingle(path: string): Promise<string> {
  const baseUrl = getApiBaseUrl();
  const token = getToken();
  const uploadRes: any = await new Promise((resolve, reject) => {
    uni.uploadFile({
      url: baseUrl + "/api/upload",
      filePath: path,
      name: "file",
      formData: { scene: "certificate" },
      header: { Authorization: token },
      success: resolve,
      fail: reject,
    });
  });
  if (uploadRes.statusCode !== 200) {
    throw new Error("上传失败");
  }
  let data = uploadRes.data;
  if (typeof data === "string") data = JSON.parse(data);
  if (!data.success) {
    throw new Error(data.message || "上传失败");
  }
  return data.data.url;
}

async function onSubmit() {
  if (!validateForm()) return;
  submitting.value = true;
  uni.showLoading({ title: "提交中..." });
  try {
    const urls: string[] = [];
    for (const f of fileList.value) {
      const url = await uploadSingle(f.path);
      urls.push(url);
    }
    await createCommonCertificate({
      certificateType: form.certificateType,
      trademarkType: form.trademarkType || "未知",
      trademarkContent: form.trademarkContent.trim(),
      principal: form.principal.trim(),
      endDate: form.endDate,
      fileUrls: urls,
    });
    uni.hideLoading();
    uni.showToast({ title: "证书已保存", icon: "success" });
    setTimeout(() => uni.navigateBack(), 500);
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || "提交失败", icon: "none" });
  } finally {
    submitting.value = false;
  }
}
</script>

<style scoped>
.cert-add-page {
  min-height: 100vh;
  background: #F8FAFC;
  padding-bottom: 40rpx;
}
.custom-nav {
  height: 88rpx;
  display: flex;
  align-items: center;
  padding: 0 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #E2E8F0;
}
.back-btn {
  width: 64rpx;
  font-size: 40rpx;
}
.title {
  font-size: 32rpx;
  font-weight: 600;
}
.form-card {
  margin: 24rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
}
.form-item {
  margin-bottom: 24rpx;
}
.label {
  display: block;
  margin-bottom: 10rpx;
  font-size: 26rpx;
  color: #334155;
}
.input-box, .picker-box {
  height: 84rpx;
  border: 1rpx solid #E2E8F0;
  border-radius: 10rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  background: #fff;
}
.picker-select {
  justify-content: space-between;
}
.picker-text {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.picker-arrow {
  font-size: 20rpx;
  color: #64748B;
  margin-left: 12rpx;
}
.upload-btn {
  height: 84rpx;
  border: 1rpx dashed #94A3B8;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0F4C81;
}
.hint {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #64748B;
}
.file-list {
  margin-top: 12rpx;
}
.file-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
  border-bottom: 1rpx solid #F1F5F9;
}
.file-name {
  font-size: 24rpx;
  color: #334155;
  flex: 1;
  padding-right: 16rpx;
}
.remove-btn {
  color: #DC2626;
  font-size: 24rpx;
}
.submit-btn {
  margin: 0 24rpx;
  height: 88rpx;
  background: #0F4C81;
  color: #fff;
  border-radius: 10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
}
.submit-btn.disabled {
  opacity: 0.6;
}
</style>
