<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider collapsible v-model:collapsed="collapsed" :width="240">
      <div class="sider-brand">
        <div class="sider-logo">W</div>
        <transition name="fade">
          <span v-if="!collapsed" class="sider-name">Wonderful</span>
        </transition>
      </div>
      <a-menu theme="dark" mode="inline" :selectedKeys="selectedKeys" @click="onMenuClick">
        <a-menu-item key="/products">
          <template #icon><span class="menu-icon">📦</span></template>
          产品管理
        </a-menu-item>
        <a-menu-item key="/categories">
          <template #icon><span class="menu-icon">📂</span></template>
          分类管理
        </a-menu-item>
        <a-menu-item key="/orders">
          <template #icon><span class="menu-icon">📋</span></template>
          订单管理
        </a-menu-item>
        <a-menu-item key="/quote">
          <template #icon><span class="menu-icon">💰</span></template>
          报价演示
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="top-header">
        <div class="header-left">
          <span class="page-title">{{ pageTitle }}</span>
        </div>
        <div class="header-right">
          <div class="user-info">
            <div class="user-avatar">A</div>
            <span class="user-name">管理员</span>
          </div>
          <a-button size="small" type="text" @click="doLogout" class="logout-btn">
            退出登录
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="main-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { logout } from '../api/auth'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()

const selectedKeys = computed(() => {
  const p = route.path
  if (p.startsWith('/orders')) return ['/orders']
  if (p.startsWith('/quote')) return ['/quote']
  if (p.startsWith('/categories')) return ['/categories']
  return ['/products']
})

const pageTitle = computed(() => {
  const p = route.path
  if (p.startsWith('/orders')) return '订单管理'
  if (p.startsWith('/quote')) return '报价演示'
  if (p.startsWith('/categories')) return '分类管理'
  if (p.includes('/parameters')) return '参数配置'
  return '产品管理'
})

function onMenuClick(info: any) {
  router.push(info.key)
}

async function doLogout() {
  try {
    await logout()
  } catch {
  } finally {
    localStorage.removeItem('tokenName')
    localStorage.removeItem('tokenValue')
    message.success('已退出')
    router.replace('/login')
  }
}
</script>

<style scoped>
.sider-brand {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  padding: 0 16px;
}
.sider-logo {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #C8963E, #DAB06A);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  font-family: 'DM Sans', sans-serif;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(200, 150, 62, 0.3);
}
.sider-name {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  font-family: 'DM Sans', sans-serif;
  letter-spacing: -0.02em;
  white-space: nowrap;
}

.menu-icon {
  font-size: 16px;
}

.top-header {
  background: #fff !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px !important;
  height: 64px;
  box-shadow: 0 1px 4px rgba(0, 65, 120, 0.04);
  border-bottom: 1px solid var(--c-border);
}
.header-left {
  display: flex;
  align-items: center;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
  letter-spacing: 0.01em;
}
.header-right {
  display: flex;
  gap: 16px;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #004178, #0B5EA6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  font-family: 'DM Sans', sans-serif;
}
.user-name {
  font-size: 13px;
  color: var(--c-text-secondary);
  font-weight: 500;
}
.logout-btn {
  color: var(--c-text-muted) !important;
  font-size: 13px;
}
.logout-btn:hover {
  color: var(--c-danger) !important;
}

.main-content {
  padding: 24px;
  background: var(--c-surface);
}

/* Transition for sider brand text */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
