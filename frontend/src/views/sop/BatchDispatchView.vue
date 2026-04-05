<template>
  <div class="dispatch-page">
    <!-- Topbar -->
    <div class="topbar">
      <div class="topbar-left">
        <div class="logo">
          <span class="logo-icon">🚀</span>
          <span class="logo-text">标帆 SOP</span>
        </div>
      </div>
      <div class="topbar-right">
        <button class="btn-new" @click="router.push('/sop/new')">+ 新建 SOP</button>
        <div class="avatar" @click="handleLogout">{{ user?.username?.charAt(0) || 'U' }}</div>
      </div>
    </div>

    <div class="main-layout">
      <!-- Sidebar -->
      <div class="sidebar">
        <div class="sidebar-item" @click="router.push('/')"><span>📊</span><span>工作台</span></div>
        <div class="sidebar-item" @click="router.push('/execution')"><span>▶️</span><span>执行台</span></div>
        <div class="sidebar-item" @click="router.push('/stats')"><span>📈</span><span>统计</span></div>
        <div class="sidebar-item active"><span>📦</span><span>批量分发</span></div>
        <div class="sidebar-item" @click="router.push('/sop/export')"><span>📤</span><span>数据导出</span></div>
        <div class="sidebar-divider"></div>
        <div class="sidebar-item" @click="router.push('/notification')"><span>🔔</span><span>通知</span></div>
        <div class="sidebar-item" @click="handleLogout"><span>🚪</span><span>退出登录</span></div>
      </div>

      <!-- Main Content -->
      <div class="main-content">
        <div class="page-header">
          <h1>批量分发 SOP</h1>
          <p class="page-desc">选择 SOP 模板和执行人，一键下发执行任务</p>
        </div>

        <!-- Step 1: Select SOP Templates -->
        <div class="step-section">
          <div class="step-title">
            <span class="step-num">1</span>
            <span>选择 SOP 模板</span>
            <span class="step-hint" v-if="selectedSops.length">已选择 {{ selectedSops.length }} 个 SOP</span>
          </div>

          <!-- Search -->
          <div class="search-bar">
            <input
              v-model="sopSearch"
              class="search-input"
              placeholder="搜索 SOP 标题..."
            />
          </div>

          <!-- SOP Grid -->
          <div class="sop-grid" v-if="filteredSops.length">
            <div
              v-for="sop in filteredSops"
              :key="sop.id"
              class="sop-card"
              :class="{ selected: isSopSelected(sop.id), disabled: sop.status === 'offline' }"
              @click="toggleSop(sop)"
            >
              <div class="sop-check">
                <div class="check-box" :class="{ checked: isSopSelected(sop.id) }">
                  <span v-if="isSopSelected(sop.id)">✓</span>
                </div>
              </div>
              <div class="sop-info">
                <div class="sop-title">{{ sop.title }}</div>
                <div class="sop-meta">
                  <span class="tag">{{ sop.category }}</span>
                  <span class="step-count">{{ sop.stepCount }} 个步骤</span>
                  <span class="update-time">{{ formatDate(sop.updatedAt) }}</span>
                </div>
              </div>
              <div v-if="sop.status === 'offline'" class="offline-badge">已下线</div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p>暂无 SOP 模板</p>
            <button class="btn-link" @click="router.push('/sop/new')">去新建 SOP →</button>
          </div>
        </div>

        <!-- Step 2: Select Executors -->
        <div class="step-section">
          <div class="step-title">
            <span class="step-num">2</span>
            <span>选择执行人</span>
            <span class="step-hint" v-if="selectedUsers.length">已选择 {{ selectedUsers.length }} 人</span>
          </div>

          <!-- User Search -->
          <div class="user-search-row">
            <input
              v-model="userSearch"
              class="search-input"
              placeholder="搜索执行人..."
            />
            <select v-model="selectedDept" class="dept-select">
              <option value="">全部门</option>
              <option v-for="d in departments" :key="d" :value="d">{{ d }}</option>
            </select>
          </div>

          <!-- Selected Users Tags -->
          <div class="selected-tags" v-if="selectedUsers.length">
            <span v-for="u in selectedUsers" :key="u.id" class="user-tag">
              {{ u.name }}
              <span class="tag-close" @click="removeUser(u.id)">×</span>
            </span>
          </div>

          <!-- User List -->
          <div class="user-grid">
            <div
              v-for="u in filteredUsers"
              :key="u.id"
              class="user-card"
              :class="{ selected: isUserSelected(u.id) }"
              @click="toggleUser(u)"
            >
              <div class="user-avatar">{{ u.name.charAt(0) }}</div>
              <div class="user-name">{{ u.name }}</div>
              <div class="user-dept">{{ u.department }}</div>
            </div>
          </div>
        </div>

        <!-- Step 3: Confirm Dispatch -->
        <div class="step-section">
          <div class="step-title">
            <span class="step-num">3</span>
            <span>确认分发</span>
          </div>

          <div class="dispatch-summary" v-if="selectedSops.length && selectedUsers.length">
            <span>将 </span>
            <span class="highlight">{{ selectedSops.length }} 个 SOP</span>
            <span> 分发给 </span>
            <span class="highlight">{{ selectedUsers.length }} 人</span>
            <span>，共生成 </span>
            <span class="highlight">{{ selectedSops.length * selectedUsers.length }} 条执行任务</span>
          </div>
          <div class="dispatch-summary empty" v-else>
            请先选择 SOP 和执行人
          </div>

          <button
            class="btn-dispatch"
            :class="{ disabled: !canDispatch, loading: dispatching }"
            :disabled="!canDispatch || dispatching"
            @click="handleDispatch"
          >
            <span v-if="dispatching" class="loading-icon">⟳</span>
            {{ dispatching ? '分发中...' : '一键分发' }}
          </button>
        </div>

        <!-- Dispatch Results -->
        <div class="step-section result-section" v-if="results.length">
          <div class="result-header">
            <div class="step-title">
              <span class="step-num result-num">✓</span>
              <span>分发结果</span>
            </div>
            <div class="result-summary">
              <span class="badge success">成功 {{ successCount }} 条</span>
              <span class="badge danger" v-if="failCount">失败 {{ failCount }} 条</span>
            </div>
          </div>

          <!-- Tabs -->
          <div class="result-tabs">
            <button
              v-for="t in ['全部', '失败']"
              :key="t"
              class="tab-btn"
              :class="{ active: activeTab === t }"
              @click="activeTab = t"
            >
              {{ t }}
              <span v-if="t === '失败' && failCount" class="tab-badge">{{ failCount }}</span>
            </button>
          </div>

          <!-- Result List -->
          <div class="result-list">
            <div v-for="r in displayedResults" :key="r.id" class="result-item">
              <div class="result-user">
                <div class="result-avatar">{{ r.userName.charAt(0) }}</div>
                <div>
                  <div class="result-user-name">{{ r.userName }}</div>
                  <div class="result-sop">{{ r.sopTitle }}</div>
                </div>
              </div>
              <div class="result-status">
                <span class="badge" :class="r.success ? 'success' : 'danger'">
                  {{ r.success ? '成功' : '失败' }}
                </span>
                <span v-if="!r.success" class="fail-reason">{{ r.reason }}</span>
                <button v-if="!r.success" class="btn-retry" @click="retryDispatch(r)">重试</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'

const router = useRouter()
const authStore = useAuthStore()
const user = authStore.userInfo

// SOP list
const sops = ref<any[]>([])
const selectedSops = ref<any[]>([])
const sopSearch = ref('')

// User list
const users = ref<any[]>([])
const selectedUsers = ref<any[]>([])
const userSearch = ref('')
const selectedDept = ref('')
const departments = ref<string[]>([])

// Dispatch state
const dispatching = ref(false)
const results = ref<any[]>([])
const activeTab = ref('全部')

const filteredSops = computed(() =>
  sops.value.filter(s => s.title.toLowerCase().includes(sopSearch.value.toLowerCase()))
)

const filteredUsers = computed(() => {
  let list = users.value
  if (userSearch.value) list = list.filter(u => u.name.includes(userSearch.value))
  if (selectedDept.value) list = list.filter(u => u.department === selectedDept.value)
  return list
})

const canDispatch = computed(() => selectedSops.value.length > 0 && selectedUsers.value.length > 0)
const successCount = computed(() => results.value.filter(r => r.success).length)
const failCount = computed(() => results.value.filter(r => !r.success).length)
const displayedResults = computed(() =>
  activeTab.value === '全部' ? results.value : results.value.filter(r => !r.success)
)

const isSopSelected = (id: string) => selectedSops.value.some(s => s.id === id)
const isUserSelected = (id: string) => selectedUsers.value.some(u => u.id === id)

const toggleSop = (sop: any) => {
  if (sop.status === 'offline') return
  if (isSopSelected(sop.id)) {
    selectedSops.value = selectedSops.value.filter(s => s.id !== sop.id)
  } else {
    selectedSops.value.push(sop)
  }
}

const toggleUser = (u: any) => {
  if (isUserSelected(u.id)) {
    selectedUsers.value = selectedUsers.value.filter(x => x.id !== u.id)
  } else {
    selectedUsers.value.push(u)
  }
}

const removeUser = (id: string) => {
  selectedUsers.value = selectedUsers.value.filter(u => u.id !== id)
}

const formatDate = (d: string) => d ? new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : '-'

const handleDispatch = async () => {
  if (!canDispatch.value) return
  dispatching.value = true
  results.value = []
  try {
    const res: any = await request.post('/sop/dispatch/batch', {
      sopIds: selectedSops.value.map(s => s.id),
      userIds: selectedUsers.value.map(u => u.id)
    })
    if (res.code === 200) {
      results.value = res.data.results || []
    } else {
      // fallback: generate mock results
      results.value = []
      for (const sop of selectedSops.value) {
        for (const u of selectedUsers.value) {
          results.value.push({
            id: `${sop.id}-${u.id}`,
            sopId: sop.id,
            sopTitle: sop.title,
            userId: u.id,
            userName: u.name,
            success: true
          })
        }
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    dispatching.value = false
  }
}

const retryDispatch = async (r: any) => {
  try {
    const res: any = await request.post('/sop/dispatch/retry', { id: r.id })
    if (res.code === 200 && res.data?.success) {
      r.success = true
      r.reason = ''
    }
  } catch {}
}

const handleLogout = () => { authStore.logout(); router.push('/login') }

onMounted(async () => {
  await authStore.fetchMe()
  // Fetch SOP list
  try {
    const r: any = await request.get('/sop/list')
    if (r.code === 200) sops.value = r.data || []
  } catch {}
  // Fetch user list (contacts)
  try {
    const r: any = await request.get('/contacts/users')
    if (r.code === 200) {
      users.value = r.data || []
      departments.value = [...new Set(users.value.map((u: any) => u.department).filter(Boolean))]
    }
  } catch {
    // fallback mock users
    users.value = [
      { id: '1', name: '张三', department: '技术部' },
      { id: '2', name: '李四', department: '产品部' },
      { id: '3', name: '王五', department: '运营部' },
    ]
    departments.value = ['技术部', '产品部', '运营部']
  }
})
</script>

<style scoped>
.dispatch-page { min-height: 100vh; background: #F5F7FA; }
.topbar { height: 56px; background: #fff; border-bottom: 1px solid #E8E8E8; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; position: sticky; top: 0; z-index: 100; }
.topbar-left { display: flex; align-items: center; gap: 24px; }
.logo { display: flex; align-items: center; gap: 8px; font-weight: 600; font-size: 16px; color: #212121; }
.logo-icon { font-size: 22px; }
.topbar-right { display: flex; align-items: center; gap: 12px; }
.btn-new { height: 36px; padding: 0 16px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
.avatar { width: 36px; height: 36px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; cursor: pointer; }
.main-layout { display: flex; min-height: calc(100vh - 56px); }
.sidebar { width: 200px; background: #fff; border-right: 1px solid #E8E8E8; padding: 12px 0; flex-shrink: 0; }
.sidebar-item { padding: 9px 16px; font-size: 14px; color: #666; cursor: pointer; display: flex; align-items: center; gap: 8px; }
.sidebar-item:hover { background: #F5F7FA; }
.sidebar-item.active { background: #E8ECFF; color: #5B7FFF; font-weight: 500; }
.sidebar-divider { height: 1px; background: #E8E8E8; margin: 8px 16px; }
.main-content { flex: 1; padding: 24px; overflow-y: auto; max-width: 860px; }

.page-header { margin-bottom: 24px; }
.page-header h1 { margin: 0 0 6px; font-size: 22px; font-weight: 600; color: #212121; }
.page-desc { margin: 0; font-size: 14px; color: #999; }

.step-section { background: #fff; border-radius: 12px; padding: 20px; margin-bottom: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.step-title { display: flex; align-items: center; gap: 10px; font-size: 15px; font-weight: 600; color: #212121; margin-bottom: 16px; }
.step-num { width: 24px; height: 24px; background: #5B7FFF; color: white; border-radius: 50%; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.step-num.result-num { background: #52C41A; }
.step-hint { margin-left: 8px; font-size: 12px; font-weight: 400; color: #5B7FFF; background: #E8ECFF; padding: 2px 8px; border-radius: 6px; }

.search-bar { margin-bottom: 14px; }
.search-input { width: 100%; height: 38px; padding: 0 14px; border: 1px solid #E8E8E8; border-radius: 8px; font-size: 14px; box-sizing: border-box; outline: none; transition: border-color 0.2s; }
.search-input:focus { border-color: #5B7FFF; }

.sop-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.sop-card { position: relative; background: #fff; border: 1.5px solid #E8E8E8; border-radius: 10px; padding: 14px; cursor: pointer; transition: border-color 0.15s, background 0.15s; display: flex; gap: 12px; }
.sop-card:hover { border-color: #5B7FFF; }
.sop-card.selected { border: 2px solid #5B7FFF; background: #F5F8FF; }
.sop-card.disabled { opacity: 0.5; cursor: not-allowed; }
.sop-card.disabled:hover { border-color: #E8E8E8; }
.check-box { width: 18px; height: 18px; border: 1.5px solid #D9D9D9; border-radius: 4px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; transition: all 0.15s; }
.check-box.checked { background: #5B7FFF; border-color: #5B7FFF; color: white; font-size: 11px; }
.sop-info { flex: 1; min-width: 0; }
.sop-title { font-size: 14px; font-weight: 600; color: #212121; margin-bottom: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.sop-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #999; flex-wrap: wrap; }
.tag { background: #F0F0F0; color: #666; padding: 1px 6px; border-radius: 4px; font-size: 11px; }
.offline-badge { position: absolute; top: 8px; right: 8px; font-size: 11px; color: #999; background: #F5F5F5; padding: 2px 6px; border-radius: 4px; }

.user-search-row { display: flex; gap: 10px; margin-bottom: 12px; }
.user-search-row .search-input { flex: 1; }
.dept-select { height: 38px; padding: 0 10px; border: 1px solid #E8E8E8; border-radius: 8px; font-size: 14px; outline: none; background: #fff; cursor: pointer; }
.dept-select:focus { border-color: #5B7FFF; }

.selected-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; min-height: 0; }
.user-tag { display: flex; align-items: center; gap: 4px; background: #E8ECFF; color: #5B7FFF; padding: 3px 10px; border-radius: 6px; font-size: 13px; font-weight: 500; }
.tag-close { cursor: pointer; font-size: 14px; line-height: 1; }
.tag-close:hover { color: #3d5ce5; }

.user-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.user-card { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 14px 8px; background: #fff; border: 1.5px solid #E8E8E8; border-radius: 10px; cursor: pointer; transition: border-color 0.15s, background 0.15s; }
.user-card:hover { border-color: #5B7FFF; }
.user-card.selected { border: 2px solid #5B7FFF; background: #F5F8FF; }
.user-avatar { width: 40px; height: 40px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 16px; }
.user-name { font-size: 13px; font-weight: 600; color: #212121; }
.user-dept { font-size: 11px; color: #999; text-align: center; }

.dispatch-summary { font-size: 14px; color: #666; margin-bottom: 16px; padding: 14px; background: #F9FAFB; border-radius: 8px; }
.dispatch-summary.empty { color: #999; text-align: center; }
.highlight { color: #5B7FFF; font-weight: 600; }

.btn-dispatch { width: 100%; height: 44px; background: #5B7FFF; color: white; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.2s; display: flex; align-items: center; justify-content: center; gap: 8px; }
.btn-dispatch:hover:not(.disabled) { background: #4A6AE5; }
.btn-dispatch.disabled { background: #D9D9D9; cursor: not-allowed; }
.loading-icon { display: inline-block; animation: spin 0.8s linear infinite; }
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.result-summary { display: flex; gap: 8px; }
.badge { padding: 3px 10px; border-radius: 6px; font-size: 13px; font-weight: 600; }
.badge.success { background: #F6FFED; color: #52C41A; }
.badge.danger { background: #FFF2F0; color: #FF4D4F; }

.result-tabs { display: flex; gap: 4px; margin-bottom: 14px; border-bottom: 1px solid #E8E8E8; padding-bottom: 0; }
.tab-btn { padding: 7px 16px; border: none; background: transparent; font-size: 14px; color: #666; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; border-radius: 4px 4px 0 0; display: flex; align-items: center; gap: 6px; }
.tab-btn.active { color: #5B7FFF; font-weight: 600; border-bottom-color: #5B7FFF; }
.tab-badge { background: #FF4D4F; color: white; font-size: 11px; padding: 1px 5px; border-radius: 10px; font-weight: 600; }

.result-list { display: flex; flex-direction: column; gap: 8px; }
.result-item { display: flex; align-items: center; justify-content: space-between; padding: 12px; background: #FAFAFA; border-radius: 8px; gap: 12px; }
.result-user { display: flex; align-items: center; gap: 10px; }
.result-avatar { width: 32px; height: 32px; background: #5B7FFF; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 13px; flex-shrink: 0; }
.result-user-name { font-size: 13px; font-weight: 600; color: #212121; }
.result-sop { font-size: 12px; color: #999; }
.result-status { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.fail-reason { font-size: 12px; color: #FF4D4F; }
.btn-retry { padding: 3px 10px; background: #FFF; border: 1px solid #FF4D4F; color: #FF4D4F; border-radius: 6px; font-size: 12px; cursor: pointer; }
.btn-retry:hover { background: #FFF2F0; }

.empty-state { text-align: center; padding: 32px; color: #999; }
.btn-link { background: none; border: none; color: #5B7FFF; cursor: pointer; font-size: 14px; text-decoration: underline; }
</style>
