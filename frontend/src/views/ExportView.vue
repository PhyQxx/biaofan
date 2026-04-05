<template>
  <div class="export-page">
    <!-- Topbar -->
    <div class="topbar">
      <div class="topbar-left">
        <div class="logo">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
        </div>
      </div>
      <div class="topbar-right">
        <button class="btn-new" @click="router.push('/sop/new')">+ 新建 SOP</button>
        <div class="avatar" @click="handleLogout">{{ user?.username?.charAt(0) || 'U' }}</div>
      </div>
    </div>

    <div class="main-layout">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-item" @click="router.push('/')"><span>📊</span><span>工作台</span></div>
        <div class="sidebar-item" @click="router.push('/execution')"><span>▶️</span><span>执行台</span></div>
        <div class="sidebar-item" @click="router.push('/stats')"><span>📈</span><span>统计</span></div>
        <div class="sidebar-item" @click="router.push('/sop/dispatch')"><span>📦</span><span>批量分发</span></div>
        <div class="sidebar-item active"><span>📤</span><span>数据导出</span></div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item" @click="router.push('/notification')"><span>🔔</span><span>通知</span></div>
        <div class="sidebar-item" @click="handleLogout"><span>🚪</span><span>退出登录</span></div>
      </div>

      <!-- Main Content -->
      <div class="main-content">
        <div class="page-header">
          <h1>数据导出</h1>
          <p class="page-desc">选择时间范围和导出字段，生成 SOP 执行数据报表</p>
        </div>

        <!-- Time Range -->
        <div class="card-section">
          <div class="card-title">时间范围</div>
          <div class="time-buttons">
            <button
              v-for="t in timeRanges"
              :key="t.value"
              class="time-btn"
              :class="{ active: timeRange === t.value }"
              @click="setTimeRange(t.value)"
            >{{ t.label }}</button>
          </div>
          <div class="custom-range" v-if="timeRange === 'custom'">
            <div class="date-inputs">
              <div class="date-field">
                <label>开始日期</label>
                <input type="date" v-model="customStart" class="date-input" />
              </div>
              <span class="date-sep">至</span>
              <div class="date-field">
                <label>结束日期</label>
                <input type="date" v-model="customEnd" class="date-input" />
              </div>
            </div>
          </div>
        </div>

        <!-- Export Format -->
        <div class="card-section">
          <div class="card-title">导出格式</div>
          <div class="format-options">
            <label class="format-option" :class="{ active: exportFormat === 'xlsx' }">
              <input type="radio" v-model="exportFormat" value="xlsx" />
              <div class="format-icon">📊</div>
              <div class="format-info">
                <div class="format-name">Excel (.xlsx)</div>
                <div class="format-desc">推荐，支持格式、公式、多 Sheet</div>
              </div>
              <div class="radio-dot" v-if="exportFormat === 'xlsx'"></div>
            </label>
            <label class="format-option" :class="{ active: exportFormat === 'csv' }">
              <input type="radio" v-model="exportFormat" value="csv" />
              <div class="format-icon">📄</div>
              <div class="format-info">
                <div class="format-name">CSV (.csv)</div>
                <div class="format-desc">轻量格式，兼容所有数据工具</div>
              </div>
              <div class="radio-dot" v-if="exportFormat === 'csv'"></div>
            </label>
          </div>
        </div>

        <!-- Field Selection -->
        <div class="card-section">
          <div class="card-title">
            <span>导出字段</span>
            <label class="check-all">
              <div class="check-box" :class="{ checked: allFieldsSelected }" @click="toggleAllFields">
                <span v-if="allFieldsSelected">✓</span>
              </div>
              <span>全选</span>
            </label>
          </div>
          <div class="fields-grid">
            <label
              v-for="f in exportFields"
              :key="f.key"
              class="field-item"
              :class="{ checked: isFieldSelected(f.key), disabled: f.required }"
            >
              <div class="check-box" :class="{ checked: isFieldSelected(f.key) }" @click.stop="toggleField(f.key)">
                <span v-if="isFieldSelected(f.key)">✓</span>
              </div>
              <div class="field-info">
                <span class="field-name">{{ f.label }}</span>
                <span class="field-desc">{{ f.desc }}</span>
              </div>
              <span v-if="f.required" class="required-tag">必选</span>
            </label>
          </div>
        </div>

        <!-- Export Actions -->
        <div class="card-section action-section">
          <div class="preview-info" v-if="previewData.length">
            共 <span class="count">{{ previewData.length }}</span> 条数据（显示前 50 条预览）
          </div>
          <div class="action-buttons">
            <button class="btn-preview" @click="handlePreview" :disabled="exporting">
              👁 导出预览
            </button>
            <button
              class="btn-export"
              :class="{ loading: exporting }"
              :disabled="!hasSelectedFields || exporting"
              @click="handleExport"
            >
              <span v-if="exporting" class="loading-icon">⟳</span>
              {{ exporting ? '导出中...' : '开始导出' }}
            </button>
          </div>
        </div>

        <!-- Preview Modal -->
        <div class="modal-overlay" v-if="showPreview" @click.self="showPreview = false">
          <div class="modal">
            <div class="modal-header">
              <span>导出预览</span>
              <button class="modal-close" @click="showPreview = false">×</button>
            </div>
            <div class="modal-body">
              <table class="preview-table">
                <thead>
                  <tr>
                    <th v-for="f in selectedFields" :key="f">{{ f }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, i) in previewData.slice(0, 50)" :key="i">
                    <td v-for="f in selectedFields" :key="f">{{ row[f] ?? '-' }}</td>
                  </tr>
                </tbody>
              </table>
              <div class="preview-footer" v-if="previewData.length > 50">
                仅显示前 50 条，共 {{ previewData.length }} 条
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()
const user = authStore.userInfo

// Time range
const timeRanges = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本季度', value: 'quarter' },
  { label: '自定义', value: 'custom' },
]
const timeRange = ref('month')
const customStart = ref('')
const customEnd = ref('')

// Format
const exportFormat = ref('xlsx')

// Fields
const exportFields = [
  { key: 'sopTitle', label: 'SOP 名称', desc: 'SOP 标题', required: true },
  { key: 'category', label: '分类', desc: '工作/生活/学习等', required: false },
  { key: 'tags', label: '标签', desc: '自定义标签', required: false },
  { key: 'status', label: '状态', desc: '进行中/已完成/超时', required: true },
  { key: 'executor', label: '执行人', desc: '执行人员姓名', required: true },
  { key: 'startTime', label: '开始时间', desc: '执行开始时间', required: true },
  { key: 'endTime', label: '完成时间', desc: '执行完成时间', required: false },
  { key: 'duration', label: '耗时(分钟)', desc: '执行总耗时', required: false },
  { key: 'result', label: '结果', desc: '执行结果备注', required: false },
]
const selectedFieldKeys = ref<string[]>(['sopTitle', 'status', 'executor', 'startTime'])
const showPreview = ref(false)
const previewData = ref<any[]>([])
const exporting = ref(false)

const hasSelectedFields = computed(() => selectedFieldKeys.value.length > 0)
const allFieldsSelected = computed(() => selectedFieldKeys.value.length === exportFields.length)

const isFieldSelected = (key: string) => selectedFieldKeys.value.includes(key)

const setTimeRange = (val: string) => {
  timeRange.value = val
}

const toggleField = (key: string) => {
  const f = exportFields.find(x => x.key === key)
  if (f?.required) return
  if (isFieldSelected(key)) {
    selectedFieldKeys.value = selectedFieldKeys.value.filter(k => k !== key)
  } else {
    selectedFieldKeys.value.push(key)
  }
}

const toggleAllFields = () => {
  if (allFieldsSelected.value) {
    selectedFieldKeys.value = exportFields.filter(f => f.required).map(f => f.key)
  } else {
    selectedFieldKeys.value = exportFields.map(f => f.key)
  }
}

const selectedFields = computed(() =>
  selectedFieldKeys.value.map(k => exportFields.find(f => f.key === k)?.label || k)
)

const getTimeParams = () => {
  const now = new Date()
  let start = '', end = ''
  if (timeRange.value === 'week') {
    const d = new Date(now); d.setDate(d.getDate() - d.getDay() + 1)
    start = d.toISOString().slice(0, 10)
    end = now.toISOString().slice(0, 10)
  } else if (timeRange.value === 'month') {
    start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10)
    end = now.toISOString().slice(0, 10)
  } else if (timeRange.value === 'quarter') {
    const q = Math.floor(now.getMonth() / 3)
    start = new Date(now.getFullYear(), q * 3, 1).toISOString().slice(0, 10)
    end = now.toISOString().slice(0, 10)
  } else {
    start = customStart.value
    end = customEnd.value
  }
  return { start, end }
}

const handlePreview = async () => {
  try {
    const { start, end } = getTimeParams()
    const res: any = await request.get('/stats/export-preview', {
      params: { start, end, fields: selectedFieldKeys.value.join(',') }
    })
    if (res.code === 200) {
      previewData.value = res.data || []
    } else {
      previewData.value = []
    }
  } catch {
    previewData.value = []
  }
  showPreview.value = true
}

const handleExport = async () => {
  if (!hasSelectedFields.value) return
  exporting.value = true
  try {
    const { start, end } = getTimeParams()
    const res: any = await request.get('/stats/export', {
      params: { start, end, fields: selectedFieldKeys.value.join(','), format: exportFormat.value },
      responseType: 'blob'
    })
    // Trigger download
    const blob = res instanceof Blob ? res : new Blob([res], { type: exportFormat.value === 'xlsx' ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' : 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `SOP导出_${new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')}.${exportFormat.value}`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error(e)
  } finally {
    exporting.value = false
  }
}

const handleLogout = () => { authStore.logout(); router.push('/login') }

onMounted(async () => {
  await authStore.fetchMe()
})
</script>

<style scoped>
.export-page { min-height: 100vh; background: #F5F7FA; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid #E8E8E8; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; position: sticky; top: 0; z-index: 100; }
.topbar-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 16px; color: #212121; }
.logo-icon { font-size: 22px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.btn-new { height: 36px; padding: 0 16px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
.avatar { width: 36px; height: 36px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; }
.main-layout { display: flex; min-height: calc(100vh - 56px); }
.sidebar { width: 200px; background: #fff; border-right: 1px solid #E8E8E8; padding: 12px 0; flex-shrink: 0; }
.sidebar-item { padding: 9px 16px; font-size: 14px; color: #666; cursor: pointer; display: flex; align-items: center; gap: 8px; }
.sidebar-item:hover { background: #F5F7FA; }
.sidebar-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.sidebar-divider { height: 1px; background: #E8E8E8; margin: 8px 16px; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; max-width: 700px; }

.page-header { margin-bottom: 24px; }
.page-header h1 { margin: 0 0 6px; font-size: 22px; font-weight: 600; color: #212121; }
.page-desc { margin: 0; font-size: 14px; color: #999; }

.card-section { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.card-title { font-size: 15px; font-weight: 600; color: #212121; margin-bottom: 14px; display: flex; align-items: center; justify-content: space-between; }

.time-buttons { display: flex; gap: 8px; flex-wrap: wrap; }
.time-btn { height: 36px; padding: 0 16px; border: 1.5px solid #E8E8E8; background: #fff; border-radius: 8px; font-size: 14px; color: #666; cursor: pointer; transition: all 0.15s; }
.time-btn:hover { border-color: #5B7FFF; color: #5B7FFF; }
.time-btn.active { background: #5B7FFF; border-color: #5B7FFF; color: white; font-weight: 600; }

.custom-range { margin-top: 14px; }
.date-inputs { display: flex; align-items: center; gap: 12px; }
.date-field { display: flex; flex-direction: column; gap: 4px; }
.date-field label { font-size: 12px; color: #999; }
.date-input { height: 36px; padding: 0 12px; border: 1px solid #E8E8E8; border-radius: 8px; font-size: 14px; outline: none; }
.date-input:focus { border-color: #5B7FFF; }
.date-sep { color: #999; padding-top: 20px; }

.format-options { display: flex; flex-direction: column; gap: 10px; }
.format-option { display: flex; align-items: center; gap: 14px; padding: 14px 16px; border: 1.5px solid #E8E8E8; border-radius: 10px; cursor: pointer; transition: border-color 0.15s; position: relative; }
.format-option input { display: none; }
.format-option:hover { border-color: #5B7FFF; }
.format-option.active { border: 2px solid #5B7FFF; background: #F5F8FF; }
.format-icon { font-size: 28px; flex-shrink: 0; }
.format-info { flex: 1; }
.format-name { font-size: 14px; font-weight: 600; color: #212121; }
.format-desc { font-size: 12px; color: #999; margin-top: 2px; }
.radio-dot { width: 14px; height: 14px; background: #5B7FFF; border-radius: 50%; flex-shrink: 0; }

.check-all { display: flex; align-items: center; gap: 8px; cursor: pointer; font-size: 13px; color: #666; font-weight: 400; }
.check-box { width: 18px; height: 18px; border: 1.5px solid #D9D9D9; border-radius: 4px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; cursor: pointer; transition: all 0.15s; }
.check-box.checked { background: #5B7FFF; border-color: #5B7FFF; color: white; font-size: 11px; }

.fields-grid { display: flex; flex-direction: column; gap: 8px; }
.field-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: background 0.15s; }
.field-item:hover { background: #F5F7FA; }
.field-item.checked .check-box { background: #5B7FFF; border-color: #5B7FFF; color: white; }
.field-item.disabled { opacity: 0.6; cursor: not-allowed; }
.field-info { flex: 1; display: flex; flex-direction: column; }
.field-name { font-size: 14px; color: #212121; font-weight: 500; }
.field-desc { font-size: 12px; color: #999; }
.required-tag { font-size: 11px; color: #999; background: #F0F0F0; padding: 1px 6px; border-radius: 4px; }

.action-section { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.preview-info { font-size: 14px; color: #666; }
.preview-info .count { color: #5B7FFF; font-weight: 600; }
.action-buttons { display: flex; gap: 10px; }
.btn-preview { height: 40px; padding: 0 18px; background: #fff; border: 1.5px solid #E8E8E8; border-radius: 8px; font-size: 14px; color: #666; cursor: pointer; transition: all 0.15s; }
.btn-preview:hover { border-color: #5B7FFF; color: #5B7FFF; }
.btn-preview:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-export { height: 40px; padding: 0 24px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; transition: background 0.2s; display: flex; align-items: center; gap: 8px; }
.btn-export:hover:not(:disabled) { background: #4A6AE5; }
.btn-export:disabled { background: #D9D9D9; cursor: not-allowed; }
.loading-icon { display: inline-block; animation: spin 0.8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { background: #fff; border-radius: 12px; width: 90%; max-width: 800px; max-height: 80vh; display: flex; flex-direction: column; box-shadow: 0 8px 32px rgba(0,0,0,0.12); }
.modal-header { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; border-bottom: 1px solid #E8E8E8; font-size: 15px; font-weight: 600; color: #212121; }
.modal-close { width: 28px; height: 28px; background: #F5F7FA; border: none; border-radius: 50%; font-size: 18px; color: #666; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.modal-body { overflow-y: auto; flex: 1; }
.preview-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.preview-table th { background: #F5F7FA; padding: 10px 12px; text-align: left; font-weight: 600; color: #212121; border-bottom: 1px solid #E8E8E8; position: sticky; top: 0; }
.preview-table td { padding: 9px 12px; color: #666; border-bottom: 1px solid #F0F0F0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.preview-table tr:hover td { background: #FAFAFA; }
.preview-footer { text-align: center; padding: 12px; font-size: 13px; color: #999; }
</style>
