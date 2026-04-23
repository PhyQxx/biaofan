<template>
  <div class="versions-page">
    <div class="topbar">
      <div class="topbar-left">
        <button class="btn-back" @click="router.back()">← 返回</button>
        <span class="sop-title">{{ sopTitle }}</span>
      </div>
      <div class="topbar-right">
        <button class="btn-secondary" @click="router.push(`/sop/${sopId}/edit`)">编辑 SOP</button>
      </div>
    </div>

    <div class="page-body">
      <div class="section-title">📋 版本历史</div>

      <div class="version-list" v-if="versions.length">
        <div
          v-for="v in versions"
          :key="v.id"
          class="version-card"
          :class="{ current: v.isCurrent }"
        >
          <div class="version-header">
            <div class="version-info">
              <span class="version-num">v{{ v.version }}</span>
              <span v-if="v.isCurrent" class="current-badge">当前版本</span>
              <span class="version-date">{{ formatDate(v.createdAt) }}</span>
            </div>
            <div class="version-actions" v-if="!v.isCurrent">
              <button class="btn-sm" @click="viewVersion(v)">查看</button>
              <button class="btn-sm btn-rollback" @click="rollback(v)">回滚</button>
            </div>
          </div>
          <div class="version-summary">{{ v.changeSummary || '无变更说明' }}</div>
          <div class="version-steps" v-if="expandedId === v.id">
            <div v-for="(step, i) in parseSteps(v.content)" :key="i" class="step-item">
              <div class="step-num">{{ Number(i) + 1 }}</div>
              <div class="step-body">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-desc" v-if="step.description">{{ step.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无版本记录</p>
        <p class="empty-sub">发布 SOP 后会产生版本历史</p>
      </div>
    </div>

    <!-- Version Detail Modal -->
    <div class="modal-overlay" v-if="viewingVersion" @click.self="viewingVersion = null">
      <div class="modal">
        <div class="modal-header">
          <span class="modal-title">v{{ viewingVersion.version }} 版本详情</span>
          <button class="modal-close" @click="viewingVersion = null">✕</button>
        </div>
        <div class="modal-body">
          <div class="modal-meta">
            <span>版本号：v{{ viewingVersion.version }}</span>
            <span>发布时间：{{ formatDate(viewingVersion.createdAt) }}</span>
            <span>变更说明：{{ viewingVersion.changeSummary || '无' }}</span>
          </div>
          <div class="steps-preview">
            <div v-for="(step, i) in parseSteps(viewingVersion.content)" :key="i" class="step-item">
              <div class="step-num">{{ Number(i) + 1 }}</div>
              <div class="step-body">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-desc" v-if="step.description">{{ step.description }}</div>
                <div class="step-duration" v-if="step.duration">⏱ {{ step.duration }} 分钟</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * SOP 版本历史页
 * - 展示某 SOP 的所有版本
 * - 版本号、创建时间、状态
 * - 可切换查看不同版本
 */
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const route = useRoute()
const router = useRouter()
const sopId = Number(route.params.id)
const sopTitle = ref('')
const versions = ref<any[]>([])
const expandedId = ref<number | null>(null)
const viewingVersion = ref<any>(null)

const formatDate = (d: string) => d ? new Date(d).toLocaleString('zh-CN') : '-'

const parseSteps = (content: any) => {
  if (!content) return []
  try { return typeof content === 'string' ? JSON.parse(content) : content } catch { return [] }
}

const viewVersion = (v: any) => { viewingVersion.value = v }

const rollback = async (v: any) => {
  try {
    await ElMessageBox.confirm(`确定回滚到 v${v.version}？当前内容将被快照保存。`, '确认回滚', { type: 'warning' })
    const res: any = await request.post(`/sop/${sopId}/rollback/${v.version}`)
    if (res.code === 200) {
      ElMessage.success('回滚成功')
      loadVersions()
    } else {
      ElMessage.error(res.message || '回滚失败')
    }
  } catch (e: unknown) {
    const err = e as { message?: string }
    if (err.message !== 'cancel') {
      console.error('[SopVersionsView] rollback failed:', e)
    }
  }
}

const loadVersions = async () => {
  try {
    const res: any = await request.get(`/sop/${sopId}`)
    if (res.code === 200) sopTitle.value = res.data.title
  } catch (e) {
    console.error('[SopVersionsView] loadSopTitle failed:', e)
  }
  const res: any = await request.get(`/sop/${sopId}/versions`)
  if (res.code === 200) versions.value = res.data || []
}

onMounted(loadVersions)
</script>

<style scoped>
.versions-page { min-height: 100vh; background: var(--color-bg-light); }
.topbar { display: flex; align-items: center; justify-content: space-between; padding: 12px 24px; background: var(--color-bg-light-elevated); border-bottom: 1px solid var(--color-border-light); }
.topbar-left { display: flex; align-items: center; gap: 12px; }
.btn-back { background: none; border: none; font-size: 14px; color: var(--color-text-light-secondary); cursor: pointer; padding: 6px 12px; border-radius: 6px; }
.btn-back:hover { background: var(--color-bg-light); }
.sop-title { font-size: 16px; font-weight: 600; color: var(--color-text-light-primary); }
.btn-secondary { height: 36px; padding: 0 16px; background: var(--color-bg-light-elevated); color: var(--color-text-light-primary); border: 1.5px solid var(--color-border-light); border-radius: 8px; font-size: 14px; cursor: pointer; }
.page-body { max-width: 720px; margin: 24px auto; padding: 0 24px; }
.section-title { font-size: 15px; font-weight: 600; color: var(--color-text-light-primary); margin-bottom: 16px; }
.version-list { display: flex; flex-direction: column; gap: 12px; }
.version-card { background: var(--color-bg-light-elevated); border-radius: 12px; padding: 16px; border: 1px solid var(--color-border-light); transition: border-color 0.15s; }
.version-card:hover { border-color: #D0D8FF; }
.version-card.current { border-color: var(--color-primary); background: #F8FAFF; }
.version-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.version-info { display: flex; align-items: center; gap: 10px; }
.version-num { font-size: 15px; font-weight: 700; color: var(--color-text-light-primary); }
.current-badge { font-size: 11px; background: #E8ECFF; color: var(--color-primary); padding: 2px 8px; border-radius: 4px; font-weight: 600; }
.version-date { font-size: 12px; color: var(--color-text-light-muted); }
.version-actions { display: flex; gap: 6px; }
.btn-sm { height: 28px; padding: 0 12px; background: var(--color-bg-light-elevated); color: var(--color-text-light-primary); border: 1px solid var(--color-border-light); border-radius: 6px; font-size: 12px; cursor: pointer; }
.btn-sm:hover { background: var(--color-bg-light); }
.btn-rollback { color: var(--color-warning); border-color: var(--color-warning); }
.btn-rollback:hover { background: #FFF8E6; }
.version-summary { font-size: 13px; color: var(--color-text-light-secondary); }
.version-steps { margin-top: 12px; padding-top: 12px; border-top: 1px solid #F0F0F0; display: flex; flex-direction: column; gap: 8px; }
.step-item { display: flex; gap: 10px; }
.step-num { width: 24px; height: 24px; background: #E8ECFF; color: var(--color-primary); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; flex-shrink: 0; }
.step-body { flex: 1; }
.step-title { font-size: 13px; font-weight: 600; color: var(--color-text-light-primary); }
.step-desc { font-size: 12px; color: var(--color-text-light-muted); margin-top: 2px; }
.step-duration { font-size: 11px; color: var(--color-text-light-muted); margin-top: 2px; }
.empty-state { text-align: center; padding: 40px; color: var(--color-text-light-muted); }
.empty-sub { font-size: 13px; margin-top: 6px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--color-bg-light-elevated); border-radius: 16px; width: 560px; max-height: 80vh; overflow-y: auto; }
.modal-header { padding: 16px 20px; border-bottom: 1px solid var(--color-border-light); display: flex; justify-content: space-between; align-items: center; }
.modal-title { font-size: 15px; font-weight: 700; }
.modal-close { background: none; border: none; font-size: 18px; cursor: pointer; color: var(--color-text-light-muted); }
.modal-body { padding: 16px 20px; }
.modal-meta { display: flex; flex-wrap: wrap; gap: 12px; font-size: 12px; color: var(--color-text-light-secondary); margin-bottom: 16px; }
.steps-preview { display: flex; flex-direction: column; gap: 10px; }
</style>
