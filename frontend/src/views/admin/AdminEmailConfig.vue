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
  margin-bottom: 24px;
}
.page-title { font-size: 20px; font-weight: 600; color: #E8EAF0; }
.card { background: #1A1D27; border-radius: 8px; border: 1px solid #2D3348; }
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-group { display: flex; flex-direction: column; }
.form-group label { font-size: 13px; color: #8B90A0; margin-bottom: 6px; font-weight: 500; }
.form-group input, .form-group select {
  padding: 8px 12px;
  border: 1px solid #2D3348;
  border-radius: 4px;
  font-size: 14px;
  background: #0F1117;
  color: #E8EAF0;
}
.form-group input:focus, .form-group select:focus {
  outline: none;
  border-color: #5B7FFF;
}
.btn-primary {
  padding: 8px 24px;
  background: #5B7FFF;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}
.btn-primary:disabled { background: #3B3F52; cursor: not-allowed; }
.btn-secondary {
  padding: 8px 16px;
  background: transparent;
  color: #5B7FFF;
  border: 1px solid #5B7FFF;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
.test-group { display: flex; gap: 8px; }
.test-input {
  padding: 8px 12px;
  border: 1px solid #2D3348;
  border-radius: 4px;
  font-size: 14px;
  background: #0F1117;
  color: #E8EAF0;
  width: 200px;
}
.test-input:focus { outline: none; border-color: #5B7FFF; }
</style>
