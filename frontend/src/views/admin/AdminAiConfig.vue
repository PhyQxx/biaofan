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
.page-title { font-size: 20px; font-weight: 600; color: #1a1a2e; }
.section-title { font-size: 14px; font-weight: 600; color: #606266; margin-bottom: 12px; }
.card { background: #fff; border-radius: 8px; border: 1px solid #e4e7ed; overflow: hidden; }
.loading-state, .empty-state { padding: 40px; text-align: center; color: #909399; }
.data-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.data-table th { background: #f5f7fa; padding: 12px 16px; text-align: left; font-weight: 600; color: #606266; border-bottom: 1px solid #e4e7ed; }
.data-table td { padding: 12px 16px; border-bottom: 1px solid #f0f0f0; color: #303133; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #f5f7fa; }
.name-cell { font-weight: 500; }
.url-cell { font-size: 12px; color: #909399; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.key-cell { font-size: 12px; color: #c0c4cc; font-family: monospace; }
.time-cell { font-size: 12px; color: #909399; }
.num-cell { text-align: center; }
.model-type-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; background: #ecf5ff; color: #409eff; font-size: 12px; }
.status-tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-on { background: #f0f9eb; color: #67c23a; }
.status-off { background: #f4f4f5; color: #909399; }
.action-btns { display: flex; gap: 8px; }
.btn-primary { padding: 8px 16px; background: #409eff; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-primary:disabled { background: #a0cfff; cursor: not-allowed; }
.btn-secondary { padding: 8px 16px; background: #fff; color: #606266; border: 1px solid #dcdfe6; border-radius: 4px; cursor: pointer; font-size: 14px; }
.btn-text { background: none; border: none; color: #409eff; cursor: pointer; font-size: 14px; padding: 0; }
.btn-text.danger { color: #f56c6c; }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: #fff; border-radius: 8px; width: 520px; max-width: 90vw; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e4e7ed; }
.dialog-header h3 { margin: 0; font-size: 16px; font-weight: 600; }
.dialog-close { background: none; border: none; font-size: 20px; color: #909399; cursor: pointer; }
.dialog-body { padding: 20px; max-height: 60vh; overflow-y: auto; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 12px 20px; border-top: 1px solid #e4e7ed; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 14px; color: #606266; margin-bottom: 6px; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea { width: 100%; padding: 8px 12px; border: 1px solid #dcdfe6; border-radius: 4px; font-size: 14px; box-sizing: border-box; }
.form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #409eff; }
.checkbox-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
</style>
