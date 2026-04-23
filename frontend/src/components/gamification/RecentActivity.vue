<template>
  <div class="recent-activity">
    <div class="activity-title">📅 近期动态</div>
    <div class="activity-list">
      <div class="activity-item" v-for="item in activities" :key="item.id">
        <div class="activity-icon">{{ activityIcon(item.type) }}</div>
        <div class="activity-body">
          <div class="activity-desc">{{ item.description }}</div>
          <div class="activity-time">{{ formatTime(item.createdAt) }}</div>
        </div>
        <div class="activity-rewards" v-if="item.exp || item.score">
          <span class="reward-exp" v-if="item.exp">+{{ item.exp }} EXP</span>
          <span class="reward-score" v-if="item.score">+{{ item.score }} 积分</span>
        </div>
      </div>
      <div class="empty-activity" v-if="!activities.length">
        <span>📋</span>
        <p>还没有动态，开始执行 SOP 吧！</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 近期活动记录组件
 * - 展示最近的积分变动、徽章获得等
 */
import type { Activity } from '@/api/gamification'

defineProps<{ activities: Activity[] }>()

function activityIcon(type: string) {
  return { execution: '📋', redeem: '🎁', badge: '🏅', level_up: '⬆️' }[type] || '📌'
}

function formatTime(dateStr: string) {
  const d = new Date(dateStr)
  const now = new Date()
  const diff = Math.floor((now.getTime() - d.getTime()) / 1000)
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
</script>

<style scoped>
.recent-activity {}
.activity-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-md);
}
.activity-list {}
.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--color-border);
}
.activity-item:last-child { border-bottom: none; }
.activity-icon { font-size: 20px; flex-shrink: 0; }
.activity-body { flex: 1; }
.activity-desc { font-size: 13px; color: var(--color-text-primary); margin-bottom: 2px; }
.activity-time { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.activity-rewards { display: flex; gap: 6px; flex-shrink: 0; }
.reward-exp {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  font-size: var(--font-size-xs);
  padding: 2px 7px;
  border-radius: var(--radius-sm);
  font-weight: 600;
}
.reward-score {
  background: rgba(255, 215, 0, 0.1);
  color: var(--color-gold);
  font-size: var(--font-size-xs);
  padding: 2px 7px;
  border-radius: var(--radius-sm);
  font-weight: 600;
}
.empty-activity {
  text-align: center;
  padding: var(--space-xl);
  color: var(--color-text-secondary);
}
.empty-activity span { font-size: 32px; display: block; margin-bottom: var(--space-sm); }
.empty-activity p { font-size: 13px; }
</style>
