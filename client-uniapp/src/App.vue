<script setup lang="ts">
import { onLaunch, onShow, onHide } from "@dcloudio/uni-app";
onLaunch(() => {
  console.log("App Launch");
});
onShow(() => {
  console.log("App Show");
});
onHide(() => {
  console.log("App Hide");
});
</script>
<style>
/* #ifdef MP-WEIXIN */
.content-wrapper {
  padding-left: 0 !important;
  padding-right: 0 !important;
}
/* #endif */

/* 全局响应式大屏适配 */
@media screen and (min-width: 768px) {
  body {
    background-color: #F8FAFC !important;
  }
  uni-app, #app {
    max-width: 100% !important;
    margin: 0 !important;
    background-color: transparent !important;
    box-shadow: none !important;
    position: relative;
    overflow-x: hidden;
  }
  /* 隐藏桌面端原生底部导航栏及占位符 */
  uni-tabbar {
    display: none !important;
  }
  .uni-placeholder {
    display: none !important;
  }

  /* 页面内核心内容容器，让页面自己控制内部的居中与宽度，这里做统一的基础100%宽 */
  .page-container {
    max-width: 100%;
    margin: 0;
    padding: 0;
    background-color: #FFFFFF;
    box-shadow: none;
    min-height: 60vh;
  }
}
</style>
