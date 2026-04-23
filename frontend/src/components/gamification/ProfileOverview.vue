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
/**
 * 个人概览组件
 * - 头像、昵称、等级、积分
 * - 徽章墙入口
 */
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
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  margin-bottom: var(--space-lg);
}
.overview-left { display: flex; align-items: center; gap: var(--space-lg); }
.overview-avatar-wrap { position: relative; }
.overview-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #3b5fdf);
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
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.overview-title-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 13px;
  color: var(--color-gold);
  margin-bottom: 4px;
}
.title-expire { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.overview-meta {
  display: flex;
  gap: 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}
.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: var(--space-lg);
}
.stat-card {
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  text-align: center;
}
.stat-icon { font-size: 24px; margin-bottom: var(--space-sm); }
.stat-val {
  font-size: 22px;
  font-weight: 800;
  color: var(--color-text-primary);
  font-family: 'DIN Alternate', monospace;
  margin-bottom: 4px;
}
.stat-lbl { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
</style>
