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
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
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
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getFavorites,
  unfavoriteTemplate,
  useTemplate as useTemplateApi,
  type Template
} from '@/api/marketplace'

const router = useRouter()
const PAGE_SIZE = 12
const userId = computed(() => localStorage.getItem('bf_user_id') || '1')

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
.favorites-page { min-height: 100vh; background: #F9FAFB; }

.fav-topbar {
  background: #fff;
  border-bottom: 1px solid #E5E7EB;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-link { color: #6B7280; font-size: 14px; text-decoration: none; }
.back-link:hover { color: #4F46E5; }

.page-title { font-size: 18px; font-weight: 600; color: #111827; }

.page-body { max-width: 1100px; margin: 0 auto; padding: 24px; }

.loading-state { text-align: center; padding: 80px 0; color: #9CA3AF; }

.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 18px; font-weight: 600; color: #111827; margin-bottom: 8px; }
.empty-sub { font-size: 14px; color: #9CA3AF; margin-bottom: 24px; }

.fav-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1024px) { .fav-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 768px) { .fav-grid { grid-template-columns: repeat(2, 1fr); } }

.fav-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #E5E7EB;
  transition: box-shadow 0.2s;
}

.fav-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #F3F4F6;
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
  color: #D1D5DB;
}

.card-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.fav-card:hover .card-overlay { opacity: 1; }

.card-body { padding: 12px; }

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #6B7280;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.dot { color: #D1D5DB; }

.card-actions {
  display: flex;
  gap: 6px;
  align-items: center;
}

.btn-use-card {
  flex: 1;
  background: #4F46E5;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 8px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-use-card:hover { background: #4338CA; }

.btn-unfav {
  background: transparent;
  color: #9CA3AF;
  border: none;
  font-size: 12px;
  cursor: pointer;
  padding: 4px;
  transition: color 0.2s;
}

.btn-unfav:hover { color: #EF4444; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.page-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
}

.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-btn:not(:disabled):hover { border-color: #4F46E5; color: #4F46E5; }

.page-info { font-size: 14px; color: #6B7280; }

/* Buttons */
.btn-primary {
  background: #4F46E5;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
}

.btn-outline {
  background: #fff;
  color: #4F46E5;
  border: 1px solid #4F46E5;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
}

.btn-sm { font-size: 13px; padding: 6px 12px; }

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
  background: #fff;
  border-radius: 16px;
  width: 420px;
  max-width: 90vw;
}

.dialog-title {
  padding: 20px 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.dialog-body {
  padding: 16px 24px;
  font-size: 14px;
  color: #6B7280;
}

.form-group { margin-top: 16px; }
.form-group label { display: block; font-size: 14px; color: #374151; margin-bottom: 6px; }

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus { border-color: #4F46E5; }

.dialog-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #F3F4F6;
}
</style>
