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
.badge-wall { padding: var(--space-xs) 0; }
.section-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-lg);
  display: flex;
  align-items: center;
  gap: 10px;
}
.badge-count-chip {
  background: var(--color-primary-subtle);
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  padding: 2px 8px;
  border-radius: 10px;
}
.locked-title { margin-top: var(--space-2xl); }
.toggle-btn {
  background: none;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  padding: 2px 10px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-normal);
}
.toggle-btn:hover { background: var(--color-bg-elevated); color: var(--color-text-primary); }
.badge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 14px;
}
.empty-badges {
  text-align: center;
  padding: var(--space-2xl);
  color: var(--color-text-secondary);
}
.empty-badges span { font-size: 40px; display: block; margin-bottom: var(--space-sm); }
.empty-badges p { font-size: var(--font-size-base); }
</style>
