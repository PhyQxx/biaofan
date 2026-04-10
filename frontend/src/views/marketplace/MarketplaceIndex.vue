<template>
  <div class="page-body">
      <!-- Search Bar -->
      <div class="search-bar">
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索模板名称或描述..."
          @keyup.enter="handleSearch"
        />
        <button class="btn-primary search-btn" @click="handleSearch">搜索</button>
      </div>

      <!-- Category Tabs -->
      <div class="category-tabs">
        <button
          v-for="cat in categories"
          :key="cat.value"
          class="category-tab"
          :class="{ active: currentCategory === cat.value }"
          @click="selectCategory(cat.value)"
        >
          {{ cat.icon }} {{ cat.label }}
        </button>
      </div>

      <!-- Sort Bar -->
      <div class="sort-bar">
        <span class="sort-label">排序：</span>
        <button
          v-for="s in sortOptions"
          :key="s.value"
          class="sort-btn"
          :class="{ active: currentSort === s.value }"
          @click="selectSort(s.value)"
        >
          {{ s.label }}
        </button>
        <span class="total-count">共 {{ total }} 个模板</span>
      </div>

      <!-- Featured Templates (if page 1) -->
      <div v-if="page === 1 && featuredTemplates.length > 0" class="featured-section">
        <div class="section-title">🔥 热门推荐</div>
        <div class="featured-list">
          <div
            v-for="tpl in featuredTemplates"
            :key="tpl.templateId || tpl.id"
            class="featured-card"
            @click="goDetail(tpl)"
          >
            <div class="featured-cover">
              <img v-if="tpl.coverUrl" :src="tpl.coverUrl" :alt="tpl.title" />
              <div v-else class="cover-placeholder">📋</div>
            </div>
            <div class="featured-info">
              <div class="featured-title">{{ tpl.title }}</div>
              <div class="featured-meta">
                ⭐ {{ tpl.avgRating?.toFixed(1) || '0.0' }} ({{ tpl.ratingCount || 0 }})
                <span class="dot">·</span>
                使用 {{ tpl.useCount || 0 }} 次
              </div>
              <div class="featured-sub">
                <CategoryTag :category="tpl.category" />
                <span>{{ tpl.stepCount || 0 }}步骤</span>
                <span class="dot">·</span>
                <span>by {{ tpl.authorName }}</span>
              </div>
            </div>
            <button class="btn-primary btn-sm" @click.stop="useTemplate(tpl)">立即使用 ▸</button>
          </div>
        </div>
      </div>

      <!-- Template Grid -->
      <div class="template-section">
        <div class="section-title">📋 全部模板</div>
        <div v-if="loading" class="loading-state">加载中...</div>
        <div v-else-if="templates.length === 0" class="empty-state">
          <div class="empty-icon">🔍</div>
          <div>未找到相关模板，试试其他关键词？</div>
        </div>
        <div v-else class="template-grid">
          <div
            v-for="tpl in templates"
            :key="tpl.templateId || tpl.id"
            class="template-card"
            @click="goDetail(tpl)"
          >
            <div class="card-cover">
              <img v-if="tpl.coverUrl" :src="tpl.coverUrl" :alt="tpl.title" />
              <div v-else class="cover-placeholder">📋</div>
              <div class="card-overlay">
                <button class="btn-primary btn-sm">查看详情</button>
              </div>
            </div>
            <div class="card-body">
              <div class="card-title">{{ tpl.title }}</div>
              <div class="card-rating">
                <StarRating :value="tpl.avgRating || 0" readonly size="sm" />
                <span class="rating-text">⭐ {{ tpl.avgRating?.toFixed(1) || '0.0' }} ({{ tpl.ratingCount || 0 }})</span>
              </div>
              <div class="card-meta">
                <CategoryTag :category="tpl.category" />
                <span class="use-count">使用 {{ tpl.useCount || 0 }} 次</span>
              </div>
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

    <!-- Use Template Dialog -->
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
import { getTemplates, useTemplate as useTemplateApi, type Template } from '@/api/marketplace'
import { useAuthStore } from '@/stores/auth'
import StarRating from './StarRating.vue'
import CategoryTag from './CategoryTag.vue'

const router = useRouter()
const authStore = useAuthStore()

const PAGE_SIZE = 20

const categories = [
  { value: '', label: '全部', icon: '📁' },
  { value: 'work', label: '工作类', icon: '💼' },
  { value: 'life', label: '生活类', icon: '🏠' },
  { value: 'study', label: '学习类', icon: '📚' },
  { value: 'other', label: '其他', icon: '✨' },
]

const sortOptions = [
  { value: 'recent', label: '最新' },
  { value: 'popular', label: '最热' },
  { value: 'rated', label: '评分最高' },
]

const templates = ref<Template[]>([])
const featuredTemplates = ref<Template[]>([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const keyword = ref('')
const currentCategory = ref('')
const currentSort = ref('recent')

// Use dialog
const useDialogVisible = ref(false)
const using = ref(false)
const selectedTemplate = ref<Template | null>(null)
const useForm = ref({ sop_name: '' })

const totalPages = computed(() => Math.ceil(total.value / PAGE_SIZE) || 1)

const fetchTemplates = async () => {
  loading.value = true
  try {
    const res = await getTemplates({
      category: currentCategory.value,
      keyword: keyword.value,
      sort: currentSort.value,
      page: page.value,
      page_size: PAGE_SIZE,
    })
    if (res.success) {
      templates.value = res.data?.templates || []
      total.value = res.data?.total || 0
      // featured = first 3 of approved
      if (page.value === 1 && currentCategory.value === '' && keyword.value === '') {
        featuredTemplates.value = templates.value.slice(0, 3)
      } else {
        featuredTemplates.value = []
      }
    }
  } catch (e) {
    ElMessage.error('加载模板列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchTemplates()
}

const selectCategory = (val: string) => {
  currentCategory.value = val
  page.value = 1
  fetchTemplates()
}

const selectSort = (val: string) => {
  currentSort.value = val
  page.value = 1
  fetchTemplates()
}

const changePage = (p: number) => {
  page.value = p
  fetchTemplates()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goDetail = (tpl: Template) => {
  const id = tpl.templateId || tpl.id
  router.push(`/marketplace/${id}`)
}

const useTemplate = (tpl: Template) => {
  selectedTemplate.value = tpl
  useForm.value.sop_name = tpl.title
  useDialogVisible.value = true
}

const confirmUse = async () => {
  if (!selectedTemplate.value) return
  const userId = authStore.requireUserId()
  if (!userId) {
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  using.value = true
  try {
    const res = await useTemplateApi(selectedTemplate.value.templateId || String(selectedTemplate.value.id), {
      user_id: userId,
      sop_name: useForm.value.sop_name || selectedTemplate.value.title,
    })
    if (res.success) {
      ElMessage.success('模板已复制到您的草稿，请在执行台查看')
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

onMounted(() => {
  fetchTemplates()
})
</script>

<style scoped>
.marketplace-page {
  min-height: 100vh;
  background: #F9FAFB;
}

.marketplace-topbar {
  background: #fff;
  border-bottom: 1px solid #E5E7EB;
  padding: 0 24px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-link {
  color: #6B7280;
  font-size: 14px;
  text-decoration: none;
}

.back-link:hover {
  color: #4F46E5;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.search-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
}

.search-input:focus {
  border-color: #4F46E5;
}

.search-btn {
  padding: 10px 20px;
}

.category-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 6px 16px;
  border-radius: 20px;
  border: 1px solid #E5E7EB;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #374151;
}

.category-tab:hover {
  border-color: #4F46E5;
  color: #4F46E5;
}

.category-tab.active {
  background: #4F46E5;
  color: #fff;
  border-color: #4F46E5;
}

.sort-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.sort-label {
  font-size: 14px;
  color: #6B7280;
}

.sort-btn {
  padding: 4px 12px;
  border-radius: 4px;
  border: none;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  color: #6B7280;
}

.sort-btn.active {
  background: #EEF2FF;
  color: #4F46E5;
  font-weight: 500;
}

.total-count {
  margin-left: auto;
  font-size: 13px;
  color: #9CA3AF;
}

.featured-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.featured-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.featured-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  border: 1px solid #E5E7EB;
  transition: box-shadow 0.2s;
}

.featured-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.featured-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
}

.featured-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.featured-info {
  flex: 1;
}

.featured-title {
  font-size: 16px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
}

.featured-meta {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 4px;
}

.featured-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #9CA3AF;
}

.featured-card .btn-sm {
  font-size: 13px;
  padding: 6px 12px;
  white-space: nowrap;
}

.template-section {
  margin-bottom: 32px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 0;
  color: #9CA3AF;
  font-size: 14px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

@media (max-width: 1024px) {
  .template-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .template-grid { grid-template-columns: repeat(2, 1fr); }
}

.template-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #E5E7EB;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.template-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
}

.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #F3F4F6;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

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

.template-card:hover .card-overlay {
  opacity: 1;
}

.card-body {
  padding: 12px;
}

.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 6px;
}

.rating-text {
  font-size: 12px;
  color: #6B7280;
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.use-count {
  font-size: 12px;
  color: #9CA3AF;
}

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
  transition: all 0.2s;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-btn:not(:disabled):hover {
  border-color: #4F46E5;
  color: #4F46E5;
}

.page-info {
  font-size: 14px;
  color: #6B7280;
}

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
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #4338CA;
}

.btn-outline {
  background: #fff;
  color: #4F46E5;
  border: 1px solid #4F46E5;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  transition: all 0.2s;
}

.btn-outline:hover {
  background: #EEF2FF;
}

.btn-sm {
  font-size: 13px;
  padding: 6px 12px;
}

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
  overflow: hidden;
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

.form-group {
  margin-top: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #374151;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #4F46E5;
}

.dialog-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #F3F4F6;
}

.dot {
  color: #D1D5DB;
  margin: 0 2px;
}
</style>
