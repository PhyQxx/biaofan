<template>
  <div class="rank-diagram">
    <div class="diagram-title">段位进程</div>
    <div class="rank-track">
      <div
        v-for="(r, i) in ranks"
        :key="r.id"
        class="rank-node"
        :class="{
          'rank-active': rankIndex >= i,
          'rank-current': currentRank === r.id,
          'rank-locked': rankIndex < i,
        }"
      >
        <div class="rank-circle">
          <span class="rank-node-emoji">{{ r.emoji }}</span>
          <div class="rank-glow" v-if="currentRank === r.id"></div>
        </div>
        <div class="rank-node-name">{{ r.name }}</div>
        <div class="rank-node-exp" v-if="rankIndex >= i">{{ r.threshold }} EXP</div>
        <div class="rank-node-exp" v-else>? EXP</div>
      </div>
      <div class="rank-line">
        <div class="rank-line-fill" :style="{ width: lineProgress }"></div>
      </div>
    </div>
    <div class="rank-tip" v-if="nextRank">
      <span>距离 {{ nextRank.name }} 还需 {{ expToNext }} EXP</span>
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 等级段位图表组件
 * - 用图形化方式展示等级体系
 * - 当前等级高亮
 */
import { computed } from 'vue'

const props = defineProps<{
  currentRank: string
  currentExp: number
  expToNext: number
}>()

const ranks = [
  { id: 'bronze', name: '青铜', emoji: '🥉', threshold: 0 },
  { id: 'silver', name: '白银', emoji: '🥈', threshold: 1000 },
  { id: 'gold', name: '黄金', emoji: '🥇', threshold: 3000 },
  { id: 'diamond', name: '钻石', emoji: '💎', threshold: 7000 },
  { id: 'king', name: '王者', emoji: '👑', threshold: 15000 },
]

const rankIndex = computed(() => ranks.findIndex((r) => r.id === props.currentRank))
const nextRank = computed(() => ranks[rankIndex.value + 1] || null)

const lineProgress = computed(() => {
  if (rankIndex.value >= ranks.length - 1) return '100%'
  const current = ranks[rankIndex.value]
  const next = ranks[rankIndex.value + 1]
  const range = next.threshold - current.threshold
  const progress = props.currentExp - current.threshold
  return `${Math.min(100, (progress / range) * 100)}%`
})
</script>

<style scoped>
.rank-diagram { margin-top: 8px; }
.diagram-title {
  font-size: 13px;
  font-weight: 600;
  color: #8b90a0;
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.rank-track {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  position: relative;
  padding-bottom: 8px;
}
.rank-line {
  position: absolute;
  top: 20px;
  left: 24px;
  right: 24px;
  height: 4px;
  background: #2d3348;
  border-radius: 2px;
  z-index: 0;
}
.rank-line-fill {
  height: 100%;
  background: linear-gradient(90deg, #cd7f32, #c0c0c0, #ffd700, #b9f2ff, #ff6b35);
  border-radius: 2px;
  transition: width 0.8s ease-out;
}
.rank-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
  flex: 1;
}
.rank-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  position: relative;
  margin-bottom: 6px;
  background: #2d3348;
  border: 3px solid #2d3348;
  transition: all 0.3s;
}
.rank-locked .rank-circle { opacity: 0.4; }
.rank-active .rank-circle { background: rgba(91, 127, 255, 0.1); border-color: #5b7fff; }
.rank-current .rank-circle {
  border-color: #ffd700;
  box-shadow: 0 0 16px rgba(255, 215, 0, 0.5);
  animation: current-pulse 1.5s ease-in-out infinite;
}
@keyframes current-pulse {
  0%, 100% { box-shadow: 0 0 16px rgba(255, 215, 0, 0.5); }
  50% { box-shadow: 0 0 24px rgba(255, 215, 0, 0.8); }
}
.rank-node-emoji { line-height: 1; }
.rank-glow {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid rgba(255, 215, 0, 0.3);
  animation: glow-ring 2s ease-in-out infinite;
}
@keyframes glow-ring {
  0%, 100% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.15); opacity: 0; }
}
.rank-node-name {
  font-size: 12px;
  font-weight: 600;
  color: #8b90a0;
  margin-bottom: 2px;
}
.rank-active .rank-node-name { color: #e8eaf0; }
.rank-node-exp { font-size: 10px; color: #555a6e; }
.rank-active .rank-node-exp { color: #8b90a0; }
.rank-tip {
  text-align: center;
  font-size: 12px;
  color: #5b7fff;
  margin-top: 14px;
  font-weight: 600;
}
</style>
