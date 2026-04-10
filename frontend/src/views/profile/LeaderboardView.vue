<template>
  <div class="leaderboard-page">
    <div class="page-header">
      <h1>📊 执行排行榜</h1>
      <div class="lb-range" v-if="lbData">
        {{ lbData.startDate }} ~ {{ lbData.endDate }}
      </div>
    </div>

    <!-- Tab Bar -->
    <div class="lb-tabs">
      <button
        v-for="tab in tabTypes"
        :key="tab.key"
        class="lb-tab"
        :class="{ active: activeType === tab.key }"
        @click="activeType = tab.key; loadLeaderboard()"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Loading -->
    <div class="loading-state" v-if="loading">
      <div class="loading-spinner"></div>
    </div>

    <div v-if="!loading && lbData">
      <!-- Top3 -->
      <div class="top3-section">
        <LeaderboardTop3
          v-for="item in lbData.items.filter(i => i.rank <= 3)"
          :key="item.userId"
          :item="item"
        />
      </div>

      <!-- 4~10 -->
      <div class="lb-list">
        <LeaderboardCard
          v-for="item in lbData.items.filter(i => i.rank > 3 && i.rank <= 10)"
          :key="item.userId"
          :item="item"
        />
      </div>

      <!-- My Rank -->
      <div class="my-rank-section" v-if="lbData.myRank">
        <div class="my-rank-label">📍 我的排名</div>
        <div class="my-rank-card">
          <div class="my-rank-info">
            <span class="my-rank-num">第 {{ lbData.myRank }} 名</span>
            <span class="my-rank-count">{{ lbData.myCount }} 个 SOP</span>
          </div>
          <div class="my-rank-percent">超过 {{ myPercent }}% 的用户 🏆</div>
        </div>
      </div>

      <div class="empty-state" v-if="!lbData.items.length">
        <span>📊</span>
        <p>暂无排行榜数据</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * PC 端排行榜页
 * - 积分排行榜 Top3 特殊展示
 * - 完整排名列表（头像、昵称、积分）
 * - 筛选：日 / 周 / 月 / 总榜
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { gamificationApi, type LeaderboardOverview } from '@/api/gamification'
import LeaderboardTop3 from '@/components/gamification/LeaderboardTop3.vue'
import LeaderboardCard from '@/components/gamification/LeaderboardCard.vue'

const tabTypes = [
  { key: 'weekly', label: '周榜' },
  { key: 'monthly', label: '月榜' },
  { key: 'category_work', label: '工作类' },
  { key: 'category_life', label: '生活类' },
  { key: 'category_study', label: '学习类' },
  { key: 'speed', label: '速度' },
  { key: 'streak', label: '连续' },
]

const activeType = ref('weekly')
const loading = ref(false)
const lbData = ref<LeaderboardOverview | null>(null)

const myPercent = computed(() => {
  if (!lbData.value?.myRank) return 0
  const rank = lbData.value.myRank
  const total = lbData.value.totalParticipants || 1
  return Math.min(99, Math.round((1 - rank / total) * 100))
})

onMounted(() => {
  loadLeaderboard()
})

async function loadLeaderboard() {
  loading.value = true
  try {
    const raw: any = await gamificationApi.getLeaderboard(activeType.value)
    if (Array.isArray(raw)) {
      lbData.value = { items: raw } as any
    } else if (raw && raw.items) {
      lbData.value = raw as any
    } else if (raw && raw.data) {
      lbData.value = { items: Array.isArray(raw.data) ? raw.data : [] } as any
    } else {
      lbData.value = { items: [] } as any
    }
  } catch {
    ElMessage.error('加载排行榜失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.leaderboard-page {
  padding: 0;
}
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-header h1 { font-size: 22px; font-weight: 700; color: #e8eaf0; }
.lb-range { font-size: 13px; color: #8b90a0; }
.lb-tabs {
  display: flex; gap: 6px; margin-bottom: 24px; flex-wrap: wrap;
  background: #1a1d27; border-radius: 10px; padding: 6px;
  border: 1px solid #2d3348;
}
.lb-tab {
  padding: 7px 14px; background: transparent; border: none;
  color: #8b90a0; border-radius: 8px; cursor: pointer;
  font-size: 13px; transition: all 0.2s; white-space: nowrap;
}
.lb-tab:hover { background: #22263a; color: #e8eaf0; }
.lb-tab.active { background: #5b7fff; color: white; font-weight: 600; }
.loading-state { display: flex; justify-content: center; padding: 40px; }
.loading-spinner {
  width: 36px; height: 36px; border: 3px solid #2d3348;
  border-top-color: #5b7fff; border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.top3-section { margin-bottom: 16px; }
.lb-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; }
.my-rank-section { margin-top: 20px; }
.my-rank-label { font-size: 13px; color: #8b90a0; margin-bottom: 8px; font-weight: 600; }
.my-rank-card {
  background: rgba(91, 127, 255, 0.08);
  border: 1px solid rgba(91, 127, 255, 0.3);
  border-left: 3px solid #5b7fff;
  border-radius: 10px;
  padding: 14px 18px;
  animation: my-pulse 2.5s ease-in-out infinite;
}
@keyframes my-pulse {
  0%, 100% { box-shadow: 0 0 0 rgba(91, 127, 255, 0); }
  50% { box-shadow: 0 0 12px rgba(91, 127, 255, 0.15); }
}
.my-rank-info { display: flex; align-items: center; gap: 14px; margin-bottom: 4px; }
.my-rank-num { font-size: 18px; font-weight: 800; color: #5b7fff; font-family: 'DIN Alternate', monospace; }
.my-rank-count { font-size: 14px; color: #8b90a0; }
.my-rank-percent { font-size: 13px; color: #4ade80; font-weight: 600; }
.empty-state { text-align: center; padding: 48px; color: #8b90a0; }
.empty-state span { font-size: 40px; display: block; margin-bottom: 8px; }
.empty-state p { font-size: 14px; }
@media (max-width: 768px) {
  .sidebar { display: none; }
  .main-content { padding: 16px; }
}
</style>
