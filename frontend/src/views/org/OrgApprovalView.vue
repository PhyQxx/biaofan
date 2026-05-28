<template>
  <div class="org-approval-page">
    <div class="page-header">
      <h2 class="page-title">📝 SOP 审核中心</h2>
      <p class="page-desc">审核组织成员提交的 SOP，确保流程标准与合规</p>
    </div>

    <div class="card" v-if="loading">
      <div class="loading-state">加载中...</div>
    </div>
    
    <div v-else-if="pendingList.length === 0" class="card empty-card">
      <div class="empty-state">
        <span class="empty-icon">🎉</span>
        <h3>暂无待审核申请</h3>
        <p>组织内所有的 SOP 都已处理完毕</p>
      </div>
    </div>

    <div class="approval-list" v-else>
      <div v-for="item in pendingList" :key="item.id" class="card approval-item">
        <div class="item-header">
          <div class="sop-info">
            <h3 class="sop-title">{{ item.sopTitle || '加载中...' }}</h3>
            <div class="item-meta">
              <span class="meta-tag">ID: {{ item.sopId }}</span>
              <span class="dot">·</span>
              <span class="submitter">提交人: {{ item.submitterName || '用户' + item.submitterId }}</span>
              <span class="dot">·</span>
              <span class="time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </div>
          <div class="item-actions">
            <button class="btn-secondary" @click="viewSop(item.sopId)">预览</button>
            <button class="btn-danger" @click="openActionDialog(item, 'reject')">驳回</button>
            <button class="btn-primary" @click="openActionDialog(item, 'approve')">通过</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Action Dialog -->
    <div class="dialog-overlay" v-if="dialog.visible" @click.self="dialog.visible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ dialog.type === 'approve' ? '审核通过' : '驳回申请' }}</h3>
        </div>
        <div class="dialog-body">
          <p class="dialog-hint" v-if="dialog.type === 'approve'">该 SOP 将立即发布，组织内所有成员均可查看到并执行。</p>
          <p class="dialog-hint" v-else>请说明驳回原因，方便创建者进行修改。</p>
          <textarea 
            v-model="dialog.comment" 
            class="comment-input" 
            placeholder="输入审核意见 (可选)..."
          ></textarea>
        </div>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="dialog.visible = false">取消</button>
          <button 
            :class="dialog.type === 'approve' ? 'btn-primary' : 'btn-danger'"
            @click="handleAction"
            :disabled="submitting"
          >
            {{ submitting ? '处理中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useSopStore } from '@/stores/sop'

const router = useRouter()
const authStore = useAuthStore()
const sopStore = useSopStore()

const loading = ref(false)
const submitting = ref(false)
const pendingList = ref<any[]>([])
const currentOrgId = computed(() => authStore.currentOrgId)

const dialog = reactive({
  visible: false,
  type: 'approve' as 'approve' | 'reject',
  comment: '',
  item: null as any
})

async function fetchList() {
  if (!currentOrgId.value) {
    ElMessage.warning('请先选择一个组织空间')
    return
  }
  loading.value = true
  try {
    const res = await sopStore.fetchPendingApprovals(currentOrgId.value)
    if (res.code === 200) {
      pendingList.value = res.data
      // 补充获取 SOP 标题 (简单处理)
      for (const item of pendingList.value) {
        const sRes = await sopStore.getSopById(item.sopId)
        if (sRes.code === 200) {
          item.sopTitle = sRes.data.title
        }
      }
    }
  } catch (e) {
    ElMessage.error('获取列表失败')
  } finally {
    loading.value = false
  }
}

function formatDate(d: string) {
  if (!d) return '-'
  return d.replace('T', ' ').substring(0, 16)
}

function viewSop(id: number) {
  router.push(`/sop/${id}/edit`)
}

function openActionDialog(item: any, type: 'approve' | 'reject') {
  dialog.item = item
  dialog.type = type
  dialog.comment = ''
  dialog.visible = true
}

async function handleAction() {
  if (!dialog.item) return
  submitting.value = true
  try {
    if (dialog.type === 'approve') {
      await sopStore.approveSop(dialog.item.sopId, dialog.comment)
      ElMessage.success('已审核通过')
    } else {
      await sopStore.rejectSop(dialog.item.sopId, dialog.comment)
      ElMessage.success('已驳回申请')
    }
    dialog.visible = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

onMounted(fetchList)
</script>

<style scoped>
.org-approval-page { max-width: 900px; margin: 0 auto; padding: 32px 24px; }
.page-header { margin-bottom: 32px; }
.page-title { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
.page-desc { color: var(--color-text-secondary); }

.card { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: 16px; padding: 24px; margin-bottom: 16px; }
.loading-state { text-align: center; padding: 60px; color: #999; }
.empty-card { text-align: center; padding: 80px 40px; }
.empty-icon { font-size: 48px; display: block; margin-bottom: 16px; }
.empty-card h3 { margin-bottom: 8px; }
.empty-card p { color: #999; }

.approval-item { 
  transition: transform 0.2s;
  border-left: 4px solid var(--color-primary);
}
.approval-item:hover { transform: translateX(4px); }

.item-header { display: flex; justify-content: space-between; align-items: center; }
.sop-title { font-size: 18px; font-weight: 600; margin-bottom: 8px; }
.item-meta { font-size: 13px; color: #999; display: flex; align-items: center; gap: 8px; }
.dot { font-weight: bold; }
.meta-tag { background: #f0f2f5; padding: 2px 6px; border-radius: 4px; font-family: monospace; }

.item-actions { display: flex; gap: 12px; }
.btn-primary { background: #5B7FFF; color: #fff; border: none; border-radius: 8px; padding: 8px 16px; font-weight: 500; cursor: pointer; }
.btn-danger { background: #fff; color: #FF4D4F; border: 1.5px solid #FF4D4F; border-radius: 8px; padding: 8px 16px; font-weight: 500; cursor: pointer; }
.btn-secondary { background: #fff; color: #666; border: 1.5px solid #ddd; border-radius: 8px; padding: 8px 16px; font-weight: 500; cursor: pointer; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: #fff; border-radius: 16px; width: 440px; box-shadow: 0 20px 40px rgba(0,0,0,0.2); }
.dialog-header { padding: 20px 24px; border-bottom: 1px solid #eee; }
.dialog-body { padding: 24px; }
.dialog-hint { font-size: 14px; color: #666; margin-bottom: 16px; line-height: 1.5; }
.comment-input { width: 100%; height: 100px; padding: 12px; border: 1.5px solid #eee; border-radius: 8px; resize: none; font-size: 14px; box-sizing: border-box; }
.comment-input:focus { border-color: #5B7FFF; outline: none; }
.dialog-footer { padding: 16px 24px; border-top: 1px solid #eee; display: flex; justify-content: flex-end; gap: 12px; }
</style>
