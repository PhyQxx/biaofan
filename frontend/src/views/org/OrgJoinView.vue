<template>
  <div class="org-join-page">
    <div class="page-header">
      <h2 class="page-title">🏢 加入或创建组织</h2>
      <p class="page-desc">加入团队，与同事协作管理标准操作流程</p>
    </div>

    <div class="org-actions-row">
      <!-- Join Organization -->
      <div class="action-card">
        <div class="card-icon">🔑</div>
        <h3>使用邀请码加入</h3>
        <p>输入组织管理员提供的 8 位邀请码</p>
        <div class="form-group">
          <input 
            v-model="inviteCode" 
            class="form-input" 
            placeholder="例如: ABC12345" 
            maxlength="12"
            @keyup.enter="handleJoin"
          />
        </div>
        <button 
          class="btn-primary full-width" 
          :disabled="!inviteCode || joining"
          @click="handleJoin"
        >
          {{ joining ? '加入中...' : '立即加入' }}
        </button>
      </div>

      <!-- Create Organization -->
      <div class="action-card">
        <div class="card-icon">🚀</div>
        <h3>创建新组织</h3>
        <p>为您的团队或公司建立专属的 SOP 空间</p>
        <div class="form-group">
          <input 
            v-model="newOrg.name" 
            class="form-input" 
            placeholder="组织名称 (如: 某某科技研发部)" 
          />
        </div>
        <button 
          class="btn-outline full-width" 
          :disabled="!newOrg.name || creating"
          @click="handleCreate"
        >
          {{ creating ? '创建中...' : '开始创建' }}
        </button>
      </div>
    </div>

    <!-- My Organizations -->
    <div class="my-orgs-section" v-if="userOrganizations.length > 0">
      <h3 class="section-title">我的组织</h3>
      <div class="org-list">
        <div 
          v-for="org in userOrganizations" 
          :key="org.id" 
          class="org-list-item"
        >
          <div class="org-info">
            <span class="org-logo">🏢</span>
            <div class="org-text">
              <div class="org-name">{{ org.name }}</div>
              <div class="org-meta">创建于 {{ org.createdAt?.slice(0, 10) }}</div>
            </div>
          </div>
          <div class="org-actions">
            <button class="btn-text" @click="handleManage(org.id)">管理</button>
            <button class="btn-secondary btn-sm" @click="handleSwitch(org.id)">进入空间</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { joinOrganization, createOrganization } from '@/api/org'

const router = useRouter()
const authStore = useAuthStore()

const userOrganizations = computed(() => authStore.userOrganizations)

const inviteCode = ref('')
const joining = ref(false)

const newOrg = reactive({
  name: '',
  description: ''
})
const creating = ref(false)

async function handleJoin() {
  if (!inviteCode.value) return
  joining.value = true
  try {
    const res = await joinOrganization(inviteCode.value)
    if (res.code === 200) {
      ElMessage.success('成功加入组织！')
      await authStore.fetchMyOrgs()
      inviteCode.value = ''
    } else {
      ElMessage.error(res.message || '加入失败')
    }
  } catch (e) {
    ElMessage.error('邀请码无效或已加入该组织')
  } finally {
    joining.value = false
  }
}

async function handleCreate() {
  if (!newOrg.name) return
  creating.value = true
  try {
    const res = await createOrganization(newOrg)
    if (res.code === 200) {
      ElMessage.success('组织创建成功！')
      await authStore.fetchMyOrgs()
      newOrg.name = ''
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建组织失败')
  } finally {
    creating.value = false
  }
}

function handleSwitch(orgId: number) {
  authStore.switchOrg(orgId)
  router.push('/')
}

function handleManage(orgId: number) {
  // 可以跳转到组织管理页
  router.push({ path: '/org/manage', query: { id: orgId } })
}
</script>

<style scoped>
.org-join-page { max-width: 900px; margin: 0 auto; padding: 40px 24px; }
.page-header { text-align: center; margin-bottom: 48px; }
.page-title { font-size: 32px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 12px; }
.page-desc { color: var(--color-text-secondary); font-size: 16px; }

.org-actions-row { display: grid; grid-template-columns: 1fr 1fr; gap: 32px; margin-bottom: 64px; }
.action-card { 
  background: var(--color-bg-elevated); 
  border: 1px solid var(--color-border); 
  border-radius: 16px; 
  padding: 32px; 
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}
.action-card:hover { transform: translateY(-4px); box-shadow: 0 12px 30px rgba(0,0,0,0.1); }
.card-icon { font-size: 40px; margin-bottom: 16px; }
.action-card h3 { font-size: 20px; font-weight: 600; margin-bottom: 8px; }
.action-card p { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 24px; }

.form-group { margin-bottom: 20px; }
.form-input { 
  width: 100%; 
  height: 44px; 
  padding: 0 16px; 
  border: 1.5px solid var(--color-border); 
  border-radius: 8px; 
  font-size: 15px; 
  box-sizing: border-box;
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
  text-align: center;
}
.form-input:focus { border-color: var(--color-primary); outline: none; }

.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: 8px; height: 44px; font-weight: 600; cursor: pointer; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-outline { background: transparent; color: var(--color-primary); border: 1.5px solid var(--color-primary); border-radius: 8px; height: 44px; font-weight: 600; cursor: pointer; }
.full-width { width: 100%; }

.my-orgs-section { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: 16px; padding: 24px; }
.section-title { font-size: 18px; font-weight: 600; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border); }
.org-list { display: flex; flex-direction: column; gap: 16px; }
.org-list-item { 
  display: flex; 
  align-items: center; 
  justify-content: space-between; 
  padding: 12px 16px; 
  background: var(--color-bg-surface); 
  border-radius: 12px; 
}
.org-info { display: flex; align-items: center; gap: 12px; }
.org-logo { font-size: 24px; width: 40px; height: 40px; background: var(--color-primary-subtle); display: flex; align-items: center; justify-content: center; border-radius: 8px; }
.org-name { font-weight: 600; font-size: 15px; }
.org-meta { font-size: 12px; color: var(--color-text-secondary); }
.org-actions { display: flex; gap: 12px; }
.btn-text { background: none; border: none; color: var(--color-primary); cursor: pointer; font-size: 14px; }
.btn-sm { height: 32px; padding: 0 12px; font-size: 13px; }
</style>
