<template>
  <div class="store-card" :class="[`rarity-${product.owned ? 'owned' : 'available'}`]">
    <div class="store-icon-wrap">
      <span class="store-icon">{{ product.icon }}</span>
      <span class="category-badge">{{ categoryLabel }}</span>
    </div>
    <div class="store-name">{{ product.name }}</div>
    <div class="store-price">
      <span class="price-num">{{ product.price }}</span>
      <span class="price-unit">积分</span>
    </div>
    <div class="store-duration">
      {{ product.isPermanent ? '永久有效' : product.durationDays ? `${product.durationDays}天` : '' }}
    </div>
    <div class="store-stock" v-if="product.stock >= 0">库存 {{ product.stock }}</div>
    <button
      class="store-btn"
      :class="{
        'btn-disabled': !canRedeem,
        'btn-owned': product.owned && !product.equipped,
        'btn-equipped': product.equipped,
      }"
      :disabled="!canRedeem && !product.owned"
      @click="handleRedeem"
    >
      {{ btnText }}
    </button>
  </div>
</template>

<script setup lang="ts">


/**
 * 积分商城商品卡片
 * - 商品图、名称、所需积分
 * - 兑换按钮
 */
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { StoreProduct } from '@/api/gamification'

const props = defineProps<{
  product: StoreProduct
  userScore: number
}>()
const emit = defineEmits<{ redeem: [product: StoreProduct] }>()

const categoryLabel = computed(() => ({
  title: '称号', avatar_frame: '头像框', background: '背景', emotion: '表情',
}[props.product.category] || ''))

const canRedeem = computed(() => props.userScore >= props.product.price && !props.product.owned && props.product.stock > 0)

const btnText = computed(() => {
  if (props.product.equipped) return '已装备 ✓'
  if (props.product.owned) return '已拥有'
  if (props.userScore < props.product.price) return '积分不足'
  if (props.product.stock === 0) return '已售罄'
  return '兑换'
})

function handleRedeem() {
  if (!canRedeem.value && !props.product.owned) {
    if (props.userScore < props.product.price) {
      ElMessage.warning(`积分不足，还需 ${props.product.price - props.userScore} 积分`)
    }
    return
  }
  if (!props.product.owned) {
    emit('redeem', props.product)
  }
}
</script>

<style scoped>
.store-card {
  width: 160px;
  background: var(--color-bg-elevated);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 18px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all var(--transition-normal);
}
.store-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
  background: var(--color-bg-surface);
}
.store-card.rarity-available {
  border-color: var(--color-border);
}
.store-icon-wrap {
  position: relative;
  margin-bottom: 10px;
}
.store-icon {
  font-size: 40px;
  display: block;
}
.category-badge {
  position: absolute;
  bottom: -4px;
  right: -8px;
  background: var(--color-primary);
  color: white;
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 6px;
}
.store-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: center;
  margin-bottom: 6px;
  line-height: 1.3;
}
.store-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  margin-bottom: 2px;
}
.price-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
  font-family: 'DIN Alternate', monospace;
}
.price-unit { font-size: 11px; color: var(--color-text-secondary); }
.store-duration {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}
.store-stock {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-bottom: 10px;
}
.store-btn {
  width: 100%;
  padding: 8px;
  border-radius: var(--radius-md);
  border: none;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-normal);
  background: var(--color-primary);
  color: white;
}
.store-btn:hover:not(:disabled) { background: #4a6fee; }
.store-btn:active:not(:disabled) { transform: scale(0.96); }
.btn-disabled {
  background: transparent !important;
  border: 1px solid #ff6b6b;
  color: #ff6b6b;
  cursor: not-allowed;
}
.btn-owned {
  background: var(--color-border) !important;
  color: var(--color-text-secondary);
  cursor: default;
}
.btn-equipped {
  background: rgba(74, 222, 128, 0.15) !important;
  color: #4ade80;
  cursor: default;
}
</style>
