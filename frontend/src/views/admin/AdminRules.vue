<template>
  <div class="page-header">
    <h2 class="page-title">📈 成长规则配置</h2>
    <button class="btn-primary" @click="handleSaveAll" :disabled="saving">
      {{ saving ? '保存中...' : '💾 保存全部' }}
    </button>
  </div>

  <div v-if="loading" class="loading-state">加载中...</div>
  <template v-else>
    <div class="config-section">
      <div class="section-header">
        <h3>配置项列表</h3>
        <p class="section-desc">管理系统中所有的成长与奖励规则</p>
      </div>
      <div class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>规则类型</th>
              <th>规则标识</th>
              <th>规则值</th>
              <th>备注</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td><span class="type-tag">{{ rule.ruleType }}</span></td>
              <td class="key-cell"><code>{{ rule.ruleKey }}</code></td>
              <td>
                <input v-model="rule.ruleValue" class="form-input" />
              </td>
              <td class="text-muted">{{ rule.comment || '-' }}</td>
              <td>
                <span class="status-tag" :class="rule.isActive ? 'active' : 'inactive'">
                  {{ rule.isActive ? '启用' : '禁用' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </template>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getGrowthRules, updateGrowthRules, type GrowthRule } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const rules = ref<GrowthRule[]>([])

const loadData = async () => {
  loading.value = true
  try {
    const resp = await getGrowthRules()
    rules.value = resp.data || []
  } catch {
    ElMessage.error('加载成长规则失败')
  } finally {
    loading.value = false
  }
}

const handleSaveAll = async () => {
  saving.value = true
  try {
    await updateGrowthRules(rules.value)
    ElMessage.success('成长规则保存成功')
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-xl); }
.page-title { font-size: var(--font-size-3xl); font-weight: 600; color: var(--color-text-primary); }
.loading-state { text-align: center; color: var(--color-text-secondary); padding: 60px 0; }
.config-section { margin-bottom: 32px; }
.section-header { margin-bottom: var(--space-md); }
.section-header h3 { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-primary); }
.section-desc { font-size: var(--font-size-sm); color: var(--color-text-secondary); margin-top: 4px; }
.card { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-xl); }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; font-size: var(--font-size-xs); font-weight: 600; color: var(--color-text-secondary); padding: var(--space-sm) var(--space-md); border-bottom: 1px solid var(--color-border); }
.data-table td { padding: 12px var(--space-md); border-bottom: 1px solid var(--color-border); font-size: var(--font-size-base); color: var(--color-text-primary); vertical-align: middle; }
.type-tag { background: var(--color-primary-subtle); color: var(--color-primary); padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.key-cell code { font-family: monospace; color: var(--color-text-secondary); }
.form-input { background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: 4px; padding: 6px 10px; width: 100%; color: var(--color-text-primary); }
.status-tag { font-size: 12px; padding: 2px 6px; border-radius: 4px; }
.status-tag.active { background: var(--color-success-subtle); color: var(--color-success); }
.status-tag.inactive { background: var(--color-bg-surface); color: var(--color-text-muted); }
.text-muted { color: var(--color-text-muted); font-size: 13px; }
.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: 6px; padding: 10px 20px; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { background: var(--color-border); cursor: not-allowed; }
</style>
