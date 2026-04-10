<template>
  <div class="rank-badge" :class="`rank-${rank}`" :style="badgeStyle">
    <span class="rank-emoji">{{ rankEmoji }}</span>
    <span class="rank-name" v-if="showName">{{ rankLabel }}</span>
  </div>
</template>

<script setup lang="ts">


/**
 * 排名徽章组件
 * - 显示数字排名（第1名金色等）
 */
import { computed } from 'vue'

const props = defineProps<{
  rank: string
  size?: 'sm' | 'md' | 'lg'
  showName?: boolean
}>()

const RANK_MAP: Record<string, { emoji: string; label: string }> = {
  bronze: { emoji: '🥉', label: '青铜' },
  silver: { emoji: '🥈', label: '白银' },
  gold: { emoji: '🥇', label: '黄金' },
  diamond: { emoji: '💎', label: '钻石' },
  king: { emoji: '👑', label: '王者' },
}

const rankEmoji = computed(() => RANK_MAP[props.rank]?.emoji || '🥉')
const rankLabel = computed(() => RANK_MAP[props.rank]?.label || '青铜')

const sizeMap = { sm: 24, md: 32, lg: 48 }
const fontMap = { sm: 12, md: 16, lg: 24 }
const badgeStyle = computed(() => ({
  width: `${sizeMap[props.size || 'md']}px`,
  height: `${sizeMap[props.size || 'md']}px`,
  fontSize: `${fontMap[props.size || 'md']}px`,
}))
</script>

<style scoped>
.rank-badge {
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
  border: 2px solid transparent;
}
.rank-bronze { background: rgba(205, 127, 50, 0.2); border-color: #cd7f32; }
.rank-silver { background: rgba(192, 192, 192, 0.2); border-color: #c0c0c0; box-shadow: 0 0 6px rgba(192, 192, 192, 0.3); }
.rank-gold { background: rgba(255, 215, 0, 0.2); border-color: #ffd700; box-shadow: 0 0 8px rgba(255, 215, 0, 0.35); }
.rank-diamond { background: rgba(185, 242, 255, 0.2); border-color: #b9f2ff; box-shadow: 0 0 10px rgba(185, 242, 255, 0.4); }
.rank-king {
  background: rgba(255, 107, 53, 0.25);
  border-color: #ff6b35;
  box-shadow: 0 0 10px rgba(255, 107, 53, 0.5);
  animation: fire-pulse 1.5s ease-in-out infinite;
}
@keyframes fire-pulse {
  0%, 100% { box-shadow: 0 0 10px rgba(255, 107, 53, 0.5); }
  50% { box-shadow: 0 0 18px rgba(255, 140, 66, 0.7); }
}
.rank-emoji { line-height: 1; }
.rank-name {
  position: absolute;
  bottom: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  white-space: nowrap;
  color: #8b90a0;
}
</style>
