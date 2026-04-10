<template>
  <div class="version-topbar">
    <button class="btn-back" @click="router.push(`/sop/${sopId}/edit`)">← 返回</button>
    <div class="topbar-title">版本历史</div>
    <div class="topbar-right">
      <button class="btn-publish" @click="showPublishModal = true">发布新版本</button>
    </div>
  </div>

  <div class="page-body">
      <!-- SOP Info -->
      <div class="sop-info-card" v-if="sop">
        <div class="sop-info-title">{{ sop.title }}</div>
        <div class="sop-info-meta">
          <span>当前版本：v{{ sop.version }}</span>
          <span>•</span>
          <span>{{ statusLabel }}</span>
        </div>
      </div>

      <!-- Version List -->
      <div class="version-list" v-if="versions.length">
        <div v-for="v in versions" :key="v.id" class="version-card" :class="{ current: v.isCurrent }">
          <div class="version-badge" :class="{ current: v.isCurrent }">
            {{ v.isCurrent ? '当前' : `v${v.version}` }}
          </div>
          <div class="version-body">
            <div class="version-header">
              <div class="version-num">版本 {{ v.version }}</div>
              <div class="version-time">{{ formatDate(v.createTime) }}</div>
            </div>
            <div class="version-summary" v-if="v.changeSummary">{{ v.changeSummary }}</div>
            <div class="version-summary empty" v-else>无变更说明</div>
          </div>
          <div class="version-actions">
            <button class="btn-text" @click="viewVersion(v)">查看</button>
            <button v-if="!v.isCurrent" class="btn-rollback" @click="confirmRollback(v)">回滚</button>
          </div>
        </div>
      </div>
      <div v-else class="empty-state">
        <p>暂无版本记录</p>
        <p class="empty-sub">发布 SOP 后将自动创建版本</p>
      </div>
    </div>

    <!-- Publish Modal -->
    <div class="modal-overlay" v-if="showPublishModal" @click.self="showPublishModal = false">
      <div class="modal-card">
        <div class="modal-header">发布新版本</div>
        <div class="modal-body">
          <div class="form-label">变更说明</div>
          <textarea v-model="changeSummary" class="modal-textarea" placeholder="描述本次变更内容..." rows="4"></textarea>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showPublishModal = false">取消</button>
          <button class="btn-confirm" @click="doPublish" :disabled="publishing">
            {{ publishing ? '发布中...' : '确认发布' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Version Detail Modal -->
    <div class="modal-overlay" v-if="showDetailModal" @click.self="showDetailModal = false">
      <div class="modal-card modal-large">
        <div class="modal-header">版本 {{ selectedVersion?.version }} 详情</div>
        <div class="modal-body">
          <div class="detail-meta">发布时间：{{ formatDate(selectedVersion?.createTime) }}</div>
          <div class="detail-summary" v-if="selectedVersion?.changeSummary">
            变更说明：{{ selectedVersion.changeSummary }}
          </div>
          <div v-if="versionSteps.length" class="detail-steps">
            <div v-for="(step, idx) in versionSteps" :key="idx" class="detail-step">
              <div class="detail-step-num">{{ getStepNumber(idx) }}</div>
              <div class="detail-step-content">
                <div class="detail-step-title">{{ step.title }}</div>
                <div class="detail-step-desc" v-if="step.description">{{ step.description }}</div>
                <div class="detail-step-check" v-if="step.checkItems && step.checkItems.length">
                  <span class="check-tag">📋 含 {{ step.checkItems.length }} 个检查项</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showDetailModal = false">关闭</button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">


/**
 * SOP 版本详情对比页
 * - 展示指定版本的完整内容
 * - 支持与当前版本对比
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api'

const route = useRoute()
const router = useRouter()
const sopId = Number(route.params.id)

const sop = ref<any>(null)
const versions = ref<any[]>([])
const showPublishModal = ref(false)
const showDetailModal = ref(false)
const selectedVersion = ref<any>(null)
const versionSteps = ref<any[]>([])
const changeSummary = ref('')
const publishing = ref(false)

const getStepNumber = (idx: number) => idx + 1

const statusLabel = computed(() => {
  const s = sop.value?.status
  return s === 'published' ? '已发布' : s === 'draft' ? '草稿' : s
})

const formatDate = (d: string) => {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

const loadVersions = async () => {
  const res: any = await request.get(`/sop/${sopId}/versions`)
  if (res.code === 200) {
    versions.value = res.data || []
  }
}

const loadSop = async () => {
  const res: any = await request.get(`/sop/${sopId}`)
  if (res.code === 200) {
    sop.value = res.data
  }
}

const viewVersion = async (v: any) => {
  selectedVersion.value = v
  try {
    versionSteps.value = (v.content && v.content !== 'null' && v.content !== 'undefined') ? JSON.parse(v.content) : []
  } catch { versionSteps.value = [] }
  showDetailModal.value = true
}

const doPublish = async () => {
  publishing.value = true
  try {
    const res: any = await request.post(`/sop/${sopId}/publish`, {
      changeSummary: changeSummary.value,
    })
    if (res.code === 200) {
      ElMessage.success('新版本发布成功')
      showPublishModal.value = false
      changeSummary.value = ''
      await loadVersions()
      await loadSop()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

const confirmRollback = async (v: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到版本 v${v.version} 吗？这将创建新版本记录。`,
      '确认回滚',
      { confirmButtonText: '确认回滚', cancelButtonText: '取消', type: 'warning' }
    )
    const res: any = await request.post(`/sop/${sopId}/rollback/${v.version}`)
    if (res.code === 200) {
      ElMessage.success('回滚成功')
      await loadVersions()
      await loadSop()
    } else {
      ElMessage.error(res.message || '回滚失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '回滚失败')
  }
}

onMounted(async () => {
  await Promise.all([loadSop(), loadVersions()])
})
</script>

<style scoped>
.version-page { min-height: 100vh; background: #F5F7FA; }

.version-topbar {
  display: flex; align-items: center; padding: 12px 20px;
  background: #fff; border-bottom: 1px solid #E8E8E8; gap: 12px;
  position: sticky; top: 0; z-index: 100;
}
.btn-back { border: none; background: transparent; font-size: 18px; cursor: pointer; padding: 4px 8px; color: #333; }
.topbar-title { flex: 1; font-size: 16px; font-weight: 600; color: #212121; text-align: center; }
.btn-publish { height: 32px; padding: 0 14px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; }

.page-body { max-width: 720px; margin: 0 auto; padding: 20px; }

.sop-info-card { background: #fff; border-radius: 12px; padding: 16px 20px; margin-bottom: 20px; border: 1px solid #E8E8E8; }
.sop-info-title { font-size: 16px; font-weight: 600; color: #212121; margin-bottom: 6px; }
.sop-info-meta { display: flex; gap: 8px; font-size: 13px; color: #999; }

.version-list { display: flex; flex-direction: column; gap: 10px; }
.version-card {
  background: #fff; border-radius: 12px; padding: 16px;
  display: flex; align-items: flex-start; gap: 14px;
  border: 1px solid #E8E8E8;
}
.version-card.current { border-color: #5B7FFF; background: #F8FAFF; }
.version-badge {
  width: 52px; height: 52px; border-radius: 12px; background: #F0F0F0;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; color: #666; flex-shrink: 0;
}
.version-badge.current { background: #5B7FFF; color: white; }
.version-body { flex: 1; }
.version-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.version-num { font-size: 14px; font-weight: 600; color: #333; }
.version-time { font-size: 12px; color: #999; }
.version-summary { font-size: 13px; color: #666; line-height: 1.5; }
.version-summary.empty { color: #CCC; font-style: italic; }
.version-actions { display: flex; flex-direction: column; gap: 6px; }
.btn-text { border: none; background: none; font-size: 13px; color: #5B7FFF; cursor: pointer; padding: 2px 6px; text-align: right; }
.btn-rollback { border: 1px solid #FF4D4F; background: none; font-size: 12px; color: #FF4D4F; border-radius: 6px; padding: 3px 10px; cursor: pointer; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-card { background: #fff; border-radius: 16px; width: 90%; max-width: 420px; overflow: hidden; }
.modal-large { max-width: 560px; max-height: 80vh; display: flex; flex-direction: column; }
.modal-large .modal-body { overflow-y: auto; flex: 1; }
.modal-header { padding: 18px 20px 14px; font-size: 16px; font-weight: 600; color: #212121; border-bottom: 1px solid #F0F0F0; }
.modal-body { padding: 16px 20px; }
.modal-footer { padding: 14px 20px; display: flex; gap: 10px; justify-content: flex-end; border-top: 1px solid #F0F0F0; }
.form-label { font-size: 13px; font-weight: 500; color: #666; margin-bottom: 8px; }
.modal-textarea { width: 100%; padding: 10px 12px; border: 1.5px solid #E8E8E8; border-radius: 8px; font-size: 14px; resize: vertical; outline: none; box-sizing: border-box; }
.modal-textarea:focus { border-color: #5B7FFF; box-shadow: 0 0 0 3px rgba(91,127,255,0.10); }
.btn-cancel { height: 38px; padding: 0 18px; background: #fff; color: #333; border: 1.5px solid #E8E8E8; border-radius: 8px; font-size: 14px; cursor: pointer; }
.btn-confirm { height: 38px; padding: 0 18px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 500; cursor: pointer; }
.btn-confirm:disabled { opacity: 0.6; cursor: not-allowed; }

.detail-meta { font-size: 13px; color: #999; margin-bottom: 10px; }
.detail-summary { font-size: 13px; color: #666; margin-bottom: 14px; padding: 10px; background: #F5F7FA; border-radius: 8px; }
.detail-steps { display: flex; flex-direction: column; gap: 10px; }
.detail-step { display: flex; gap: 12px; }
.detail-step-num { width: 24px; height: 24px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; margin-top: 2px; }
.detail-step-content { flex: 1; }
.detail-step-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 4px; }
.detail-step-desc { font-size: 13px; color: #666; margin-bottom: 4px; }
.check-tag { font-size: 12px; color: #5B7FFF; background: #E8ECFF; padding: 2px 8px; border-radius: 4px; }

.empty-state { text-align: center; padding: 60px 0; color: #999; }
.empty-sub { font-size: 13px; margin-top: 6px; }
</style>
