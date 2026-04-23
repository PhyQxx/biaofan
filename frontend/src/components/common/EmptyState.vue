<template>
  <div class="empty-state">
    <div class="empty-icon">{{ icon }}</div>
    <div class="empty-text">{{ message }}</div>
    <div class="empty-sub" v-if="sub">{{ sub }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    icon?: string
    message?: string
    sub?: string
  }>(),
  {
    icon: '📋',
    message: '暂无数据',
    sub: '',
  }
)
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: var(--space-lg);
  opacity: 0.8;
}

.empty-text {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
}

.empty-sub {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.light-theme .empty-text {
  color: var(--color-text-light-primary);
}

.light-theme .empty-sub {
  color: var(--color-text-light-muted);
}
</style>
