<template>
  <div class="top3-card" :class="`rank-${item.rank}`">
    <div class="rank-medal">{{ rankEmoji }}</div>
    <div class="rank-glow" v-if="item.rank === 1">✨</div>
    <div class="top3-info">
      <div class="top3-name">{{ item.username }}</div>
      <div class="top3-count">{{ item.count }} 个 SOP</div>
      <div class="top3-streak" v-if="item.streakDays > 0">🔥 连续 {{ item.streakDays }} 天</div>
    </div>
    <div class="rank-decoration"></div>
  </div>
</template>

<script setup lang="ts">


/**
 * 排行榜 Top3 特殊展示
 * - 第1/2/3名特殊样式（奖牌图标）
 * - 头像 + 昵称 + 积分
 */
import { computed } from 'vue'
import type { LeaderboardItem } from '@/api/gamification'

const props = defineProps<{ item: LeaderboardItem }>()

const rankEmoji = computed(() => ['', '🥇', '🥈', '🥉'][props.item.rank] || `${props.item.rank}.`)
</script>

<style scoped>
.top3-card {
  border-radius: var(--radius-lg);
  padding: 16px 18px;
  position: relative;
  overflow: hidden;
  margin-bottom: 12px;
}
.rank-1 {
  background: linear-gradient(135deg, #2a2000, #3d2e00);
  border: 2px solid var(--color-gold);
  box-shadow: 0 0 20px rgba(255, 215, 0, 0.3);
  animation: gold-float 3s ease-in-out infinite;
}
.rank-2 {
  background: linear-gradient(135deg, #1a1a20, #252530);
  border: 2px solid var(--color-silver);
  box-shadow: 0 0 12px rgba(192, 192, 192, 0.25);
  animation: silver-pulse 2.5s ease-in-out infinite;
}
.rank-3 {
  background: linear-gradient(135deg, #1a1208, #251a0c);
  border: 2px solid var(--color-bronze);
}
@keyframes gold-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-3px); }
}
@keyframes silver-pulse {
  0%, 100% { box-shadow: 0 0 12px rgba(192, 192, 192, 0.25); }
  50% { box-shadow: 0 0 18px rgba(192, 192, 192, 0.4); }
}
.rank-medal {
  font-size: 28px;
  margin-bottom: var(--space-sm);
}
.rank-glow {
  position: absolute;
  top: 8px;
  right: 12px;
  font-size: 16px;
  animation: sparkle 2s ease-in-out infinite;
}
@keyframes sparkle {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(1.2); }
}
.top3-info {}
.top3-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.rank-1 .top3-name { color: var(--color-gold); }
.top3-count {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 2px;
}
.top3-streak {
  font-size: var(--font-size-sm);
  color: #ffa500;
}
.rank-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.rank-1 .rank-decoration { background: linear-gradient(90deg, transparent, var(--color-gold), transparent); }
.rank-2 .rank-decoration { background: linear-gradient(90deg, transparent, var(--color-silver), transparent); }
.rank-3 .rank-decoration { background: linear-gradient(90deg, transparent, var(--color-bronze), transparent); }
</style>
