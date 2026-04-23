<template>
  <Teleport to="body">
    <div class="celebration-overlay" @click.self="$emit('close')">
      <div class="celebration-box">
        <div class="particles">
          <span v-for="i in 12" :key="i" class="particle" :style="particleStyle(i)">✨</span>
        </div>
        <div class="celebration-icon">🎉</div>
        <h2 class="celebration-title">恭喜升级！</h2>
        <div class="celebration-level">
          <span class="level-label">Lv.{{ newLevel }}</span>
          <span class="level-arrow">✨ 进阶 ✨</span>
        </div>
        <div class="celebration-rank" v-if="rank">
          <RankBadge :rank="rank" size="lg" />
          <span>{{ rankName }}</span>
        </div>
        <div class="celebration-rewards" v-if="rewards">
          <span class="reward">+{{ rewards.exp }} EXP</span>
          <span class="reward" v-if="rewards.score">+{{ rewards.score }} 积分</span>
        </div>
        <div class="celebration-btns">
          <button class="btn-primary" @click="$emit('view-achievements')">查看成就</button>
          <button class="btn-secondary" @click="$emit('close')">关闭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">



/**
 * 升级庆祝动画组件
 * - 等级提升时弹出粒子庆祝效果
 * - 显示新等级信息
 */
import RankBadge from './RankBadge.vue'

const props = defineProps<{
  newLevel: number
  rank?: string
  rankName?: string
  rewards?: { exp: number; score?: number }
}>()
defineEmits<{ close: []; 'view-achievements': [] }>()

function particleStyle(i: number) {
  const angle = (i / 12) * 360
  const radius = 80 + Math.random() * 40
  const x = Math.cos((angle * Math.PI) / 180) * radius
  const y = Math.sin((angle * Math.PI) / 180) * radius
  const delay = (i / 12) * 0.5
  const size = 12 + Math.random() * 8
  return {
    transform: `translate(${x}px, ${y}px)`,
    animationDelay: `${delay}s`,
    fontSize: `${size}px`,
  }
}
</script>

<style scoped>
.celebration-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: overlayIn 0.3s ease;
}
@keyframes overlayIn {
  from { background: rgba(0, 0, 0, 0); }
  to { background: rgba(0, 0, 0, 0.8); }
}
.celebration-box {
  width: 380px;
  max-width: 90vw;
  background: linear-gradient(135deg, var(--color-bg-elevated), var(--color-bg-surface));
  border-radius: 20px;
  padding: 36px 28px;
  text-align: center;
  position: relative;
  border: 2px solid rgba(255, 215, 0, 0.3);
  box-shadow: 0 0 40px rgba(255, 215, 0, 0.2);
  animation: boxBounce 0.4s ease;
  overflow: visible;
}
@keyframes boxBounce {
  0% { transform: scale(0.7); opacity: 0; }
  60% { transform: scale(1.05); }
  100% { transform: scale(1); opacity: 1; }
}
.particles {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
}
.particle {
  position: absolute;
  animation: float-out 1.5s ease-out infinite;
  opacity: 0;
}
@keyframes float-out {
  0% { opacity: 0; transform: translate(0, 0) scale(0); }
  20% { opacity: 1; }
  100% { opacity: 0; transform: var(--tx, 60px) var(--ty, -60px) scale(1); }
}
.celebration-icon { font-size: 56px; margin-bottom: 12px; animation: iconPop 0.6s ease 0.2s both; }
@keyframes iconPop { 0% { transform: scale(0) rotate(-20deg); } 60% { transform: scale(1.2) rotate(10deg); } 100% { transform: scale(1) rotate(0deg); } }
.celebration-title {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 16px;
  text-shadow: 0 0 20px rgba(255, 215, 0, 0.5);
}
.celebration-level {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}
.level-label {
  font-size: 40px;
  font-weight: 900;
  color: var(--color-text-primary);
  font-family: 'DIN Alternate', monospace;
}
.level-arrow { font-size: var(--font-size-base); color: var(--color-gold); }
.celebration-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  color: var(--color-text-primary);
  margin-bottom: 14px;
}
.celebration-rewards {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}
.reward {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  padding: 4px 14px;
  border-radius: 20px;
  font-size: var(--font-size-base);
  font-weight: 600;
}
.celebration-btns { display: flex; gap: 12px; justify-content: center; }
.btn-primary {
  padding: 10px 24px;
  background: var(--color-primary);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
}
.btn-primary:hover { background: #4a6fee; }
.btn-secondary {
  padding: 10px 24px;
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  border-radius: 10px;
  font-size: var(--font-size-base);
  cursor: pointer;
}
.btn-secondary:hover { background: var(--color-bg-elevated); color: var(--color-text-primary); }
</style>
