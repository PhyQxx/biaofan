<template>
  <div class="login-page">
    <div class="auth-card">
      <div class="auth-logo">
        <div class="logo-icon">🚀</div>
        <h1>标帆 SOP</h1>
        <p>让每一步都有标准</p>
      </div>

      <div class="form-group">
        <label>用户名 / 手机号</label>
        <div class="input-wrap">
          <span class="input-icon">👤</span>
          <input v-model="form.phone" class="form-input" placeholder="请输入用户名或手机号" type="text" />
        </div>
      </div>

      <div class="form-group">
        <label>密码</label>
        <div class="input-wrap">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" class="form-input" placeholder="请输入密码" type="password" @keyup.enter="handleLogin" />
        </div>
      </div>

      <div class="auth-links">
        <label class="checkbox-row">
          <input type="checkbox" v-model="rememberMe" />
          <span>记住我</span>
        </label>
        <router-link to="/register">注册账号</router-link>
      </div>

      <button class="btn-primary" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端登录页
 * - 用户名/手机号 + 密码登录
 * - 记住我复选框
 * - 登录成功后跳转到首页
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const rememberMe = ref(false)

const form = reactive({ phone: '', password: '' })

const handleLogin = async () => {
  if (!form.phone || !form.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form.phone, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #F0F3FF 0%, #E8ECFF 100%);
  position: relative;
  overflow: hidden;
}
.login-page::before {
  content: '';
  position: absolute;
  width: 500px; height: 500px;
  background: rgba(91,127,255,0.07);
  border-radius: 50%;
  top: -150px; right: -80px;
}
.login-page::after {
  content: '';
  position: absolute;
  width: 350px; height: 350px;
  background: rgba(91,127,255,0.05);
  border-radius: 50%;
  bottom: -80px; left: -40px;
}
.auth-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(91,127,255,0.08);
  padding: 40px;
  width: 400px;
  z-index: 1;
  position: relative;
}
.auth-logo { text-align: center; margin-bottom: 28px; }
.auth-logo .logo-icon { font-size: 40px; margin-bottom: 8px; }
.auth-logo h1 { font-size: 24px; font-weight: 600; color: #212121; margin-bottom: 4px; }
.auth-logo p { font-size: 14px; color: #666; }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: 13px; font-weight: 500; color: #666; margin-bottom: 6px; }
.input-wrap { position: relative; }
.input-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 16px; }
.form-input {
  width: 100%; height: 46px;
  padding: 0 14px 0 40px;
  border: 1.5px solid #E8E8E8;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: #5B7FFF;
  box-shadow: 0 0 0 3px rgba(91,127,255,0.10);
}
.form-input::placeholder { color: #999; }
.auth-links { display: flex; justify-content: space-between; align-items: center; margin: 12px 0 6px; font-size: 13px; }
.checkbox-row { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.checkbox-row input { width: 16px; height: 16px; accent-color: #5B7FFF; cursor: pointer; }
.auth-links a { color: #5B7FFF; text-decoration: none; }
.auth-links a:hover { text-decoration: underline; }
.btn-primary {
  width: 100%; height: 46px;
  background: #5B7FFF; color: white;
  border: none; border-radius: 8px;
  font-size: 16px; font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
  margin-top: 6px;
}
.btn-primary:hover { background: #7994FF; }
.btn-primary:active { background: #4A6BEE; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
