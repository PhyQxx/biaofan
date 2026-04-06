<template>
  <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="!template" class="empty-state">模板不存在</div>
    <div v-else class="page-body">
      <!-- Cover Image -->
      <div class="cover-wrapper">
        <img v-if="template.coverUrl" :src="template.coverUrl" :alt="template.title" class="cover-img" />
        <div v-else class="cover-placeholder">📋</div>
      </div>

      <!-- Info Header -->
      <div class="info-header">
        <h1 class="template-title">{{ template.title }}</h1>
        <div class="info-meta">
          <span>⭐ {{ template.avgRating?.toFixed(1) || '0.0' }} ({{ template.ratingCount || 0 }}条评价)</span>
          <span class="dot">·</span>
          <span>使用 {{ template.useCount || 0 }} 次</span>
          <span class="dot">·</span>
          <span>发布于 {{ formatDate(template.createdAt) }}</span>
          <span class="dot">·</span>
          <span>发布者：{{ template.authorName }}</span>
        </div>
        <div class="info-actions">
          <button
            class="btn-favorite"
            :class="{ favorited: isFavorited }"
            @click="toggleFavorite"
          >
            {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
          </button>
          <button class="btn-use" @click="openUseDialog">➕ 一键使用模板</button>
        </div>
      </div>

      <!-- Steps Preview -->
      <div class="section">
        <div class="section-title">📋 流程预览 <span class="step-count">共 {{ steps.length }} 个步骤</span></div>
        <div class="steps-preview">
          <div v-for="(step, idx) in steps" :key="idx" class="step-item">
            <div class="step-num done">✓</div>
            <div class="step-content">
              <div class="step-header">
                <span class="step-title">步骤 {{ step.stepOrder }}：{{ step.title }}</span>
                <span v-if="step.estimatedDuration" class="step-duration">{{ step.estimatedDuration }}</span>
              </div>
              <p v-if="step.description" class="step-desc">{{ step.description }}</p>
            </div>
          </div>
          <div v-if="steps.length === 0" class="empty-steps">暂无步骤信息</div>
        </div>
      </div>

      <!-- Description -->
      <div v-if="template.description" class="section">
        <div class="section-title">📝 简介</div>
        <p class="description">{{ template.description }}</p>
      </div>

      <!-- Reviews -->
      <div class="section">
        <div class="section-title">
          💬 评价 ({{ reviews.total || 0 }})
          <div class="review-controls">
            <select v-model="reviewSort" class="sort-select">
              <option value="recent">综合排序</option>
              <option value="latest">最新优先</option>
              <option value="highest">评分最高</option>
            </select>
            <label class="content-filter">
              <input v-model="contentOnly" type="checkbox" /> 只看内容
            </label>
          </div>
        </div>

        <!-- Review List -->
        <div class="review-list">
          <div v-if="reviewsLoading" class="loading-state">加载评价...</div>
          <div v-else-if="filteredReviews.length === 0" class="empty-reviews">暂无评价</div>
          <div v-else>
            <div v-for="rev in filteredReviews" :key="rev.id" class="review-item">
              <div class="review-header">
                <div class="review-stars">
                  <StarRating :value="rev.rating" readonly size="sm" />
                </div>
                <span class="review-user">{{ rev.userName }}</span>
                <span class="review-date">{{ formatDate(rev.createdAt) }}</span>
              </div>
              <p v-if="rev.comment" class="review-comment">{{ rev.comment }}</p>
            </div>
          </div>
        </div>

        <!-- Submit Review -->
        <div class="submit-review">
          <div class="section-title" style="margin-bottom:12px">✍️ 撰写评价</div>
          <div class="review-form">
            <div class="rating-select">
              <span>为本模板打分：</span>
              <StarRating v-model:value="reviewForm.rating" :readonly="false" size="lg" />
            </div>
            <textarea
              v-model="reviewForm.comment"
              class="review-textarea"
              placeholder="分享你的使用体验...（选填）"
              rows="4"
            ></textarea>
            <div class="review-form-footer">
              <span class="review-hint" v-if="hasReviewed">已评价，可修改</span>
              <button class="btn-primary" :disabled="submitting" @click="submitReview">
                {{ submitting ? '提交中...' : '提交评价' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Use Dialog -->
    <div v-if="useDialogVisible" class="dialog-overlay" @click.self="useDialogVisible = false">
      <div class="dialog">
        <div class="dialog-title">一键使用模板</div>
        <div class="dialog-body">
          <p>模板 "<strong>{{ template?.title }}</strong>" 将被复制为您的 SOP 草稿</p>
          <div class="form-group">
            <label>SOP 名称</label>
            <input v-model="useForm.sop_name" class="form-input" :placeholder="template?.title" />
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getTemplateDetail,
  useTemplate as useTemplateApi,
  favoriteTemplate,
  unfavoriteTemplate,
  getReviews,
  submitReview as submitReviewApi,
  type Template,
  type TemplateStep,
  type Review
} from '@/api/marketplace'
import StarRating from './StarRating.vue'

const route = useRoute()
const router = useRouter()

const templateId = computed(() => route.params.template_id as string)
const userId = computed(() => localStorage.getItem('bf_user_id') || '1')

const template = ref<Template | null>(null)
const steps = ref<TemplateStep[]>([])
const loading = ref(false)
const isFavorited = ref(false)
const hasReviewed = ref(false)

// Reviews
const reviews = ref<{ total: number; reviews: Review[] }>({ total: 0, reviews: [] })
const reviewsLoading = ref(false)
const reviewSort = ref('recent')
const contentOnly = ref(false)
const submitting = ref(false)
const reviewForm = ref({ rating: 0, comment: '' })

// Use dialog
const useDialogVisible = ref(false)
const using = ref(false)
const useForm = ref({ sop_name: '' })

const filteredReviews = computed(() => {
  let list = [...reviews.value.reviews]
  if (contentOnly.value) {
    list = list.filter(r => r.comment && r.comment.trim())
  }
  if (reviewSort.value === 'latest') {
    list.sort((a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime())
  } else if (reviewSort.value === 'highest') {
    list.sort((a, b) => b.rating - a.rating)
  }
  return list
})

const formatDate = (d?: string) => {
  if (!d) return ''
  return d.slice(0, 10)
}

const fetchDetail = async () => {
  loading.value = true
  try {
    const res = await getTemplateDetail(templateId.value)
    if (res.success) {
      template.value = res.data?.template || res.data
      steps.value = res.data?.steps || template.value?.steps || []
      isFavorited.value = res.data?.isFavorited || false
      hasReviewed.value = res.data?.hasReviewed || false
      useForm.value.sop_name = (template.value as Template)?.title || ''
    }
  } catch (e) {
    ElMessage.error('加载模板详情失败')
  } finally {
    loading.value = false
  }
}

const fetchReviews = async () => {
  reviewsLoading.value = true
  try {
    const res = await getReviews(templateId.value, { page_size: 50 })
    if (res.success) {
      reviews.value = { total: res.data?.total || 0, reviews: res.data?.reviews || [] }
    }
  } catch (e) {
    // ignore
  } finally {
    reviewsLoading.value = false
  }
}

const toggleFavorite = async () => {
  try {
    if (isFavorited.value) {
      await unfavoriteTemplate(templateId.value, { user_id: userId.value })
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteTemplate(templateId.value, { user_id: userId.value })
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const openUseDialog = () => {
  useForm.value.sop_name = template.value?.title || ''
  useDialogVisible.value = true
}

const confirmUse = async () => {
  using.value = true
  try {
    const res = await useTemplateApi(templateId.value, {
      user_id: userId.value,
      sop_name: useForm.value.sop_name || template.value?.title,
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

const submitReview = async () => {
  if (reviewForm.value.rating === 0) {
    ElMessage.warning('请选择评分')
    return
  }
  submitting.value = true
  try {
    const res = await submitReviewApi(templateId.value, {
      user_id: userId.value,
      rating: reviewForm.value.rating,
      comment: reviewForm.value.comment,
    })
    if (res.success) {
      ElMessage.success('评价提交成功')
      hasReviewed.value = true
      reviewForm.value = { rating: 0, comment: '' }
      fetchReviews()
      fetchDetail()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交评价失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchDetail()
  fetchReviews()
})
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background: #F9FAFB;
}

.detail-topbar {
  background: #fff;
  border-bottom: 1px solid #E5E7EB;
  padding: 0 24px;
  height: 48px;
  display: flex;
  align-items: center;
}

.back-link {
  color: #4F46E5;
  font-size: 14px;
  text-decoration: none;
  font-weight: 500;
}

.back-link:hover { text-decoration: underline; }

.loading-state, .empty-state {
  text-align: center;
  padding: 80px 0;
  color: #9CA3AF;
}

.page-body {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.cover-wrapper {
  width: 100%;
  height: 360px;
  border-radius: 16px;
  overflow: hidden;
  background: #F3F4F6;
  margin-bottom: 24px;
}

.cover-img {
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
  font-size: 80px;
  color: #D1D5DB;
}

.info-header {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #E5E7EB;
}

.template-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 8px;
}

.info-meta {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.dot { color: #D1D5DB; }

.info-actions {
  display: flex;
  gap: 12px;
}

.btn-favorite {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
  background: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #374151;
}

.btn-favorite.favorited {
  background: #FEF2F2;
  border-color: #FCA5A5;
  color: #DC2626;
}

.btn-use {
  flex: 1;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-use:hover { opacity: 0.9; }

.section {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
  border: 1px solid #E5E7EB;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.step-count {
  font-size: 13px;
  font-weight: 400;
  color: #6B7280;
}

.steps-preview {
  border-left: 3px solid #4F46E5;
  padding-left: 20px;
  background: #F9FAFB;
  border-radius: 0 8px 8px 0;
  padding: 16px 16px 16px 20px;
}

.step-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.step-item:last-child { margin-bottom: 0; }

.step-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-num.done {
  background: #D1FAE5;
  color: #059669;
}

.step-content { flex: 1; }

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.step-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
}

.step-duration {
  font-size: 12px;
  color: #9CA3AF;
  white-space: nowrap;
  margin-left: 8px;
}

.step-desc {
  font-size: 13px;
  color: #6B7280;
  margin: 0;
}

.empty-steps {
  text-align: center;
  padding: 20px;
  color: #9CA3AF;
  font-size: 14px;
}

.description {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
  margin: 0;
}

.review-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sort-select {
  padding: 4px 8px;
  border: 1px solid #E5E7EB;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  color: #374151;
}

.content-filter {
  font-size: 13px;
  color: #6B7280;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.review-list {
  margin-bottom: 24px;
}

.loading-state {
  text-align: center;
  padding: 20px;
  color: #9CA3AF;
  font-size: 14px;
}

.empty-reviews {
  text-align: center;
  padding: 20px;
  color: #9CA3AF;
  font-size: 14px;
}

.review-item {
  border-bottom: 1px solid #F3F4F6;
  padding: 12px 0;
}

.review-item:last-child { border-bottom: none; }

.review-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.review-stars { display: flex; }

.review-user {
  font-size: 13px;
  font-weight: 500;
  color: #111827;
}

.review-date {
  font-size: 12px;
  color: #9CA3AF;
  margin-left: auto;
}

.review-comment {
  font-size: 14px;
  color: #374151;
  margin: 0;
  line-height: 1.5;
}

.review-form {
  border: 1px solid #E5E7EB;
  border-radius: 12px;
  padding: 16px;
}

.rating-select {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 14px;
  color: #374151;
}

.review-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}

.review-textarea:focus { border-color: #4F46E5; }

.review-form-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.review-hint {
  font-size: 12px;
  color: #059669;
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
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) { background: #4338CA; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-outline {
  background: #fff;
  color: #4F46E5;
  border: 1px solid #4F46E5;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
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

.form-input:focus { border-color: #4F46E5; }

.dialog-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #F3F4F6;
}
</style>
