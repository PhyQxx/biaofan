<template>
  <div class="page-header">
    <h2 class="page-title">邮件服务配置</h2>
  </div>

  <div class="card" v-if="loading" style="padding: 40px; text-align: center; color: #909399;">加载中...</div>

  <div class="card" v-else style="padding: 24px;">
    <div class="form-grid">
      <div class="form-group">
        <label>SMTP 服务器</label>
        <input v-model="form.host" placeholder="smtp.qq.com" />
      </div>
      <div class="form-group">
        <label>端口</label>
        <input v-model.number="form.port" type="number" placeholder="587" />
      </div>
      <div class="form-group">
        <label>邮箱账号</label>
        <input v-model="form.username" placeholder="example@qq.com" />
      </div>
      <div class="form-group">
        <label>授权码/密码</label>
        <input v-model="form.password" type="password" :placeholder="form._hasPassword ? '留空则不修改' : '请输入授权码'" />
      </div>
      <div class="form-group">
        <label>发件人地址</label>
        <input v-model="form.fromAddress" placeholder="默认同邮箱账号" />
      </div>
      <div class="form-group">
        <label>SMTP 认证</label>
        <select v-model="form.smtpAuth">
          <option :value="true">启用</option>
          <option :value="false">关闭</option>
        </select>
      </div>
      <div class="form-group">
        <label>STARTTLS</label>
        <select v-model="form.starttlsEnable">
          <option :value="true">启用</option>
          <option :value="false">关闭</option>
        </select>
      </div>
      <div class="form-group">
        <label>邮件服务</label>
        <select v-model="form.enabled">
          <option :value="true">启用</option>
          <option :value="false">关闭</option>
        </select>
      </div>
    </div>

    <div style="margin-top: 24px; display: flex; gap: 12px; align-items: center;">
      <button class="btn-primary" :disabled="saveLoading" @click="handleSave">
        {{ saveLoading ? '保存中...' : '保存配置' }}
      </button>
      <div class="test-group">
        <input v-model="testEmail" placeholder="输入收件邮箱" class="test-input" />
        <button class="btn-secondary" :disabled="testLoading" @click="handleTest">
          {{ testLoading ? '发送中...' : '发送测试邮件' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { getEmailConfig, saveEmailConfig, testEmailConfig } from '@/api/admin'

interface EmailConfigForm {
  host: string
  port: number
  username: string
  password: string
  fromAddress: string
  smtpAuth: boolean
  starttlsEnable: boolean
  enabled: boolean
  _hasPassword?: boolean
}

const loading = ref(false)
const saveLoading = ref(false)
const testLoading = ref(false)
const testEmail = ref('')

const form = reactive<EmailConfigForm>({
  host: '',
  port: 587,
  username: '',
  password: '',
  fromAddress: '',
  smtpAuth: true,
  starttlsEnable: true,
  enabled: true,
  _hasPassword: false,
})

async function loadConfig() {
  loading.value = true
  try {
    const res = await getEmailConfig()
    if (res.code === 200 && res.data) {
      const d = res.data
      form.host = d.host || ''
      form.port = d.port || 587
      form.username = d.username || ''
      form.fromAddress = d.fromAddress || ''
      form.smtpAuth = d.smtpAuth !== false
      form.starttlsEnable = d.starttlsEnable !== false
      form.enabled = d.enabled !== false
      form.password = ''
      form._hasPassword = !!d.username
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saveLoading.value = true
  try {
    const payload: any = { ...form }
    delete payload._hasPassword
    if (!payload.password) delete payload.password
    if (!payload.fromAddress) payload.fromAddress = payload.username
    await saveEmailConfig(payload)
    alert('保存成功')
    await loadConfig()
  } catch {
    alert('保存失败')
  } finally {
    saveLoading.value = false
  }
}

onMounted(() => loadConfig())

async function handleTest() {
  if (!testEmail.value.trim()) {
    alert('请输入收件邮箱')
    return
  }
  testLoading.value = true
  try {
    const res = await testEmailConfig(testEmail.value.trim())
    if (res.code === 200) {
      alert('测试邮件已发送')
    } else {
      alert(res.message || '发送失败')
    }
  } catch {
    alert('发送失败，请检查配置')
  } finally {
    testLoading.value = false
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2xl);
}
.page-title { font-size: var(--font-size-3xl); font-weight: 600; color: var(--color-text-primary); }
.card { background: var(--color-bg-elevated); border-radius: var(--radius-md); border: 1px solid var(--color-border); }
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}
.form-group { display: flex; flex-direction: column; }
.form-group label { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-bottom: 6px; font-weight: 500; }
.form-group input, .form-group select {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  background: #0F1117;
  color: var(--color-text-primary);
}
.form-group input:focus, .form-group select:focus {
  outline: none;
  border-color: var(--color-primary);
}
.btn-primary {
  padding: var(--space-sm) var(--space-2xl);
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-base);
}
.btn-primary:disabled { background: #3B3F52; cursor: not-allowed; }
.btn-secondary {
  padding: var(--space-sm) var(--space-lg);
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--font-size-base);
  white-space: nowrap;
}
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.test-group { display: flex; gap: var(--space-sm); }
.test-input {
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-base);
  background: #0F1117;
  color: var(--color-text-primary);
  width: 200px;
}
.test-input:focus { outline: none; border-color: var(--color-primary); }
</style>
