<template>
  <div class="editor-topbar">
    <div class="topbar-left">
      <button class="btn-back" @click="router.back()">← 返回</button>
      <input v-model="form.title" class="title-input" placeholder="输入 SOP 标题..." />
    </div>
    <div class="topbar-right">
      <button v-if="isEdit" class="btn-versions" @click="router.push(`/sop/${route.params.id}/versions`)">📋 版本历史</button>
      <button v-if="isEdit" class="btn-delete" @click="handleDelete">🗑 删除</button>
      <button class="btn-secondary" @click="handleSave('draft')">保存草稿</button>
      <button class="btn-primary" @click="handleSave('published')">发布 SOP</button>
    </div>
  </div>

  <div class="editor-body">
      <div class="editor-main">
      <!-- Meta -->
      <div class="meta-row">
        <div class="meta-item">
          <label>分类</label>
          <select v-model="form.category" class="meta-select">
            <option value="daily">日 SOP</option>
            <option value="weekly">周 SOP</option>
            <option value="monthly">月 SOP</option>
            <option value="yearly">年 SOP</option>
          </select>
        </div>
        <div class="meta-item">
          <label>标签</label>
          <input v-model="tagsInput" class="meta-input" placeholder="多个标签用逗号分隔" />
        </div>
      </div>

      <div class="form-group">
        <label>描述</label>
        <textarea v-model="form.description" class="desc-input" placeholder="简要描述这个 SOP..."></textarea>
      </div>

      <!-- Steps -->
      <div class="steps-section">
        <div class="steps-header">
          <h3>步骤列表</h3>
          <button class="btn-add-step" @click="addStep">+ 添加步骤</button>
        </div>

        <div v-for="(step, index) in form.content" :key="index" class="step-card">
          <div class="step-num">{{ index + 1 }}</div>
          <div class="step-content">
            <input v-model="step.title" class="step-title-input" placeholder="步骤标题" />
            <textarea v-model="step.description" class="step-desc-input" placeholder="步骤说明..."></textarea>
            <div class="step-meta">
              <label>预计耗时（分钟）</label>
              <input v-model.number="step.duration" type="number" class="step-duration" placeholder="30" />
            </div>
          </div>
          <button class="btn-remove-step" @click="removeStep(index)">✕</button>
        </div>

        <div v-if="form.content.length === 0" class="empty-steps">
          <p>还没有步骤，点击上方"添加步骤"开始</p>
        </div>
      </div>

      <!-- 定时触发配置 -->
      <div v-if="isEdit" class="schedule-section">
        <div class="schedule-header">
          <h3>⏰ 定时触发</h3>
          <div class="schedule-toggle">
            <span class="toggle-label">{{ scheduleTask.enabled ? '已启用' : '已停用' }}</span>
            <button
              class="toggle-btn"
              :class="{ active: scheduleTask.enabled }"
              @click="toggleSchedule"
            >
              <span class="toggle-dot"></span>
            </button>
          </div>
        </div>

        <div class="schedule-body" v-if="scheduleTask.loaded">
          <!-- 当前定时状态 -->
          <div v-if="scheduleTask.current" class="schedule-info">
            <div class="schedule-info-row">
              <span class="info-label">Cron 表达式：</span>
              <code class="cron-expr">{{ scheduleTask.current.cronExpression }}</code>
              <span class="schedule-desc">（{{ cronDesc(scheduleTask.current.cronExpression) }}）</span>
            </div>
            <div class="schedule-info-row">
              <span class="info-label">下次触发：</span>
              <span class="next-fire">{{ formatDateTime(scheduleTask.current.nextFireTime) }}</span>
              <span class="status-badge" :class="scheduleTask.current.enabled ? 'active' : 'inactive'">
                {{ scheduleTask.current.enabled ? '运行中' : '已停用' }}
              </span>
            </div>
          </div>

          <!-- 未配置时显示设置表单 -->
          <div v-if="!scheduleTask.current" class="schedule-form">
            <div class="cron-presets">
              <label class="preset-label">快速选择：</label>
              <button v-for="preset in cronPresets" :key="preset.label"
                class="preset-btn"
                :class="{ selected: selectedPreset === preset.label }"
                @click="applyPreset(preset)">
                {{ preset.label }}
              </button>
            </div>

            <div class="cron-builder">
              <div class="cron-row">
                <div class="cron-field">
                  <label>频率</label>
                  <select v-model="scheduleForm.freq" class="cron-select" @change="onFreqChange">
                    <option value="daily">每天</option>
                    <option value="weekly">每周</option>
                    <option value="monthly">每月</option>
                  </select>
                </div>
                <div class="cron-field" v-if="scheduleForm.freq === 'weekly'">
                  <label>星期</label>
                  <select v-model="scheduleForm.weekday" class="cron-select">
                    <option value="1">周一</option>
                    <option value="2">周二</option>
                    <option value="3">周三</option>
                    <option value="4">周四</option>
                    <option value="5">周五</option>
                    <option value="6">周六</option>
                    <option value="7">周日</option>
                  </select>
                </div>
                <div class="cron-field" v-if="scheduleForm.freq === 'monthly'">
                  <label>日期</label>
                  <select v-model="scheduleForm.monthDay" class="cron-select">
                    <option v-for="d in 28" :key="d" :value="d">{{ d }}日</option>
                  </select>
                </div>
                <div class="cron-field">
                  <label>时间</label>
                  <input v-model="scheduleForm.time" type="time" class="cron-time" />
                </div>
              </div>

              <div class="cron-preview">
                <span class="preview-label">Cron：</span>
                <code>{{ buildCron() }}</code>
              </div>
            </div>

            <div class="schedule-actions">
              <button class="btn-save-schedule" @click="saveSchedule">💾 保存定时</button>
            </div>
          </div>

          <!-- 已配置时的操作 -->
          <div v-else class="schedule-actions-row">
            <button class="btn-reset-schedule" @click="resetSchedule">🗑 删除定时</button>
          </div>
        </div>
      </div>

      </div>

      <!-- AI 助手面板 - 常驻显示 -->
      <div class="ai-panel-wrapper">
        <SopAiPanel
          :sop-id="sopId"
          :visible-tabs="['create', 'review']"
          @apply-sop="handleApplyAiSop"
        />
      </div>
    </div>
</template>

<script setup lang="ts">


/**
 * SOP 编辑器页（新建 / 编辑）
 * - SOP 基本信息：标题、描述、分类、标签
 * - 执行人选择
 * - 步骤列表：拖拽排序 / 新增 / 删除 / 复制
 * - 每步骤：标题 + 描述 + 检查项配置（类型/是否必填/选项）
 * - 保存 / 发布
 */
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSopStore } from '@/stores/sop'
import SopAiPanel from '@/components/ai/SopAiPanel.vue'

const route = useRoute()
const router = useRouter()
const sopStore = useSopStore()

const isEdit = !!route.params.id
const tagsInput = ref('')

const form = reactive({
  title: '',
  description: '',
  category: 'daily',
  content: [] as { title: string; description: string; duration: number }[],
  status: 'draft',
})

// 定时任务状态
const scheduleTask = reactive({
  current: null as any,
  enabled: false,
  loaded: false,
})

const scheduleForm = reactive({
  freq: 'daily',
  weekday: '1',
  monthDay: '1',
  time: '08:00',
})

const selectedPreset = ref('')

// AI 助手面板
const sopId = ref<number | undefined>(undefined)

function handleApplyAiSop(jsonStr: string) {
  try {
    const parsed = JSON.parse(jsonStr)
    if (parsed.title) form.title = parsed.title
    if (parsed.description) form.description = parsed.description
    if (parsed.category) form.category = parsed.category
    if (parsed.tags) tagsInput.value = Array.isArray(parsed.tags) ? parsed.tags.join(',') : parsed.tags
    if (Array.isArray(parsed.content)) {
      form.content = parsed.content.map((s: any) => ({
        title: s.title || '',
        description: s.description || '',
        duration: s.duration || 10,
      }))
    }
    ElMessage.success('AI 生成的 SOP 已应用')
  } catch {
    ElMessage.error('JSON 解析失败，请手动复制粘贴')
  }
}

const cronPresets = [
  { label: '每天早上8点', cron: '0 8 * * *' },
  { label: '每天晚上8点', cron: '0 20 * * *' },
  { label: '每周一早上9点', cron: '0 9 * * 1' },
  { label: '每月1号早上9点', cron: '0 9 1 * *' },
]

const buildCron = () => {
  const [hh] = scheduleForm.time.split(':')
  if (scheduleForm.freq === 'daily') return `0 ${hh} * * *`
  if (scheduleForm.freq === 'weekly') return `0 ${hh} * * ${scheduleForm.weekday}`
  if (scheduleForm.freq === 'monthly') return `0 ${hh} ${scheduleForm.monthDay} * *`
  return ''
}

const applyPreset = (preset: any) => {
  selectedPreset.value = preset.label
  const parts = preset.cron.split(' ')
  const [mm, hh, , , dow] = parts
  scheduleForm.time = `${hh.padStart(2, '0')}:${mm.padStart(2, '0')}`
  if (dow !== '*') {
    scheduleForm.freq = 'weekly'
    scheduleForm.weekday = dow
  } else {
    scheduleForm.freq = 'daily'
  }
}

const onFreqChange = () => {
  selectedPreset.value = ''
}

const cronDesc = (cron: string) => {
  if (!cron) return ''
  const parts = cron.trim().split(/\s+/)
  const [mm, hh, dom, , dow] = parts
  if (dom === '*' && dow === '*') return `每天 ${hh}:${mm.padStart(2, '0')}`
  if (dom === '*' && dow !== '*') {
    const weekNames: Record<string, string> = { '1': '周一', '2': '周二', '3': '周三', '4': '周四', '5': '周五', '6': '周六', '7': '周日' }
    return `每周${weekNames[dow] || dow} ${hh}:${mm.padStart(2, '0')}`
  }
  if (dom !== '*') return `每月${dom}日 ${hh}:${mm.padStart(2, '0')}`
  return cron
}

const formatDateTime = (dt: string) => {
  if (!dt) return '-'
  return dt.replace('T', ' ').substring(0, 16)
}

const loadSchedule = async () => {
  if (!isEdit) return
  try {
    const res: any = await sopStore.getScheduleTask(Number(route.params.id))
    scheduleTask.loaded = true
    if (res.code === 200 && res.data) {
      scheduleTask.current = res.data
      scheduleTask.enabled = res.data.enabled === 1
    }
  } catch (e) {
    scheduleTask.loaded = true
  }
}

const saveSchedule = async () => {
  if (!isEdit) return
  const cron = buildCron()
  if (!cron) {
    ElMessage.warning('请选择定时时间')
    return
  }
  try {
    await sopStore.saveScheduleTask(Number(route.params.id), cron)
    ElMessage.success('定时任务保存成功')
    await loadSchedule()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

const resetSchedule = async () => {
  if (!isEdit) return
  try {
    await sopStore.deleteScheduleTask(Number(route.params.id))
    ElMessage.success('定时任务已删除')
    scheduleTask.current = null
    scheduleTask.enabled = false
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

const toggleSchedule = async () => {
  if (!scheduleTask.current) return
  const newEnabled = scheduleTask.current.enabled === 1 ? 0 : 1
  try {
    await sopStore.toggleScheduleTask(scheduleTask.current.id, newEnabled)
    scheduleTask.current.enabled = newEnabled
    scheduleTask.enabled = newEnabled === 1
    ElMessage.success(newEnabled ? '定时任务已启用' : '定时任务已停用')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const addStep = () => {
  form.content.push({ title: '', description: '', duration: 30 })
}

const removeStep = (index: number) => {
  form.content.splice(index, 1)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除该 SOP 吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await sopStore.deleteSop(Number(route.params.id))
    ElMessage.success('SOP 已删除')
    router.push('/')
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '删除失败')
    }
  }
}

const handleSave = async (status: string) => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  const tags = tagsInput.value.split(',').map((t: string) => t.trim()).filter(Boolean)
  const data = {
    ...form,
    tags,
    status: 'draft',
  }
  try {
    let sopId = Number(route.params.id)
    if (isEdit) {
      await sopStore.updateSop(sopId, data)
    } else {
      const createRes: any = await sopStore.createSop(data)
      if (createRes?.code === 200 && createRes.data?.id) {
        sopId = createRes.data.id
      } else if (createRes?.data) {
        sopId = Number(createRes.data)
      }
    }

    if (status === 'published' && sopId) {
      await sopStore.publishSop(sopId)
      ElMessage.success('发布成功')
    } else {
      ElMessage.success(isEdit ? '保存成功' : '创建成功')
    }
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(async () => {
  if (isEdit) {
    const res: any = await sopStore.getSopById(Number(route.params.id))
    if (res.code === 200) {
      const sop = res.data
      form.title = sop.title
      form.description = sop.description || ''
      form.category = sop.category || 'daily'
      form.content = (sop.content && sop.content !== 'null' && sop.content !== 'undefined') ? JSON.parse(sop.content) : []
      tagsInput.value = (sop.tags && sop.tags !== 'null' && sop.tags !== 'undefined') ? JSON.parse(sop.tags).join(',') : ''
      sopId.value = sop.id
    }
    await loadSchedule()
  }
})
</script>

<style scoped>
.editor-page { min-height: 100vh; background: #F5F7FA; }
.editor-topbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px; background: #fff;
  border-bottom: 1px solid #E8E8E8; gap: 16px;
}
.editor-body {
  display: flex;
  gap: 24px;
  align-items: flex-start;
  margin: 72px 24px 24px;
  padding: 0;
}
.title-input {
  flex: 1; font-size: 18px; font-weight: 600; border: none; outline: none;
  background: transparent; color: #212121;
}
.title-input::placeholder { color: #BDBDBD; }
.btn-secondary {
  height: 36px; padding: 0 16px;
  background: #fff; color: #333;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 14px; font-weight: 500; cursor: pointer;
}
.btn-secondary:hover { background: #F5F7FA; }
.btn-primary {
  height: 36px; padding: 0 16px;
  background: #5B7FFF; color: white;
  border: none; border-radius: 8px;
  font-size: 14px; font-weight: 600; cursor: pointer;
}
.btn-primary:hover { background: #7994FF; }
.btn-versions {
  height: 36px; padding: 0 14px;
  background: #fff; color: #5B7FFF;
  border: 1.5px solid #5B7FFF; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-versions:hover { background: #E8ECFF; }
.btn-delete {
  height: 36px; padding: 0 14px;
  background: #fff; color: #FF4D4F;
  border: 1.5px solid #FF4D4F; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-versions:hover { background: #E8ECFF; }
.editor-main {
  flex: 1;
  max-width: 860px;
  min-width: 0;
}
.topbar-left { display: flex; align-items: center; gap: 12px; flex: 1; }
.topbar-right { display: flex; gap: 8px; }
.btn-back {
  background: none; border: none; font-size: 14px; color: #666;
  cursor: pointer; padding: 6px 12px; border-radius: 6px;
}
.btn-back:hover { background: #F5F7FA; }
.ai-panel-wrapper {
  width: 380px;
  flex-shrink: 0;
  position: sticky;
  top: 24px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
}
.meta-row { display: flex; gap: 16px; margin-bottom: 16px; }
.meta-item { flex: 1; }
.meta-item label, .form-group label { display: block; font-size: 13px; font-weight: 500; color: #666; margin-bottom: 6px; }
.meta-select, .meta-input, .desc-input {
  width: 100%; height: 40px; padding: 0 12px;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 14px; outline: none; box-sizing: border-box;
  background: #fff;
}
.meta-select:focus, .meta-input:focus, .desc-input:focus {
  border-color: #5B7FFF;
  box-shadow: 0 0 0 3px rgba(91,127,255,0.10);
}
.desc-input { height: 80px; padding: 10px 12px; resize: vertical; }
.form-group { margin-bottom: 20px; }
.steps-section { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); margin-bottom: 20px; }
.steps-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.steps-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #212121; }
.btn-add-step {
  height: 32px; padding: 0 14px;
  background: #E8ECFF; color: #5B7FFF;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-add-step:hover { background: #D0D8FF; }
.step-card {
  display: flex; gap: 12px; margin-bottom: 12px;
  padding: 14px; background: #F5F7FA; border-radius: 8px;
}
.step-num {
  width: 28px; height: 28px; background: #5B7FFF; color: white;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.step-content { flex: 1; }
.step-title-input {
  width: 100%; height: 36px; padding: 0 10px;
  border: 1px solid #E8E8E8; border-radius: 6px;
  font-size: 14px; font-weight: 500; outline: none; margin-bottom: 8px; box-sizing: border-box;
  background: #fff;
}
.step-desc-input {
  width: 100%; height: 60px; padding: 8px 10px;
  border: 1px solid #E8E8E8; border-radius: 6px;
  font-size: 13px; outline: none; resize: vertical; margin-bottom: 8px; box-sizing: border-box;
  background: #fff;
}
.step-meta { display: flex; align-items: center; gap: 8px; }
.step-meta label { font-size: 12px; color: #999; margin: 0; }
.step-duration { width: 80px; height: 28px; padding: 0 8px; border: 1px solid #E8E8E8; border-radius: 6px; font-size: 13px; outline: none; background: #fff; }
.btn-remove-step {
  background: none; border: none; color: #FF4D4F;
  font-size: 16px; cursor: pointer; padding: 4px;
}
.empty-steps { text-align: center; padding: 24px; color: #999; }

/* 定时触发样式 */
.schedule-section {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.schedule-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.schedule-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #212121; }
.schedule-toggle { display: flex; align-items: center; gap: 8px; }
.toggle-label { font-size: 13px; color: #666; }
.toggle-btn {
  width: 44px; height: 24px; border-radius: 12px;
  background: #E8E8E8; border: none; cursor: pointer; position: relative;
  transition: background 0.2s;
}
.toggle-btn.active { background: #52C41A; }
.toggle-dot {
  width: 18px; height: 18px; background: #fff; border-radius: 50%;
  position: absolute; top: 3px; left: 3px;
  transition: left 0.2s;
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.toggle-btn.active .toggle-dot { left: 23px; }

.schedule-info { margin-bottom: 16px; }
.schedule-info-row {
  display: flex; align-items: center; gap: 8px; margin-bottom: 8px;
}
.info-label { font-size: 13px; color: #666; }
.cron-expr { background: #F5F7FA; padding: 2px 8px; border-radius: 4px; font-size: 13px; color: #5B7FFF; }
.schedule-desc { font-size: 13px; color: #999; }
.next-fire { font-size: 13px; color: #333; }
.status-badge {
  font-size: 12px; padding: 1px 8px; border-radius: 10px;
}
.status-badge.active { background: #E6F7E6; color: #52C41A; }
.status-badge.inactive { background: #F5F5F5; color: #999; }

.schedule-form { }
.cron-presets { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.preset-label { font-size: 13px; color: #666; }
.preset-btn {
  height: 28px; padding: 0 12px; border-radius: 14px;
  background: #F5F7FA; color: #333; border: 1px solid #E8E8E8;
  font-size: 12px; cursor: pointer; transition: all 0.2s;
}
.preset-btn:hover { border-color: #5B7FFF; color: #5B7FFF; }
.preset-btn.selected { background: #E8ECFF; border-color: #5B7FFF; color: #5B7FFF; }

.cron-builder { background: #F9FAFB; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
.cron-row { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.cron-field { display: flex; flex-direction: column; gap: 4px; }
.cron-field label { font-size: 12px; color: #999; }
.cron-select {
  height: 34px; padding: 0 10px; border: 1px solid #E8E8E8;
  border-radius: 6px; font-size: 13px; background: #fff; outline: none;
}
.cron-time {
  height: 34px; padding: 0 10px; border: 1px solid #E8E8E8;
  border-radius: 6px; font-size: 13px; background: #fff; outline: none;
}
.cron-preview { display: flex; align-items: center; gap: 8px; }
.preview-label { font-size: 13px; color: #666; }
.cron-preview code { background: #E8ECFF; color: #5B7FFF; padding: 2px 10px; border-radius: 4px; font-size: 13px; }

.schedule-actions, .schedule-actions-row { display: flex; gap: 8px; }
.btn-save-schedule {
  height: 34px; padding: 0 16px;
  background: #5B7FFF; color: white;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-save-schedule:hover { background: #7994FF; }
.btn-reset-schedule {
  height: 34px; padding: 0 16px;
  background: #fff; color: #FF4D4F;
  border: 1px solid #FF4D4F; border-radius: 8px;
  font-size: 13px; cursor: pointer;
}
.btn-reset-schedule:hover { background: #FFF1F0; }
</style>
