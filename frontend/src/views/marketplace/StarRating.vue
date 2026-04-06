<template>
  <div class="star-rating" :class="'size-' + size">
    <span
      v-for="i in max"
      :key="i"
      class="star"
      :class="getStarClass(i)"
      @click="!readonly && emit('update:value', i)"
    >★</span>
  </div>
</template>

<script setup lang="ts">


const props = withDefaults(defineProps<{
  value?: number
  max?: number
  readonly?: boolean
  size?: string
}>(), {
  value: 0,
  max: 5,
  readonly: true,
  size: 'md',
})

const emit = defineEmits<{
  'update:value': [value: number]
}>()

const getStarClass = (i: number) => {
  const val = props.value || 0
  if (val >= i) return 'full'
  if (val >= i - 0.5) return 'half'
  return 'empty'
}
</script>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 1px;
}

.star {
  color: #D1D5DB;
  transition: color 0.1s;
}

.size-sm .star { font-size: 12px; }
.size-md .star { font-size: 16px; }
.size-lg .star { font-size: 24px; }

.star.full { color: #F59E0B; }
.star.half {
  background: linear-gradient(90deg, #F59E0B 50%, #D1D5DB 50%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.star:not(.empty):not(.full):hover,
.star.half:hover {
  cursor: pointer;
}
</style>
