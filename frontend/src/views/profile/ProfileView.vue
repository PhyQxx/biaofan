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
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
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
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await gamificationApi.getProfile()
  } catch {
    ElMessage.error('加载个人中心失败')
  } finally {
    loading.value = false
  }
}

async function loadBadges() {
  try {
    badges.value = await gamificationApi.getBadges()
  } catch {
    ElMessage.error('加载徽章失败')
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    const res = await gamificationApi.getScoreHistory(historyPage.value, 20)
    historyItems.value = res.items
    totalPages.value = Math.max(1, Math.ceil(res.total / res.pageSize))
  } catch {
    ElMessage.error('加载积分历史失败')
  } finally {
    loadingHistory.value = false
  }
}

async function loadStore() {
  loadingStore.value = true
  try {
    storeProducts.value = await gamificationApi.getStore(activeCategory.value || undefined)
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
  font-size: 22px; font-weight: 700; color: #e8eaf0;
}
.page-tabs { display: flex; gap: 4px; flex-wrap: wrap; }
.tab-btn {
  padding: 7px 14px; background: #1a1d27; border: 1px solid #2d3348;
  color: #8b90a0; border-radius: 8px; cursor: pointer; font-size: 13px;
  transition: all 0.2s; white-space: nowrap;
}
.tab-btn:hover { background: #22263a; color: #e8eaf0; }
.tab-btn.active {
  background: #5b7fff; border-color: #5b7fff; color: white;
  font-weight: 600;
}
.loading-state {
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; padding: 60px; color: #8b90a0; gap: 16px;
}
.loading-spinner {
  width: 36px; height: 36px; border: 3px solid #2d3348;
  border-top-color: #5b7fff; border-radius: 50%;
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
  font-size: 13px; padding: 4px 12px; border-radius: 20px;
  font-weight: 600;
}
.stat-chip.income { background: rgba(74, 222, 128, 0.1); color: #4ade80; }
.stat-chip.expense { background: rgba(255, 107, 107, 0.1); color: #ff6b6b; }
.history-pager { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #8b90a0; }
.pager-btn {
  width: 28px; height: 28px; border-radius: 6px;
  background: #1a1d27; border: 1px solid #2d3348;
  color: #8b90a0; cursor: pointer; transition: all 0.2s;
}
.pager-btn:hover:not(:disabled) { background: #22263a; color: #e8eaf0; }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* store */
.store-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 20px; flex-wrap: wrap; gap: 12px;
}
.store-score { font-size: 14px; color: #8b90a0; }
.store-score strong { color: #5b7fff; font-size: 18px; font-family: 'DIN Alternate', monospace; }
.store-tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.cat-btn {
  padding: 6px 12px; background: #1a1d27; border: 1px solid #2d3348;
  color: #8b90a0; border-radius: 20px; cursor: pointer; font-size: 13px;
  transition: all 0.2s;
}
.cat-btn:hover { background: #22263a; color: #e8eaf0; }
.cat-btn.active { background: rgba(91, 127, 255, 0.15); border-color: #5b7fff; color: #5b7fff; }
.store-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

/* growth tips */
.growth-tips { margin-top: 24px; }
.tips-title {
  font-size: 14px; font-weight: 600; color: #8b90a0;
  margin-bottom: 12px; text-transform: uppercase; letter-spacing: 0.5px;
}
.tips-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px; }
.tip-card {
  background: #1a1d27; border: 1px solid #2d3348; border-radius: 10px;
  padding: 14px; font-size: 13px; color: #8b90a0; line-height: 1.6;
}
.tip-card strong { color: #5b7fff; }

/* Redeem modal */
.modal-overlay {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.8);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
.redeem-modal {
  width: 360px; max-width: 90vw; background: #1a1d27;
  border-radius: 16px; padding: 36px 28px; text-align: center;
  animation: scaleIn 0.25s ease; border: 1px solid #2d3348;
  position: relative; overflow: hidden;
}
@keyframes scaleIn { from { transform: scale(0.85); opacity: 0 } to { transform: scale(1); opacity: 1 } }
.redeem-particles {
  position: absolute; top: 50%; left: 50%;
  transform: translate(-50%, -50%); pointer-events: none;
}
.rp {
  position: absolute; animation: rp-float 1.5s ease-out infinite;
  opacity: 0; font-size: 14px;
}
@keyframes rp-float {
  0% { opacity: 0; transform: translate(0, 0) scale(0); }
  20% { opacity: 1; }
  100% { opacity: 0; transform: var(--tx) var(--ty) scale(1); }
}
.redeem-icon { font-size: 56px; margin-bottom: 12px; }
.redeem-modal h2 { font-size: 20px; color: #ffd700; margin-bottom: 16px; }
.redeem-product {
  display: flex; flex-direction: column; align-items: center;
  gap: 6px; margin-bottom: 12px;
}
.redeem-icon-sm { font-size: 28px; }
.redeem-duration { font-size: 13px; color: #8b90a0; }
.redeem-balance {
  display: flex; justify-content: center; gap: 16px;
  font-size: 14px; color: #8b90a0; margin-bottom: 24px;
}
.redeem-balance span:first-child { color: #ff6b6b; font-weight: 700; }
.redeem-balance span:last-child { color: #4ade80; font-weight: 700; }
.btn-primary {
  padding: 10px 32px; background: #5b7fff; color: white;
  border: none; border-radius: 10px; font-size: 14px;
  font-weight: 600; cursor: pointer; transition: all 0.2s;
}
.btn-primary:hover { background: #4a6fee; }

.empty-state {
  text-align: center; padding: 48px; color: #8b90a0;
}
.empty-state span { font-size: 40px; display: block; margin-bottom: 8px; }
.empty-state p { font-size: 14px; }

@media (max-width: 768px) {
  .sidebar { display: none; }
  .main-content { padding: 16px; }
  .page-header { flex-direction: column; align-items: flex-start; }
  .store-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
