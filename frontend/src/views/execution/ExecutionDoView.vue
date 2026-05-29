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
              'completed': index + 1 < currentStep,
              'pending': index + 1 > currentStep
            }"
            @click="index + 1 < currentStep && navigateToStep(index + 1)"
          >
            <div class="status-indicator">
              <span v-if="index + 1 < currentStep" class="icon-check">✓</span>
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
            <p>正在同步流程数据...</p>
          </div>

          <!-- Finished View -->
          <div class="finished-view" v-else-if="isCompleted">
            <div class="celebration">
              <div class="icon">🏆</div>
              <h2>任务圆满完成！</h2>
              <p>本次 SOP 执行已全部结束，数据已同步至组织中心。</p>
            </div>
            <div class="stats-grid">
              <div class="stat-card">
                <span class="label">总步骤</span>
                <span class="val">{{ totalSteps }}</span>
              </div>
              <div class="stat-card">
                <span class="label">实际耗时</span>
                <span class="val">{{ sessionTimeMinutes }} min</span>
              </div>
              <div class="stat-card success">
                <span class="label">执行状态</span>
                <span class="val">100% 合规</span>
              </div>
            </div>
            <div class="action-row">
              <button class="btn-secondary" @click="router.push('/')">返回工作台</button>
              <button class="btn-primary" @click="router.push('/execution')">查看所有记录</button>
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
                  
                  <!-- Text/Number Input -->
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

            <!-- Notes & AI Section -->
            <section class="interaction-group">
              <div class="group-header">
                <h3>📝 执行笔记</h3>
                <div class="ai-status" v-if="currentGuidance">
                  <span class="pulse"></span> 实时 AI 指导中
                </div>
              </div>
              <div class="notes-editor-box">
                <textarea 
                  v-model="notes" 
                  class="rich-notes-input"
                  placeholder="记录任何异常、心得或操作记录..."
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
                  {{ currentStep === totalSteps ? '🎉 完成所有流程' : '完成本步并继续 →' }}
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
 * 沉浸式 SOP 执行控制台
 * 经过审美重塑的 UI，提供类似“飞行座舱”的操作体验
 */
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/api'
import type { Execution, Sop, ApiResponse } from '@/types'
import SopAiPanel from '@/components/ai/SopAiPanel.vue'
import { useExecutionStep } from '@/composables/useExecutionStep'

const sessionSeconds = ref(0)
let timer: any = null

const formattedSessionTime = ref('00:00')
const sessionTimeMinutes = ref(0)

const {
  router,
  entityId,

  execution,
  steps,
  currentStep,
  notes,
  checkData,
  isSubmitting,
  justCompleted,
  stepsLoaded,
  showAi,
  currentGuidance,

  sopId,
  totalSteps,
  currentStepData,
  progressPercent,
  isCompleted,
  aiAutoFill,
  checkItems,
  completedCheckCount,

  navigateToStep,
  prevStep,
  onGuidanceReady,
  onNotesReady,
  handleBack,
  handleComplete,
} = useExecutionStep({
  logLabel: 'ExecutionImmersive',

  buildCompleteUrl: (id, step) => `/execution/${id}/step/${step}`,
  buildRefreshUrl: (id) => `/execution/${id}`,
  parseRefreshResponse: (data) => data as Execution,

  fetchInitial: async () => {
    const id = entityId
    const [execRes, sopRes] = await Promise.all([
      request.get<unknown, ApiResponse<Execution>>(`/execution/${id}`),
      request.get<unknown, ApiResponse<Sop>>(`/execution/${id}/sop`),
    ])

    let exec: Execution | null = null
    let sopTitle = ''

    if (execRes?.code === 200) {
      exec = execRes.data
      if (exec && exec.status === 'pending') {
        await request.post(`/execution/${id}/activate`)
        exec.status = 'in_progress'
        exec.currentStep = 1
      }
    }

    let parsedSteps: import('@/types').StepData[] = []
    let fetchedSopId = 0

    if (sopRes?.code === 200 && sopRes.data) {
      fetchedSopId = sopRes.data.id
      sopTitle = sopRes.data.title
      try {
        const raw = sopRes.data.content
        parsedSteps = (raw && raw !== 'null' && raw !== 'undefined') ? JSON.parse(raw) : []
      } catch { parsedSteps = [] }
    }

    return {
      execution: exec,
      steps: parsedSteps,
      sopId: fetchedSopId,
      sopTitle,
      initialNotes: '',
      guidanceHistory: {},
    }
  },
})

// Timer logic
const startTimer = () => {
  timer = setInterval(() => {
    sessionSeconds.value++
    const mins = Math.floor(sessionSeconds.value / 60)
    const secs = sessionSeconds.value % 60
    formattedSessionTime.value = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
    sessionTimeMinutes.value = mins
  }, 1000)
}

onMounted(() => {
  startTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
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

/* Header */
.exec-header {
  height: 64px;
  background: var(--color-bg-elevated);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 100;
  box-shadow: var(--shadow-sm);
}

.header-left { display: flex; align-items: center; gap: 20px; flex: 1; }
.btn-icon-back {
  width: 40px; height: 40px; border-radius: 10px;
  border: 1px solid var(--color-border); background: var(--color-bg-surface);
  color: var(--color-text-secondary); display: flex; align-items: center;
  justify-content: center; font-size: 18px; cursor: pointer; transition: all 0.2s;
}
.btn-icon-back:hover { background: var(--color-error-subtle); color: var(--color-error); border-color: var(--color-error); }

.sop-meta { display: flex; flex-direction: column; gap: 4px; }
.sop-title { font-size: 16px; font-weight: 700; margin: 0; }
.sop-progress-summary { display: flex; align-items: center; gap: 10px; }
.progress-track { width: 120px; height: 6px; background: var(--color-bg-surface); border-radius: 3px; overflow: hidden; }
.progress-thumb { height: 100%; background: var(--color-success); border-radius: 3px; transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
.progress-text { font-size: 11px; color: var(--color-text-muted); font-weight: 500; }

.header-right { display: flex; align-items: center; gap: 20px; }
.session-timer { display: flex; flex-direction: column; align-items: flex-end; }
.session-timer .label { font-size: 10px; color: var(--color-text-muted); text-transform: uppercase; }
.session-timer .time { font-family: monospace; font-size: 16px; font-weight: 700; color: var(--color-primary); }

.btn-ai-toggle {
  display: flex; align-items: center; gap: 8px; padding: 8px 16px;
  border-radius: 20px; border: 1px solid var(--color-primary);
  background: var(--color-primary-subtle); color: var(--color-primary);
  font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.btn-ai-toggle.active { background: var(--color-primary); color: white; }

/* Main Layout */
.exec-layout { display: flex; flex: 1; overflow: hidden; }

/* Sidebar */
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
.nav-item.pending { opacity: 0.5; cursor: not-allowed; }

.status-indicator {
  width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--color-border);
  display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; flex-shrink: 0;
}
.active .status-indicator { border-color: var(--color-primary); color: var(--color-primary); background: white; }
.completed .status-indicator { background: var(--color-success); border-color: var(--color-success); color: white; }

.nav-content { overflow: hidden; }
.nav-title { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.nav-meta { font-size: 10px; color: var(--color-text-muted); }

/* Main Content Area */
.exec-main-content { flex: 1; background: var(--color-bg-base); position: relative; }
.content-scroll-container { height: 100%; overflow-y: auto; padding: var(--space-3xl); }

.step-interaction-area { max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: var(--space-xl); }

/* Step Card */
.step-detail-card {
  background: var(--color-bg-elevated); border-radius: var(--radius-lg); padding: var(--space-3xl);
  border: 1px solid var(--color-border); box-shadow: var(--shadow-md);
  transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.step-detail-card.flash { transform: scale(1.02); border-color: var(--color-success); }
.step-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.step-label { font-size: 12px; font-weight: 700; color: var(--color-primary); text-transform: uppercase; letter-spacing: 1px; }
.step-timer { font-size: 12px; color: var(--color-text-muted); background: var(--color-bg-surface); padding: 4px 12px; border-radius: 20px; }
.current-step-title { font-size: 24px; font-weight: 800; margin: 0 0 16px 0; line-height: 1.3; }
.current-step-desc { font-size: 15px; color: var(--color-text-secondary); line-height: 1.6; }

/* Groups */
.interaction-group { background: var(--color-bg-elevated); border-radius: var(--radius-lg); padding: var(--space-2xl); border: 1px solid var(--color-border); }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.group-header h3 { font-size: 16px; font-weight: 700; margin: 0; }
.group-header .badge { background: var(--color-bg-surface); padding: 2px 10px; border-radius: 12px; font-size: 12px; font-weight: 600; color: var(--color-primary); }

.check-list { display: flex; flex-direction: column; gap: 12px; }
.check-item-row { 
  padding: 16px; border-radius: 12px; background: var(--color-bg-surface); 
  border: 1.5px solid transparent; transition: all 0.2s;
}
.check-item-row.is-checked { border-color: var(--color-success-subtle); background: var(--color-bg-elevated); }

.checkbox-container { display: flex; align-items: center; gap: 14px; cursor: pointer; font-size: 15px; width: 100%; }
.checkbox-container input { display: none; }
.checkmark { 
  width: 24px; height: 24px; border-radius: 50%; border: 2px solid var(--color-border);
  display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.checkbox-container input:checked + .checkmark { background: var(--color-success); border-color: var(--color-success); }
.checkbox-container input:checked + .checkmark::after { content: '✓'; color: white; font-size: 14px; font-weight: 800; }
.label-text { flex: 1; font-weight: 500; }
.required { color: var(--color-error); margin-left: 4px; }

.input-header { font-size: 14px; font-weight: 600; margin-bottom: 10px; }
.form-input, .form-textarea {
  width: 100%; background: var(--color-bg-elevated); border: 1px solid var(--color-border);
  border-radius: 10px; padding: 12px; color: var(--color-text-primary); outline: none;
}
.form-input:focus, .form-textarea:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px var(--color-primary-subtle); }

.notes-editor-box { display: flex; flex-direction: column; gap: 12px; }
.rich-notes-input {
  width: 100%; min-height: 100px; background: var(--color-bg-surface); border: 1px solid var(--color-border);
  border-radius: 12px; padding: 16px; font-size: 14px; resize: none; color: var(--color-text-primary);
}
.ai-hint-box {
  background: linear-gradient(135deg, #e0e7ff, #f3e8ff); border-radius: 12px; padding: 12px 16px;
  display: flex; gap: 12px; cursor: pointer; border: 1px solid #c7d2fe; transition: transform 0.2s;
}
.ai-hint-box:hover { transform: scale(1.01); }
.ai-hint-box .icon { font-size: 18px; }
.ai-hint-box p { font-size: 13px; color: #4338ca; margin: 0; line-height: 1.5; font-style: italic; }

/* Footer Actions */
.interaction-footer { display: flex; gap: 16px; margin-top: 20px; }
.btn-next-step {
  flex: 1; height: 56px; border-radius: 16px; border: none;
  background: linear-gradient(135deg, var(--color-primary), #7c3aed);
  color: white; font-size: 16px; font-weight: 700; cursor: pointer;
  box-shadow: 0 8px 20px rgba(79, 70, 229, 0.3); transition: all 0.2s;
}
.btn-next-step:hover { transform: translateY(-2px); box-shadow: 0 12px 25px rgba(79, 70, 229, 0.4); }
.btn-back-step {
  padding: 0 24px; border-radius: 16px; border: 1px solid var(--color-border);
  background: var(--color-bg-elevated); color: var(--color-text-secondary);
  font-weight: 600; cursor: pointer;
}

/* AI Panel */
.exec-ai-panel { width: 380px; background: var(--color-bg-elevated); border-left: 1px solid var(--color-border); }

/* Animations */
.loader-sm { width: 24px; height: 24px; border: 3px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Finished View */
.celebration { text-align: center; margin-bottom: 40px; }
.celebration .icon { font-size: 80px; margin-bottom: 20px; }
.celebration h2 { font-size: 32px; font-weight: 800; margin-bottom: 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 40px; }
.stat-card { background: var(--color-bg-elevated); padding: 24px; border-radius: 20px; border: 1px solid var(--color-border); text-align: center; }
.stat-card.success { border-color: var(--color-success); background: var(--color-success-subtle); }
.stat-card .label { font-size: 12px; color: var(--color-text-muted); text-transform: uppercase; margin-bottom: 8px; display: block; }
.stat-card .val { font-size: 24px; font-weight: 800; }
.action-row { display: flex; justify-content: center; gap: 16px; }
</style>
