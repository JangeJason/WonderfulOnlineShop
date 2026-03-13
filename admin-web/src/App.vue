<template>
  <a-config-provider :locale="zhCN" :theme="themeConfig">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

const themeConfig: ThemeConfig = {
  token: {
    colorPrimary: '#004178',
    colorSuccess: '#10B981',
    colorError: '#E53E3E',
    colorWarning: '#F59E0B',
    borderRadius: 8,
    fontFamily: "'DM Sans', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, sans-serif",
  },
}
</script>
