<template>
  <div class="page-header">
    <h2 class="page-title">📋 模板审核</h2>
      <div class="header-controls">
        <select v-model="filterStatus" class="filter-select" @change="fetchList">
          <option value="">全部状态</option>
          <option value="pending">待审核</option>
          <option value="approved">已通过</option>
          <option value="rejected">已驳回</option>
          <option value="offline">已下架</option>
        </select>
      </div>
    </div>

    <!-- Audit Cards -->
    <div class="audit-section">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="pendingList.length === 0 && filterStatus !== '' && filterStatus !== 'pending'" class="empty-state">
        <div class="empty-icon">📋</div>
        <div>暂无审核记录</div>
      </div>
      <div v-else>
        <!-- Pending cards -->
        <template v-if="filterStatus === '' || filterStatus === 'pending'">
          <div v-if="pendingList.length > 0" class="section-title-row">
            <span class="section-label">⏳ 待审核 ({{ pendingList.length }})</span>
          </div>
          <div class="audit-list">
            <div v-for="tpl in pendingList" :key="tpl.templateId || tpl.id" class="audit-card">
              <div class="card-left">
                <div class="card-cover">
                  <img v-if="tpl.coverUrl" :src="tpl.coverUrl" :alt="tpl.title" />
                  <div v-else class="cover-placeholder">📋</div>
                </div>
              </div>
              <div class="card-info">
                <div class="card-title">{{ tpl.title }}</div>
                <div class="card-meta">
                  <span>{{ tpl.authorName }}</span>
                  <span class="dot">·</span>
                  <span>{{ formatDate(tpl.createdAt) }}</span>
                  <span class="dot">·</span>
                  <CategoryTag :category="tpl.category" />
                </div>
                <p v-if="tpl.description" class="card-desc">{{ tpl.description }}</p>
                <div class="card-stats">
                  <span>{{ tpl.stepCount || 0 }} 步骤</span>
                  <span class="dot">·</span>
                  <span>使用 {{ tpl.useCount || 0 }} 次</span>
                </div>
              </div>
              <div class="card-actions">
                <button class="btn-reject" @click="openReject(tpl)">驳回</button>
                <button class="btn-approve" @click="handleApprove(tpl)">通过</button>
              </div>
            </div>
          </div>
        </template>

        <!-- Other list -->
        <template v-if="filterStatus !== 'pending' && filterStatus !== ''">
          <div class="section-title-row">
            <span class="section-label">{{ statusLabel(filterStatus) }} ({{ auditList.length }})</span>
          </div>
          <div class="audit-list">
            <div v-for="tpl in auditList" :key="tpl.templateId || tpl.id" class="audit-card">
              <div class="card-left">
                <div class="card-cover">
                  <img v-if="tpl.coverUrl" :src="tpl.coverUrl" :alt="tpl.title" />
                  <div v-else class="cover-placeholder">📋</div>
                </div>
              </div>
              <div class="card-info">
                <div class="card-title">{{ tpl.title }}</div>
                <div class="card-meta">
                  <span>{{ tpl.authorName }}</span>
                  <span class="dot">·</span>
                  <span>{{ formatDate(tpl.createdAt) }}</span>
                </div>
                <div v-if="tpl.rejectReason" class="reject-reason">
                  驳回原因：{{ tpl.rejectReason }}
                </div>
              </div>
              <div class="card-actions">
                <StatusBadge :status="tpl.status" />
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="page-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>

    <!-- Reject Dialog -->
    <div v-if="rejectDialogVisible" class="dialog-overlay" @click.self="rejectDialogVisible = false">
      <div class="dialog">
        <div class="dialog-title">驳回原因</div>
        <div class="dialog-body">
          <p>模板 "<strong>{{ selectedTemplate?.title }}</strong>"</p>
          <div class="form-group">
            <label>驳回原因（必填，至少10字）</label>
            <textarea
              v-model="rejectForm.reason"
              class="form-textarea"
              placeholder="请详细说明驳回原因..."
              rows="4"
            ></textarea>
            <div class="char-count" :class="{ warn: rejectForm.reason.length < 10 }">
              {{ rejectForm.reason.length }} / 10 字以上
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="rejectDialogVisible = false">取消</button>
          <button
            class="btn-reject-confirm"
            :disabled="rejectForm.reason.length < 10 || rejecting"
            @click="confirmReject"
          >
            {{ rejecting ? '提交中...' : '确认驳回' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getAuditList,
  auditTemplate,
  type Template
} from '@/api/marketplace'
import CategoryTag from '@/views/marketplace/CategoryTag.vue'
import StatusBadge from './StatusBadge.vue'

const PAGE_SIZE = 20

const filterStatus = ref('')
const auditList = ref<Template[]>([])
const pendingList = ref<Template[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE) || 1)

const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const selectedTemplate = ref<Template | null>(null)
const rejectForm = ref({ reason: '' })

const statusLabel = (s: string) => {
  const map: Record<string, string> = { approved: '已通过', rejected: '已驳回', offline: '已下架' }
  return map[s] || s
}

const formatDate = (d?: string) => d ? d.slice(0, 10) : ''

const fetchList = async () => {
  loading.value = true
  try {
    if (filterStatus.value === 'pending' || filterStatus.value === '') {
      // fetch pending first
      const res = await getAuditList({ status: 'pending' })
      pendingList.value = res.data?.templates || []
    }
    const status = filterStatus.value || undefined
    const res = await getAuditList({ status, page: page.value, page_size: PAGE_SIZE })
    if (res.success) {
      auditList.value = res.data?.templates || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    ElMessage.error('加载审核列表失败')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (tpl: Template) => {
  try {
    const id = tpl.templateId || String(tpl.id)
    const res = await auditTemplate(id, { status: 'approved' })
    if (res.success) {
      ElMessage.success('已通过审核')
      pendingList.value = pendingList.value.filter((x: Template) => (x.templateId || x.id) !== (tpl.templateId || tpl.id))
      fetchList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e) {
    ElMessage.error('审核操作失败')
  }
}

const openReject = (tpl: Template) => {
  selectedTemplate.value = tpl
  rejectForm.value.reason = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (!selectedTemplate.value) return
  rejecting.value = true
  try {
    const id = selectedTemplate.value.templateId || String(selectedTemplate.value.id)
    const res = await auditTemplate(id, { status: 'rejected', reject_reason: rejectForm.value.reason })
    if (res.success) {
      ElMessage.success('已驳回')
      rejectDialogVisible.value = false
      pendingList.value = pendingList.value.filter((x: Template) => (x.templateId || x.id) !== (selectedTemplate.value!.templateId || selectedTemplate.value!.id))
      fetchList()
    } else {
      ElMessage.error(res.message || '驳回失败')
    }
  } catch (e) {
    ElMessage.error('驳回操作失败')
  } finally {
    rejecting.value = false
  }
}

const changePage = (p: number) => {
  page.value = p
  fetchList()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #E8EAF0;
}

.header-controls { display: flex; gap: 8px; }

.filter-select {
  padding: 8px 12px;
  background: #1A1D27;
  border: 1px solid #2D3348;
  border-radius: 8px;
  color: #E8EAF0;
  font-size: 14px;
  outline: none;
}

.audit-section {}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 0;
  color: #8B90A0;
  font-size: 14px;
}

.empty-icon { font-size: 48px; margin-bottom: 12px; }

.section-title-row {
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: #E8EAF0;
}

.audit-list { display: flex; flex-direction: column; gap: 12px; }

.audit-card {
  background: #1A1D27;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  gap: 16px;
  border: 1px solid #2D3348;
  transition: border-color 0.2s;
}

.audit-card:hover { border-color: #3D4358; }

.card-left { flex-shrink: 0; }

.card-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #2D3348;
}

.card-cover img { width: 100%; height: 100%; object-fit: cover; }

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #555A6E;
}

.card-info { flex: 1; min-width: 0; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #E8EAF0;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8B90A0;
  margin-bottom: 6px;
}

.dot { color: #555A6E; }

.card-desc {
  font-size: 13px;
  color: #8B90A0;
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-stats {
  font-size: 12px;
  color: #555A6E;
  display: flex;
  align-items: center;
  gap: 4px;
}

.reject-reason {
  font-size: 12px;
  color: #FCA5A5;
  margin-top: 4px;
  background: rgba(239, 68, 68, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
  flex-shrink: 0;
}

.btn-reject {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #EF4444;
  background: transparent;
  color: #EF4444;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-reject:hover { background: rgba(239, 68, 68, 0.1); }

.btn-approve {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: #10B981;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-approve:hover { background: #059669; }

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.page-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #2D3348;
  background: transparent;
  color: #8B90A0;
  font-size: 14px;
  cursor: pointer;
}

.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-btn:not(:disabled):hover { border-color: #5B7FFF; color: #5B7FFF; }

.page-info { font-size: 14px; color: #8B90A0; }

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: #1A1D27;
  border-radius: 16px;
  width: 440px;
  max-width: 90vw;
  border: 1px solid #2D3348;
}

.dialog-title {
  padding: 20px 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #E8EAF0;
}

.dialog-body {
  padding: 16px 24px;
  font-size: 14px;
  color: #8B90A0;
}

.form-group { margin-top: 16px; }
.form-group label { display: block; font-size: 14px; color: #8B90A0; margin-bottom: 8px; }

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #2D3348;
  border-radius: 8px;
  font-size: 14px;
  background: #0F1117;
  color: #E8EAF0;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.form-textarea:focus { border-color: #5B7FFF; }

.char-count {
  font-size: 12px;
  color: #555A6E;
  text-align: right;
  margin-top: 4px;
}

.char-count.warn { color: #F59E0B; }

.dialog-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #2D3348;
}

.btn-cancel {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #2D3348;
  background: transparent;
  color: #8B90A0;
  font-size: 14px;
  cursor: pointer;
}

.btn-reject-confirm {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  background: #EF4444;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.btn-reject-confirm:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
