<template>
  <Teleport to="body">
    <div class="modal-overlay" @click.self="$emit('close')">
      <div class="badge-modal">
        <button class="close-btn" @click="$emit('close')">✕</button>
        <div class="modal-icon" :class="`rarity-${badge.rarity}`">
          <span>{{ badge.icon }}</span>
        </div>
        <h2 class="modal-title">{{ badge.name }}</h2>
        <div class="modal-rarity">
          <span :class="`rarity-dot rarity-dot-${badge.rarity}`"></span>
          {{ rarityText[badge.rarity] }}
        </div>
        <div class="modal-divider"></div>
        <div class="modal-section">
          <div class="modal-label">获得条件</div>
          <div class="modal-value">{{ badge.condition }}</div>
        </div>
        <div class="modal-section">
          <div class="modal-label">条件说明</div>
          <div class="modal-value">{{ badge.description }}</div>
        </div>
        <div class="modal-section" v-if="badge.unlockedAt">
          <div class="modal-label">获得时间</div>
          <div class="modal-value">{{ badge.unlockedAt }}</div>
        </div>
        <div class="modal-section">
          <div class="modal-label">奖励</div>
          <div class="modal-rewards">
            <span class="reward-chip">+{{ badge.rewardExp }} EXP</span>
            <span class="reward-chip reward-score">+{{ badge.rewardScore }} 积分</span>
          </div>
        </div>
        <div class="modal-section" v-if="!badge.unlockedAt && badge.progress !== undefined">
          <div class="modal-label">当前进度</div>
          <div class="progress-bar-wrap">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${((badge.progress || 0) / (badge.target || 1)) * 100}%` }"></div>
            </div>
            <span class="progress-text">{{ badge.progress }}/{{ badge.target }}</span>
          </div>
          <p class="progress-hint">再完成 {{ (badge.target || 0) - (badge.progress || 0) }} 个即可解锁</p>
        </div>
        <div class="modal-tip" v-if="badge.unlockedAt">
          💡 继续保持，你已经超过了大多数用户！
        </div>
        <div class="modal-tip" v-else>
          💡 建议设置每日提醒，保持执行节奏
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import type { Badge } from '@/api/gamification'

defineProps<{ badge: Badge }>()
defineEmits<{ close: [] }>()

const rarityText: Record<string, string> = {
  bronze: '普通 · 铜',
  silver: '稀有 · 银',
  gold: '传奇 · 金',
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn { from { opacity: 0 } to { opacity: 1 } }
.badge-modal {
  width: 400px;
  max-width: 90vw;
  background: #1a1d27;
  border-radius: 16px;
  padding: 32px 28px;
  position: relative;
  animation: scaleIn 0.25s ease;
  border: 1px solid #2d3348;
  max-height: 85vh;
  overflow-y: auto;
}
@keyframes scaleIn { from { transform: scale(0.85); opacity: 0 } to { transform: scale(1); opacity: 1 } }
.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: #2d3348;
  border: none;
  color: #8b90a0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
}
.close-btn:hover { background: #3d4255; color: #e8eaf0; }
.modal-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 36px;
}
.modal-icon.rarity-bronze { background: rgba(205, 127, 50, 0.2); }
.modal-icon.rarity-silver { background: rgba(192, 192, 192, 0.2); }
.modal-icon.rarity-gold { background: rgba(255, 215, 0, 0.2); box-shadow: 0 0 20px rgba(255, 215, 0, 0.3); }
.modal-title {
  text-align: center;
  font-size: 20px;
  font-weight: 700;
  color: #e8eaf0;
  margin-bottom: 8px;
}
.modal-rarity {
  text-align: center;
  font-size: 13px;
  color: #8b90a0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.rarity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.rarity-dot-bronze { background: #cd7f32; }
.rarity-dot-silver { background: #c0c0c0; }
.rarity-dot-gold { background: #ffd700; }
.modal-divider {
  height: 1px;
  background: #2d3348;
  margin: 20px 0;
}
.modal-section { margin-bottom: 16px; }
.modal-label {
  font-size: 12px;
  color: #8b90a0;
  margin-bottom: 6px;
  font-weight: 600;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}
.modal-value {
  font-size: 14px;
  color: #e8eaf0;
  line-height: 1.5;
}
.modal-rewards {
  display: flex;
  gap: 10px;
}
.reward-chip {
  background: rgba(91, 127, 255, 0.15);
  color: #5b7fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
.reward-score {
  background: rgba(255, 215, 0, 0.1);
  color: #ffd700;
}
.progress-bar-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}
.progress-bar {
  flex: 1;
  height: 8px;
  background: #2d3348;
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #5b7fff;
  border-radius: 4px;
  transition: width 0.3s ease;
}
.progress-text { font-size: 13px; color: #e8eaf0; font-weight: 600; white-space: nowrap; }
.progress-hint { font-size: 12px; color: #8b90a0; }
.modal-tip {
  background: rgba(91, 127, 255, 0.08);
  border: 1px solid rgba(91, 127, 255, 0.2);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #8b90a0;
  margin-top: 16px;
}
</style>
