<template>
  <div class="level-progress">
    <div class="level-header">
      <div class="level-info-left">
        <div class="level-avatar-wrap">
          <div class="avatar-placeholder">{{ username?.charAt(0) || 'U' }}</div>
          <div class="avatar-frame" :class="`rank-${rank}`"></div>
        </div>
        <div class="level-text">
          <div class="level-name">{{ username }}</div>
          <div class="level-rank">
            <RankBadge :rank="rank" size="sm" />
            <span>Lv.{{ level }} {{ rankName }}</span>
          </div>
          <div class="level-streak" v-if="streakDays > 0">🔥 连续 {{ streakDays }} 天</div>
        </div>
      </div>
      <div class="level-stats-right">
        <div class="stat-pill">
          <span class="stat-val">{{ totalScore }}</span>
          <span class="stat-lbl">积分</span>
        </div>
        <div class="stat-pill">
          <span class="stat-val">{{ badgeCount }}/{{ totalBadgeCount }}</span>
          <span class="stat-lbl">徽章</span>
        </div>
      </div>
    </div>

    <div class="exp-bar-wrap">
      <div class="exp-labels">
        <span>Lv.{{ level }}</span>
        <span class="exp-hint" v-if="exp < expToNext">
          距离下一级还需 {{ expToNext - exp }} EXP 💡
        </span>
        <span>Lv.{{ level + 1 }}</span>
      </div>
      <div class="exp-bar">
        <div class="exp-fill" :style="{ width: `${(exp / expToNext) * 100}%` }"></div>
      </div>
      <div class="exp-numbers">{{ exp }} / {{ expToNext }} EXP</div>
    </div>

    <RankDiagram :currentRank="rank" :currentExp="exp" :expToNext="expToNext" />
  </div>
</template>

<script setup lang="ts">
/**
 * 等级进度条组件
 * - 当前等级、积分进度条
 * - 距离下一等级还需多少积分
 */
import RankBadge from './RankBadge.vue'
import RankDiagram from './RankDiagram.vue'

defineProps<{
  username: string
  level: number
  exp: number
  expToNext: number
  rank: string
  rankName: string
  totalScore: number
  streakDays: number
  badgeCount: number
  totalBadgeCount: number
}>()
</script>

<style scoped>
.level-progress {}
.level-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-bg-elevated);
  border-radius: 14px;
  padding: 18px 20px;
  border: 1px solid var(--color-border);
  margin-bottom: var(--space-xl);
}
.level-info-left { display: flex; align-items: center; gap: 14px; }
.level-avatar-wrap { position: relative; }
.avatar-placeholder {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #3b5fdf);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 22px;
  font-weight: 700;
}
.avatar-frame {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 3px solid transparent;
}
.rank-bronze .avatar-frame { border-color: var(--color-bronze); }
.rank-silver .avatar-frame { border-color: var(--color-silver); box-shadow: 0 0 8px rgba(192, 192, 192, 0.4); }
.rank-gold .avatar-frame { border-color: var(--color-gold); box-shadow: 0 0 10px rgba(255, 215, 0, 0.4); }
.rank-diamond .avatar-frame { border-color: #b9f2ff; box-shadow: 0 0 12px rgba(185, 242, 255, 0.4); }
.rank-king .avatar-frame {
  border-color: #ff6b35;
  box-shadow: 0 0 14px rgba(255, 107, 53, 0.5);
  animation: fire-ring 1.5s ease-in-out infinite;
}
@keyframes fire-ring {
  0%, 100% { box-shadow: 0 0 14px rgba(255, 107, 53, 0.5); }
  50% { box-shadow: 0 0 22px rgba(255, 140, 66, 0.7); }
}
.level-text {}
.level-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}
.level-rank {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-base);
  color: var(--color-text-secondary);
  font-weight: 600;
  margin-bottom: 2px;
}
.level-streak { font-size: var(--font-size-sm); color: #ffa500; }
.level-stats-right { display: flex; gap: 10px; }
.stat-pill {
  background: rgba(91, 127, 255, 0.1);
  border: 1px solid rgba(91, 127, 255, 0.2);
  border-radius: 10px;
  padding: 8px 14px;
  text-align: center;
}
.stat-val {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'DIN Alternate', monospace;
}
.stat-lbl { font-size: var(--font-size-xs); color: var(--color-text-secondary); }
.exp-bar-wrap { margin-bottom: var(--space-2xl); }
.exp-labels {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-sm);
}
.exp-hint { color: var(--color-primary); font-weight: 600; }
.exp-bar {
  height: 10px;
  background: var(--color-border);
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 6px;
}
.exp-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b5fdf, var(--color-primary));
  border-radius: 5px;
  transition: width 0.8s ease-out;
  position: relative;
}
.exp-fill::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 20px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3));
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0%, 100% { opacity: 0; }
  50% { opacity: 1; }
}
.exp-numbers {
  text-align: right;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  font-family: 'DIN Alternate', monospace;
}
</style>
