<template>
  <div class="execution-do-page" :class="{ 'ai-active': showAi }">
    <!-- Topbar: Immersive Header -->
    <header class="exec-header">
      <div class="header-left">
        <button class="btn-icon-back" @click="handleBack" title="退出执行">
          <span class="icon">✕</span>
        </button>
        <div class="sop-meta">
          <h1 class="sop-title">{{ execution?.sopTitle || 'SOP 执行' }}</h1>
          <div class="sop-progress-summary">
            <div class="progress-track">
              <div class="progress-thumb" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="progress-text">{{ progressPercent }}% 已完成</span>
          </div>
        </div>
      </div>
      
      <div class="header-right">
        <div class="session-timer" v-if="execution?.startedAt">
          <span class="label">已用时</span>
          <span class="time">{{ formattedSessionTime }}</span>
        </div>
        <button class="btn-ai-toggle" :class="{ active: showAi }" @click="showAi = !showAi">
          <span class="icon">🤖</span>
          <span class="text">AI 助手</span>
        </button>
      </div>
    </header>

    <div class="exec-layout">
      <!-- Left: Step Navigation Sidebar -->
      <aside class="exec-sidebar">
        <div class="sidebar-header">
          <span>流程步骤</span>
          <span class="count">{{ currentStep }} / {{ totalSteps }}</span>
        </div>
        <div class="step-nav-list">
          <div 
            v-for="(s, index) in steps" 
            :key="index"
            class="nav-item"
            :class="{ 
              'active': index + 1 === currentStep,
              'completed': index + 1 < (execution?.currentStep || 1),
              'pending': index + 1 > (execution?.currentStep || 1) && index + 1 !== currentStep
            }"
            @click="index + 1 < (execution?.currentStep || 1) && navigateToStep(index + 1)"
          >
            <div class="status-indicator">
              <span v-if="index + 1 < (execution?.currentStep || 1)" class="icon-check">✓</span>
              <span v-else class="num">{{ index + 1 }}</span>
            </div>
            <div class="nav-content">
              <div class="nav-title">{{ s.title }}</div>
              <div class="nav-meta" v-if="s.duration">{{ s.duration }} min</div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Center: Main Execution Area -->
      <main class="exec-main-content">
        <div class="content-scroll-container">
          <!-- Loading Overlay -->
          <div class="loading-overlay" v-if="!stepsLoaded">
            <div class="loader"></div>
            <p>正在初始化执行实例...</p>
          </div>

          <!-- Finished View -->
          <div class="finished-view" v-else-if="isCompleted">
            <div class="celebration">
              <div class="icon">🏆</div>
              <h2>任务圆满完成！</h2>
              <p>周期性任务已归档，您已获得相应的经验值与积分奖励。</p>
            </div>
            <div class="stats-grid">
              <div class="stat-card">
                <span class="label">完成步骤</span>
                <span class="val">{{ totalSteps }}</span>
              </div>
              <div class="stat-card">
                <span class="label">实际耗时</span>
                <span class="val">{{ sessionTimeMinutes }} min</span>
              </div>
              <div class="stat-card success">
                <span class="label">周期状态</span>
                <span class="val">已达成</span>
              </div>
            </div>
            <div class="action-row">
              <button class="btn-secondary" @click="router.push('/')">返回工作台</button>
              <button class="btn-primary" @click="router.push('/execution')">查看其他任务</button>
            </div>
          </div>

          <!-- Current Step Area -->
          <div class="step-interaction-area" v-else-if="currentStepData">
            <!-- Step Heading Card -->
            <section class="step-detail-card" :class="{ 'flash': justCompleted }">
              <div class="step-head">
                <span class="step-label">当前步骤 {{ currentStep }}</span>
                <div class="step-timer" v-if="currentStepData.duration">
                  建议时长: {{ currentStepData.duration }} 分钟
                </div>
              </div>
              <h2 class="current-step-title">{{ currentStepData.title }}</h2>
              <div class="current-step-desc" v-if="currentStepData.description">
                {{ currentStepData.description }}
              </div>
            </section>

            <!-- Check Items Group -->
            <section class="interaction-group" v-if="checkItems.length">
              <div class="group-header">
                <h3>📋 核心检查项</h3>
                <span class="badge">{{ completedCheckCount }} / {{ checkItems.length }}</span>
              </div>
              <div class="check-list">
                <div 
                  v-for="(item, idx) in checkItems" 
                  :key="idx" 
                  class="check-item-row"
                  :class="{ 'is-checked': !!checkData[String(idx)] }"
                >
                  <!-- Checkbox -->
                  <template v-if="item.itemType === 'checkbox' || !item.itemType">
                    <label class="checkbox-container">
                      <input type="checkbox" v-model="checkData[String(idx)]" />
                      <span class="checkmark"></span>
                      <span class="label-text">{{ item.label }}</span>
                      <span class="required" v-if="item.isRequired">*</span>
                    </label>
                  </template>
                  
                  <!-- Other Inputs -->
                  <template v-else>
                    <div class="input-item-wrap">
                      <div class="input-header">
                        {{ item.label }} <span class="required" v-if="item.isRequired">*</span>
                      </div>
                      <input 
                        v-if="item.itemType === 'number'"
                        type="number" 
                        v-model="checkData[String(idx)]"
                        class="form-input"
                        :placeholder="item.placeholder || '请输入数值...'"
                      />
                      <textarea 
                        v-else
                        v-model="checkData[String(idx)]"
                        class="form-textarea"
                        :placeholder="item.placeholder || '请输入内容...'"
                        rows="2"
                      ></textarea>
                    </div>
                  </template>
                </div>
              </div>
            </section>

            <!-- Notes Section -->
            <section class="interaction-group">
              <div class="group-header">
                <h3>📝 执行笔记</h3>
                <div class="ai-status" v-if="currentGuidance">
                  <span class="pulse"></span> AI 专家在线
                </div>
              </div>
              <div class="notes-editor-box">
                <textarea 
                  v-model="notes" 
                  class="rich-notes-input"
                  placeholder="记录执行过程中的任何细节..."
                ></textarea>
                <div class="ai-hint-box" v-if="currentGuidance" @click="showAi = true">
                  <span class="icon">💡</span>
                  <p>{{ currentGuidance }}</p>
                </div>
              </div>
            </section>

            <!-- Action Footer -->
            <footer class="interaction-footer">
              <button class="btn-back-step" v-if="currentStep > 1" @click="prevStep">
                ← 上一步
              </button>
              <button 
                class="btn-next-step" 
                :class="{ 'is-loading': isSubmitting }"
                @click="handleComplete"
                :disabled="isSubmitting"
              >
                <span v-if="!isSubmitting">
                  {{ currentStep === totalSteps ? '🎉 提交并完成周期任务' : '完成本步并继续 →' }}
                </span>
                <span v-else class="loader-sm"></span>
              </button>
            </footer>
          </div>
        </div>
      </main>

      <!-- Right: AI Guidance Panel -->
      <aside class="exec-ai-panel" v-if="showAi">
        <SopAiPanel
          :sop-id="sopId"
          :visible-tabs="['execute']"
          :auto-fill-execute="aiAutoFill"
          @guidance-ready="onGuidanceReady"
          @notes-ready="onNotesReady"
          @close="showAi = false"
        />
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 周期性 SOP 实例执行控制台
 * 适配“座舱式”交互，支持复杂的检查项与 AI 协同
 */
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'
import type { Execution, Sop, StepData, CheckItem, ApiResponse } from '@/types'
import SopAiPanel from '@/components/ai/SopAiPanel.vue'

const route = useRoute()
const router = useRouter()
const instanceId = Number(route.params.id)

const execution = ref<Execution | null>(null)
const sop = ref<Sop | null>(null)
const steps = ref<StepData[]>([])
const currentStep = ref(1)
const notes = ref('')
const isSubmitting = ref(false)
const justCompleted = ref(false)
const stepsLoaded = ref(false)
const showAi = ref(false)
const currentGuidance = ref('')
const stepGuidanceHistory = ref<Record<number, any>>({})

// Session Timer
const sessionSeconds = ref(0)
const formattedSessionTime = ref('00:00')
const sessionTimeMinutes = ref(0)
let timer: any = null

const startTimer = () => {
  timer = setInterval(() => {
    sessionSeconds.value++
    const mins = Math.floor(sessionSeconds.value / 60)
    const secs = sessionSeconds.value % 60
    formattedSessionTime.value = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    sessionTimeMinutes.value = mins
  }, 1000)
}

const sopId = computed(() => execution.value?.sopId)
const aiAutoFill = computed(() => {
  if (!currentStepData.value) return undefined
  return {
    stepTitle: currentStepData.value.title || '',
    stepDescription: currentStepData.value.description || '',
    stepIndex: currentStep.value,
    totalSteps: totalSteps.value,
  }
})

const totalSteps = computed(() => steps.value.length || 0)
const currentStepData = computed(() => steps.value[currentStep.value - 1])
const progressPercent = computed(() => totalSteps.value ? Math.round((currentStep.value / totalSteps.value) * 100) : 0)
const isCompleted = computed(() => execution.value?.status === 'completed')

const checkData = ref<Record<string, any>>({})
const completedCheckCount = computed(() => {
  return checkItems.value.filter((_: CheckItem, idx: number) => {
    const val = checkData.value[String(idx)]
    return val === true || (typeof val === 'string' && val.trim())
  }).length
})

const checkItems = computed(() => {
  if (!currentStepData.value) return []
  const ci = currentStepData.value.checkItems
  if (!ci) return []
  if (Array.isArray(ci)) return ci
  try { return JSON.parse(ci) } catch { return [] }
})

const prevStep = () => {
  if (currentStep.value > 1) navigateToStep(currentStep.value - 1)
}

function navigateToStep(n: number) {
  if (n >= currentStep.value) return
  currentStep.value = n
  if (stepGuidanceHistory.value[n]) {
    currentGuidance.value = stepGuidanceHistory.value[n]
  }
}

function onGuidanceReady(guidance: string) {
  currentGuidance.value = guidance
  stepGuidanceHistory.value[currentStep.value] = guidance
}

const onNotesReady = (n: string) => { notes.value = n }

const handleBack = () => {
  if (!isCompleted.value) {
    ElMessageBox.confirm('确定要退出执行吗？进度将自动保存。', '提示', {
      type: 'warning'
    }).then(() => router.push('/execution'))
  } else {
    router.push('/execution')
  }
}

const handleComplete = async () => {
  // Validate
  for (let i = 0; i < checkItems.value.length; i++) {
    if (checkItems.value[i].isRequired && !checkData.value[String(i)]) {
      ElMessage.warning(`请完成必填项：${checkItems.value[i].label}`)
      return
    }
  }

  isSubmitting.value = true
  try {
    const dataMap: Record<string, any> = {}
    Object.entries(checkData.value).forEach(([k, v]) => { dataMap[k] = { value: v } })

    const res = await request.post<any, ApiResponse<any>>(`/instance/${instanceId}/steps/${currentStep.value}/complete`, {
      notes: notes.value,
      checkData: dataMap,
      guidance: currentGuidance.value || null
    })

    if (res.code === 200) {
      justCompleted.value = true
      setTimeout(() => justCompleted.value = false, 600)

      if (currentStep.value >= totalSteps.value) {
        if (execution.value) execution.value.status = 'completed'
        ElMessage.success('🎉 SOP 全部执行完成！')
      } else {
        currentStep.value++
        notes.value = ''
        checkData.value = {}
        currentGuidance.value = ''
        ElMessage.success('步骤已确认')
      }
    }
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  startTimer()
  try {
    const res: any = await request.get(`/instance/${instanceId}`)
    if (res.code === 200) {
      const instData = res.data?.instance || res.data
      execution.value = instData
      currentStep.value = instData.currentStep || 1
      
      const sopRes: any = await request.get(`/sop/${instData.sopId}`)
      if (sopRes.code === 200) {
        sop.value = sopRes.data
        execution.value!.sopTitle = sop.value!.title
        steps.value = JSON.parse(sop.value!.content || '[]')
        stepsLoaded.value = true
      }
    }
  } catch (e) {
    ElMessage.error('数据加载失败')
  }
})

onBeforeUnmount(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.execution-do-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--color-bg-base);
  color: var(--color-text-primary);
  overflow: hidden;
}

.exec-header {
  height: 64px;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
}

.header-left { display: flex; align-items: center; gap: 20px; flex: 1; }
.btn-icon-back {
  width: 40px; height: 40px; border-radius: 10px;
  border: 1px solid var(--color-border); background: var(--color-bg-surface);
  color: var(--color-text-secondary); display: flex; align-items: center;
  justify-content: center; font-size: 18px; cursor: pointer;
}

.sop-meta { display: flex; flex-direction: column; gap: 4px; }
.sop-title { font-size: 16px; font-weight: 700; margin: 0; }
.sop-progress-summary { display: flex; align-items: center; gap: 10px; }
.progress-track { width: 120px; height: 6px; background: var(--color-bg-surface); border-radius: 3px; overflow: hidden; }
.progress-thumb { height: 100%; background: var(--color-success); transition: width 0.4s; }
.progress-text { font-size: 11px; color: var(--color-text-muted); }

.header-right { display: flex; align-items: center; gap: 20px; }
.session-timer { display: flex; flex-direction: column; align-items: flex-end; }
.session-timer .label { font-size: 10px; color: var(--color-text-muted); }
.session-timer .time { font-family: monospace; font-size: 16px; font-weight: 700; color: var(--color-primary); }

.btn-ai-toggle {
  display: flex; align-items: center; gap: 8px; padding: 8px 16px;
  border-radius: 20px; border: 1px solid var(--color-primary);
  background: var(--color-primary-subtle); color: var(--color-primary);
  font-size: 13px; font-weight: 600; cursor: pointer;
}
.btn-ai-toggle.active { background: var(--color-primary); color: white; }

.exec-layout { display: flex; flex: 1; overflow: hidden; }

.exec-sidebar {
  width: 260px; background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  display: flex; flex-direction: column;
}
.sidebar-header { padding: 20px; font-size: 13px; font-weight: 700; color: var(--color-text-muted); display: flex; justify-content: space-between; border-bottom: 1px solid var(--color-border); }
.step-nav-list { flex: 1; overflow-y: auto; padding: 10px 0; }
.nav-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 20px;
  cursor: pointer; transition: all 0.2s; border-left: 3px solid transparent;
}
.nav-item.active { background: var(--color-primary-subtle); border-left-color: var(--color-primary); }
.nav-item.completed { opacity: 0.7; }
.nav-item.pending { opacity: 0.4; }

.status-indicator {
  width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--color-border);
  display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700;
}
.completed .status-indicator { background: var(--color-success); border-color: var(--color-success); color: white; }
.active .status-indicator { border-color: var(--color-primary); color: var(--color-primary); background: white; }

.nav-title { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.nav-meta { font-size: 10px; color: var(--color-text-muted); }

.exec-main-content { flex: 1; background: var(--color-bg-base); position: relative; }
.content-scroll-container { height: 100%; overflow-y: auto; padding: 40px; }

.step-interaction-area { max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: var(--space-xl); }

.step-detail-card {
  background: var(--color-bg-elevated); border-radius: var(--radius-lg); padding: var(--space-3xl);
  border: 1px solid var(--color-border); box-shadow: var(--shadow-md);
  transition: all 0.3s;
}
.step-detail-card.flash { border-color: var(--color-success); background: var(--color-success-subtle); }
.step-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.step-label { font-size: 12px; font-weight: 700; color: var(--color-primary); text-transform: uppercase; }
.step-timer { font-size: 12px; color: var(--color-text-muted); background: var(--color-bg-surface); padding: 4px 12px; border-radius: 20px; }
.current-step-title { font-size: 24px; font-weight: 800; margin: 0 0 16px 0; }
.current-step-desc { font-size: 15px; color: var(--color-text-secondary); line-height: 1.6; }

.interaction-group { background: var(--color-bg-elevated); border-radius: var(--radius-lg); padding: var(--space-2xl); border: 1px solid var(--color-border); }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.group-header h3 { font-size: 16px; font-weight: 700; margin: 0; }
.group-header .badge { background: var(--color-bg-surface); padding: 2px 10px; border-radius: 12px; font-size: 12px; color: var(--color-primary); }

.check-list { display: flex; flex-direction: column; gap: 12px; }
.check-item-row { padding: 16px; border-radius: 12px; background: var(--color-bg-surface); border: 1.5px solid transparent; }
.check-item-row.is-checked { border-color: var(--color-success-subtle); }

.checkbox-container { display: flex; align-items: center; gap: 14px; cursor: pointer; }
.checkbox-container input { display: none; }
.checkmark { width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--color-border); display: flex; align-items: center; justify-content: center; }
.checkbox-container input:checked + .checkmark { background: var(--color-success); border-color: var(--color-success); }
.checkbox-container input:checked + .checkmark::after { content: '✓'; color: white; font-weight: 800; }

.rich-notes-input { width: 100%; min-height: 100px; background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: 12px; padding: 16px; color: var(--color-text-primary); resize: none; }

.ai-hint-box { background: linear-gradient(135deg, #e0e7ff, #f3e8ff); border-radius: 12px; padding: 12px 16px; display: flex; gap: 12px; border: 1px solid #c7d2fe; margin-top: 12px; }
.ai-hint-box p { font-size: 13px; color: #4338ca; margin: 0; font-style: italic; }

.interaction-footer { display: flex; gap: 16px; margin-top: 20px; }
.btn-next-step { flex: 1; height: 56px; border-radius: 16px; border: none; background: linear-gradient(135deg, var(--color-primary), #7c3aed); color: white; font-size: 16px; font-weight: 700; cursor: pointer; box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3); }
.btn-back-step { padding: 0 24px; border-radius: 16px; border: 1px solid var(--color-border); background: var(--color-bg-elevated); color: var(--color-text-secondary); font-weight: 600; }

.exec-ai-panel { width: 380px; background: var(--color-bg-elevated); border-left: 1px solid var(--color-border); }

.finished-view { text-align: center; max-width: 600px; margin: 0 auto; }
.celebration .icon { font-size: 80px; margin-bottom: 20px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 40px 0; }
.stat-card { background: var(--color-bg-elevated); padding: 24px; border-radius: 20px; border: 1px solid var(--color-border); }
.stat-card .label { font-size: 11px; color: var(--color-text-muted); display: block; margin-bottom: 8px; }
.stat-card .val { font-size: 24px; font-weight: 800; }

.loading-overlay { text-align: center; padding-top: 100px; }
.loader { width: 40px; height: 40px; border: 4px solid var(--color-bg-surface); border-top-color: var(--color-primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
