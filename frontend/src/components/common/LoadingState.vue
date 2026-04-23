<template>
  <div class="loading-state" :class="{ 'loading-inline': inline }">
    <div class="loading-spinner" v-if="!inline"></div>
    <span>{{ text }}</span>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    text?: string
    inline?: boolean
  }>(),
  {
    text: '加载中...',
    inline: false,
  }
)
</script>

<style scoped>
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  padding: 40px 0;
  color: var(--color-text-secondary);
  text-align: center;
}

.loading-inline {
  padding: 0;
  flex-direction: row;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.light-theme .loading-state {
  color: var(--color-text-light-muted);
}

.light-theme .loading-spinner {
  border-color: var(--color-border-light);
  border-top-color: var(--color-primary);
}
</style>
