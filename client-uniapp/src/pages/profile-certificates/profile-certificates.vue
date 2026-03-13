<template>
  <view class="cert-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="toolbar">
        <view class="add-btn" @click="goAdd">新增证书</view>
      </view>

      <view v-if="loading" class="loading">加载中...</view>
      <view v-else-if="certs.length === 0" class="empty">暂无常用证书</view>
      <view v-else class="cert-list">
        <view class="cert-item" v-for="cert in certs" :key="cert.id">
          <view class="cert-top">
            <text class="cert-type">{{ cert.certificateType }}</text>
            <text class="cert-date">截止 {{ cert.endDate }}</text>
          </view>
          <text class="cert-line">{{ cert.trademarkContent }}</text>
          <text class="cert-line">{{ cert.principal }}</text>
        </view>
      </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
import { listCommonCertificates, type UserCertificate } from "../../api/certificate";

const loading = ref(false);
const certs = ref<UserCertificate[]>([]);

async function loadList() {
  loading.value = true;
  try {
    certs.value = await listCommonCertificates();
  } catch (e: any) {
    uni.showToast({ title: e?.message || "加载失败", icon: "none" });
  } finally {
    loading.value = false;
  }
}

function goAdd() {
  uni.navigateTo({ url: "/pages/certificate-add/certificate-add" });
}

onShow(() => {
  loadList();
});
</script>

<style scoped>
.cert-page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 80rpx;
}
.content-wrapper {
  padding: 20rpx;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16rpx;
}
.add-btn {
  background: #004178;
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  padding: 14rpx 24rpx;
  border-radius: 8rpx;
}
.loading,
.empty {
  text-align: center;
  color: #64748b;
  font-size: 26rpx;
  padding: 40rpx 0;
}
.cert-list {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}
.cert-item {
  background: #fff;
  border-radius: 10rpx;
  padding: 20rpx;
  border: 1rpx solid #e2e8f0;
}
.cert-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10rpx;
}
.cert-type {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
}
.cert-date {
  color: #64748b;
  font-size: 22rpx;
}
.cert-line {
  display: block;
  color: #475569;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
