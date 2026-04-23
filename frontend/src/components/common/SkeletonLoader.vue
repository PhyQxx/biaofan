<template>
  <div class="skeleton-wrapper" :class="variant">
    <template v-if="variant === 'card'">
      <div class="skeleton-card" v-for="i in count" :key="i">
        <el-skeleton variant="text" width="60%" height="20px" style="margin-bottom: 10px" />
        <el-skeleton variant="text" width="40%" height="16px" style="margin-bottom: 10px" />
        <el-skeleton variant="text" width="30%" height="16px" />
      </div>
    </template>
    <template v-else-if="variant === 'list'">
      <div class="skeleton-list" v-for="i in count" :key="i">
        <el-skeleton
          variant="text"
          :width="i % 2 === 0 ? '40%' : '60%'"
          height="16px"
          style="margin-bottom: 8px"
        />
      </div>
    </template>
    <template v-else>
      <el-skeleton :rows="rows" animated />
    </template>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    variant?: 'card' | 'list' | 'default'
    count?: number
    rows?: number
  }>(),
  {
    variant: 'default',
    count: 3,
    rows: 4,
  }
)
</script>

<style scoped>
.skeleton-wrapper {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.skeleton-card {
  background: var(--color-bg-light-elevated);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}

.skeleton-list {
  padding: var(--space-sm) 0;
}
</style>
