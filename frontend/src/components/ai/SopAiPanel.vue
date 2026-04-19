<template>
  <div class="ai-panel">
    <!-- Header -->
    <div class="ai-header">
      <span class="ai-title">🤖 SOP AI 助手</span>
      <button class="ai-close" @click="$emit('close')">✕</button>
    </div>

    <!-- Tabs -->
    <div class="ai-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >{{ tab.label }}</button>
    </div>

    <!-- Tab: AI 创建 SOP -->
    <div v-if="activeTab === 'create'" class="tab-content">
      <div class="form-hint">
        描述你的目标或场景，AI 将生成一个结构化的 SOP
      </div>
      <div class="form-group">
        <label>目标描述</label>
        <textarea
          v-model="createForm.goal"
          class="ai-textarea"
          placeholder="例如：每日早会流程，要求简洁高效，包含签到、汇报、总结三个环节"
          rows="4"
        ></textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>标题（可选）</label>
          <input v-model="createForm.title" class="ai-input" placeholder="AI 会自动生成" />
        </div>
        <div class="form-group">
          <label>分类</label>
          <select v-model="createForm.category" class="ai-select">
            <option value="">自动</option>
            <option value="daily">日 SOP</option>
            <option value="weekly">周 SOP</option>
            <option value="monthly">月 SOP</option>
            <option value="yearly">年 SOP</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <label>标签（可选）</label>
        <input v-model="createForm.tags" class="ai-input" placeholder="逗号分隔" />
      </div>

      <div v-if="generateLoading" class="ai-loading">
        <span class="loading-spinner"></span> AI 正在生成 SOP...
      </div>
      <div v-if="generateResult" class="generate-result">
        <div class="result-label">生成结果：</div>
        <pre class="result-json">{{ generateResult }}</pre>
        <div class="result-actions">
          <button class="btn-apply" @click="applyGenerated">应用到此 SOP</button>
        </div>
      </div>
      <div v-if="generateError" class="ai-error">{{ generateError }}</div>

      <button
        class="btn-ai-primary"
        :disabled="!createForm.goal.trim() || generateLoading"
        @click="handleGenerate"
      >✨ AI 生成 SOP</button>
    </div>

    <!-- Tab: AI 执行指导 -->
    <div v-if="activeTab === 'execute'" class="tab-content">
      <div class="form-hint">
        正在执行步骤时可获取 AI 实时指导
      </div>
      <div class="form-group">
        <label>步骤标题</label>
        <input v-model="executeForm.stepTitle" class="ai-input" placeholder="当前步骤标题" />
      </div>
      <div class="form-group">
        <label>步骤说明</label>
        <textarea
          v-model="executeForm.stepDescription"
          class="ai-textarea"
          placeholder="当前步骤的详细说明"
          rows="3"
        ></textarea>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>当前步骤</label>
          <input v-model.number="executeForm.stepIndex" type="number" class="ai-input" min="0" />
        </div>
        <div class="form-group">
          <label>总步骤数</label>
          <input v-model.number="executeForm.totalSteps" type="number" class="ai-input" min="1" />
        </div>
      </div>
      <div class="form-group">
        <label>用户备注（可选）</label>
        <input v-model="executeForm.notes" class="ai-input" placeholder="执行中的备注" />
      </div>

      <div v-if="guidanceLoading" class="ai-loading">
        <span class="loading-spinner"></span> AI 分析中...
      </div>
      <div v-if="guidanceResult" class="guidance-result">
        <div class="result-label">AI 指导：</div>
        <div class="guidance-text">{{ guidanceResult }}</div>
        <div class="result-actions">
          <button class="btn-regenerate" @click="handleGuidance">🔄 重新生成</button>
        </div>
      </div>
      <div v-if="guidanceError" class="ai-error">{{ guidanceError }}</div>

      <button
        class="btn-ai-primary"
        :disabled="!executeForm.stepTitle.trim() || guidanceLoading"
        @click="handleGuidance"
      >💡 获取 AI 指导</button>
    </div>

    <!-- Tab: AI 审核 -->
    <div v-if="activeTab === 'review'" class="tab-content">
      <div class="form-hint">
        SOP 发布前，AI 将审核内容完整性、步骤合理性和风险点
      </div>

      <div v-if="reviewLoading" class="ai-loading">
        <span class="loading-spinner"></span> AI 审核中...
      </div>

      <div v-if="reviewResult" class="review-result">
        <div class="verdict-badge" :class="reviewResult.verdict">
          {{ verdictLabel(reviewResult.verdict) }}
        </div>
        <div v-if="reviewResult.score" class="score-label">
          质量评分：<strong>{{ reviewResult.score }}</strong>/100
        </div>

        <div v-if="parsedIssues.length" class="issues-section">
          <div class="result-label">问题列表：</div>
          <ul class="issues-list">
            <li v-for="(issue, i) in parsedIssues" :key="i" class="issue-item">
              <span class="issue-type">{{ issue.type }}</span>
              <span v-if="issue.step"> 步骤{{ issue.step }}</span>
              {{ issue.message }}
            </li>
          </ul>
        </div>

        <div v-if="parsedSuggestions.length" class="suggestions-section">
          <div class="result-label">改进建议：</div>
          <ul class="suggestions-list">
            <li v-for="(s, i) in parsedSuggestions" :key="i" class="suggestion-item">
              <span v-if="s.step">步骤{{ s.step }}：</span>{{ s.message }}
            </li>
          </ul>
        </div>

        <div class="cost-info">
          审核耗时：{{ reviewResult.costMs }}ms | 模型：{{ reviewResult.modelType }}
        </div>
      </div>

      <div v-if="reviewError" class="ai-error">{{ reviewError }}</div>

      <button
        class="btn-ai-primary"
        :disabled="!sopId || reviewLoading"
        @click="handleReview"
      >🔍 AI 审核此 SOP</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import {
  generateSopByAi,
  getExecuteGuidance,
  reviewSopByAi,
  type AiReviewResponse
} from '@/api/ai'

const props = defineProps<{ sopId?: number; visibleTabs?: string[]; autoFillExecute?: { stepTitle: string; stepDescription: string; stepIndex: number; totalSteps: number } }>()

// 默认显示所有 tab，可通过 visibleTabs 过滤
const allTabs = [
  { key: 'create', label: '✨ AI 创建' },
  { key: 'execute', label: '💡 执行指导' },
  { key: 'review', label: '🔍 AI 审核' },
]
const tabs = computed(() =>
  props.visibleTabs ? allTabs.filter(t => props.visibleTabs!.includes(t.key)) : allTabs
)
const activeTab = ref('create')

// 监听 autoFillExecute，自动切换到执行指导 tab 并填入数据
watch(() => props.autoFillExecute, (val) => {
  if (!val) return
  activeTab.value = 'execute'
  executeForm.value = {
    stepTitle: val.stepTitle,
    stepDescription: val.stepDescription,
    stepIndex: val.stepIndex,
    totalSteps: val.totalSteps,
    notes: ''
  }
}, { immediate: true })

// ========== AI 创建 ==========
const createForm = ref({ goal: '', title: '', category: '', tags: '' })
const generateLoading = ref(false)
const generateResult = ref('')
const generateError = ref('')

async function handleGenerate() {
  generateLoading.value = true
  generateError.value = ''
  generateResult.value = ''
  try {
    const res = await generateSopByAi(createForm.value)
    if (res.code === 200) {
      generateResult.value = res.data
    } else {
      generateError.value = res.message || '生成失败'
    }
  } catch (e: any) {
    generateError.value = e.message || '请求失败'
  } finally {
    generateLoading.value = false
  }
}

function applyGenerated() {
  // Emit back to parent SopEditorView to apply the generated JSON
  emit('apply-sop', generateResult.value)
}

// ========== AI 执行指导 ==========
const executeForm = ref({ stepTitle: '', stepDescription: '', stepIndex: 1, totalSteps: 5, notes: '' })
const guidanceLoading = ref(false)
const guidanceResult = ref('')
const guidanceError = ref('')

async function handleGuidance() {
  guidanceLoading.value = true
  guidanceError.value = ''
  try {
    const res = await getExecuteGuidance({
      ...executeForm.value,
      stepIndex: (executeForm.value.stepIndex || 1) - 1  // 0-based
    })
    if (res.code === 200) {
      guidanceResult.value = res.data
      const keyPoints = stripThinking(res.data)
      emit('guidance-ready', res.data)
      emit('notes-ready', keyPoints)
    } else {
      guidanceError.value = res.message || '指导失败'
    }
  } catch (e: any) {
    guidanceError.value = e.message || '请求失败'
  } finally {
    guidanceLoading.value = false
  }
}

function stripThinking(content: string): string {
  if (!content) return "";

  // 正则匹配  开头 到  结尾，支持任意字符（含换行）
  const thinkRegex = /[\s\S]*?<\/think>/g;

  // 替换为空字符串，并清理首尾多余空白
  return content.replace(thinkRegex, "").trim();
}

// ========== AI 审核 ==========
const reviewLoading = ref(false)
const reviewResult = ref<AiReviewResponse | null>(null)
const reviewError = ref('')

const parsedIssues = computed(() => {
  if (!reviewResult.value?.issues) return []
  try { return JSON.parse(reviewResult.value.issues) } catch { return [] }
})
const parsedSuggestions = computed(() => {
  if (!reviewResult.value?.suggestions) return []
  try { return JSON.parse(reviewResult.value.suggestions) } catch { return [] }
})

function verdictLabel(v: string) {
  return { pass: '✅ 通过', warning: '⚠️ 警告', reject: '❌ 拒绝' }[v] || v
}

async function handleReview() {
  if (!props.sopId) return
  reviewLoading.value = true
  reviewError.value = ''
  reviewResult.value = null
  try {
    const res = await reviewSopByAi({ sopId: props.sopId })
    if (res.code === 200) {
      reviewResult.value = res.data
    } else {
      reviewError.value = res.message || '审核失败'
    }
  } catch (e: any) {
    reviewError.value = e.message || '请求失败'
  } finally {
    reviewLoading.value = false
  }
}

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'apply-sop', json: string): void
  (e: 'guidance-ready', guidance: string): void
  (e: 'notes-ready', notes: string): void
}>()
</script>

<style scoped>
.ai-panel {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
  border-radius: 8px 8px 0 0;
}
.ai-title { font-weight: 600; color: #303133; }
.ai-close { background: none; border: none; cursor: pointer; color: #909399; font-size: 16px; }
.ai-tabs {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 8px;
}
.tab-btn {
  padding: 8px 12px;
  border: none;
  background: none;
  cursor: pointer;
  color: #606266;
  font-size: 13px;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.tab-btn.active { color: #409eff; border-bottom-color: #409eff; }
.tab-content { padding: 16px; overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 12px; }
.form-hint { font-size: 12px; color: #909399; background: #f5f7fa; padding: 8px 12px; border-radius: 4px; }
.form-group { display: flex; flex-direction: column; gap: 4px; }
.form-group label { font-size: 12px; color: #606266; font-weight: 500; }
.form-row { display: flex; gap: 12px; }
.form-row .form-group { flex: 1; }
.ai-input, .ai-select, .ai-textarea {
  width: 100%; box-sizing: border-box;
  border: 1px solid #dcdfe6; border-radius: 4px; padding: 6px 10px;
  font-size: 13px; color: #303133; background: #fff;
}
.ai-textarea { resize: vertical; font-family: inherit; }
.ai-select { background: #fff; cursor: pointer; }
.ai-loading { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #409eff; }
.loading-spinner {
  width: 14px; height: 14px; border: 2px solid #409eff; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block;
}
@keyframes spin { to { transform: rotate(360deg); } }
.ai-error { font-size: 12px; color: #f56c6c; background: #fef0f0; padding: 8px; border-radius: 4px; }
.ai-success { font-size: 13px; color: #67c23a; }
.generate-result, .guidance-result, .review-result {
  background: #f5f7fa; border-radius: 6px; padding: 12px;
}
.result-label { font-size: 12px; color: #606266; margin-bottom: 6px; font-weight: 500; }
.result-json { font-size: 11px; color: #303133; white-space: pre-wrap; word-break: break-all; max-height: 200px; overflow-y: auto; font-family: monospace; }
.guidance-text { font-size: 13px; color: #303133; white-space: pre-wrap; line-height: 1.6; }
.result-actions { margin-top: 8px; display: flex; gap: 8px; }
.verdict-badge {
  display: inline-block; padding: 4px 12px; border-radius: 4px; font-size: 14px; font-weight: 600;
}
.verdict-badge.pass { background: #e7f7e7; color: #67c23a; }
.verdict-badge.warning { background: #fff3e0; color: #e6a23c; }
.verdict-badge.reject { background: #fee; color: #f56c6c; }
.score-label { margin-top: 8px; font-size: 13px; color: #303133; }
.issues-section, .suggestions-section { margin-top: 8px; }
.issues-list, .suggestions-list { padding-left: 18px; font-size: 12px; color: #303133; }
.issue-type { font-weight: 600; color: #f56c6c; text-transform: uppercase; font-size: 10px; }
.suggestion-item { color: #409eff; margin: 4px 0; }
.cost-info { font-size: 11px; color: #909399; margin-top: 8px; }
.btn-ai-primary {
  background: #409eff; color: #fff; border: none; border-radius: 4px;
  padding: 8px 16px; cursor: pointer; font-size: 13px;
}
.btn-ai-primary:disabled { background: #a0cfff; cursor: not-allowed; }
.btn-apply {
  background: #67c23a; color: #fff; border: none; border-radius: 4px;
  padding: 6px 12px; cursor: pointer; font-size: 12px;
}
.btn-regenerate {
  background: #909399; color: #fff; border: none; border-radius: 4px;
  padding: 6px 12px; cursor: pointer; font-size: 12px;
}
</style>
