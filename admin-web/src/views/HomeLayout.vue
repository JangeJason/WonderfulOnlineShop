<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider collapsible v-model:collapsed="collapsed">
      <div class="logo">Wonderful</div>
      <a-menu theme="dark" mode="inline" :selectedKeys="selectedKeys" @click="onMenuClick">
        <a-menu-item key="/products">Products</a-menu-item>
        <a-menu-item key="/quote">Quote Demo</a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div />
        <div class="right">
          <a-button size="small" @click="doLogout">Logout</a-button>
        </div>
      </a-layout-header>

      <a-layout-content style="padding: 16px">
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
  if (p.startsWith('/quote')) return ['/quote']
  return ['/products']
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
.logo {
  height: 48px;
  line-height: 48px;
  text-align: center;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 600;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.right {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
