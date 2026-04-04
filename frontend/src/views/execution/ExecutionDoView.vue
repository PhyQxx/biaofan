<template>
  <div class="execution-do-page">
    <div class="topbar">
      <div class="topbar-left">
        <button class="btn-back" @click="router.push('/execution')">← 返回</button>
        <div class="sop-title">{{ execution?.sopTitle || '执行中' }}</div>
      </div>
      <div class="topbar-right">
        <span class="step-indicator">第 {{ currentStep }} / {{ totalSteps }} 步</span>
      </div>
    </div>

    <!-- Progress -->
    <div class="progress-section">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>
    </div>

    <!-- Step Content -->
    <div class="step-content" v-if="currentStepData">
      <div class="step-card">
        <div class="step-num">步骤 {{ currentStep }}</div>
        <h2 class="step-title">{{ currentStepData.title }}</h2>
        <p class="step-desc" v-if="currentStepData.description">{{ currentStepData.description }}</p>
        <div class="step-duration" v-if="currentStepData.duration">
          ⏱️ 预计 {{ currentStepData.duration }} 分钟
        </div>
      </div>

      <!-- Check items placeholder -->
      <div class="check-section" v-if="currentStepData.checkItems">
        <h3>检查项</h3>
        <div v-for="(item, idx) in currentStepData.checkItems" :key="idx" class="check-item">
          <input type="checkbox" v-model="checkStatus[idx]" />
          <span>{{ item.label || item }}</span>
        </div>
      </div>

      <!-- Notes -->
      <div class="notes-section">
        <textarea v-model="notes" class="notes-input" placeholder="执行笔记（选填）..."></textarea>
      </div>

      <!-- Actions -->
      <div class="step-actions">
        <button v-if="currentStep > 1" class="btn-secondary" @click="prevStep">← 上一步</button>
        <button class="btn-primary" @click="completeStep">
          {{ currentStep >= totalSteps ? '✓ 完成 SOP' : '完成本步 →' }}
        </button>
      </div>
    </div>

    <!-- Completed State -->
    <div class="complete-state" v-else-if="isCompleted">
      <div class="complete-icon">🎉</div>
      <h2>执行完成！</h2>
      <p>恭喜你完成了本次 SOP 执行</p>
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
const checkStatus = ref<Record<number, boolean>>({})

const totalSteps = computed(() => steps.value.length)
const currentStepData = computed(() => steps.value[currentStep.value - 1])
const progressPercent = computed(() => totalSteps.value ? Math.round((currentStep.value / totalSteps.value) * 100) : 0)
const isCompleted = computed(() => execution.value?.status === 'completed')

const prevStep = () => { if (currentStep.value > 1) currentStep.value-- }

const completeStep = async () => {
  try {
    await request.post(`/execution/${executionId.value}/step/${currentStep.value}`, { notes: notes.value })
    if (currentStep.value >= totalSteps.value) {
      await request.post(`/execution/${executionId.value}/finish`)
      execution.value.status = 'completed'
      ElMessage.success('SOP 执行完成！')
    } else {
      currentStep.value++
      notes.value = ''
      ElMessage.success('步骤完成，继续加油！')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(async () => {
  try {
    const [execRes, sopRes] = await Promise.all([
      request.get(`/execution/${executionId}`),
      request.get(`/execution/${executionId}/sop`),
    ])
    if (execRes.code === 200) {
      execution.value = execRes.data
      execution.value.sopTitle = ''
      currentStep.value = execution.value.currentStep || 1
    }
    if (sopRes.code === 200) {
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
.execution-do-page { min-height: 100vh; background: #F5F7FA; }
.topbar { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: #fff; border-bottom: 1px solid #E8E8E8; }
.topbar-left { display: flex; align-items: center; gap: 12px; }
.btn-back { background: none; border: none; font-size: 14px; color: #666; cursor: pointer; padding: 6px 12px; border-radius: 6px; }
.btn-back:hover { background: #F5F7FA; }
.sop-title { font-size: 16px; font-weight: 600; color: #212121; }
.step-indicator { font-size: 14px; color: #5B7FFF; font-weight: 500; }
.progress-section { background: #fff; padding: 0 24px 16px; }
.progress-bar { height: 6px; background: #F0F0F0; border-radius: 3px; overflow: hidden; }
.progress-fill { height: 100%; background: #5B7FFF; border-radius: 3px; transition: width 0.3s; }
.step-content { max-width: 640px; margin: 24px auto; padding: 0 24px; }
.step-card { background: #fff; border-radius: 16px; padding: 28px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.step-num { font-size: 12px; color: #5B7FFF; font-weight: 600; margin-bottom: 10px; }
.step-title { font-size: 20px; font-weight: 700; color: #212121; margin: 0 0 10px; }
.step-desc { font-size: 15px; color: #666; line-height: 1.7; margin: 0 0 12px; }
.step-duration { font-size: 13px; color: #999; }
.check-section { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px; }
.check-section h3 { margin: 0 0 12px; font-size: 14px; font-weight: 600; color: #333; }
.check-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; border-bottom: 1px solid #F0F0F0; font-size: 14px; color: #333; }
.check-item:last-child { border-bottom: none; }
.check-item input[type=checkbox] { width: 18px; height: 18px; accent-color: #5B7FFF; }
.notes-section { margin-bottom: 16px; }
.notes-input { width: 100%; min-height: 100px; padding: 14px; border: 1.5px solid #E8E8E8; border-radius: 10px; font-size: 14px; resize: vertical; outline: none; box-sizing: border-box; background: #fff; }
.notes-input:focus { border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.10); }
.step-actions { display: flex; gap: 12px; justify-content: flex-end; }
.btn-primary { height: 44px; padding: 0 28px; background: #5B7FFF; color: white; border: none; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; }
.btn-primary:hover { background: #7994FF; }
.btn-secondary { height: 44px; padding: 0 20px; background: #fff; color: #333; border: 1.5px solid #E8E8E8; border-radius: 10px; font-size: 15px; cursor: pointer; }
.btn-secondary:hover { background: #F5F7FA; }
.complete-state { max-width: 400px; margin: 80px auto; text-align: center; padding: 0 24px; }
.complete-icon { font-size: 64px; margin-bottom: 20px; }
.complete-state h2 { font-size: 24px; font-weight: 700; color: #212121; margin-bottom: 10px; }
.complete-state p { font-size: 15px; color: #999; margin-bottom: 28px; }
.complete-actions { display: flex; gap: 12px; justify-content: center; }
</style>
