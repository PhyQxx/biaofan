<template>
  <div class="page-header">
    <h2 class="page-title">🤖 AI 模型配置</h2>
    <button class="btn-primary" @click="openForm()">+ 新增全局配置</button>
  </div>

  <!-- Global Config Section -->
  <div class="section-title">全局配置（所有用户默认使用）</div>
  <div class="card">
    <div v-if="globalLoading" class="loading-state">加载中...</div>
    <div v-else-if="globalConfigs.length === 0" class="empty-state">暂无全局配置，点击上方按钮新增</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>模型类型</th>
          <th>模型名称</th>
          <th>API 地址</th>
          <th>API Key</th>
          <th>温度</th>
          <th>状态</th>
          <th>更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="cfg in globalConfigs" :key="cfg.id">
          <td><span class="model-type-tag">{{ modelTypeLabel(cfg.modelType) }}</span></td>
          <td class="name-cell">{{ cfg.modelName || '-' }}</td>
          <td class="url-cell">{{ cfg.apiUrl || '-' }}</td>
          <td class="key-cell">{{ cfg.apiKey ? '****' + cfg.apiKey.slice(-4) : '-' }}</td>
          <td class="num-cell">{{ cfg.temperature ?? 0.7 }}</td>
          <td>
            <span class="status-tag" :class="cfg.enabled ? 'status-on' : 'status-off'">
              {{ cfg.enabled ? '启用' : '禁用' }}
            </span>
          </td>
          <td class="time-cell">{{ formatTime(cfg.updatedAt) }}</td>
          <td>
            <div class="action-btns">
              <button class="btn-text" @click="openForm(cfg)">编辑</button>
              <button class="btn-text danger" @click="handleDelete(cfg)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- User Configs Section -->
  <div class="section-title" style="margin-top: 32px;">用户级配置（覆盖全局）</div>
  <div class="card">
    <div v-if="userLoading" class="loading-state">加载中...</div>
    <div v-else-if="userConfigs.length === 0" class="empty-state">暂无用户配置</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>用户ID</th>
          <th>模型类型</th>
          <th>模型名称</th>
          <th>API 地址</th>
          <th>状态</th>
          <th>更新时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="cfg in userConfigs" :key="cfg.id">
          <td><code>{{ cfg.userId }}</code></td>
          <td><span class="model-type-tag">{{ modelTypeLabel(cfg.modelType) }}</span></td>
          <td class="name-cell">{{ cfg.modelName || '-' }}</td>
          <td class="url-cell">{{ cfg.apiUrl || '-' }}</td>
          <td>
            <span class="status-tag" :class="cfg.enabled ? 'status-on' : 'status-off'">
              {{ cfg.enabled ? '启用' : '禁用' }}
            </span>
          </td>
          <td class="time-cell">{{ formatTime(cfg.updatedAt) }}</td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Form Dialog -->
  <div class="dialog-overlay" v-if="formVisible" @click.self="formVisible = false">
    <div class="dialog">
      <div class="dialog-header">
        <h3>{{ editingConfig ? '编辑全局配置' : '新增全局配置' }}</h3>
        <button class="dialog-close" @click="formVisible = false">×</button>
      </div>
      <div class="dialog-body">
        <div class="form-group">
          <label>模型类型</label>
          <select v-model="form.modelType">
            <option value="deepseek">DeepSeek</option>
            <option value="glm">GLM（智谱）</option>
            <option value="minimax">MiniMax</option>
          </select>
        </div>
        <div class="form-group">
          <label>模型名称</label>
          <input v-model="form.modelName" placeholder="如 deepseek-chat、glm-4-flash" />
        </div>
        <div class="form-group">
          <label>API 地址</label>
          <input v-model="form.apiUrl" placeholder="https://api.deepseek.com/chat/completions" />
        </div>
        <div class="form-group">
          <label>API Key</label>
          <input v-model="form.apiKey" type="password" placeholder="sk-..." />
        </div>
        <div class="form-group">
          <label>系统提示词</label>
          <textarea v-model="form.systemPrompt" rows="3" placeholder="可选，设置 AI 助手默认行为"></textarea>
        </div>
        <div class="form-group">
          <label>温度参数（0-1）</label>
          <input v-model.number="form.temperature" type="number" min="0" max="1" step="0.1" />
        </div>
        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.enabled" type="checkbox" />
            启用此配置
          </label>
        </div>
      </div>
      <div class="dialog-footer">
        <button class="btn-secondary" @click="formVisible = false">取消</button>
        <button class="btn-primary" :disabled="saveLoading" @click="handleSave">
          {{ saveLoading ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getGlobalAiConfigs, getUserAiConfigs, saveGlobalAiConfig, deleteGlobalAiConfig, type AiModelConfig } from '@/api/admin'

const globalConfigs = ref<AiModelConfig[]>([])
const userConfigs = ref<AiModelConfig[]>([])
const globalLoading = ref(false)
const userLoading = ref(false)

const formVisible = ref(false)
const editingConfig = ref<AiModelConfig | null>(null)
const saveLoading = ref(false)

const defaultForm = (): Partial<AiModelConfig> => ({
  modelType: 'deepseek',
  modelName: '',
  apiUrl: '',
  apiKey: '',
  systemPrompt: '',
  temperature: 0.7,
  enabled: true,
})

const form = ref<Partial<AiModelConfig>>(defaultForm())

async function loadGlobal() {
  globalLoading.value = true
  try {
    const res = await getGlobalAiConfigs()
    if (res.code === 200) {
      globalConfigs.value = res.data.list
    }
  } finally {
    globalLoading.value = false
  }
}

async function loadUser() {
  userLoading.value = true
  try {
    const res = await getUserAiConfigs()
    if (res.code === 200) {
      userConfigs.value = res.data.list
    }
  } finally {
    userLoading.value = false
  }
}

function openForm(cfg?: AiModelConfig) {
  editingConfig.value = cfg || null
  form.value = cfg ? { ...cfg } : defaultForm()
  formVisible.value = true
}

async function handleSave() {
  saveLoading.value = true
  try {
    await saveGlobalAiConfig(form.value as AiModelConfig)
    formVisible.value = false
    await loadGlobal()
  } finally {
    saveLoading.value = false
  }
}

async function handleDelete(cfg: AiModelConfig) {
  if (!cfg.id || !confirm(`确定删除该全局配置？`)) return
  await deleteGlobalAiConfig(cfg.id)
  await loadGlobal()
}

function modelTypeLabel(t?: string) {
  return { deepseek: 'DeepSeek', glm: 'GLM（智谱）', minimax: 'MiniMax' }[t || ''] || t || '-'
}

function formatTime(ts?: string) {
  if (!ts) return '-'
  return new Date(ts).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => {
  loadGlobal()
  loadUser()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.page-title { font-size: var(--font-size-4xl); font-weight: 600; color: var(--color-text-primary); }
.section-title { font-size: var(--font-size-base); font-weight: 600; color: var(--color-text-secondary); margin-bottom: var(--space-md); }
.card { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-lg); overflow: hidden; }
.loading-state, .empty-state { padding: 40px; text-align: center; color: var(--color-text-secondary); }
.data-table { width: 100%; border-collapse: collapse; font-size: var(--font-size-base); }
.data-table th { background: var(--color-bg-surface); padding: var(--space-md) var(--space-lg); text-align: left; font-weight: 600; color: var(--color-text-secondary); border-bottom: 1px solid var(--color-border); }
.data-table td { padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--color-border); color: var(--color-text-primary); }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--color-bg-surface); }
.name-cell { font-weight: 500; }
.url-cell { font-size: var(--font-size-sm); color: var(--color-text-muted); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.key-cell { font-size: var(--font-size-sm); color: var(--color-text-muted); font-family: monospace; }
.time-cell { font-size: var(--font-size-sm); color: var(--color-text-muted); }
.num-cell { text-align: center; }
.model-type-tag { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); background: var(--color-primary-subtle); color: var(--color-primary); font-size: var(--font-size-sm); }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: var(--font-size-sm); }
.status-on { background: var(--color-success-subtle); color: var(--color-success); }
.status-off { background: var(--color-bg-surface); color: var(--color-text-muted); }
.action-btns { display: flex; gap: var(--space-sm); }
.btn-primary { padding: 8px 16px; background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-md); cursor: pointer; font-size: var(--font-size-base); }
.btn-primary:disabled { background: var(--color-text-muted); cursor: not-allowed; }
.btn-secondary { padding: 8px 16px; background: var(--color-bg-surface); color: var(--color-text-primary); border: 1px solid var(--color-border); border-radius: var(--radius-md); cursor: pointer; font-size: var(--font-size-base); }
.btn-text { background: none; border: none; color: var(--color-primary); cursor: pointer; font-size: var(--font-size-base); padding: 0; }
.btn-text.danger { color: var(--color-error); }
.dialog-overlay { position: fixed; inset: 0; background: var(--color-bg-overlay); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: var(--color-bg-elevated); border-radius: var(--radius-lg); width: 520px; max-width: 90vw; border: 1px solid var(--color-border); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: var(--space-lg) var(--space-xl); border-bottom: 1px solid var(--color-border); }
.dialog-header h3 { margin: 0; font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-primary); }
.dialog-close { background: none; border: none; font-size: 20px; color: var(--color-text-secondary); cursor: pointer; }
.dialog-body { padding: var(--space-xl); max-height: 60vh; overflow-y: auto; }
.dialog-footer { display: flex; justify-content: flex-end; gap: var(--space-md); padding: var(--space-md) var(--space-xl); border-top: 1px solid var(--color-border); }
.form-group { margin-bottom: var(--space-lg); }
.form-group label { display: block; font-size: var(--font-size-base); color: var(--color-text-secondary); margin-bottom: var(--space-sm); font-weight: 500; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: var(--space-sm) var(--space-md); border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: var(--font-size-base); box-sizing: border-box; background: var(--color-bg-surface); color: var(--color-text-primary); }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: var(--color-primary); }
.form-group input::placeholder { color: var(--color-text-muted); }
.form-group select option { background: var(--color-bg-elevated); color: var(--color-text-primary); }
.checkbox-label { display: flex; align-items: center; gap: var(--space-sm); cursor: pointer; color: var(--color-text-primary); }
</style>
