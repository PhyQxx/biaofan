<template>
  <div class="page-header">
    <h2 class="page-title">🏷️ 分类管理</h2>
      <button class="btn-primary" @click="openForm()">+ 新增分类</button>
    </div>

    <!-- Category List -->
    <div class="card">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="categories.length === 0" class="empty-state">暂无分类</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>分类图标</th>
            <th>分类名称</th>
            <th>分类Key</th>
            <th>描述</th>
            <th>排序</th>
            <th>模板数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cat in categories" :key="cat.key">
            <td><span class="cat-icon">{{ cat.icon }}</span></td>
            <td class="name-cell">{{ cat.name }}</td>
            <td><code>{{ cat.key }}</code></td>
            <td class="desc-cell">{{ cat.description || '-' }}</td>
            <td class="num-cell">{{ cat.sortOrder }}</td>
            <td class="num-cell">{{ cat.templateCount || 0 }}</td>
            <td>
              <div class="action-btns">
                <button class="btn-text" @click="openForm(cat)">编辑</button>
                <button class="btn-text danger" @click="handleDelete(cat)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Form Dialog -->
    <div v-if="formVisible" class="dialog-overlay" @click.self="formVisible = false">
      <div class="dialog">
        <div class="dialog-title">{{ editingCategory ? '编辑分类' : '新增分类' }}</div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-group">
              <label>分类名称 *</label>
              <input v-model="form.name" class="form-input" placeholder="如：工作类" />
            </div>
            <div class="form-group">
              <label>分类Key *</label>
              <input v-model="form.key" class="form-input" placeholder="如：work" :disabled="!!editingCategory" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>图标 Emoji</label>
              <input v-model="form.icon" class="form-input" placeholder="如：💼" />
            </div>
            <div class="form-group">
              <label>排序</label>
              <input v-model.number="form.sortOrder" type="number" class="form-input" placeholder="数字越小越靠前" />
            </div>
          </div>
          <div class="form-group">
            <label>描述</label>
            <input v-model="form.description" class="form-input" placeholder="分类描述（选填）" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-cancel" @click="formVisible = false">取消</button>
          <button class="btn-primary" :disabled="saving || !form.name || !form.key" @click="handleSave">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">


/**
 * 管理后台 - 分类管理页
 * - 模板市场分类增删改
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category {
  key: string
  name: string
  icon: string
  description?: string
  sortOrder?: number
  templateCount?: number
}

// Default categories (hardcoded for now, as no backend API for category CRUD)
const defaultCategories: Category[] = [
  { key: 'work', name: '工作类', icon: '💼', description: '工作相关的SOP模板', sortOrder: 1, templateCount: 0 },
  { key: 'life', name: '生活类', icon: '🏠', description: '日常生活相关的SOP模板', sortOrder: 2, templateCount: 0 },
  { key: 'study', name: '学习类', icon: '📚', description: '学习相关的SOP模板', sortOrder: 3, templateCount: 0 },
  { key: 'other', name: '其他', icon: '✨', description: '其他类型的SOP模板', sortOrder: 4, templateCount: 0 },
] as Category[]

const categories = ref<Category[]>([...defaultCategories])
const loading = ref(false)
const formVisible = ref(false)
const editingCategory = ref<Category | null>(null)
const saving = ref(false)
const form = ref<{ name: string; key: string; icon: string; description?: string; sortOrder?: number }>({ name: '', key: '', icon: '✨', description: '', sortOrder: 0 })

const openForm = (cat?: Category) => {
  if (cat) {
    editingCategory.value = cat
    form.value = { ...cat }
  } else {
    editingCategory.value = null
    form.value = { name: '', key: '', icon: '✨', description: '', sortOrder: 0 }
  }
  formVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name || !form.value.key) {
    ElMessage.warning('请填写必填项')
    return
  }
  saving.value = true
  try {
    if (editingCategory.value) {
      const idx = categories.value.findIndex(c => c.key === editingCategory.value!.key)
      if (idx !== -1) categories.value[idx] = { ...form.value }
      ElMessage.success('分类已更新')
    } else {
      categories.value.push({ ...form.value })
      ElMessage.success('分类已添加')
    }
    formVisible.value = false
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (cat: Category) => {
  try {
    await ElMessageBox.confirm(
      `确定删除分类 "${cat.name}" 吗？`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    categories.value = categories.value.filter(c => c.key !== cat.key)
    ElMessage.success('分类已删除')
  } catch {
    // cancelled
  }
}

onMounted(() => {
  // In a real app, would fetch from API
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2xl);
}

.page-title {
  font-size: var(--font-size-3xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.card {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  overflow: hidden;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: var(--space-md) var(--space-lg);
  text-align: left;
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  background: #0F1117;
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: var(--space-md) var(--space-lg);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  border-bottom: 1px solid var(--color-border);
}

.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--color-bg-surface); }

.cat-icon { font-size: 24px; }

.name-cell { font-weight: 500; }

.desc-cell { color: var(--color-text-secondary); font-size: var(--font-size-sm); }

.num-cell { text-align: center; }

code {
  background: #0F1117;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-primary);
}

.action-btns {
  display: flex;
  gap: var(--space-sm);
}

.btn-text {
  background: transparent;
  border: none;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  cursor: pointer;
  padding: 4px var(--space-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-normal);
}

.btn-text:hover { background: rgba(91, 127, 255, 0.1); }
.btn-text.danger { color: #EF4444; }
.btn-text.danger:hover { background: rgba(239, 68, 68, 0.1); }

/* Buttons */
.btn-primary {
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: var(--radius-md);
  padding: var(--space-sm) var(--space-lg);
  font-size: var(--font-size-base);
  cursor: pointer;
  transition: background var(--transition-normal);
}

.btn-primary:hover:not(:disabled) { background: var(--color-primary-hover); }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: var(--color-bg-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: var(--color-bg-elevated);
  border-radius: var(--radius-xl);
  width: 480px;
  max-width: 90vw;
  border: 1px solid var(--color-border);
}

.dialog-title {
  padding: var(--space-xl) var(--space-2xl) 0;
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--color-text-primary);
}

.dialog-body {
  padding: var(--space-lg) var(--space-2xl);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.form-group {
  margin-bottom: var(--space-md);
}

.form-group label {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px var(--space-md);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  font-size: var(--font-size-base);
  background: #0F1117;
  color: var(--color-text-primary);
  outline: none;
  box-sizing: border-box;
}

.form-input:focus { border-color: var(--color-primary); }
.form-input:disabled { opacity: 0.5; }

.dialog-footer {
  padding: var(--space-lg) var(--space-2xl);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
  border-top: 1px solid var(--color-border);
}

.btn-cancel {
  padding: var(--space-sm) var(--space-lg);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-base);
  cursor: pointer;
}
</style>
