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
  margin-bottom: 24px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #E8EAF0;
}

.card {
  background: #1A1D27;
  border-radius: 12px;
  border: 1px solid #2D3348;
  overflow: hidden;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 0;
  color: #8B90A0;
  font-size: 14px;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #555A6E;
  text-transform: uppercase;
  background: #0F1117;
  border-bottom: 1px solid #2D3348;
}

.data-table td {
  padding: 12px 16px;
  font-size: 14px;
  color: #E8EAF0;
  border-bottom: 1px solid #2D3348;
}

.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #22263A; }

.cat-icon { font-size: 24px; }

.name-cell { font-weight: 500; }

.desc-cell { color: #8B90A0; font-size: 13px; }

.num-cell { text-align: center; }

code {
  background: #0F1117;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #5B7FFF;
}

.action-btns {
  display: flex;
  gap: 8px;
}

.btn-text {
  background: transparent;
  border: none;
  color: #5B7FFF;
  font-size: 13px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-text:hover { background: rgba(91, 127, 255, 0.1); }
.btn-text.danger { color: #EF4444; }
.btn-text.danger:hover { background: rgba(239, 68, 68, 0.1); }

/* Buttons */
.btn-primary {
  background: #5B7FFF;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) { background: #4A6FEF; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }

/* Dialog */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog {
  background: #1A1D27;
  border-radius: 16px;
  width: 480px;
  max-width: 90vw;
  border: 1px solid #2D3348;
}

.dialog-title {
  padding: 20px 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #E8EAF0;
}

.dialog-body {
  padding: 16px 24px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.form-group {
  margin-bottom: 12px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: #8B90A0;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #2D3348;
  border-radius: 8px;
  font-size: 14px;
  background: #0F1117;
  color: #E8EAF0;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus { border-color: #5B7FFF; }
.form-input:disabled { opacity: 0.5; }

.dialog-footer {
  padding: 16px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #2D3348;
}

.btn-cancel {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid #2D3348;
  background: transparent;
  color: #8B90A0;
  font-size: 14px;
  cursor: pointer;
}
</style>
