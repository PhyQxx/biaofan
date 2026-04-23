<template>
  <div class="profile-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1>👤 个人中心</h1>
      <div class="page-tabs">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- Tab: 概览 -->
    <div v-if="!loading && activeTab === 'overview'">
      <ProfileOverview
        :username="profile?.username || '用户'"
        :level="profile?.level || 1"
        :rank="profile?.rank || 'bronze'"
        :rankName="profile?.rankName || '青铜'"
        :totalScore="profile?.totalScore || 0"
        :streakDays="profile?.streakDays || 0"
        :thisWeekCount="profile?.thisWeekCount || 0"
        :badgeCount="profile?.badgeCount || 0"
        :totalBadgeCount="profile?.totalBadgeCount || 16"
        :equippedTitle="profile?.equippedTitle"
        :titleExpireAt="profile?.titleExpireAt"
      />
      <RecentActivity :activities="profile?.recentActivities || []" />
    </div>

    <!-- Tab: 徽章墙 -->
    <div v-if="!loading && activeTab === 'badges'">
      <BadgeWall
        :badges="badges"
        @badge-click="openBadgeModal"
      />
    </div>

    <!-- Tab: 成长进度 -->
    <div v-if="!loading && activeTab === 'growth'">
      <LevelProgress
        :username="profile?.username || '用户'"
        :level="profile?.level || 1"
        :exp="profile?.exp || 0"
        :expToNext="profile?.expToNext || 100"
        :rank="profile?.rank || 'bronze'"
        :rankName="profile?.rankName || '青铜'"
        :totalScore="profile?.totalScore || 0"
        :streakDays="profile?.streakDays || 0"
        :badgeCount="profile?.badgeCount || 0"
        :totalBadgeCount="profile?.totalBadgeCount || 16"
      />

      <div class="growth-tips">
        <div class="tips-title">💡 成长攻略</div>
        <div class="tips-grid">
          <div class="tip-card">📋 每完成1个 SOP<br><strong>获得 10-50 EXP</strong></div>
          <div class="tip-card">⏰ 按时完成额外奖励<br><strong>+5 EXP</strong></div>
          <div class="tip-card">🔥 连续7天完成<br><strong>额外奖励 +20</strong></div>
          <div class="tip-card">🏅 获得徽章<br><strong>大量 EXP + 积分</strong></div>
        </div>
      </div>
    </div>

    <!-- Tab: 积分明细 -->
    <div v-if="!loading && activeTab === 'history'">
      <div class="history-header">
        <div class="history-stats">
          <span class="stat-chip income">📈 本月收入：+{{ monthlyIncome }}</span>
          <span class="stat-chip expense">📉 本月支出：{{ monthlyExpense }}</span>
        </div>
        <div class="history-pager">
          <button class="pager-btn" :disabled="historyPage <= 1" @click="historyPage--; loadHistory()">◀</button>
          <span>第 {{ historyPage }}/{{ totalPages }} 页</span>
          <button class="pager-btn" :disabled="historyPage >= totalPages" @click="historyPage++; loadHistory()">▶</button>
        </div>
      </div>
      <ScoreHistoryItem
        v-for="item in historyItems"
        :key="item.id"
        :item="item"
      />
      <div class="empty-state" v-if="!historyItems.length && !loadingHistory">
        <span>💰</span>
        <p>暂无积分记录</p>
      </div>
    </div>

    <!-- Tab: 积分商城 -->
    <div v-if="!loading && activeTab === 'store'">
      <div class="store-header">
        <div class="store-score">
          我的积分：<strong>{{ profile?.totalScore || 0 }}</strong>
        </div>
        <div class="store-tabs">
          <button
            v-for="cat in categories"
            :key="cat.key"
            class="cat-btn"
            :class="{ active: activeCategory === cat.key }"
            @click="activeCategory = cat.key; loadStore()"
          >
            {{ cat.icon }} {{ cat.label }}
          </button>
        </div>
      </div>
      <div class="store-grid">
        <ScoreStoreCard
          v-for="product in storeProducts"
          :key="product.id"
          :product="product"
          :userScore="profile?.totalScore || 0"
          @redeem="handleRedeem"
        />
      </div>
      <div class="empty-state" v-if="!storeProducts.length && !loadingStore">
        <span>🎁</span>
        <p>暂无商品</p>
      </div>
    </div>

    <!-- Tab: 设置 -->
    <div v-if="!loading && activeTab === 'settings'">
      <!-- 账号设置 -->
      <div class="settings-card">
        <div class="settings-title">🔑 账号设置</div>
        <div class="settings-group">
          <label class="settings-label">用户名</label>
          <div class="settings-row">
            <input v-model="settingsForm.username" class="settings-input" placeholder="输入用户名" />
            <button class="settings-btn" @click="saveProfile">保存</button>
          </div>
        </div>
        <div class="settings-group">
          <label class="settings-label">修改密码</label>
          <input v-model="settingsForm.oldPassword" type="password" class="settings-input" placeholder="旧密码" />
          <input v-model="settingsForm.newPassword" type="password" class="settings-input" placeholder="新密码（至少6位）" style="margin-top:8px" />
          <input v-model="settingsForm.confirmPassword" type="password" class="settings-input" placeholder="确认新密码" style="margin-top:8px" />
          <button class="settings-btn" style="margin-top:10px" @click="savePassword">修改密码</button>
        </div>
        <div class="settings-group">
          <label class="settings-label">手机号</label>
          <div class="settings-row">
            <input v-model="settingsForm.phone" class="settings-input" placeholder="输入手机号" />
            <button class="settings-btn" @click="savePhone">保存</button>
          </div>
        </div>
      </div>

      <!-- 通知设置 -->
      <div class="settings-card" style="margin-top:16px">
        <div class="settings-title">🔔 通知设置</div>
        <div class="settings-group">
          <label class="settings-label">Webhook 通知</label>
          <select v-model="notifForm.platform" class="settings-input">
            <option value="feishu">飞书</option>
            <option value="dingtalk">钉钉</option>
          </select>
          <input v-model="notifForm.webhookUrl" class="settings-input" placeholder="Webhook URL" style="margin-top:8px" />
          <input v-model="notifForm.secret" class="settings-input" placeholder="Secret（选填）" style="margin-top:8px" />
          <input v-model="notifForm.botName" class="settings-input" placeholder="Bot 名称（选填）" style="margin-top:8px" />
          <div class="settings-toggle-row" style="margin-top:10px">
            <span class="settings-label" style="margin:0">启用 Webhook</span>
            <button class="settings-toggle" :class="{ active: notifForm.webhookEnabled }" @click="notifForm.webhookEnabled = !notifForm.webhookEnabled">
              <span class="settings-toggle-dot"></span>
            </button>
          </div>
          <button class="settings-btn" style="margin-top:12px" @click="saveWebhook">保存 Webhook</button>
        </div>
        <div class="settings-divider"></div>
        <div class="settings-group">
          <label class="settings-label">邮箱通知</label>
          <div class="settings-row">
            <input v-model="notifForm.email" class="settings-input" placeholder="输入邮箱地址" />
            <button class="settings-btn" @click="saveEmail">保存</button>
          </div>
          <div class="settings-toggle-row" style="margin-top:10px">
            <span class="settings-label" style="margin:0">启用邮箱通知</span>
            <button class="settings-toggle" :class="{ active: notifForm.emailEnabled }" @click="notifForm.emailEnabled = !notifForm.emailEnabled">
              <span class="settings-toggle-dot"></span>
            </button>
          </div>
          <button class="settings-btn" style="margin-top:12px" @click="saveEmail">保存邮箱设置</button>
        </div>
      </div>
    </div>

    <!-- Badge Modal -->
    <BadgeModal
      v-if="selectedBadge"
      :badge="selectedBadge"
      @close="selectedBadge = null"
    />

    <!-- Redeem Success Modal -->
    <Teleport to="body">
      <div class="modal-overlay" v-if="redeemResult">
        <div class="redeem-modal">
          <div class="redeem-particles">
            <span v-for="i in 8" :key="i" class="rp" :style="rpStyle(i)">✨</span>
          </div>
          <div class="redeem-icon">🎉</div>
          <h2>✨ 兑换成功！✨</h2>
          <div class="redeem-product">
            <span class="redeem-icon-sm">{{ redeemResult.productName }}</span>
            <span class="redeem-duration" v-if="redeemResult.expireAt">
              有效期至 {{ redeemResult.expireAt }}
            </span>
          </div>
          <div class="redeem-balance">
            <span>-{{ redeemAmount }} 积分</span>
            <span>剩余 {{ redeemResult.newBalance }} 积分</span>
          </div>
          <button class="btn-primary" @click="redeemResult = null">确 定</button>
        </div>
      </div>
    </Teleport>

    <!-- Level Up Celebration -->
    <GrowthCelebration
      v-if="showCelebration"
      :newLevel="celebrationLevel"
      :rank="celebrationRank"
      :rankName="celebrationRankName"
      @close="showCelebration = false"
      @view-achievements="activeTab = 'badges'; showCelebration = false"
    />
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端个人中心页
 * - 用户信息展示
 * - 游戏化数据：等级、积分、徽章墙
 * - 近期活动记录
 */
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import request from '@/api'
import { gamificationApi, type ProfileDTO, type Badge, type StoreProduct, type ScoreHistory, type RedeemResult } from '@/api/gamification'

// Components
import ProfileOverview from '@/components/gamification/ProfileOverview.vue'
import RecentActivity from '@/components/gamification/RecentActivity.vue'
import BadgeWall from '@/components/gamification/BadgeWall.vue'
import BadgeModal from '@/components/gamification/BadgeModal.vue'
import LevelProgress from '@/components/gamification/LevelProgress.vue'
import ScoreHistoryItem from '@/components/gamification/ScoreHistoryItem.vue'
import ScoreStoreCard from '@/components/gamification/ScoreStoreCard.vue'
import GrowthCelebration from '@/components/gamification/GrowthCelebration.vue'

const authStore = useAuthStore()

const tabs = [
  { key: 'overview', label: '概览', icon: '👤' },
  { key: 'badges', label: '徽章墙', icon: '🏅' },
  { key: 'growth', label: '成长', icon: '📈' },
  { key: 'history', label: '积分明细', icon: '💰' },
  { key: 'store', label: '积分商城', icon: '🎁' },
  { key: 'settings', label: '设置', icon: '⚙️' },
]
const categories = [
  { key: '', label: '全部', icon: '📦' },
  { key: 'title', label: '称号', icon: '🏅' },
  { key: 'avatar_frame', label: '头像框', icon: '🖼️' },
  { key: 'background', label: '背景', icon: '🌌' },
  { key: 'emotion', label: '表情', icon: '😀' },
]

const activeTab = ref('overview')
const activeCategory = ref('')
const loading = ref(false)
const loadingHistory = ref(false)
const loadingStore = ref(false)
const profile = ref<ProfileDTO | null>(null)
const badges = ref<Badge[]>([])
const storeProducts = ref<StoreProduct[]>([])
const historyItems = ref<ScoreHistory[]>([])
const historyPage = ref(1)
const totalPages = ref(1)
const selectedBadge = ref<Badge | null>(null)
const redeemResult = ref<RedeemResult | null>(null)
const redeemAmount = ref(0)
const showCelebration = ref(false)
const celebrationLevel = ref(1)
const celebrationRank = ref<string>('bronze')
const celebrationRankName = ref('青铜')

const monthlyIncome = computed(() =>
  historyItems.value
    .filter((h) => h.change > 0)
    .reduce((sum, h) => sum + h.change, 0)
)
const monthlyExpense = computed(() =>
  historyItems.value
    .filter((h) => h.change < 0)
    .reduce((sum, h) => sum + Math.abs(h.change), 0)
)

onMounted(async () => {
  authStore.token && authStore.fetchMe()
  await loadProfile()
  loadBadges()
  loadHistory()
  loadSettings()
})

async function loadProfile() {
  loading.value = true
  try {
    const res: any = await gamificationApi.getProfile()
    if (res.code === 200 && res.data) {
      profile.value = res.data
    }
  } catch {
    ElMessage.error('加载个人中心失败')
  } finally {
    loading.value = false
  }
}

async function loadBadges() {
  try {
    const res: any = await gamificationApi.getBadges()
    badges.value = Array.isArray(res) ? res : (res?.data || [])
  } catch {
    ElMessage.error('加载徽章失败')
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res: any = await gamificationApi.getScoreHistory(historyPage.value, 20)
    const data = res?.data || res || {}
    historyItems.value = data.items || []
    totalPages.value = Math.max(1, Math.ceil((data.total || 0) / (data.pageSize || 20)))
  } catch {
    ElMessage.error('加载积分历史失败')
  } finally {
    loadingHistory.value = false
  }
}

async function loadStore() {
  loadingStore.value = true
  try {
    const res: any = await gamificationApi.getStore(activeCategory.value || undefined)
    storeProducts.value = Array.isArray(res) ? res : (res?.data || [])
  } catch {
    ElMessage.error('加载商城失败')
  } finally {
    loadingStore.value = false
  }
}

function openBadgeModal(badge: Badge) {
  selectedBadge.value = badge
}

async function handleRedeem(product: StoreProduct) {
  try {
    redeemAmount.value = product.price
    const result = await gamificationApi.redeem(product.id)
    redeemResult.value = result
    await loadProfile()
    await loadStore()
  } catch {
    ElMessage.error('兑换失败，请重试')
  }
}

function rpStyle(i: number) {
  const angle = (i / 8) * 360
  const r = 70 + Math.random() * 30
  const x = Math.cos((angle * Math.PI) / 180) * r
  const y = Math.sin((angle * Math.PI) / 180) * r
  return { transform: `translate(${x}px, ${y}px)`, animationDelay: `${(i / 8) * 0.4}s` }
}

const settingsForm = reactive({
  username: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  phone: '',
})

const notifForm = reactive({
  platform: 'feishu',
  webhookUrl: '',
  secret: '',
  botName: '',
  webhookEnabled: false,
  email: '',
  emailEnabled: false,
})

async function loadSettings() {
  try {
    const userRes: any = await request.get('/auth/me')
    if (userRes.code === 200 && userRes.data) {
      settingsForm.username = userRes.data.username || ''
      settingsForm.phone = userRes.data.phone || ''
    }
  } catch (e) {
    console.error('[ProfileView] fetchProfile user info failed:', e)
  }
  try {
    const notifRes: any = await request.get('/notification-config')
    if (notifRes.code === 200 && notifRes.data) {
      const c = notifRes.data
      notifForm.platform = c.platform || 'feishu'
      notifForm.webhookUrl = c.webhookUrl || ''
      notifForm.secret = c.secret || ''
      notifForm.botName = c.botName || ''
      notifForm.webhookEnabled = !!c.enabled
      notifForm.email = c.email || ''
      notifForm.emailEnabled = !!c.enabled && !!c.email
    }
  } catch (e) {
    console.error('[ProfileView] fetchProfile notification config failed:', e)
  }
}

async function saveProfile() {
  if (!settingsForm.username.trim()) return ElMessage.warning('用户名不能为空')
  try {
    const res: any = await request.put('/user/profile', { username: settingsForm.username })
    if (res.code === 200) { ElMessage.success('用户名已更新'); authStore.fetchMe() }
    else ElMessage.error(res.message || '更新失败')
  } catch (e: any) { ElMessage.error(e.message || '更新失败') }
}

async function savePassword() {
  if (!settingsForm.oldPassword || !settingsForm.newPassword) return ElMessage.warning('请填写完整')
  if (settingsForm.newPassword.length < 6) return ElMessage.warning('新密码至少6位')
  if (settingsForm.newPassword !== settingsForm.confirmPassword) return ElMessage.warning('两次密码不一致')
  try {
    const res: any = await request.put('/user/password', { oldPassword: settingsForm.oldPassword, newPassword: settingsForm.newPassword })
    if (res.code === 200) { ElMessage.success('密码已更新'); settingsForm.oldPassword = ''; settingsForm.newPassword = ''; settingsForm.confirmPassword = '' }
    else ElMessage.error(res.message || '修改失败')
  } catch (e: any) { ElMessage.error(e.message || '修改失败') }
}

async function savePhone() {
  if (!settingsForm.phone.match(/^1[3-9]\d{9}$/)) return ElMessage.warning('手机号格式不正确')
  try {
    const res: any = await request.put('/user/phone', { phone: settingsForm.phone })
    if (res.code === 200) { ElMessage.success('手机号已更新'); authStore.fetchMe() }
    else ElMessage.error(res.message || '更新失败')
  } catch (e: any) { ElMessage.error(e.message || '更新失败') }
}

async function saveWebhook() {
  if (!notifForm.webhookUrl) return ElMessage.warning('请填写 Webhook URL')
  try {
    const res: any = await request.post('/notification-config/save', {
      platform: notifForm.platform,
      webhookUrl: notifForm.webhookUrl,
      secret: notifForm.secret || null,
      botName: notifForm.botName || null,
    })
    if (res.code === 200) ElMessage.success('Webhook 已保存')
    else ElMessage.error(res.message || '保存失败')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}

async function saveEmail() {
  if (notifForm.email && !notifForm.email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) return ElMessage.warning('邮箱格式不正确')
  try {
    const res: any = await request.post('/notification-config/save', {
      platform: notifForm.platform,
      webhookUrl: notifForm.webhookUrl || null,
      secret: notifForm.secret || null,
      botName: notifForm.botName || null,
      email: notifForm.email,
    })
    if (res.code === 200) ElMessage.success('邮箱已保存')
    else ElMessage.error(res.message || '保存失败')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
}
</script>

<style scoped>
.profile-page {
  padding: 0;
}
.page-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px; flex-wrap: wrap; gap: 12px;
}
.page-header h1 {
  font-size: var(--font-size-4xl); font-weight: 700; color: var(--color-text-primary);
}
.page-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.tab-btn {
  padding: 7px 14px; background: var(--color-bg-elevated); border: 1px solid var(--color-border);
  color: var(--color-text-secondary); border-radius: var(--radius-md); cursor: pointer; font-size: var(--font-size-sm);
  transition: var(--transition-normal); white-space: nowrap;
}
.tab-btn:hover { background: var(--color-bg-surface); color: var(--color-text-primary); }
.tab-btn.active {
  background: var(--color-primary); border-color: var(--color-primary); color: white;
  font-weight: 600;
}
.loading-state {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 60px; color: var(--color-text-secondary); gap: 16px;
}
.loading-spinner {
  width: 36px; height: 36px; border: 3px solid var(--color-border);
  border-top-color: var(--color-primary); border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* history */
.history-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; flex-wrap: wrap; gap: 10px;
}
.history-stats { display: flex; gap: 10px; }
.stat-chip {
  font-size: var(--font-size-sm); padding: 4px 12px; border-radius: 20px;
  font-weight: 600;
}
.stat-chip.income { background: rgba(74, 222, 128, 0.1); color: #4ade80; }
.stat-chip.expense { background: rgba(255, 107, 107, 0.1); color: #ff6b6b; }
.history-pager { display: flex; align-items: center; gap: 10px; font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.pager-btn {
  width: 28px; height: 28px; border-radius: var(--radius-sm);
  background: var(--color-bg-elevated); border: 1px solid var(--color-border);
  color: var(--color-text-secondary); cursor: pointer; transition: var(--transition-normal);
}
.pager-btn:hover:not(:disabled) { background: var(--color-bg-surface); color: var(--color-text-primary); }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* store */
.store-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: var(--space-xl); flex-wrap: wrap; gap: 12px;
}
.store-score { font-size: var(--font-size-base); color: var(--color-text-secondary); }
.store-score strong { color: var(--color-primary); font-size: var(--font-size-xl); font-family: 'DIN Alternate', monospace; }
.store-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.cat-btn {
  padding: 6px 12px; background: var(--color-bg-elevated); border: 1px solid var(--color-border);
  color: var(--color-text-secondary); border-radius: var(--radius-full); cursor: pointer; font-size: var(--font-size-sm);
  transition: var(--transition-normal);
}
.cat-btn:hover { background: var(--color-bg-surface); color: var(--color-text-primary); }
.cat-btn.active { background: var(--color-gold-subtle); border-color: var(--color-primary); color: var(--color-primary); }
.store-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

/* growth tips */
.growth-tips { margin-top: var(--space-2xl); }
.tips-title {
  font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-secondary);
  margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;
}
.tips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.tip-card {
  background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-md);
  padding: 14px; font-size: var(--font-size-sm); color: var(--color-text-secondary); line-height: 1.6;
}
.tip-card strong { color: var(--color-primary); }

/* Redeem modal */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.8);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
.redeem-modal {
  width: 360px; max-width: 90vw; background: var(--color-bg-elevated);
  border-radius: var(--radius-lg); padding: 36px 28px; text-align: center;
  animation: scaleIn 0.25s ease; border: 1px solid var(--color-border);
  position: relative; overflow: hidden;
}
@keyframes scaleIn { from { transform: scale(0.85); opacity: 0 } to { transform: scale(1); opacity: 1 } }
.redeem-particles {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%); pointer-events: none;
}
.rp {
  position: absolute; animation: rp-float 1.5s ease-out infinite;
  opacity: 0; font-size: var(--font-size-sm);
}
@keyframes rp-float {
  0% { opacity: 0; transform: translate(0, 0) scale(0); }
  20% { opacity: 1; }
  100% { opacity: 0; transform: var(--tx) var(--ty) scale(1); }
}
.redeem-icon { font-size: 56px; margin-bottom: 12px; }
.redeem-modal h2 { font-size: var(--font-size-3xl); color: var(--color-gold); margin-bottom: 16px; }
.redeem-product {
  display: flex; flex-direction: column; align-items: center;
  gap: 6px; margin-bottom: 12px;
}
.redeem-icon-sm { font-size: 28px; }
.redeem-duration { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.redeem-balance {
  display: flex; justify-content: center; gap: 16px;
  font-size: var(--font-size-base); color: var(--color-text-secondary); margin-bottom: 24px;
}
.redeem-balance span:first-child { color: #ff6b6b; font-weight: 700; }
.redeem-balance span:last-child { color: #4ade80; font-weight: 700; }
.btn-primary {
  padding: 10px 32px; background: var(--color-primary); color: white;
  border: none; border-radius: var(--radius-md); font-size: var(--font-size-base);
  font-weight: 600; cursor: pointer; transition: var(--transition-normal);
}
.btn-primary:hover { background: #4a6fee; }

.empty-state {
  text-align: center; padding: 48px; color: var(--color-text-secondary);
}
.empty-state span { font-size: 40px; display: block; margin-bottom: 8px; }
.empty-state p { font-size: var(--font-size-base); }

@media (max-width: 768px) {
  .sidebar { display: none; }
  .main-content { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .store-grid { grid-template-columns: repeat(2, 1fr); }
}

/* Settings */
.settings-card {
  background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 24px;
}
.settings-title {
  font-size: var(--font-size-xl); font-weight: 700; color: var(--color-text-primary); margin-bottom: var(--space-xl);
}
.settings-group { margin-bottom: var(--space-xl); }
.settings-group:last-child { margin-bottom: 0; }
.settings-label {
  display: block; font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-secondary); margin-bottom: 8px;
}
.settings-input {
  width: 100%; height: 40px; padding: 0 14px;
  background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md);
  color: var(--color-text-primary); font-size: var(--font-size-base); outline: none; box-sizing: border-box;
  transition: border-color var(--transition-normal);
}
.settings-input:focus { border-color: var(--color-primary); }
.settings-input::placeholder { color: var(--color-text-muted); }
select.settings-input { cursor: pointer; }
select.settings-input option { background: var(--color-bg-elevated); color: var(--color-text-primary); }
.settings-row { display: flex; gap: 10px; }
.settings-row .settings-input { flex: 1; }
.settings-btn {
  height: 40px; padding: 0 20px; background: var(--color-primary); color: white;
  border: none; border-radius: var(--radius-md); font-size: var(--font-size-base); font-weight: 600;
  cursor: pointer; transition: var(--transition-normal); white-space: nowrap;
}
.settings-btn:hover { background: #4a6fee; }
.settings-divider { height: 1px; background: var(--color-border); margin: var(--space-xl) 0; }
.settings-toggle-row { display: flex; align-items: center; justify-content: space-between; }
.settings-toggle {
  width: 44px; height: 24px; border-radius: 12px;
  background: var(--color-border); border: none; cursor: pointer; position: relative;
  transition: var(--transition-normal);
}
.settings-toggle.active { background: var(--color-success); }
.settings-toggle-dot {
  width: 18px; height: 18px; background: #fff; border-radius: 50%;
  position: absolute; top: 3px; left: 3px; transition: left var(--transition-normal);
  box-shadow: 0 1px 3px rgba(0,0,0,0.2);
}
.settings-toggle.active .settings-toggle-dot { left: 23px; }
</style>
