<template>
  <span class="status-badge" :class="'status-' + status">{{ label }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const statusMap: Record<string, { label: string }> = {
  published: { label: '已发布' },
  pending: { label: '待审核' },
  approved: { label: '已通过' },
  rejected: { label: '已驳回' },
  offline: { label: '已下架' },
}

const label = computed(() => statusMap[props.status]?.label || props.status)
</script>

<style scoped>
.status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-published { background: rgba(239,68,68,0.15); color: #EF4444; }
.status-pending { background: rgba(245,158,11,0.15); color: #F59E0B; }
.status-approved { background: rgba(16,185,129,0.15); color: #10B981; }
.status-rejected { background: rgba(239,68,68,0.15); color: #EF4444; text-decoration: line-through; }
.status-offline { background: rgba(107,114,128,0.15); color: #6B7280; }
</style>
