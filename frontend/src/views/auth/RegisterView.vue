<template>
  <div class="register-page">
    <div class="auth-card">
      <div class="auth-logo">
        <div class="logo-icon">🚀</div>
        <h1>注册账号</h1>
        <p>开启你的 SOP 之旅</p>
      </div>

      <div class="form-group">
        <label>用户名</label>
        <div class="input-wrap">
          <span class="input-icon">👤</span>
          <input v-model="form.username" class="form-input" placeholder="请输入用户名" />
        </div>
      </div>

      <div class="form-group">
        <label>手机号</label>
        <div class="input-wrap">
          <span class="input-icon">📱</span>
          <input v-model="form.phone" class="form-input" placeholder="请输入手机号" type="tel" />
        </div>
      </div>

      <div class="form-group">
        <label>密码</label>
        <div class="input-wrap">
          <span class="input-icon">🔒</span>
          <input v-model="form.password" class="form-input" placeholder="请输入密码" type="password" />
        </div>
      </div>

      <label class="checkbox-row">
        <input type="checkbox" v-model="agreePolicy" />
        <span>我已阅读并同意</span>
        <a href="#">《服务协议》</a>
        <span>和</span>
        <a href="#">《隐私政策》</a>
      </label>

      <button class="btn-primary" :disabled="loading || !agreePolicy" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>

      <div class="mode-switch">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端注册页
 * - 手机号 + 验证码注册
 * - 跳转已有账号去登录
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const agreePolicy = ref(false)

const form = reactive({ username: '', phone: '', password: '' })

const handleRegister = async () => {
  if (!form.username || !form.phone || !form.password) {
    ElMessage.warning('请填写所有字段')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  loading.value = true
  try {
    await authStore.register(form)
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg-light) 0%, var(--color-bg-light) 100%);
  position: relative;
  overflow: hidden;
}
.register-page::before {
  content: '';
  position: absolute;
  width: 500px; height: 500px;
  background: rgba(91,127,255,0.07);
  border-radius: var(--radius-full);
  top: -150px; right: -80px;
}
.register-page::after {
  content: '';
  position: absolute;
  width: 350px; height: 350px;
  background: rgba(91,127,255,0.05);
  border-radius: var(--radius-full);
  bottom: -80px; left: -40px;
}
.auth-card {
  background: var(--color-bg-light-elevated);
  border-radius: var(--radius-lg);
  box-shadow: 0 4px 12px rgba(91,127,255,0.08);
  padding: 40px;
  width: 400px;
  z-index: 1;
  position: relative;
}
.auth-logo { text-align: center; margin-bottom: 28px; }
.auth-logo .logo-icon { font-size: 40px; margin-bottom: 8px; }
.auth-logo h1 { font-size: var(--font-size-4xl); font-weight: 600; color: var(--color-text-light-primary); margin-bottom: 4px; }
.auth-logo p { font-size: var(--font-size-base); color: var(--color-text-light-secondary); }
.form-group { margin-bottom: 14px; }
.form-group label { display: block; font-size: var(--font-size-sm); font-weight: 500; color: var(--color-text-light-secondary); margin-bottom: 6px; }
.input-wrap { position: relative; }
.input-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); font-size: 16px; }
.form-input {
  width: 100%; height: 46px;
  padding: 0 14px 0 40px;
  border: 1.5px solid var(--color-border-light);
  border-radius: var(--radius-md);
  font-size: var(--font-size-lg);
  outline: none;
  transition: var(--transition-normal);
  box-sizing: border-box;
}
.form-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(91,127,255,0.10);
}
.form-input::placeholder { color: var(--color-text-light-muted); }
.checkbox-row {
  display: flex; align-items: center; flex-wrap: wrap; gap: 4px;
  margin: 12px 0 6px; font-size: var(--font-size-sm); color: var(--color-text-light-secondary);
}
.checkbox-row input { width: 16px; height: 16px; accent-color: var(--color-primary); cursor: pointer; }
.checkbox-row a { color: var(--color-primary); text-decoration: none; }
.checkbox-row a:hover { text-decoration: underline; }
.btn-primary {
  width: 100%; height: 46px;
  background: var(--color-primary); color: white;
  border: none; border-radius: var(--radius-md);
  font-size: var(--font-size-xl); font-weight: 600;
  cursor: pointer;
  transition: var(--transition-normal);
  margin-top: 4px;
}
.btn-primary:hover { background: #7994FF; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.mode-switch { text-align: center; margin-top: 14px; font-size: var(--font-size-sm); color: var(--color-text-light-muted); }
.mode-switch a { color: var(--color-primary); text-decoration: none; }
.mode-switch a:hover { text-decoration: underline; }
</style>
