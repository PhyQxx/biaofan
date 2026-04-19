<template>
  <div class="execution-do-page">
      <!-- 顶部：顶栏 + 进度条（通栏 sticky） -->
    <div class="exec-header">
      <div class="exec-topbar">
        <button class="exec-back" @click="handleBack">←</button>
        <div class="exec-title">{{ execution?.sopTitle || 'SOP 执行' }}</div>
        <div class="exec-steps-count" v-if="totalSteps">{{ currentStep }}/{{ totalSteps }}</div>
      </div>
      <div class="step-progress-wrap" v-if="totalSteps">
        <div class="step-dots-row">
          <div
            v-for="n in totalSteps"
            :key="n"
            class="step-dot"
            :class="{
              'done': n < currentStep,
              'active': n === currentStep,
              'future': n > currentStep
            }"
            @click="n < currentStep && navigateToStep(n)"
          >
            <span class="dot-inner" v-if="n < currentStep">✓</span>
            <span class="dot-num" v-else>{{ n }}</span>
          </div>
        </div>
        <div class="step-progress-bar">
          <div class="step-progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <div class="step-progress-meta">
          <span class="progress-percent">{{ progressPercent }}%</span>
          <span class="progress-remaining" v-if="currentStep < totalSteps">剩余 {{ totalSteps - currentStep }} 步</span>
          <span class="progress-remaining success" v-else>即将完成</span>
        </div>
      </div>
    </div>

    <!-- 主体：左右布局 -->
    <div class="exec-body">
      <div class="exec-content">
        <!-- Loading -->
        <div class="loading-state" v-if="!stepsLoaded">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>

        <!-- Step Content -->
        <div class="step-main-card" :class="{ 'just-completed': justCompleted }" v-else-if="currentStepData && !isCompleted">
          <div class="step-header-row">
            <span class="step-badge">步骤 {{ currentStep }}</span>
            <span class="step-duration" v-if="currentStepData.duration">⏱️ {{ currentStepData.duration }} 分钟</span>
          </div>
          <h2 class="step-title">{{ currentStepData.title }}</h2>
          <p class="step-desc" v-if="currentStepData.description">{{ currentStepData.description }}</p>
        </div>

        <!-- Next Step Preview -->
        <div class="next-step-preview" v-if="currentStep < totalSteps && steps[currentStep]">
          <div class="next-label">
            <span class="next-arrow">↑</span>
            下一步预览
          </div>
          <div class="next-title">{{ steps[currentStep].title }}</div>
        </div>

        <!-- Check Items -->
        <div class="check-section" v-if="checkItems.length">
          <div class="check-section-header">
            <span class="check-section-title">📋 检查项</span>
            <span class="check-progress-tag">
              {{ completedCheckCount }}/{{ checkItems.length }} 已完成
            </span>
          </div>
          <div v-for="(item, idx) in checkItems" :key="idx" class="check-item">
            <!-- Checkbox type -->
            <div v-if="item.itemType === 'checkbox' || !item.itemType" class="check-row">
              <label class="check-label" :class="{ 'is-done': checkData[String(idx)] }">
                <div class="custom-checkbox" :class="{ checked: checkData[String(idx)] }">
                  <span v-if="checkData[String(idx)]">✓</span>
                </div>
                <span class="check-text">{{ item.label }}</span>
                <span class="required-dot" v-if="item.isRequired">*</span>
              </label>
            </div>
            <!-- Text type -->
            <div v-else-if="item.itemType === 'text'" class="check-text-row">
              <div class="check-label-row">
                {{ item.label }}
                <span class="required-dot" v-if="item.isRequired">*</span>
              </div>
              <textarea
                v-model="checkData[String(idx)]"
                class="check-text-input"
                :class="{ 'has-value': checkData[String(idx)] }"
                :placeholder="item.placeholder || '请输入...'"
                rows="2"
              ></textarea>
            </div>
            <!-- Number type -->
            <div v-else-if="item.itemType === 'number'" class="check-text-row">
              <div class="check-label-row">
                {{ item.label }}
                <span class="required-dot" v-if="item.isRequired">*</span>
              </div>
              <input
                type="number"
                v-model="checkData[String(idx)]"
                class="check-num-input"
                :class="{ 'has-value': checkData[String(idx)] !== undefined && checkData[String(idx)] !== '' }"
                :placeholder="item.placeholder || '请输入数字'"
              />
            </div>
            <!-- Date type -->
            <div v-else-if="item.itemType === 'date'" class="check-text-row">
              <div class="check-label-row">
                {{ item.label }}
                <span class="required-dot" v-if="item.isRequired">*</span>
              </div>
              <input type="date" v-model="checkData[String(idx)]" class="check-text-input" :class="{ 'has-value': checkData[String(idx)] }" />
            </div>
            <!-- Select type -->
            <div v-else-if="item.itemType === 'select'" class="check-text-row">
              <div class="check-label-row">
                {{ item.label }}
                <span class="required-dot" v-if="item.isRequired">*</span>
              </div>
              <select v-model="checkData[String(idx)]" class="check-select-input" :class="{ 'has-value': checkData[String(idx)] }">
                <option value="">请选择...</option>
                <option v-for="opt in (item.options || [])" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>
          </div>
        </div>

        <!-- No check items hint -->
        <div class="no-check-hint" v-else-if="stepsLoaded && !isCompleted">
          <span>此步骤无检查项，可直接提交</span>
        </div>

        <!-- Notes -->
        <div class="notes-section">
          <div class="notes-label">📝 执行笔记 <span class="optional-hint">（选填）</span>
            <span v-if="currentGuidance" class="guidance-badge">✓ 含AI指导</span>
          </div>
          <textarea
            v-if="notesEditing"
            v-model="notes"
            class="notes-input"
            :class="{ 'has-value': notes }"
            placeholder="记录执行过程中的备注、问题或心得..."
            @blur="notesEditing = false"
            ref="notesTextareaRef"
          ></textarea>
          <div
            v-else
            class="notes-preview"
            :class="{ 'has-value': notes, 'empty': !notes }"
            @click="notesEditing = true"
          >
            <span v-if="notes" v-html="marked.parse(notes)"></span>
            <span v-else class="placeholder">记录执行过程中的备注、问题或心得...</span>
          </div>
          <div v-if="currentGuidance" class="guidance-history-hint">
              📋 本步AI指导：{{ currentGuidance.substring(0, 60) }}{{ currentGuidance.length > 60 ? '...' : '' }}
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="step-actions" v-if="!isCompleted">
          <button v-if="currentStep > 1" class="btn-secondary" @click="prevStep">
            ← 上一步
          </button>
          <button
            class="btn-primary flex-1"
            @click="handleComplete"
            :disabled="isSubmitting"
            :class="{ 'is-submitting': isSubmitting }"
          >
            <span v-if="isSubmitting" class="btn-spinner"></span>
            <span v-else>{{ currentStep >= totalSteps ? '✓ 完成 SOP' : '完成本步 →' }}</span>
          </button>
        </div>

        <!-- Empty SOP State -->
        <div class="empty-state" v-if="stepsLoaded && totalSteps === 0">
          <div class="empty-icon">📋</div>
          <h2>暂无步骤</h2>
          <p>当前 SOP 还没有添加步骤，请先编辑 SOP 添加执行步骤</p>
          <button class="btn-primary" @click="router.push(`/sop/${route.params.id}/edit`)">去编辑 SOP</button>
        </div>

        <!-- Completed State -->
        <div class="complete-state" v-else-if="isCompleted">
          <div class="complete-icon">🎉</div>
          <h2>执行完成！</h2>
          <p>恭喜你完成了本次 SOP 执行</p>
          <div class="complete-stats">
            <div class="complete-stat">
              <div class="cs-num">{{ totalSteps }}</div>
              <div class="cs-label">总步骤</div>
            </div>
            <div class="complete-stat">
              <div class="cs-num success">{{ totalSteps }}</div>
              <div class="cs-label">已完成</div>
            </div>
            <div class="complete-stat">
              <div class="cs-num primary">{{ Math.round((Date.now() - new Date(execution?.startedAt ?? '').getTime()) / 60000) }}</div>
              <div class="cs-label">用时(分钟)</div>
            </div>
          </div>
          <div class="complete-actions">
            <button class="btn-secondary" @click="router.push('/')">返回首页</button>
            <button class="btn-primary" @click="router.push('/execution')">继续其他 SOP</button>
          </div>
        </div>
      </div>

      <!-- AI 助手面板 - 执行指导（常驻） -->
      <div class="ai-panel-wrapper">
        <SopAiPanel
          :sop-id="sopId"
          :visible-tabs="['execute']"
          :auto-fill-execute="aiAutoFill"
          @guidance-ready="onGuidanceReady"
          @notes-ready="onNotesReady"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端周期实例执行页
 * - 类似 ExecutionDoView，但针对周期实例
 * - 激活实例 -> 逐步执行步骤 -> 完成
 */
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
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
const notesEditing = ref(false)
const notesTextareaRef = ref<HTMLTextAreaElement | null>(null)
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const checkData = ref<Record<string, any>>({})
const isSubmitting = ref(false)
const justCompleted = ref(false)
const completionTimeout = ref<ReturnType<typeof setTimeout> | null>(null)
const stepsLoaded = ref(false)
const currentGuidance = ref('')
const stepGuidanceHistory = ref<Record<number, any>>({})

// 自动聚焦 notes textarea
watch(notesEditing, (editing) => {
  if (editing) {
    nextTick(() => notesTextareaRef.value?.focus())
  }
})

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

const completedCheckCount = computed(() => {
  return checkItems.value.filter((_: CheckItem, idx: number) => {
    const val = checkData.value[String(idx)]
    if (val === true || (typeof val === 'string' && val.trim())) return true
    return false
  }).length
})

const checkItems = computed(() => {
  if (!currentStepData.value) return []
  const ci = currentStepData.value.checkItems
  if (!ci) return []
  if (Array.isArray(ci)) return ci
  try { return (ci && ci !== 'null' && ci !== 'undefined') ? JSON.parse(ci) : [] } catch { return [] }
})

const prevStep = () => {
  if (currentStep.value > 1) navigateToStep(currentStep.value - 1)
}

function navigateToStep(n: number) {
  if (n >= currentStep.value) return
  if (currentGuidance.value) {
    stepGuidanceHistory.value[currentStep.value] = currentGuidance.value
  }
  currentGuidance.value = ''
  notes.value = ''
  checkData.value = {}
  currentStep.value = n
  if (stepGuidanceHistory.value[n]) {
    currentGuidance.value = stepGuidanceHistory.value[n]
  }
}

function onGuidanceReady(guidance: string) {
  currentGuidance.value = guidance
  stepGuidanceHistory.value[currentStep.value] = guidance
}

const handleBack = () => {
  if (currentStep.value > 1 && !isCompleted.value) {
    ElMessageBox.confirm('当前步骤尚未完成，确定要退出吗？退出后进度将保留。', '确认退出', {
      confirmButtonText: '确定退出',
      cancelButtonText: '继续执行',
      type: 'warning',
    }).then(() => {
      router.push('/execution')
    }).catch(() => {}) // ignore — user cancelled confirm dialog
  } else {
    router.push('/execution')
  }
}

const handleComplete = () => {
  // Validate required check items
  for (let i = 0; i < checkItems.value.length; i++) {
    const item = checkItems.value[i]
    if (item.isRequired) {
      const val = checkData.value[String(i)]
      if (val === undefined || val === null || val === '' || val === false) {
        ElMessage.warning(`请完成检查项：${item.label}`)
        return
      }
    }
  }

  const isLastStep = currentStep.value >= totalSteps.value
  const confirmMsg = isLastStep
    ? `确定要完成 SOP「${execution.value?.sopTitle}」吗？完成后将无法再修改执行记录。`
    : `确定要完成步骤 ${currentStep.value}「${currentStepData.value?.title}」吗？`

  ElMessageBox.confirm(confirmMsg, isLastStep ? '确认完成 SOP' : '确认完成步骤', {
    confirmButtonText: isLastStep ? '✓ 完成 SOP' : '✓ 完成本步',
    cancelButtonText: '取消',
    type: 'info',
  }).then(() => {
    completeStep()
  }).catch(() => {}) // ignore — user cancelled confirm dialog
}

const completeStep = async () => {
  isSubmitting.value = true
  try {
    const dataMap: Record<string, unknown> = {}
    for (const [k, v] of Object.entries(checkData.value)) {
      dataMap[k] = { value: v }
    }

    const res = await request.post<unknown, ApiResponse<{ completed?: boolean }>>(`/instance/${instanceId}/steps/${currentStep.value}/complete`, {
      notes: notes.value,
      checkData: dataMap,
      guidance: currentGuidance.value || null,
    })

    if (res?.code === 200) {
      // H-17: Re-fetch execution status from server after step submission
      try {
        const instRes = await request.get<unknown, ApiResponse<{ instance?: Execution }>>(`/instance/${instanceId}`)
        if (instRes?.code === 200) {
          const instData = (instRes.data?.instance || instRes.data) as Execution
          if (instData) execution.value = instData
        }
      } catch (e) {
        console.error('[InstanceDoView] re-fetch instance status failed:', e)
      }

      // Flash animation — clear any existing timeout to prevent stale resets
      justCompleted.value = true
      if (completionTimeout.value) clearTimeout(completionTimeout.value)
      completionTimeout.value = setTimeout(() => {
        justCompleted.value = false
        completionTimeout.value = null
      }, 600)

      const completed = res.data?.completed || execution.value?.status === 'completed'
      if (completed || currentStep.value >= totalSteps.value) {
        if (execution.value) execution.value.status = 'completed'
        ElMessage.success('🎉 SOP 执行完成！')
      } else {
        if (currentGuidance.value) {
          stepGuidanceHistory.value[currentStep.value] = currentGuidance.value
        }
        currentGuidance.value = ''
        currentStep.value = execution.value?.currentStep || currentStep.value + 1
        notes.value = ''
        checkData.value = {}
        ElMessage({ message: '✓ 步骤完成，已自动进入下一步', type: 'success', duration: 2000 })
      }
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '操作失败'
    ElMessage.error(msg)
  } finally {
    isSubmitting.value = false
  }
}

const onNotesReady = (note: string) => {
  notes.value = note
}

onBeforeUnmount(() => {
  if (completionTimeout.value) {
    clearTimeout(completionTimeout.value)
    completionTimeout.value = null
  }
})

onMounted(async () => {
  try {
    const instRes = await request.get<unknown, ApiResponse<{ instance?: Execution } & Execution>>(`/instance/${instanceId}`)

    let sopId = 0
    if (instRes?.code === 200) {
      const instData = instRes.data?.instance || instRes.data
      if (instData) {
        execution.value = instData
        execution.value.sopTitle = ''
        currentStep.value = instData.currentStep || 1
        sopId = instData.sopId

        if (instData.status === 'pending') {
          await request.post(`/instance/${instanceId}/activate`)
          execution.value.status = 'in_progress'
          execution.value.currentStep = 1
          currentStep.value = 1
        }
      }
    }

    if (sopId) {
      // 优先使用 /instance/{id} 接口直接返回的 sop 数据，避免重复请求
      const instSop = (instRes.data as any)?.sop
      if (instSop && instSop.id) {
        sop.value = instSop
        if (execution.value) execution.value.sopTitle = sop.value!.title || ''
        try {
          const raw = sop.value!.content
          steps.value = (raw && raw !== 'null' && raw !== 'undefined') ? JSON.parse(raw) : []
        } catch { steps.value = [] }
        stepsLoaded.value = true
      } else {
        // 兜底：单独请求 SOP 详情
        const sopDataRes = await request.get<unknown, ApiResponse<Sop>>(`/sop/${sopId}`)
        if (sopDataRes?.code === 200 && sopDataRes.data) {
          sop.value = sopDataRes.data
          if (execution.value) execution.value.sopTitle = sop.value.title
          try {
            const raw = sop.value.content
            steps.value = (raw && raw !== 'null' && raw !== 'undefined') ? JSON.parse(raw) : []
          } catch { steps.value = [] }
          stepsLoaded.value = true
        }
      }
    }

    // 加载历史 guidance
    try {
      const latestExec = execution.value
      if (latestExec?.id) {
        const recordsRes = await request.get<unknown, ApiResponse<any[]>>(`/execution/${latestExec.id}/records`)
        if (recordsRes?.code === 200 && recordsRes.data) {
          for (const rec of recordsRes.data) {
            if (rec.guidance) {
              stepGuidanceHistory.value[rec.stepIndex] = rec.guidance
            }
          }
          if (stepGuidanceHistory.value[currentStep.value]) {
            currentGuidance.value = stepGuidanceHistory.value[currentStep.value]
          }
          const currentRec = recordsRes.data.find((r: any) => r.stepIndex === currentStep.value)
          if (currentRec?.notes) {
            notes.value = currentRec.notes
          }
        }
      }
    } catch (e) {
      console.error('[InstanceDoView] load records failed:', e)
    }
  } catch (e) {
    ElMessage.error('加载失败')
    router.push('/execution')
  }
})
</script>

<style scoped>
.execution-do-page { min-height: 100vh; background: #F5F7FA; }

/* 顶部 sticky header */
.exec-header {
  position: sticky; top: 0; z-index: 100;
  background: #fff; border-bottom: 1px solid #E8E8E8;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

/* Topbar */
.exec-topbar { display: flex; align-items: center; padding: 12px 20px; background: #fff; gap: 12px; }
.exec-back { border: none; background: transparent; font-size: 20px; cursor: pointer; padding: 4px 8px; color: #333; border-radius: 8px; }
.exec-back:hover { background: #F0F0F0; }
.exec-title { flex: 1; font-size: 15px; font-weight: 600; color: #212121; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.exec-steps-count { font-size: 12px; font-weight: 600; color: #5B7FFF; background: #E8ECFF; padding: 3px 10px; border-radius: 20px; flex-shrink: 0; }

/* Step Progress Dots */
.step-progress-wrap { background: #fff; padding: 16px 20px 12px; }
.step-dots-row { display: flex; align-items: center; gap: 6px; margin-bottom: 10px; overflow-x: auto; }
.step-dot { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 600; flex-shrink: 0; cursor: default; transition: all 0.2s; }
.step-dot.done { background: #52C41A; color: white; cursor: pointer; }
.step-dot.done:hover { background: #73D13D; }
.step-dot.active { background: #5B7FFF; color: white; box-shadow: 0 0 0 4px rgba(91,127,255,0.2); transform: scale(1.1); }
.step-dot.future { background: #F0F0F0; color: #999; }
.dot-inner { font-size: 12px; }
.dot-num { line-height: 1; }
.step-progress-bar { height: 5px; background: #F0F0F0; border-radius: 3px; overflow: hidden; }
.step-progress-fill { height: 100%; background: linear-gradient(90deg, #5B7FFF, #7994FF); border-radius: 3px; transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1); }
.step-progress-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 6px; }
.progress-percent { font-size: 12px; font-weight: 700; color: #5B7FFF; }
.progress-remaining { font-size: 11px; color: #999; }
.progress-remaining.success { color: #52C41A; }

/* 主体：左右布局 */
.exec-body { display: flex; gap: 24px; align-items: flex-start; padding: 24px; max-width: 1280px; margin: 0 auto; }

/* 左侧内容区 */
.exec-content { flex: 1; min-width: 0; }

/* Loading */
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 16px; color: #999; padding: 60px 0; }
.loading-spinner { width: 36px; height: 36px; border: 3px solid #E8E8E8; border-top-color: #5B7FFF; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Step Main Card */
.step-main-card { background: #fff; border-radius: 16px; padding: 20px; margin-bottom: 12px; border: 1.5px solid #E8E8E8; transition: all 0.3s; }
.step-main-card.just-completed { border-color: #52C41A; box-shadow: 0 0 0 4px rgba(82,196,26,0.1); animation: flashGreen 0.6s ease; }
@keyframes flashGreen { 0% { background: #fff; } 30% { background: #F6FFED; } 100% { background: #fff; } }
.step-header-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.step-badge { display: inline-block; background: #5B7FFF; color: white; font-size: 11px; padding: 3px 12px; border-radius: 20px; font-weight: 600; }
.step-duration { font-size: 12px; color: #999; }
.step-title { font-size: 18px; font-weight: 700; color: #212121; margin: 0 0 8px; line-height: 1.4; }
.step-desc { font-size: 14px; color: #555; line-height: 1.7; margin: 0; }

/* Next Step Preview */
.next-step-preview { background: #F9FAFF; border: 1px dashed #C3C8FF; border-radius: 12px; padding: 12px 16px; margin-bottom: 14px; }
.next-label { display: flex; align-items: center; gap: 6px; font-size: 11px; color: #8B8FA8; font-weight: 600; margin-bottom: 4px; }
.next-arrow { color: #5B7FFF; font-size: 14px; }
.next-title { font-size: 13px; color: #5B7FFF; font-weight: 500; }

/* Check Items */
.check-section { background: #fff; border-radius: 16px; padding: 18px; margin-bottom: 14px; border: 1.5px solid #E8E8E8; }
.check-section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.check-section-title { font-size: 14px; font-weight: 600; color: #333; }
.check-progress-tag { font-size: 11px; background: #E8F3FF; color: #5B7FFF; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.required-dot { color: #FF4D4F; margin-left: 2px; }
.check-item { margin-bottom: 14px; }
.check-item:last-child { margin-bottom: 0; }
.check-row { display: flex; align-items: center; }
.check-label { display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 8px 10px; border-radius: 10px; transition: background 0.15s; }
.check-label:hover { background: #F5F7FA; }
.check-label.is-done .check-text { color: #52C41A; text-decoration: line-through; }
.custom-checkbox { width: 20px; height: 20px; border-radius: 50%; border: 2px solid #D9D9D9; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.2s; background: #fff; }
.custom-checkbox.checked { background: #52C41A; border-color: #52C41A; color: white; }
.check-text { font-size: 14px; color: #333; }
.check-label-row { font-size: 13px; color: #333; font-weight: 500; margin-bottom: 6px; }
.check-text-input, .check-num-input, .check-select-input { width: 100%; padding: 10px 12px; border: 1.5px solid #E8E8E8; border-radius: 10px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff; transition: border-color 0.2s, box-shadow 0.2s; }
.check-text-input:focus, .check-num-input:focus, .check-select-input:focus { border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.12); }
.check-text-input.has-value, .check-num-input.has-value, .check-select-input.has-value { border-color: #B7D0FF; background: #F9FBFF; }
.check-select-input { cursor: pointer; }

/* No check items hint */
.no-check-hint { text-align: center; padding: 14px; background: #FAFAFA; border-radius: 12px; margin-bottom: 14px; font-size: 13px; color: #999; }

/* Notes */
.notes-section { background: #fff; border-radius: 16px; padding: 18px; margin-bottom: 14px; border: 1.5px solid #E8E8E8; }
.notes-label { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }
.optional-hint { font-size: 12px; color: #999; font-weight: 400; }
.notes-input { width: 100%; min-height: 90px; padding: 10px 12px; border: 1.5px solid #E8E8E8; border-radius: 10px; font-size: 14px; resize: vertical; outline: none; box-sizing: border-box; background: #fff; transition: border-color 0.2s, box-shadow 0.2s; }
.notes-input:focus { border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.12); }
.notes-input.has-value { border-color: #B7D0FF; background: #F9FBFF; }
.notes-preview {
  width: 100%; min-height: 90px; padding: 10px 12px;
  border: 1.5px solid #E8E8E8; border-radius: 10px;
  font-size: 14px; box-sizing: border-box; background: #fff;
  cursor: text; transition: border-color 0.2s, box-shadow 0.2s;
}
.notes-preview.has-value { border-color: #B7D0FF; background: #F9FBFF; }
.notes-preview.empty { color: #999; }
.notes-preview .placeholder { color: #bbb; }
.notes-preview :deep(p) { margin: 0 0 6px 0; }
.notes-preview :deep(p:last-child) { margin-bottom: 0; }
.notes-preview :deep(ul), .notes-preview :deep(ol) { margin: 4px 0; padding-left: 18px; }
.notes-preview :deep(li) { margin: 2px 0; }
.notes-preview :deep(strong) { color: #333; }
.guidance-badge { font-size: 11px; background: #e7f3ff; color: #409eff; padding: 2px 8px; border-radius: 10px; font-weight: 400; margin-left: 8px; }
.guidance-history-hint { margin-top: 8px; font-size: 12px; color: #409eff; padding: 6px 10px; background: #f0f7ff; border-radius: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* Action Buttons */
.step-actions { display: flex; gap: 10px; margin-top: 8px; }
.btn-primary { flex: 1; height: 48px; background: linear-gradient(135deg, #5B7FFF, #7994FF); color: white; border: none; border-radius: 12px; font-size: 15px; font-weight: 600; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 6px; transition: opacity 0.2s; }
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-primary.is-submitting { background: linear-gradient(135deg, #A0A0A0, #BDBDBD); }
.btn-secondary { height: 48px; padding: 0 20px; background: #fff; color: #333; border: 1.5px solid #E8E8E8; border-radius: 12px; font-size: 15px; cursor: pointer; transition: background 0.15s; }
.btn-secondary:hover { background: #F5F7FA; }
.flex-1 { flex: 1; }
.btn-spinner { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.4); border-top-color: white; border-radius: 50%; animation: spin 0.7s linear infinite; display: inline-block; }

/* Completed State */
.complete-state { max-width: 480px; margin: 40px auto; text-align: center; padding: 0 24px; }
.complete-icon { font-size: 72px; margin-bottom: 24px; }
.complete-state h2 { font-size: 24px; font-weight: 700; color: #212121; margin-bottom: 10px; }
.complete-state p { font-size: 15px; color: #999; margin-bottom: 28px; }
.complete-stats { display: flex; justify-content: center; gap: 28px; margin-bottom: 32px; }
.complete-stat { text-align: center; }
.cs-num { font-size: 28px; font-weight: 700; color: #333; }
.cs-num.success { color: #52C41A; }
.cs-num.primary { color: #5B7FFF; }
.cs-label { font-size: 12px; color: #999; margin-top: 4px; }
.complete-actions { display: flex; gap: 12px; justify-content: center; }

/* Empty State */
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-icon { font-size: 48px; margin-bottom: 16px; }

/* AI 助手面板 */
.ai-panel-wrapper { width: 380px; flex-shrink: 0; position: sticky; top: 120px; max-height: calc(100vh - 144px); overflow-y: auto; }
</style>
