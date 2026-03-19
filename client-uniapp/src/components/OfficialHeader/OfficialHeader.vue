<template>
  <view class="official-header-wrapper" :class="{ 'sticky-header-active': isSticky }">
    <!-- #ifdef H5 -->
    <div class="top-bar-wrapper">
      <div class="top-menu-container">
        <div class="container top-bar-container">
          <div class="links">
            <view class="tab-button">公司官网</view>
            <view class="tab-button active">在线商城</view>
          </div>
          <div class="top-controls">
            <view class="icon-btn" @click="goCart" title="购物车">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>
            </view>
            <view class="icon-btn" @click="goOrders" title="我的订单">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>
            </view>
            <view class="icon-btn" title="中/En">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="2" y1="12" x2="22" y2="12"></line><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"></path></svg>
            </view>
            <view class="avatar-btn" @click="goProfile" title="个人中心">
              <image class="avatar-img" :src="avatarSrc()" mode="aspectFill" v-if="userAvatar"></image>
              <svg v-else xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
            </view>
          </div>
        </div>
      </div>
    </div>

    <div class="header-main-bar" id="header-main-bar">
      <div class="container main-bar-container">
        <div class="logo">
          <view class="logo-link" @click="goHome">
            <img src="/static/logo.png" alt="Logo">
            <div class="logo-text-group">
              <span class="logo-text">环地福在线商城</span>
              <span class="logo-subtext">Shenzhen Huandifu Paper Products Co., Ltd.</span>
            </div>
          </view>
        </div>
        <div class="search-and-menu">
          <div class="search-box hide-in-phone">
            <input type="text" placeholder="搜索商品..." v-model="searchKeyword" @confirm="onSearch" />
            <button type="button" class="search-submit-btn" @click="onSearch">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            </button>
          </div>

          <button class="mobile-search-toggle" @click="toggleSearch" :class="{ active: isSearchOpen }">
            <svg v-if="!isSearchOpen" xmlns="http://www.w3.org/2000/svg" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <image class="h5-icon-close" v-else src="/static/icons/close.svg" mode="aspectFit" />
          </button>

          <button class="mobile-menu-toggle" @click="toggleMenu">
            <image class="h5-icon-menu" src="/static/icons/menu.svg" mode="aspectFit" />
          </button>
        </div>
      </div>

      <div class="mobile-search-overlay" :class="{ active: isSearchOpen }">
        <div class="container search-overlay-container">
          <input type="text" placeholder="搜索商品..." class="mobile-search-input" v-model="searchKeyword" @confirm="onSearch" />
          <button type="button" class="mobile-search-submit-btn" @click="onSearch">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div class="mobile-menu-overlay" :class="{ active: isMenuOpen }" @click="closeMenuIfOverlay">
      <div class="mobile-menu-panel" @click.stop>
        <div class="mobile-menu-header">
          <div class="links">
            <view class="tab-button">官网</view>
            <view class="tab-button active">商城</view>
          </div>
          <div class="mobile-header-controls">
            <button class="mobile-menu-close-btn" @click="toggleMenu">
              <image class="h5-icon-close" src="/static/icons/close.svg" mode="aspectFit" />
            </button>
          </div>
        </div>

        <div class="mobile-menu-content">
          <nav class="mobile-main-navigation">
            <ul>
              <li><view class="mobile-nav-link" @click="goHome">商城首页 <image class="mobile-right-arrow" src="/static/icons/right.svg" mode="aspectFit" /></view></li>
              <li><view class="mobile-nav-link" @click="goCart">购物车 <image class="mobile-right-arrow" src="/static/icons/right.svg" mode="aspectFit" /></view></li>
              <li><view class="mobile-nav-link" @click="goOrders">我的订单 <image class="mobile-right-arrow" src="/static/icons/right.svg" mode="aspectFit" /></view></li>
              <li><view class="mobile-nav-link" @click="goProfile">个人中心 <image class="mobile-right-arrow" src="/static/icons/right.svg" mode="aspectFit" /></view></li>
            </ul>
          </nav>
        </div>
      </div>
    </div>
    <!-- #endif -->

    <!-- #ifndef H5 -->
    <view class="mp-header" :style="mpHeaderStyle">
      <view class="mp-header-top" :style="mpHeaderTopStyle">
        <view class="mp-logo-wrap" @click="goHome">
          <image src="/static/logo.png" mode="heightFix" class="mp-logo" />
          <text class="mp-title">环地福在线商城</text>
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <view class="mp-header-actions">
          <view class="mp-config-trigger" v-if="showMpConfigTrigger" @click="onOpenConfigCode">
            <image class="mp-config-icon" src="/static/icons/share.svg" mode="aspectFit" />
          </view>
          <view class="mp-search-trigger" v-if="showMpSearchTrigger" @click="toggleMpSearch">
            <image class="mp-search-icon" :src="isMpSearchOpen ? '/static/icons/close.svg' : '/static/icons/search.svg'" mode="aspectFit" />
          </view>
        </view>
        <!-- #endif -->
      </view>
      <view class="mp-search-row" v-if="showMpSearchTrigger && isMpSearchOpen">
        <input class="mp-search-input" type="text" placeholder="搜索商品..." v-model="searchKeyword" @confirm="onSearch" />
        <view class="mp-search-btn" @click="onSearch">搜索</view>
      </view>
    </view>
    <view class="mp-header-spacer" :style="mpHeaderSpacerStyle"></view>
    <!-- #endif -->
  </view>
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, ref, onMounted, onUnmounted } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { getMe } from '@/api/auth';
import { getToken } from '@/utils/storage';
import { toAbsoluteAssetUrl } from '@/utils/url';

const isMenuOpen = ref(false);
const isSearchOpen = ref(false);
const searchKeyword = ref('');
const isSticky = ref(false);
const userAvatar = ref('');
const mpHeaderStyle = ref<Record<string, string>>({});
const mpHeaderSpacerStyle = ref<Record<string, string>>({});
const mpHeaderTopStyle = ref<Record<string, string>>({});
const isMpSearchOpen = ref(false);
const showMpSearchTrigger = ref(true);
const showMpConfigTrigger = ref(true);
const instance = getCurrentInstance();
const emit = defineEmits<{
  (e: "open-config-code"): void;
}>();

function avatarSrc() {
  return toAbsoluteAssetUrl(userAvatar.value);
}

function toggleMenu() {
  isMenuOpen.value = !isMenuOpen.value;
  if (isMenuOpen.value) {
    isSearchOpen.value = false;
  }
}

function toggleSearch() {
  isSearchOpen.value = !isSearchOpen.value;
  if (isSearchOpen.value) {
    isMenuOpen.value = false;
  }
}

function closeMenuIfOverlay() {
  isMenuOpen.value = false;
}

function onSearch() {
  uni.$emit('global-search', searchKeyword.value);
  if (isSearchOpen.value) {
    isSearchOpen.value = false;
  }
  // Navigate if we are not on index
  const pages = getCurrentPages();
  const currentRoute = pages[pages.length - 1]?.route;
  if (currentRoute !== 'pages/index/index') {
    uni.switchTab({ url: '/pages/index/index' });
  }
}

function toggleMpSearch() {
  if (!showMpSearchTrigger.value) return;
  isMpSearchOpen.value = !isMpSearchOpen.value;
  // 展开/收起后重算占位高度，避免内容跳盖
  setTimeout(syncMpHeaderSpacer, 0);
}

function onOpenConfigCode() {
  emit("open-config-code");
}

function syncMpSearchVisibility() {
  // #ifdef MP-WEIXIN
  const pages = getCurrentPages();
  const route = pages[pages.length - 1]?.route || "";
  const isHome = route === "pages/index/index";
  showMpSearchTrigger.value = isHome;
  showMpConfigTrigger.value = isHome;
  if (!showMpSearchTrigger.value) {
    isMpSearchOpen.value = false;
  }
  setTimeout(syncMpHeaderSpacer, 0);
  // #endif
}

function syncMpHeaderSpacer() {
  const query = uni.createSelectorQuery().in(instance?.proxy as any);
  query.select(".mp-header").boundingClientRect((rect: any) => {
    const height = Number(rect?.height || 0);
    if (height > 0) {
      mpHeaderSpacerStyle.value = { height: `${Math.ceil(height)}px` };
    }
  }).exec();
}

function goHome() {
  isMenuOpen.value = false;
  uni.switchTab({ url: '/pages/index/index' });
}

function goCart() {
  isMenuOpen.value = false;
  uni.switchTab({ url: '/pages/cart/cart' });
}

function goOrders() {
  isMenuOpen.value = false;
  uni.switchTab({ url: '/pages/orders/orders' });
}

function goProfile() {
  isMenuOpen.value = false;
  uni.switchTab({ url: '/pages/profile/profile' });
}

// 粘性导航栏监听（H5/Web）
function onScroll() {
  // 在 UniApp H5 当 window scroll 超过 headerHeight 时候设置 sticky
  isSticky.value = window.scrollY > 80; 
}

onMounted(async () => {
  // #ifndef H5
  syncMpSearchVisibility();
  try {
    const info = uni.getSystemInfoSync();
    const statusBarHeight = Number(info?.statusBarHeight || 0);
    mpHeaderStyle.value = {
      paddingTop: `${Math.ceil(statusBarHeight + 8)}px`,
    };
  } catch {
    mpHeaderStyle.value = { paddingTop: "28px" };
  }
  // #ifdef MP-WEIXIN
  try {
    const menuRect = wx.getMenuButtonBoundingClientRect();
    mpHeaderTopStyle.value = {
      paddingRight: `${Math.max(12, Math.ceil(menuRect.width) + 14)}px`,
    };
  } catch {
    mpHeaderTopStyle.value = { paddingRight: "96px" };
  }
  // #endif
  await nextTick();
  syncMpHeaderSpacer();
  // #endif

  // #ifdef H5
  window.addEventListener('scroll', onScroll);
  // #endif
  
  try {
    const token = getToken();
    if (token) {
      const dbUser = await getMe();
      if (dbUser) {
        userAvatar.value = dbUser.avatarUrl || '';
      }
    }
  } catch (e) {
    console.error('Failed to load user avatar in header', e);
  }
});

onUnmounted(() => {
  // #ifdef H5
  window.removeEventListener('scroll', onScroll);
  // #endif
});

onShow(() => {
  syncMpSearchVisibility();
});
</script>

<style scoped>
/* #ifdef H5 */
.official-header-wrapper {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  width: 100%;
  padding-top: var(--status-bar-height);
  background-color: #e8eaeb;
}

.top-bar-wrapper {
  background-color: #e8eaeb;
  position: relative;
  z-index: 2;
}
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
}
.top-bar-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 45px;
  align-items: flex-end;
}
.links { display: flex; align-items: flex-end; }
.links .tab-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 18px;
  color: #555;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  background-color: #e8eaeb;
  border-radius: 4px 4px 0 0;
  cursor: pointer;
  z-index: 1;
}
.links .tab-button.active {
  background-color: #fff;
  color: #24333e;
  z-index: 3;
  padding-bottom: 13px;
  margin-bottom: -5px;
}
.links .tab-button:not(.active):hover { color: #24333e; text-decoration: underline; }

.top-controls {
  display: flex;
  align-items: center;
  gap: 15px;
  height: 100%;
  padding-bottom: 5px; /* align visually */
}
.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #555;
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
}
.icon-btn:hover {
  color: #004178;
}
.avatar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  color: #555;
  transition: color 0.2s;
  background-color: #f0f2f5;
}
.avatar-btn:hover {
  color: #004178;
}
.avatar-img {
  width: 100%;
  height: 100%;
}

.header-main-bar {
  background-color: #fff;
  padding: 25px 0;
  position: relative;
  z-index: 11;
}
.main-bar-container { display: flex; justify-content: space-between; align-items: center; }

.logo img { max-height: 70px; width: auto; display: block; }
.logo-link { display: flex; align-items: center; gap: 15px; cursor: pointer; text-decoration: none; }
.logo-text-group { display: flex; flex-direction: column; justify-content: center; }
.logo-text { font-size: 26px; margin-bottom: 4px; font-weight: bold; color: #004178; line-height: 1.2; }
.logo-subtext { font-size: 12px; font-weight: 500; color: #555; line-height: 1; letter-spacing: 0.5px; }

.search-and-menu { display: flex; align-items: center; }
.search-box {
  display: flex; align-items: center; border: 1px solid #ccc; border-radius: 4px; overflow: hidden; width: 400px;
}
.search-box input {
  flex-grow: 1; border: none; padding: 8px 15px; font-size: 14px; outline: none; color: #333; height: 100%;
}
.search-box button {
  background-color: #f0f2f5; border: none; border-radius: 0; padding: 8px 12px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: #555;
}
.search-box button:hover { background-color: #e0e2e5; }
.search-box button svg { width: 20px; height: 20px; }
.search-box button::after { display: none !important; }

.mobile-search-toggle { display: none; background-color: transparent; border: 1px solid #ccc; border-radius: 4px; padding: 4px 10px; cursor: pointer; margin-left: 15px; align-items: center; justify-content: center; position: relative; }
.mobile-menu-toggle { display: none; background-color: transparent; border: 1px solid #ccc; border-radius: 4px; padding: 4px 10px; cursor: pointer; margin-left: 10px; align-items: center; justify-content: center; }
.h5-icon-close {
  width: 20px;
  height: 20px;
}
.h5-icon-menu {
  width: 20px;
  height: 20px;
}

.mobile-search-overlay {
  position: absolute; top: 100%; left: 0; width: 100%; height: 0; background-color: #fff; overflow: hidden; transition: height 0.3s ease-out; z-index: 2; padding-top: 0; box-sizing: border-box;
}
.mobile-search-overlay.active { height: 60px; border-bottom: 1px solid #e1e6eb; }
.search-overlay-container { display: flex; align-items: center; justify-content: center; height: 100%; }
.mobile-search-input { flex-grow: 1; border: 1px solid #e1e6eb; border-radius: 4px; padding: 5px 15px; font-size: 16px; outline: none; height: 40px; }
.mobile-search-submit-btn { background-color: transparent; border: none; padding: 10px; cursor: pointer; }

.main-navigation { background-color: #fff; border-bottom: 1px solid #e1e6eb; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05); position: relative; z-index: 10; }
.nav-container { justify-content: flex-start; height: auto; }
.main-navigation ul { display: flex; list-style: none; margin: 0; padding: 0; }
.main-navigation li { margin: 0; }
.nav-link {
  display: block; padding: 20px 25px; color: #004178; font-size: 16px; font-weight: 500; position: relative; cursor: pointer; transition: color 0.3s ease;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 4px;
  background-color: #aab2bb;
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.3s ease-out;
}
.nav-link:hover { color: #005a9e; }
.nav-link:hover::after { transform: scaleX(1); }

/* Sticky Header Logic */
.sticky-header-active .header-main-bar {
  position: fixed; top: 0; left: 0; width: 100%; z-index: 999; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1); padding: 5px 0;
}
.sticky-header-active .main-navigation { display: none; }

/* Mobile Menu */
.mobile-menu-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background-color: rgba(0, 0, 0, 0.2); z-index: 999; visibility: hidden; opacity: 0; transition: opacity 0.3s ease-out, visibility 0.3s; }
.mobile-menu-overlay.active { visibility: visible; opacity: 1; }
.mobile-menu-panel { position: absolute; top: 0; right: 0; width: 100%; max-width: 320px; height: 100%; background-color: #fff; box-shadow: -2px 0 5px rgba(0, 0, 0, 0.2); transform: translateX(100%); transition: transform 0.3s ease-out; z-index: 1000; overflow-y: auto; display: flex; flex-direction: column; }
.mobile-menu-overlay.active .mobile-menu-panel { transform: translateX(0); }

.mobile-menu-header { display: flex; justify-content: space-between; align-items: flex-end; padding: 10px 20px 0; background-color: #e8eaeb; }
.mobile-menu-header .links { display: flex; gap: 5px; }
.mobile-menu-header .tab-button { padding: 8px 15px; font-size: 14px; border-radius: 4px 4px 0 0; background-color: transparent; color: #555; position: relative; bottom: -1px; }
.mobile-menu-header .tab-button.active { background-color: #fff; color: #24333e; z-index: 2; }
.mobile-header-controls { display: flex; align-items: center; padding-bottom: 8px; }
.mobile-menu-close-btn { background: none; border: none; padding: 5px; cursor: pointer; color: #555; }
.mobile-menu-content { padding: 20px 0; flex-grow: 1; }
.mobile-main-navigation ul { list-style: none; padding: 0; margin: 0; }
.mobile-nav-link { display: flex; align-items: center; justify-content: space-between; padding: 15px 20px; color: #004178; font-size: 16px; border-bottom: 1px solid #eee; }
.mobile-right-arrow { width: 16px; height: 16px; opacity: 0.6; }
/* #endif */

/* #ifndef H5 */
/* Mini Program fallback layout */
.mp-header {
  background: #fff;
  border-bottom: 1rpx solid #e2e8f0;
  padding: 20rpx 24rpx 16rpx;
  position: fixed;
  left: 0;
  right: 0;
  top: 0;
  z-index: 1200;
  box-sizing: border-box;
}
.mp-header-spacer {
  width: 100%;
}
.mp-header-top {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
}
.mp-logo-wrap {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.mp-logo {
  width: 56rpx;
  height: 56rpx;
}
.mp-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #004178;
}
.mp-search-trigger {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8rpx;
}
.mp-config-trigger {
  width: 56rpx;
  height: 56rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-config-icon {
  width: 32rpx;
  height: 32rpx;
}
.mp-search-icon {
  width: 34rpx;
  height: 34rpx;
}
.mp-search-row {
  margin-top: 14rpx;
  display: flex;
  gap: 10rpx;
}
.mp-search-input {
  flex: 1;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
  border-radius: 8rpx;
  height: 64rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
}
.mp-search-btn {
  width: 120rpx;
  height: 64rpx;
  border-radius: 8rpx;
  background: #004178;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* #endif */

/* #ifdef H5 */
/* Responsive Adjustments */
.hide-in-phone { display: flex; }
@media screen and (max-width: 900px) {
  .logo-text { font-size: 20px; }
  .logo-subtext { font-size: 10px; }
  .search-box { width: 300px; }
}
@media screen and (max-width: 768px) {
  .hide-in-phone { display: none !important; }
  .mobile-menu-toggle { display: flex; font-size: 24px; padding: 2px 8px; border: none;}
  .mobile-search-toggle { display: flex; font-size: 20px; padding: 2px 8px; border: none;}
  .logo-subtext { display: none; }
  .header-main-bar { padding: 15px 0; }
  .logo img { max-height: 40px; }
  .logo-text { font-size: 22px; margin-bottom: 0; }
  .top-bar-container { justify-content: flex-end; }
  .top-menu-container .links { display: none; }
}
/* #endif */
</style>
