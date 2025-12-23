<template>
  <div class="wrap">
    <a-card title="Admin Login" style="width: 360px">
      <a-form layout="vertical" @finish="onSubmit">
        <a-form-item label="Username" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" autocomplete="username" />
        </a-form-item>
        <a-form-item label="Password" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" autocomplete="current-password" />
        </a-form-item>
        <a-button type="primary" html-type="submit" block :loading="loading">Login</a-button>
      </a-form>
      <div style="margin-top: 12px; color: rgba(0,0,0,.45)">
        Demo account: admin / admin
      </div>
    </a-card>
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
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
}
</style>
