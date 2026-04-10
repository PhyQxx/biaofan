<template>
  <div class="history-item" :class="item.change > 0 ? 'history-income' : 'history-expense'">
    <div class="history-indicator"></div>
    <div class="history-icon">{{ icon }}</div>
    <div class="history-body">
      <div class="history-desc">{{ item.description }}</div>
      <div class="history-meta">
        <span class="history-date">{{ formatDate(item.createdAt) }}</span>
        <span class="history-balance">余额：{{ item.balance }}</span>
      </div>
    </div>
    <div class="history-change" :class="item.change > 0 ? 'income' : 'expense'">
      {{ item.change > 0 ? '+' : '' }}{{ item.change }}
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 积分历史单条记录
 * - 行为描述、积分增减、时间
 */
import { computed } from 'vue'
import type { ScoreHistory } from '@/api/gamification'

const props = defineProps<{ item: ScoreHistory }>()

const icon = computed(() => ({
  execution: '📋', redeem: '🎁', badge: '🏅', streak: '🔥', level_up: '⬆️',
}[props.item.type] || '💰'))

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}
</script>

<style scoped>
.history-item {
  display: flex;
  align-items: center;
  gap: 14px;
  background: #1a1d27;
  border-radius: 10px;
  padding: 14px 16px;
  border: 1px solid #2d3348;
  margin-bottom: 10px;
  position: relative;
  overflow: hidden;
}
.history-indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
}
.history-income .history-indicator { background: #4ade80; }
.history-expense .history-indicator { background: #ff6b6b; }
.history-icon {
  font-size: 24px;
  flex-shrink: 0;
}
.history-body { flex: 1; }
.history-desc {
  font-size: 14px;
  color: #e8eaf0;
  margin-bottom: 4px;
}
.history-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #8b90a0;
}
.history-change {
  font-size: 16px;
  font-weight: 700;
  font-family: 'DIN Alternate', monospace;
  flex-shrink: 0;
}
.change.income { color: #4ade80; }
.change.expense { color: #ff6b6b; }
</style>
