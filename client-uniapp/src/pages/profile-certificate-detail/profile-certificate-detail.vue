<template>
  <view class="cert-detail-page">
    <view class="custom-nav" :style="customNavStyle">
      <view class="back-btn" @click="onBack">
        <image class="back-arrow" src="/static/icons/right.svg" mode="aspectFit" />
      </view>
      <text class="title">证书明细</text>
      <view class="nav-right"></view>
    </view>

    <view class="form-card" v-if="cert">
      <view class="form-item">
        <text class="label">证书类型</text>
        <text class="value">{{ cert.certificateType || "-" }}</text>
      </view>
      <view class="form-item">
        <text class="label">状态</text>
        <text class="status-tag" :class="isExpired(cert.endDate) ? 'expired' : 'valid'">
          {{ isExpired(cert.endDate) ? "已过期" : "有效" }}
        </text>
      </view>
      <view class="form-item">
        <text class="label">商标类型</text>
        <text class="value">{{ cert.trademarkType || "未知" }}</text>
      </view>
      <view class="form-item">
        <text class="label">商标/授权内容</text>
        <text class="value">{{ cert.trademarkContent || "-" }}</text>
      </view>
      <view class="form-item">
        <text class="label">委托方</text>
        <text class="value">{{ cert.principal || "-" }}</text>
      </view>
      <view class="form-item">
        <text class="label">结束日期</text>
        <text class="value">{{ cert.endDate || "-" }}</text>
      </view>
      <view class="form-item">
        <text class="label">证书文件</text>
        <view class="file-list" v-if="fileRows.length > 0">
          <view class="file-row" v-for="(f, idx) in fileRows" :key="idx" @click="openFile(f.url)">
            <text class="file-name">{{ f.name }}</text>
            <text class="preview-btn">查看</text>
          </view>
        </view>
        <text class="value" v-else>-</text>
      </view>

      <view class="extend-panel" v-if="isExpired(cert.endDate)">
        <view v-if="!extendMode" class="extend-btn" @click="openExtendMode">延长证书期限</view>
        <view v-else>
          <view class="form-item">
            <text class="label">新结束日期 *</text>
            <picker mode="date" :value="extendEndDate" @change="onExtendDateChange">
              <view class="picker-box">{{ extendEndDate || "请选择新的结束日期" }}</view>
            </picker>
          </view>
          <view class="form-item">
            <text class="label">上传新的证书文件 *</text>
            <view class="upload-btn" @click="chooseNewCertificateFile">
              {{ newFileUrl ? "重新选择文件" : "选择文件" }}
            </view>
            <text class="upload-hint">支持 jpg / png / gif / pdf</text>
            <view class="new-file-row" v-if="newFileUrl">
              <text class="new-file-name">{{ newFileName || extractName(newFileUrl) }}</text>
              <text class="new-file-remove" @click="removeNewFile">删除</text>
            </view>
          </view>
          <view class="extend-actions">
            <view class="extend-cancel-btn" @click="cancelExtendMode">取消</view>
            <view class="extend-save-btn" :class="{ disabled: saving }" @click="saveExtension">
              {{ saving ? "保存中..." : "保存" }}
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { toAbsoluteAssetUrl } from "../../utils/url";
import { getApiBaseUrl } from "../../utils/url";
import { getToken } from "../../utils/storage";
import { updateCommonCertificate, type UserCertificate } from "../../api/certificate";

const cert = ref<UserCertificate | null>(null);
const customNavStyle = ref<Record<string, string>>({});
const extendMode = ref(false);
const extendEndDate = ref("");
const newFileUrl = ref("");
const newFileName = ref("");
const saving = ref(false);
const CERT_ALLOWED_EXT = ["jpg", "png", "gif", "pdf"];

// #ifdef MP-WEIXIN
try {
  const info = uni.getSystemInfoSync();
  const statusBarHeight = Number(info?.statusBarHeight || 0);
  customNavStyle.value = { paddingTop: `${Math.ceil(statusBarHeight + 6)}px` };
} catch {
  customNavStyle.value = { paddingTop: "24px" };
}
// #endif

const fileRows = computed(() => {
  const urls = cert.value?.fileUrls || [];
  const names = cert.value?.fileNames || [];
  return urls.map((url, idx) => ({
    url,
    name: names[idx] || extractName(url),
  }));
});

function onBack() {
  uni.navigateBack();
}

function extractName(url: string): string {
  if (!url) return "";
  const clean = url.split("?")[0];
  return decodeURIComponent(clean.split("/").pop() || clean);
}

function isExpired(endDate: string): boolean {
  if (!endDate) return false;
  const target = new Date(`${endDate}T23:59:59`).getTime();
  if (!Number.isFinite(target)) return false;
  return Date.now() > target;
}

function getFileExtension(filename: string): string {
  if (!filename || !filename.includes(".")) return "";
  return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
}

function getFilenameFromPath(path: string): string {
  return path.split("/").pop() || path.split("\\").pop() || "";
}

function openExtendMode() {
  if (!cert.value || !isExpired(cert.value.endDate)) return;
  extendMode.value = true;
  extendEndDate.value = cert.value.endDate || "";
  newFileUrl.value = "";
  newFileName.value = "";
}

function cancelExtendMode() {
  extendMode.value = false;
  extendEndDate.value = "";
  newFileUrl.value = "";
  newFileName.value = "";
}

function onExtendDateChange(e: any) {
  extendEndDate.value = e?.detail?.value || "";
}

async function pickCertificateFile(): Promise<{ path: string; name: string } | null> {
  // #ifdef MP-WEIXIN
  const mpRes: any = await new Promise((resolve, reject) => {
    uni.chooseMessageFile({
      count: 1,
      type: "file",
      extension: CERT_ALLOWED_EXT,
      success: resolve,
      fail: reject,
    });
  });
  const mpFile = mpRes?.tempFiles?.[0];
  return mpFile ? { path: mpFile.path || mpFile.tempFilePath, name: mpFile.name || "" } : null;
  // #endif

  // #ifdef H5
  const h5Res: any = await new Promise((resolve, reject) => {
    uni.chooseFile({
      count: 1,
      extension: CERT_ALLOWED_EXT,
      success: resolve,
      fail: reject,
    });
  });
  const h5File = h5Res?.tempFiles?.[0];
  return h5File ? { path: h5File.path || h5File.tempFilePath, name: h5File.name || "" } : null;
  // #endif

  // #ifndef MP-WEIXIN
  return null;
  // #endif
}

async function chooseNewCertificateFile() {
  try {
    const selected = await pickCertificateFile();
    if (!selected || !selected.path) return;
    const ext = getFileExtension(selected.name || selected.path);
    if (!CERT_ALLOWED_EXT.includes(ext)) {
      uni.showToast({ title: "仅支持 jpg/png/gif/pdf", icon: "none" });
      return;
    }
    uni.showLoading({ title: "上传中..." });
    const uploadRes: any = await new Promise((resolve, reject) => {
      uni.uploadFile({
        url: `${getApiBaseUrl()}/api/upload`,
        filePath: selected.path,
        name: "file",
        formData: { scene: "certificate" },
        header: { Authorization: getToken() },
        success: resolve,
        fail: reject,
      });
    });
    uni.hideLoading();
    if (uploadRes.statusCode !== 200) {
      uni.showToast({ title: "上传失败", icon: "none" });
      return;
    }
    let data = uploadRes.data;
    if (typeof data === "string") data = JSON.parse(data);
    if (!data?.success) {
      uni.showToast({ title: data?.message || "上传失败", icon: "none" });
      return;
    }
    newFileUrl.value = data.data.url;
    newFileName.value = selected.name || getFilenameFromPath(selected.path);
    uni.showToast({ title: "上传成功", icon: "success" });
  } catch (e: any) {
    uni.hideLoading();
    uni.showToast({ title: e?.message || "上传失败", icon: "none" });
  }
}

function removeNewFile() {
  newFileUrl.value = "";
  newFileName.value = "";
}

async function saveExtension() {
  if (!cert.value) return;
  if (!extendEndDate.value) {
    uni.showToast({ title: "请选择新的结束日期", icon: "none" });
    return;
  }
  if (!newFileUrl.value) {
    uni.showToast({ title: "请上传新的证书文件", icon: "none" });
    return;
  }
  saving.value = true;
  try {
    const updated = await updateCommonCertificate(cert.value.id, {
      certificateType: cert.value.certificateType,
      trademarkType: cert.value.trademarkType || "未知",
      trademarkContent: cert.value.trademarkContent,
      principal: cert.value.principal,
      endDate: extendEndDate.value,
      fileUrls: [newFileUrl.value],
      fileNames: [newFileName.value || extractName(newFileUrl.value)],
    });
    cert.value = updated;
    uni.setStorageSync("profile_certificate_detail", updated);
    uni.showToast({ title: "证书已更新", icon: "success" });
    cancelExtendMode();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "保存失败", icon: "none" });
  } finally {
    saving.value = false;
  }
}

async function openFile(url: string) {
  if (!url) return;
  const absolute = toAbsoluteAssetUrl(url);
  const lower = absolute.toLowerCase();
  if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
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

onLoad(() => {
  const data = uni.getStorageSync("profile_certificate_detail");
  if (data && typeof data === "object") {
    cert.value = data as UserCertificate;
  } else {
    uni.showToast({ title: "证书数据不存在", icon: "none" });
    setTimeout(() => onBack(), 300);
  }
});
</script>

<style scoped>
.cert-detail-page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 40rpx;
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
}
.form-card {
  margin: 20rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
}
.form-item {
  margin-bottom: 20rpx;
}
.label {
  display: block;
  font-size: 24rpx;
  color: #64748b;
  margin-bottom: 8rpx;
}
.value {
  display: block;
  font-size: 28rpx;
  color: #0f172a;
  line-height: 1.6;
  word-break: break-all;
}
.status-tag {
  display: inline-block;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  font-weight: 600;
}
.status-tag.valid {
  color: #15803d;
  background: #dcfce7;
}
.status-tag.expired {
  color: #b91c1c;
  background: #fee2e2;
}
.file-list {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}
.file-row {
  border: 1rpx solid #e2e8f0;
  border-radius: 8rpx;
  padding: 14rpx 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.file-name {
  color: #1f2937;
  font-size: 24rpx;
  flex: 1;
  padding-right: 16rpx;
  word-break: break-all;
}
.preview-btn {
  color: #0f4c81;
  font-size: 24rpx;
  font-weight: 600;
}
.extend-panel {
  margin-top: 12rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #e2e8f0;
}
.extend-btn {
  height: 72rpx;
  border: 1rpx solid #0f4c81;
  color: #0f4c81;
  border-radius: 10rpx;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.picker-box {
  min-height: 68rpx;
  border: 1rpx solid #dbe3ef;
  border-radius: 10rpx;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  font-size: 26rpx;
  color: #0f172a;
}
.upload-btn {
  height: 68rpx;
  border: 1rpx dashed #0f4c81;
  color: #0f4c81;
  border-radius: 10rpx;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-hint {
  margin-top: 8rpx;
  display: block;
  color: #64748b;
  font-size: 22rpx;
}
.new-file-row {
  margin-top: 10rpx;
  border: 1rpx solid #e2e8f0;
  border-radius: 8rpx;
  padding: 10rpx 12rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.new-file-name {
  flex: 1;
  font-size: 24rpx;
  color: #1f2937;
  word-break: break-all;
}
.new-file-remove {
  color: #ef4444;
  font-size: 24rpx;
}
.extend-actions {
  margin-top: 18rpx;
  display: flex;
  gap: 12rpx;
}
.extend-cancel-btn,
.extend-save-btn {
  flex: 1;
  height: 72rpx;
  border-radius: 10rpx;
  font-size: 26rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.extend-cancel-btn {
  border: 1rpx solid #cbd5e1;
  color: #64748b;
}
.extend-save-btn {
  background: #0f4c81;
  color: #fff;
}
.extend-save-btn.disabled {
  opacity: 0.6;
}
</style>
