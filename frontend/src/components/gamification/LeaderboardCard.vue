<template>
  <div class="lb-card" :class="{ 'lb-me': item.isMe }">
    <div class="lb-rank">{{ item.rank }}.</div>
    <div class="lb-avatar">{{ item.username.charAt(0) }}</div>
    <div class="lb-info">
      <div class="lb-name">{{ item.username }}</div>
      <div class="lb-sub">{{ item.count }} 个 SOP · 连续{{ item.streakDays }}天</div>
    </div>
    <div class="lb-count">{{ item.count }}</div>
  </div>
</template>

<script setup lang="ts">


/**
 * 排行榜单条记录卡片
 * - 排名、头像、昵称、积分
 */
import type { LeaderboardItem } from '@/api/gamification'
defineProps<{ item: LeaderboardItem }>()
</script>

<style scoped>
.lb-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-bg-elevated);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px 16px;
  transition: all var(--transition-normal);
}
.lb-card:hover {
  background: var(--color-bg-surface);
  transform: translateX(4px);
}
.lb-me {
  background: rgba(91, 127, 255, 0.08);
  border-left: 3px solid var(--color-primary);
}
.lb-rank {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-weight: 600;
  min-width: 24px;
}
.lb-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #3b5fdf);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 700;
  font-size: 14px;
}
.lb-info { flex: 1; }
.lb-name {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--color-text-primary);
}
.lb-sub { font-size: var(--font-size-sm); color: var(--color-text-secondary); }
.lb-count {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'DIN Alternate', 'Roboto', monospace;
}
</style>
