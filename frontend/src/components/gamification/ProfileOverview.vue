<template>
  <div class="profile-overview">
    <div class="overview-card">
      <div class="overview-left">
        <div class="overview-avatar-wrap">
          <div class="overview-avatar">{{ username?.charAt(0) || 'U' }}</div>
          <RankBadge :rank="rank" size="sm" class="overview-rank" />
        </div>
        <div class="overview-info">
          <div class="overview-name">{{ username }}</div>
          <div class="overview-title-row" v-if="equippedTitle">
            <span>{{ equippedTitle }}</span>
            <span class="title-expire" v-if="titleExpireAt">有效期至 {{ titleExpireAt }}</span>
          </div>
          <div class="overview-meta">
            <span>🔥 连续 {{ streakDays }} 天</span>
            <span>📊 本周 {{ thisWeekCount }} 个</span>
          </div>
        </div>
      </div>
    </div>

    <div class="overview-stats">
      <div class="stat-card">
        <div class="stat-icon">🏅</div>
        <div class="stat-val">{{ badgeCount }}/{{ totalBadgeCount }}</div>
        <div class="stat-lbl">徽章</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🎯</div>
        <div class="stat-val">{{ totalScore }}</div>
        <div class="stat-lbl">积分</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">📈</div>
        <div class="stat-val">Lv.{{ level }}</div>
        <div class="stat-lbl">{{ rankName }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import RankBadge from './RankBadge.vue'

defineProps<{
  username: string
  level: number
  rank: string
  rankName: string
  totalScore: number
  streakDays: number
  thisWeekCount: number
  badgeCount: number
  totalBadgeCount: number
  equippedTitle?: string
  titleExpireAt?: string
}>()
</script>

<style scoped>
.profile-overview {}
.overview-card {
  background: #1a1d27;
  border: 1px solid #2d3348;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
}
.overview-left { display: flex; align-items: center; gap: 16px; }
.overview-avatar-wrap { position: relative; }
.overview-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #5b7fff, #3b5fdf);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: 700;
}
.overview-rank {
  position: absolute;
  bottom: -2px;
  right: -2px;
}
.overview-info {}
.overview-name {
  font-size: 18px;
  font-weight: 700;
  color: #e8eaf0;
  margin-bottom: 4px;
}
.overview-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #ffd700;
  margin-bottom: 4px;
}
.title-expire { font-size: 11px; color: #8b90a0; }
.overview-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8b90a0;
}
.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: #1a1d27;
  border: 1px solid #2d3348;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}
.stat-icon { font-size: 24px; margin-bottom: 8px; }
.stat-val {
  font-size: 22px;
  font-weight: 800;
  color: #e8eaf0;
  font-family: 'DIN Alternate', monospace;
  margin-bottom: 4px;
}
.stat-lbl { font-size: 12px; color: #8b90a0; }
</style>
