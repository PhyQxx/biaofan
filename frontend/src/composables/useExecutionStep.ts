/**
 * useExecutionStep - Shared composable for SOP step execution logic
 *
 * Encapsulates the common state and behaviour used by both
 * ExecutionDoView (direct execution) and InstanceDoView (periodic instance).
 *
 * Config params control the API prefix ("/execution" vs "/instance"),
 * step-completion URL pattern, and the initial data-fetch strategy.
 */
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import request from '@/api'
import type { Execution, StepData, CheckItem, ApiResponse } from '@/types'

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

/** Result returned by the custom fetchInitial callback */
export interface FetchInitialResult {
  execution: Execution | null
  steps: StepData[]
  sopId: number
  sopTitle: string
  /** optional: current-step notes loaded from history */
  initialNotes?: string
  /** optional: guidance history keyed by stepIndex */
  guidanceHistory?: Record<number, string>
}

/** Configuration that differs between the two views */
export interface UseExecutionStepConfig {
  /** Build the URL used to complete a step */
  buildCompleteUrl: (entityId: number, stepIndex: number) => string
  /** Build the URL used to re-fetch the entity after completion */
  buildRefreshUrl: (entityId: number) => string
  /** Parse the refresh response into an Execution object */
  parseRefreshResponse: (data: unknown) => Execution | null
  /** Custom fetcher executed during onMounted.
   *  Must return the Execution (or null) and the parsed StepData array. */
  fetchInitial: () => Promise<FetchInitialResult>
  /** Label used in console error logs */
  logLabel: string
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

export function useExecutionStep(cfg: UseExecutionStepConfig) {
  const route = useRoute()
  const router = useRouter()
  const entityId = Number(route.params.id)

  // ---- Core state ----
  const execution = ref<Execution | null>(null)
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
  const showAi = ref(false)
  const currentGuidance = ref('')
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const stepGuidanceHistory = ref<Record<number, any>>({})

  // ---- Auto-focus notes textarea ----
  watch(notesEditing, (editing) => {
    if (editing) {
      nextTick(() => notesTextareaRef.value?.focus())
    }
  })

  // ---- Computed ----
  const sopId = computed(() => execution.value?.sopId)

  const totalSteps = computed(() => steps.value.length || 0)

  const currentStepData = computed(() => steps.value[currentStep.value - 1])

  const progressPercent = computed(() =>
    totalSteps.value ? Math.round((currentStep.value / totalSteps.value) * 100) : 0,
  )

  const isCompleted = computed(() => execution.value?.status === 'completed')

  const aiAutoFill = computed(() => {
    if (!currentStepData.value) return undefined
    return {
      stepTitle: currentStepData.value.title || '',
      stepDescription: currentStepData.value.description || '',
      stepIndex: currentStep.value,
      totalSteps: totalSteps.value,
    }
  })

  const checkItems = computed<CheckItem[]>(() => {
    if (!currentStepData.value) return []
    const ci = currentStepData.value.checkItems
    if (!ci) return []
    if (Array.isArray(ci)) return ci
    try {
      return ci && ci !== 'null' && ci !== 'undefined' ? JSON.parse(ci) : []
    } catch {
      return []
    }
  })

  const completedCheckCount = computed(() => {
    return checkItems.value.filter((_: CheckItem, idx: number) => {
      const val = checkData.value[String(idx)]
      if (val === true || (typeof val === 'string' && val.trim())) return true
      return false
    }).length
  })

  // ---- Navigation helpers ----

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

  function prevStep() {
    if (currentStep.value > 1) {
      navigateToStep(currentStep.value - 1)
    }
  }

  // ---- AI callbacks ----

  function onGuidanceReady(guidance: string) {
    currentGuidance.value = guidance
    stepGuidanceHistory.value[currentStep.value] = guidance
  }

  function onNotesReady(note: string) {
    notes.value = note
  }

  // ---- Back / confirm-exit ----

  function handleBack() {
    if (currentStep.value > 1 && !isCompleted.value) {
      ElMessageBox.confirm(
        '当前步骤尚未完成，确定要退出吗？退出后进度将保留。',
        '确认退出',
        {
          confirmButtonText: '确定退出',
          cancelButtonText: '继续执行',
          type: 'warning',
        },
      )
        .then(() => {
          router.push('/execution')
        })
        .catch(() => {
          /* user cancelled */
        })
    } else {
      router.push('/execution')
    }
  }

  // ---- Complete step ----

  function handleComplete() {
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
    })
      .then(() => {
        completeStep()
      })
      .catch(() => {
        /* user cancelled */
      })
  }

  async function completeStep() {
    isSubmitting.value = true
    try {
      const dataMap: Record<string, unknown> = {}
      for (const [k, v] of Object.entries(checkData.value)) {
        dataMap[k] = { value: v }
      }

      const res = await request.post<unknown, ApiResponse<{ completed?: boolean }>>(
        cfg.buildCompleteUrl(entityId, currentStep.value),
        {
          notes: notes.value,
          checkData: dataMap,
          guidance: currentGuidance.value || null,
        },
      )

      if (res?.code === 200) {
        // Re-fetch execution status from server after step submission
        try {
          const refreshRes = await request.get<unknown, ApiResponse<unknown>>(
            cfg.buildRefreshUrl(entityId),
          )
          if (refreshRes?.code === 200) {
            const refreshed = cfg.parseRefreshResponse(refreshRes.data)
            if (refreshed) execution.value = refreshed
          }
        } catch (e) {
          console.error(`[${cfg.logLabel}] re-fetch status failed:`, e)
        }

        // Flash animation
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

  // ---- Lifecycle ----

  onBeforeUnmount(() => {
    if (completionTimeout.value) {
      clearTimeout(completionTimeout.value)
      completionTimeout.value = null
    }
  })

  onMounted(async () => {
    try {
      const result = await cfg.fetchInitial()
      if (result.execution) {
        execution.value = result.execution
        execution.value.sopTitle = result.sopTitle
        currentStep.value = result.execution.currentStep || 1
      }
      steps.value = result.steps
      stepsLoaded.value = true
      if (result.initialNotes) {
        notes.value = result.initialNotes
      }
      if (result.guidanceHistory) {
        stepGuidanceHistory.value = result.guidanceHistory
        if (stepGuidanceHistory.value[currentStep.value]) {
          currentGuidance.value = stepGuidanceHistory.value[currentStep.value]
        }
      }
    } catch (_e) {
      ElMessage.error('加载失败')
      router.push('/execution')
    }
  })

  // ---- Expose everything the views need ----
  return {
    // Route helpers
    route,
    router,
    entityId,

    // Core state
    execution,
    steps,
    currentStep,
    notes,
    notesEditing,
    notesTextareaRef,
    checkData,
    isSubmitting,
    justCompleted,
    stepsLoaded,
    showAi,
    currentGuidance,
    stepGuidanceHistory,

    // Computed
    sopId,
    totalSteps,
    currentStepData,
    progressPercent,
    isCompleted,
    aiAutoFill,
    checkItems,
    completedCheckCount,

    // Methods
    navigateToStep,
    prevStep,
    onGuidanceReady,
    onNotesReady,
    handleBack,
    handleComplete,

    // Libraries (for template use)
    DOMPurify,
    marked,
  }
}
