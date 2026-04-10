<template>
  <div class="badge-wall">
    <div class="section-title">
      <span>已获得徽章</span>
      <span class="badge-count-chip">{{ unlockedBadges.length }}/{{ badges.length }}</span>
    </div>
    <div class="badge-grid" v-if="unlockedBadges.length">
      <BadgeCard
        v-for="badge in unlockedBadges"
        :key="badge.id"
        :badge="badge"
        @click="$emit('badge-click', badge)"
      />
    </div>
    <div class="empty-badges" v-else>
      <span>🏅</span>
      <p>还没有徽章，继续加油！</p>
    </div>

    <div class="section-title locked-title" v-if="lockedBadges.length">
      <span>未解锁徽章</span>
      <button class="toggle-btn" @click="showLocked = !showLocked">
        {{ showLocked ? '收起' : `展开 ${lockedBadges.length} 个` }}
      </button>
    </div>
    <div class="badge-grid" v-if="showLocked && lockedBadges.length">
      <BadgeCard
        v-for="badge in lockedBadges"
        :key="badge.id"
        :badge="badge"
        @click="$emit('badge-click', badge)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">


/**
 * 徽章墙组件
 * - 网格展示用户已获得的全部徽章
 * - 点击打开 BadgeModal
 */
import { ref, computed } from 'vue'
import BadgeCard from './BadgeCard.vue'
import type { Badge } from '@/api/gamification'

const props = defineProps<{ badges: Badge[] }>()
defineEmits<{ 'badge-click': [badge: Badge] }>()

const showLocked = ref(false)
const unlockedBadges = computed(() => props.badges.filter((b) => b.unlockedAt))
const lockedBadges = computed(() => props.badges.filter((b) => !b.unlockedAt))
</script>

<style scoped>
.badge-wall { padding: 4px 0; }
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #e8eaf0;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.badge-count-chip {
  background: rgba(91, 127, 255, 0.15);
  color: #5b7fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}
.locked-title { margin-top: 24px; }
.toggle-btn {
  background: none;
  border: 1px solid #2d3348;
  color: #8b90a0;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.toggle-btn:hover { background: #1a1d27; color: #e8eaf0; }
.badge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 14px;
}
.empty-badges {
  text-align: center;
  padding: 32px;
  color: #8b90a0;
}
.empty-badges span { font-size: 40px; display: block; margin-bottom: 8px; }
.empty-badges p { font-size: 14px; }
</style>
