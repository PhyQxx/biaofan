<template>
  <div class="execution-do-page">
    <!-- Topbar -->
    <div class="exec-topbar">
      <button class="exec-back" @click="router.push('/execution')">←</button>
      <div class="exec-title">{{ execution?.sopTitle || 'SOP 执行' }}</div>
      <div class="exec-menu" @click="showMenu = !showMenu">⋮</div>
    </div>

    <!-- Progress -->
    <div class="exec-progress-wrap">
      <div class="exec-progress-label">
        <span>执行进度</span>
        <span>第 {{ currentStep }} / {{ totalSteps }} 步</span>
      </div>
      <div class="exec-progress-bar">
        <div class="exec-progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>

    <!-- Step Content -->
    <div class="exec-content" v-if="currentStepData && !isCompleted">
      <div class="step-main-card">
        <div class="step-indicator">步骤 {{ currentStep }}</div>
        <h2 class="step-title">{{ currentStepData.title }}</h2>
        <p class="step-desc" v-if="currentStepData.description">{{ currentStepData.description }}</p>
        <div class="step-duration" v-if="currentStepData.duration">
          ⏱️ 预计 {{ currentStepData.duration }} 分钟
        </div>
      </div>

      <!-- Check Items -->
      <div class="check-section" v-if="checkItems.length">
        <div class="check-section-title">📋 检查项 <span class="required-hint">（必填）</span></div>
        <div v-for="(item, idx) in checkItems" :key="idx" class="check-item">
          <!-- Checkbox type -->
          <div v-if="item.itemType === 'checkbox' || !item.itemType" class="check-row">
            <label class="check-label">
              <input type="checkbox" v-model="checkData[idx]" :true-value="true" :false-value="false" class="check-box" />
              <span class="check-text">{{ item.label }}</span>
            </label>
          </div>
          <!-- Text type -->
          <div v-else-if="item.itemType === 'text'" class="check-text-row">
            <div class="check-label-row">
              {{ item.label }}
              <span v-if="item.isRequired" class="required-star">*</span>
            </div>
            <textarea v-model="checkData[idx]" class="check-text-input" :placeholder="item.placeholder || '请输入...'" rows="2"></textarea>
          </div>
          <!-- Number type -->
          <div v-else-if="item.itemType === 'number'" class="check-text-row">
            <div class="check-label-row">
              {{ item.label }}
              <span v-if="item.isRequired" class="required-star">*</span>
            </div>
            <input type="number" v-model="checkData[idx]" class="check-num-input" :placeholder="item.placeholder || '请输入数字'" />
          </div>
          <!-- Date type -->
          <div v-else-if="item.itemType === 'date'" class="check-text-row">
            <div class="check-label-row">
              {{ item.label }}
              <span v-if="item.isRequired" class="required-star">*</span>
            </div>
            <input type="date" v-model="checkData[idx]" class="check-text-input" />
          </div>
          <!-- Select type -->
          <div v-else-if="item.itemType === 'select'" class="check-text-row">
            <div class="check-label-row">
              {{ item.label }}
              <span v-if="item.isRequired" class="required-star">*</span>
            </div>
            <select v-model="checkData[idx]" class="check-select-input">
              <option value="">请选择...</option>
              <option v-for="opt in (item.options || [])" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Notes -->
      <div class="notes-section">
        <div class="notes-label">📝 执行笔记 <span class="optional-hint">（选填）</span></div>
        <textarea v-model="notes" class="notes-input" placeholder="记录执行过程中的备注、问题或心得..."></textarea>
      </div>

      <!-- Action Buttons -->
      <div class="step-actions">
        <button v-if="currentStep > 1" class="btn-secondary" @click="prevStep">← 上一步</button>
        <button class="btn-primary flex-1" @click="completeStep" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : (currentStep >= totalSteps ? '✓ 完成 SOP' : '完成本步 →') }}
        </button>
      </div>
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
          <div class="cs-num primary">{{ Math.round((Date.now() - new Date(execution?.startedAt).getTime()) / 60000) }}</div>
          <div class="cs-label">用时(分钟)</div>
        </div>
      </div>
      <div class="complete-actions">
        <button class="btn-secondary" @click="router.push('/')">返回首页</button>
        <button class="btn-primary" @click="router.push('/execution')">继续其他 SOP</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api'

const route = useRoute()
const router = useRouter()
const executionId = Number(route.params.id)

const execution = ref<any>(null)
const sop = ref<any>(null)
const steps = ref<any[]>([])
const currentStep = ref(1)
const notes = ref('')
const checkData = ref<Record<string, any>>({})
const isSubmitting = ref(false)
const showMenu = ref(false)

const totalSteps = computed(() => steps.value.length || 0)
const currentStepData = computed(() => steps.value[currentStep.value - 1])
const progressPercent = computed(() => totalSteps.value ? Math.round((currentStep.value / totalSteps.value) * 100) : 0)
const isCompleted = computed(() => execution.value?.status === 'completed')

// Parse check items from current step
const checkItems = computed(() => {
  if (!currentStepData.value) return []
  const ci = currentStepData.value.checkItems
  if (!ci) return []
  if (Array.isArray(ci)) return ci
  try { return JSON.parse(ci) } catch { return [] }
})

const prevStep = () => {
  if (currentStep.value > 1) currentStep.value--
}

const completeStep = async () => {
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

  isSubmitting.value = true
  try {
    // Build checkData object keyed by item index
    const dataMap: Record<string, any> = {}
    for (const [k, v] of Object.entries(checkData.value)) {
      dataMap[k] = { value: v }
    }

    const res: any = await request.post(`/execution/${executionId}/step/${currentStep.value}`, {
      notes: notes.value,
      checkData: dataMap,
    })

    if (res?.code === 200) {
      const completed = res.data?.completed
      if (completed || currentStep.value >= totalSteps.value) {
        execution.value.status = 'completed'
        ElMessage.success('🎉 SOP 执行完成！')
      } else {
        currentStep.value++
        notes.value = ''
        checkData.value = {}
        ElMessage.success('步骤完成，继续加油！')
      }
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    isSubmitting.value = false
  }
}

onMounted(async () => {
  try {
    const [execRes, sopRes] = await Promise.all([
      request.get(`/execution/${executionId}`),
      request.get(`/execution/${executionId}/sop`),
    ]) as any[]

    if (execRes?.code === 200) {
      execution.value = execRes.data
      execution.value.sopTitle = ''
      currentStep.value = execution.value.currentStep || 1
    }

    if (sopRes?.code === 200) {
      sop.value = sopRes.data
      execution.value.sopTitle = sop.value.title
      try {
        steps.value = JSON.parse(sop.value.content || '[]')
      } catch { steps.value = [] }
    }
  } catch (e) {
    ElMessage.error('加载失败')
    router.push('/execution')
  }
})
</script>

<style scoped>
.execution-do-page { min-height: 100vh; background: #F5F7FA; display: flex; flex-direction: column; }

.exec-topbar {
  display: flex; align-items: center; padding: 12px 20px;
  background: #fff; border-bottom: 1px solid #E8E8E8; gap: 12px; flex-shrink: 0;
}
.exec-back { border: none; background: transparent; font-size: 18px; cursor: pointer; padding: 4px 8px; color: #333; }
.exec-title { flex: 1; font-size: 15px; font-weight: 600; color: #212121; text-align: center; }
.exec-menu { font-size: 18px; cursor: pointer; padding: 4px 8px; color: #666; }

.exec-progress-wrap { background: #fff; padding: 16px 20px 14px; border-bottom: 1px solid #E8E8E8; flex-shrink: 0; }
.exec-progress-label { display: flex; justify-content: space-between; font-size: 12px; color: #999; margin-bottom: 8px; }
.exec-progress-bar { height: 6px; background: #F0F0F0; border-radius: 3px; overflow: hidden; }
.exec-progress-fill { height: 100%; background: linear-gradient(90deg, #5B7FFF, #7994FF); border-radius: 3px; transition: width 0.4s ease; }

.exec-content { flex: 1; overflow-y: auto; padding: 20px; padding-bottom: 100px; }

.step-main-card { background: #fff; border-radius: 16px; padding: 20px; margin-bottom: 14px; border: 1px solid #E8E8E8; }
.step-indicator { display: inline-block; background: #5B7FFF; color: white; font-size: 11px; padding: 2px 10px; border-radius: 20px; font-weight: 600; margin-bottom: 10px; }
.step-title { font-size: 18px; font-weight: 700; color: #212121; margin: 0 0 10px; }
.step-desc { font-size: 14px; color: #666; line-height: 1.7; margin: 0 0 12px; }
.step-duration { font-size: 13px; color: #999; }

.check-section { background: #fff; border-radius: 14px; padding: 16px; margin-bottom: 14px; border: 1px solid #E8E8E8; }
.check-section-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 14px; }
.required-hint { font-size: 12px; color: #999; font-weight: 400; }
.optional-hint { font-size: 12px; color: #999; font-weight: 400; }
.required-star { color: #FF4D4F; }

.check-item { margin-bottom: 14px; }
.check-item:last-child { margin-bottom: 0; }
.check-row { display: flex; align-items: center; gap: 10px; }
.check-label { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.check-box { width: 18px; height: 18px; accent-color: #5B7FFF; cursor: pointer; }
.check-text { font-size: 14px; color: #333; }

.check-text-row { }
.check-label-row { font-size: 13px; color: #333; font-weight: 500; margin-bottom: 6px; }
.check-text-input, .check-num-input, .check-select-input {
  width: 100%; padding: 10px 12px; border: 1.5px solid #E8E8E8;
  border-radius: 8px; font-size: 14px; outline: none; box-sizing: border-box; background: #fff;
}
.check-text-input:focus, .check-num-input:focus, .check-select-input:focus {
  border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.10);
}
.check-select-input { cursor: pointer; }

.notes-section { background: #fff; border-radius: 14px; padding: 16px; margin-bottom: 14px; border: 1px solid #E8E8E8; }
.notes-label { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }
.notes-input {
  width: 100%; min-height: 90px; padding: 10px 12px;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 14px; resize: vertical; outline: none; box-sizing: border-box; background: #fff;
}
.notes-input:focus { border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.10); }

.step-actions { display: flex; gap: 10px; }
.btn-primary {
  flex: 1; height: 46px; background: linear-gradient(135deg, #5B7FFF, #7994FF);
  color: white; border: none; border-radius: 12px; font-size: 15px; font-weight: 600; cursor: pointer;
}
.btn-primary:hover:not(:disabled) { opacity: 0.9; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-secondary {
  height: 46px; padding: 0 20px; background: #fff; color: #333;
  border: 1.5px solid #E8E8E8; border-radius: 12px;
  font-size: 15px; cursor: pointer;
}
.btn-secondary:hover { background: #F5F7FA; }
.flex-1 { flex: 1; }

.complete-state { flex: 1; max-width: 480px; margin: 60px auto; text-align: center; padding: 0 24px; }
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
</style>
