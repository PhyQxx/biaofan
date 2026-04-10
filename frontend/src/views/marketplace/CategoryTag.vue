<template>
  <span class="category-tag" :class="'cat-' + category">{{ icon }} {{ label }}</span>
</template>

<script setup lang="ts">


/**
 * 分类标签组件
 * - 显示 SOP 分类名称标签
 */
import { computed } from 'vue'

const props = defineProps<{ category: string }>()

const catMap: Record<string, { label: string; icon: string }> = {
  work: { label: '工作类', icon: '💼' },
  life: { label: '生活类', icon: '🏠' },
  study: { label: '学习类', icon: '📚' },
  other: { label: '其他', icon: '✨' },
}

const info = computed(() => catMap[props.category] || { label: props.category, icon: '📁' })
const label = computed(() => info.value.label)
const icon = computed(() => info.value.icon)
</script>

<style scoped>
.category-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.cat-work { background: #DBEAFE; color: #1D4ED8; }
.cat-life { background: #D1FAE5; color: #065F46; }
.cat-study { background: #EDE9FE; color: #5B21B6; }
.cat-other { background: #F3F4F6; color: #374151; }
</style>
