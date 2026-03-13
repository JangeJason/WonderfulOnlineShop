<template>
  <div class="login-wrap">
    <!-- Decorative elements -->
    <div class="login-orb login-orb-1"></div>
    <div class="login-orb login-orb-2"></div>

    <div class="login-card animate-fade-in">
      <div class="login-brand">
        <div class="login-logo">W</div>
        <h1 class="login-title">Wonderful</h1>
        <p class="login-subtitle">管理后台</p>
      </div>

      <div class="login-divider"></div>

      <a-form layout="vertical" class="login-form">
        <a-form-item label="用户名">
          <a-input
            v-model:value="form.username"
            autocomplete="username"
            size="large"
            placeholder="请输入用户名"
          />
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password
            v-model:value="form.password"
            autocomplete="current-password"
            size="large"
            placeholder="请输入密码"
            @pressEnter="onSubmit"
          />
        </a-form-item>
        <a-button
          type="primary"
          block
          size="large"
          :loading="loading"
          @click="onSubmit"
          class="login-btn"
        >
          登 录
        </a-button>
      </a-form>

      <div class="login-hint">演示账号：admin / admin</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { login } from '../api/auth'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin'
})

async function onSubmit() {
  loading.value = true
  try {
    const resp = await login(form.username, form.password)
    localStorage.setItem('tokenName', resp.tokenName)
    localStorage.setItem('tokenValue', resp.tokenValue)
    message.success('登录成功')
    const redirect = (route.query.redirect as string) || '/products'
    router.replace(redirect)
  } catch (e: any) {
    message.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #00315A 0%, #004178 40%, #0B5EA6 100%);
  position: relative;
  overflow: hidden;
}

.login-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.15;
  pointer-events: none;
}
.login-orb-1 {
  width: 500px;
  height: 500px;
  background: #C8963E;
  top: -10%;
  right: -8%;
}
.login-orb-2 {
  width: 400px;
  height: 400px;
  background: #0B5EA6;
  bottom: -15%;
  left: -10%;
}

.login-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  border-radius: 20px;
  padding: 48px 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-brand {
  text-align: center;
  margin-bottom: 8px;
}
.login-logo {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #004178, #0B5EA6);
  color: #fff;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  font-family: 'DM Sans', sans-serif;
  margin-bottom: 16px;
  box-shadow: 0 4px 16px rgba(0, 65, 120, 0.3);
}
.login-title {
  font-size: 26px;
  font-weight: 700;
  color: #1A2332;
  margin: 0;
  letter-spacing: -0.02em;
  font-family: 'DM Sans', sans-serif;
}
.login-subtitle {
  font-size: 14px;
  color: #6B7A8D;
  margin: 4px 0 0;
}

.login-divider {
  width: 40px;
  height: 3px;
  background: linear-gradient(90deg, #C8963E, #DAB06A);
  border-radius: 2px;
  margin: 20px auto 28px;
}

.login-form :deep(.ant-form-item-label > label) {
  color: #4A5568;
  font-weight: 500;
  font-size: 13px;
}

.login-btn {
  margin-top: 8px;
  height: 44px !important;
  font-size: 15px !important;
  letter-spacing: 0.1em;
  border-radius: 10px !important;
}

.login-hint {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #9FADBF;
}
</style>
