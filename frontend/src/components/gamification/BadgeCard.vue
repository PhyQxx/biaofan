<template>
  <div
    class="badge-card"
    :class="{
      'badge-unlocked': badge.unlockedAt,
      'badge-locked': !badge.unlockedAt,
      [`rarity-${badge.rarity}`]: badge.unlockedAt,
    }"
    @click="$emit('click')"
  >
    <div class="badge-icon-wrap">
      <span class="badge-icon">{{ badge.unlockedAt ? badge.icon : '❓' }}</span>
    </div>
    <div class="badge-name">{{ badge.name }}</div>
    <div class="badge-date" v-if="badge.unlockedAt">{{ formatDate(badge.unlockedAt) }}</div>
    <div class="badge-date" v-else-if="badge.progress !== undefined">
      {{ badge.progress }}/{{ badge.target }}
    </div>
    <div class="badge-progress-bar" v-if="!badge.unlockedAt && badge.progress !== undefined">
      <div
        class="badge-progress-fill"
        :style="{ width: `${((badge.progress || 0) / (badge.target || 1)) * 100}%` }"
      ></div>
    </div>
    <div class="badge-rarity-line" :class="`rarity-line-${badge.rarity}`"></div>
  </div>
</template>

<script setup lang="ts">


/**
 * 徽章卡片组件
 * - 展示单个徽章（图标、名称、描述、是否已获得）
 * - 已获得 vs 未获得（灰色）样式区分
 */
import type { Badge } from '@/api/gamification'

defineProps<{ badge: Badge }>()
defineEmits<{ click: [] }>()

function formatDate(dateStr?: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}-${d.getDate()}`
}
</script>

<style scoped>
.badge-card {
  width: 120px;
  height: 160px;
  background: #1a1d27;
  border-radius: 12px;
  padding: 14px 10px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  border: 2px solid transparent;
}
.badge-card:hover {
  transform: translateY(-4px);
}
.badge-unlocked.rarity-bronze {
  border-color: #cd7f32;
}
.badge-unlocked.rarity-silver {
  border-color: #c0c0c0;
  box-shadow: 0 0 12px rgba(192, 192, 192, 0.3);
}
.badge-unlocked.rarity-gold {
  border-color: #ffd700;
  box-shadow: 0 0 16px rgba(255, 215, 0, 0.4);
}
.badge-unlocked:hover {
  box-shadow: 0 8px 24px rgba(91, 127, 255, 0.3);
}
.badge-locked {
  border: 2px dashed #2d3348;
  opacity: 0.7;
}
.badge-locked:hover {
  opacity: 0.9;
}
.badge-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}
.badge-unlocked.rarity-bronze .badge-icon-wrap { background: rgba(205, 127, 50, 0.15); }
.badge-unlocked.rarity-silver .badge-icon-wrap { background: rgba(192, 192, 192, 0.15); }
.badge-unlocked.rarity-gold .badge-icon-wrap { background: rgba(255, 215, 0, 0.15); }
.badge-locked .badge-icon-wrap { background: rgba(45, 51, 72, 0.5); }
.badge-icon {
  font-size: 28px;
  line-height: 1;
}
.badge-name {
  font-size: 12px;
  color: #e8eaf0;
  text-align: center;
  font-weight: 600;
  line-height: 1.3;
  margin-bottom: 4px;
}
.badge-locked .badge-name {
  color: #8b90a0;
}
.badge-date {
  font-size: 11px;
  color: #8b90a0;
  margin-bottom: 4px;
}
.badge-progress-bar {
  width: 80%;
  height: 4px;
  background: #2d3348;
  border-radius: 2px;
  margin-bottom: 4px;
  overflow: hidden;
}
.badge-progress-fill {
  height: 100%;
  background: #5b7fff;
  border-radius: 2px;
  transition: width 0.3s ease;
}
.badge-rarity-line {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
}
.rarity-line-bronze { background: #cd7f32; }
.rarity-line-silver { background: linear-gradient(90deg, #c0c0c0, #e8e8e8); }
.rarity-line-gold { background: linear-gradient(90deg, #ffa500, #ffd700); }
</style>
