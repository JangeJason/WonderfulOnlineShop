<template>
  <view class="after-page">
    <OfficialHeader />
    <view class="content-wrapper">
      <view class="empty-card">
        <image class="empty-icon" src="/static/icons/empty-state.svg" mode="aspectFit" />
        <text class="empty-title">暂无售后记录</text>
        <text class="empty-desc">如发起退款、补印或工单，都会在这里展示。</text>
      </view>
    </view>
    <OfficialFooter />
  </view>
</template>

<script setup lang="ts">
import OfficialHeader from "../../components/OfficialHeader/OfficialHeader.vue";
import OfficialFooter from "../../components/OfficialFooter/OfficialFooter.vue";
</script>

<style scoped>
.after-page {
  min-height: 100vh;
  background: #f8fafc;
  padding-bottom: 80rpx;
}
.content-wrapper {
  padding: 20rpx;
}
.empty-card {
  background: #fff;
  border: 1rpx solid #e2e8f0;
  border-radius: 12rpx;
  padding: 48rpx 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.empty-icon {
  width: 160rpx;
  height: 160rpx;
  margin-bottom: 20rpx;
}
.empty-title {
  color: #0f172a;
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 10rpx;
}
.empty-desc {
  color: #64748b;
  font-size: 24rpx;
  text-align: center;
}
</style>
