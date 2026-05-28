<template>
  <div class="org-manage-page" v-if="org">
    <div class="page-header">
      <div class="header-left">
        <button class="btn-back" @click="router.back()">←</button>
        <h2 class="page-title">⚙️ {{ org.name }} 管理</h2>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="handleRefreshInvite">🔄 刷新邀请码</button>
      </div>
    </div>

    <div class="manage-grid">
      <!-- Info Section -->
      <div class="card info-card">
        <h3>基本信息</h3>
        <div class="info-row">
          <label>组织名称</label>
          <span>{{ org.name }}</span>
        </div>
        <div class="info-row">
          <label>创建时间</label>
          <span>{{ org.createdAt }}</span>
        </div>
        <div class="info-row invite-row">
          <label>组织邀请码</label>
          <div class="invite-code-wrapper">
            <code class="invite-code">{{ org.inviteCode || '未生成' }}</code>
            <button class="btn-copy" @click="copyInviteCode">复制</button>
          </div>
        </div>
        <p class="invite-hint">将邀请码发送给同事，他们即可加入此组织。</p>
      </div>

      <!-- Members Section -->
      <div class="card members-card">
        <div class="section-header">
          <h3>成员列表</h3>
          <span class="member-count">{{ members.length }} 人</span>
        </div>
        <div class="member-list">
          <div v-for="m in members" :key="m.userId" class="member-item">
            <div class="member-info">
              <div class="member-avatar">{{ m.username.charAt(0) }}</div>
              <div class="member-text">
                <div class="member-name">{{ m.username }}</div>
                <div class="member-joined">加入于 {{ m.joinedAt?.slice(0, 10) }}</div>
              </div>
            </div>
            <div class="member-role-tag" :class="m.role">{{ roleLabel(m.role) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="loading-state">
    加载中...
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getOrgMembers, refreshInviteCode, type OrgMember } from '@/api/org'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const orgId = computed(() => Number(route.query.id))
const org = computed(() => authStore.userOrganizations.find(o => o.id === orgId.value))
const members = ref<OrgMember[]>([])

const roleLabel = (r: string) => ({
  owner: '创建者',
  admin: '管理员',
  member: '成员'
}[r] || r)

async function fetchMembers() {
  if (!orgId.value) return
  try {
    const res = await getOrgMembers(orgId.value)
    if (res.code === 200) {
      members.value = res.data
    }
  } catch (e) {
    ElMessage.error('获取成员列表失败')
  }
}

async function handleRefreshInvite() {
  if (!orgId.value) return
  try {
    const res = await refreshInviteCode(orgId.value)
    if (res.code === 200) {
      ElMessage.success('邀请码已更新')
      await authStore.fetchMyOrgs()
    }
  } catch (e) {
    ElMessage.error('刷新失败，仅所有者可操作')
  }
}

function copyInviteCode() {
  if (org.value?.inviteCode) {
    navigator.clipboard.writeText(org.value.inviteCode)
    ElMessage.success('邀请码已复制到剪贴板')
  }
}

onMounted(() => {
  if (!org.value) {
    authStore.fetchMyOrgs().then(fetchMembers)
  } else {
    fetchMembers()
  }
})
</script>

<style scoped>
.org-manage-page { max-width: 1000px; margin: 0 auto; padding: 24px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 16px; }
.btn-back { background: none; border: 1px solid var(--color-border); border-radius: 8px; width: 36px; height: 36px; cursor: pointer; color: var(--color-text-secondary); }
.page-title { font-size: 24px; font-weight: 600; }

.manage-grid { display: grid; grid-template-columns: 1fr 1.5fr; gap: 24px; }
.card { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: 16px; padding: 24px; }

.info-card h3, .members-card h3 { margin-top: 0; margin-bottom: 20px; font-size: 18px; }
.info-row { display: flex; flex-direction: column; gap: 4px; margin-bottom: 16px; }
.info-row label { font-size: 12px; color: var(--color-text-secondary); font-weight: 500; }
.info-row span { font-size: 15px; color: var(--color-text-primary); }

.invite-code-wrapper { display: flex; align-items: center; gap: 12px; margin-top: 4px; }
.invite-code { background: var(--color-primary-subtle); color: var(--color-primary); padding: 8px 16px; border-radius: 8px; font-family: monospace; font-size: 18px; font-weight: 700; letter-spacing: 1px; }
.btn-copy { background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: 6px; padding: 6px 12px; font-size: 12px; cursor: pointer; }
.invite-hint { font-size: 12px; color: var(--color-text-muted); margin-top: 12px; line-height: 1.5; }

.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.member-count { font-size: 13px; color: var(--color-text-secondary); background: var(--color-bg-surface); padding: 2px 8px; border-radius: 10px; }
.member-list { display: flex; flex-direction: column; gap: 12px; }
.member-item { display: flex; align-items: center; justify-content: space-between; padding: 12px; border-radius: 12px; background: var(--color-bg-surface); }
.member-info { display: flex; align-items: center; gap: 12px; }
.member-avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--color-primary); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 14px; }
.member-name { font-size: 14px; font-weight: 600; }
.member-joined { font-size: 11px; color: var(--color-text-muted); }
.member-role-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.member-role-tag.owner { background: #fef3c7; color: #92400e; }
.member-role-tag.admin { background: #e0e7ff; color: #3730a3; }
.member-role-tag.member { background: #f3f4f6; color: #374151; }

.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: 8px; padding: 8px 16px; font-weight: 500; cursor: pointer; font-size: 14px; }
.loading-state { text-align: center; padding: 100px; color: var(--color-text-secondary); }
</style>
