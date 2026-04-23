<template>
  <div class="page-body">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="favorites.length === 0" class="empty-state">
        <div class="empty-icon">💔</div>
        <div class="empty-title">还没有收藏任何模板</div>
        <div class="empty-sub">去逛逛模板市场，发现更多优质模板吧～</div>
        <router-link to="/marketplace" class="btn-primary">去逛逛 →</router-link>
      </div>
      <div v-else class="fav-grid">
        <div
          v-for="item in favorites"
          :key="item.templateId || item.id"
          class="fav-card"
        >
          <div class="card-cover" @click="goDetail(item)">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" loading="lazy" />
            <div v-else class="cover-placeholder">📋</div>
            <div class="card-overlay">
              <button class="btn-primary btn-sm">查看详情</button>
            </div>
          </div>
          <div class="card-body">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-meta">
              <span>⭐ {{ item.avgRating?.toFixed(1) || '0.0' }} ({{ item.ratingCount || 0 }})</span>
              <span class="dot">·</span>
              <span>使用 {{ item.useCount || 0 }} 次</span>
            </div>
            <div class="card-actions">
              <button class="btn-use-card" @click="useTemplate(item)">立即使用</button>
              <button class="btn-unfav" @click="handleUnfavorite(item)">取消收藏</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="pagination">
        <button class="page-btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
        <span class="page-info">第 {{ page }} / {{ totalPages }} 页</span>
        <button class="page-btn" :disabled="page >= totalPages" @click="changePage(page + 1)">下一页</button>
      </div>
    </div>

    <!-- Use Dialog -->
    <div v-if="useDialogVisible" class="dialog-overlay" @click.self="useDialogVisible = false">
      <div class="dialog">
        <div class="dialog-title">一键使用模板</div>
        <div class="dialog-body">
          <p>模板 "<strong>{{ selectedTemplate?.title }}</strong>" 将被复制为您的 SOP 草稿</p>
          <div class="form-group">
            <label>SOP 名称</label>
            <input v-model="useForm.sop_name" class="form-input" :placeholder="selectedTemplate?.title" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-outline" @click="useDialogVisible = false">取消</button>
          <button class="btn-primary" :disabled="using" @click="confirmUse">
            {{ using ? '复制中...' : '确认使用' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">


/**
 * PC 端收藏页
 * - 收藏的 SOP 模板列表
 * - 取消收藏
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getFavorites,
  unfavoriteTemplate,
  useTemplate as useTemplateApi,
  type Template
} from '@/api/marketplace'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const PAGE_SIZE = 12
const userId = computed(() => authStore.getUserId())

const favorites = ref<Template[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE) || 1)

const useDialogVisible = ref(false)
const using = ref(false)
const selectedTemplate = ref<Template | null>(null)
const useForm = ref({ sop_name: '' })

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavorites({ user_id: userId.value, page: page.value, page_size: PAGE_SIZE })
    if (res.success) {
      favorites.value = res.data?.templates || []
      total.value = res.data?.total || 0
    }
  } catch (e) {
    ElMessage.error('加载收藏列表失败')
  } finally {
    loading.value = false
  }
}

const goDetail = (item: Template) => {
  const id = item.templateId || item.id
  router.push(`/marketplace/${id}`)
}

const handleUnfavorite = async (item: Template) => {
  try {
    await unfavoriteTemplate(item.templateId || String(item.id), { user_id: userId.value })
    ElMessage.success('已取消收藏')
    fetchFavorites()
  } catch (e) {
    ElMessage.error('取消收藏失败')
  }
}

const useTemplate = (item: Template) => {
  selectedTemplate.value = item
  useForm.value.sop_name = item.title
  useDialogVisible.value = true
}

const confirmUse = async () => {
  if (!selectedTemplate.value) return
  using.value = true
  try {
    const res = await useTemplateApi(selectedTemplate.value.templateId || String(selectedTemplate.value.id), {
      user_id: userId.value,
      sop_name: useForm.value.sop_name || selectedTemplate.value.title,
    })
    if (res.success) {
      ElMessage.success('模板已复制到您的草稿')
      useDialogVisible.value = false
      router.push('/execution')
    } else {
      ElMessage.error(res.message || '使用失败')
    }
  } catch (e) {
    ElMessage.error('使用模板失败')
  } finally {
    using.value = false
  }
}

const changePage = (p: number) => {
  page.value = p
  fetchFavorites()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page { min-height: 100vh; background: var(--color-bg-light); }

.fav-topbar {
  background: var(--color-bg-light-elevated);
  border-bottom: 1px solid var(--color-border-light);
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

.back-link { color: var(--color-text-light-secondary); font-size: var(--font-size-base); text-decoration: none; }
.back-link:hover { color: var(--color-primary); }

.page-title { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-light-primary); }

.page-body { max-width: 1100px; margin: 0 auto; padding: var(--space-xl); }

.loading-state { text-align: center; padding: 80px 0; color: var(--color-text-light-muted); }

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-light-primary); margin-bottom: 8px; }
.empty-sub { font-size: var(--font-size-base); color: var(--color-text-light-muted); margin-bottom: var(--space-xl); }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-lg);
}

@media (max-width: 1024px) { .fav-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .fav-grid { grid-template-columns: repeat(2, 1fr); } }

.fav-card {
  background: var(--color-bg-light-elevated);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid var(--color-border-light);
  transition: var(--transition-normal);
}

.fav-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: var(--color-bg-light);
  cursor: pointer;
}

.card-cover img { width: 100%; height: 100%; object-fit: cover; }

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: var(--color-text-light-muted);
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: var(--transition-normal);
}

.fav-card:hover .card-overlay { opacity: 1; }

.card-body { padding: 12px; }

.card-title {
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--color-text-light-primary);
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: var(--font-size-xs);
  color: var(--color-text-light-secondary);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot { color: var(--color-text-light-muted); }

.card-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.btn-use-card {
  flex: 1;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-sm);
  padding: 6px 8px;
  font-size: var(--font-size-xs);
  cursor: pointer;
  transition: var(--transition-normal);
}

.btn-use-card:hover { background: #7994FF; }

.btn-unfav {
  background: transparent;
  color: var(--color-text-light-muted);
  border: none;
  font-size: var(--font-size-xs);
  cursor: pointer;
  padding: 4px;
  transition: var(--transition-normal);
}

.btn-unfav:hover { color: var(--color-error); }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-lg);
  margin-top: var(--space-2xl);
}

.page-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
  background: var(--color-bg-light-elevated);
  font-size: var(--font-size-base);
  cursor: pointer;
}

.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-btn:not(:disabled):hover { border-color: var(--color-primary); color: var(--color-primary); }

.page-info { font-size: var(--font-size-base); color: var(--color-text-light-secondary); }

/* Buttons */
.btn-primary {
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: 8px 16px;
  font-size: var(--font-size-base);
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
}

.btn-outline {
  background: var(--color-bg-light-elevated);
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  padding: 8px 16px;
  font-size: var(--font-size-base);
  cursor: pointer;
}

.btn-sm { font-size: var(--font-size-sm); padding: 6px 12px; }

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: var(--color-bg-light-elevated);
  border-radius: var(--radius-xl);
  width: 420px;
  max-width: 90vw;
}

.dialog-title {
  padding: var(--space-xl) 24px 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-light-primary);
}

.dialog-body {
  padding: 16px 24px;
  font-size: var(--font-size-base);
  color: var(--color-text-light-secondary);
}

.form-group { margin-top: var(--space-lg); }
.form-group label { display: block; font-size: var(--font-size-base); color: var(--color-text-light-primary); margin-bottom: 6px; }

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  outline: none;
  box-sizing: border-box;
}

.form-input:focus { border-color: var(--color-primary); }

.dialog-footer {
  padding: var(--space-lg) 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid var(--color-bg-light);
}
</style>
