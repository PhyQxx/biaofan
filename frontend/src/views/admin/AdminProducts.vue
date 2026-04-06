<template>
  <div class="page-header">
    <h2 class="page-title">🎁 商品管理</h2>
      <button class="btn-primary" @click="openForm()">+ 新增商品</button>
    </div>

    <!-- Product Table -->
    <div class="card">
      <div v-if="loading" class="loading-state">加载中...</div>
      <div v-else-if="products.length === 0" class="empty-state">暂无商品，点击上方按钮新增</div>
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>图标</th>
            <th>商品名称</th>
            <th>分类</th>
            <th>价格（积分）</th>
            <th>库存</th>
            <th>有效期</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in products" :key="product.id">
            <td><span class="product-icon">{{ product.icon }}</span></td>
            <td class="name-cell">
              {{ product.name }}
              <div class="product-desc">{{ product.description }}</div>
            </td>
            <td>
              <span class="category-tag" :class="'cat-' + product.category">
                {{ categoryLabel(product.category) }}
              </span>
            </td>
            <td class="num-cell">{{ product.price }}</td>
            <td>
              <span :class="product.stock === 0 ? 'text-danger' : product.stock < 10 ? 'text-warning' : ''">
                {{ product.stock }}
              </span>
            </td>
            <td class="text-secondary">{{ product.valid_days > 0 ? product.valid_days + '天' : '永久' }}</td>
            <td>
              <span class="status-tag" :class="product.status === 1 ? 'status-on' : 'status-off'">
                {{ product.status === 1 ? '上架' : '下架' }}
              </span>
            </td>
            <td>
              <div class="action-btns">
                <button class="btn-text" @click="openForm(product)">编辑</button>
                <button class="btn-text danger" @click="handleDelete(product)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Product Form Dialog -->
    <div class="dialog-overlay" v-if="formVisible" @click.self="formVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ editingProduct ? '编辑商品' : '新增商品' }}</h3>
          <button class="dialog-close" @click="formVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-group">
              <label>商品名称 <span class="required">*</span></label>
              <input v-model="form.name" class="form-input" placeholder="如：金色头像框" />
            </div>
            <div class="form-group">
              <label>分类 <span class="required">*</span></label>
              <select v-model="form.category" class="form-input">
                <option value="title">称号</option>
                <option value="avatar_frame">头像框</option>
                <option value="background">背景</option>
                <option value="emotion">表情</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>图标 Emoji <span class="required">*</span></label>
              <div class="icon-picker">
                <input v-model="form.icon" class="form-input icon-input" placeholder="如：🏅" maxlength="4" />
                <span class="icon-preview">{{ form.icon || '❓' }}</span>
              </div>
            </div>
            <div class="form-group">
              <label>价格（积分） <span class="required">*</span></label>
              <input v-model.number="form.price" type="number" class="form-input" placeholder="如：500" min="0" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>库存</label>
              <input v-model.number="form.stock" type="number" class="form-input" placeholder="如：999" min="0" />
            </div>
            <div class="form-group">
              <label>有效期（天）</label>
              <input v-model.number="form.valid_days" type="number" class="form-input" placeholder="0=永久，如：30" min="0" />
            </div>
          </div>
          <div class="form-group">
            <label>商品描述</label>
            <input v-model="form.description" class="form-input" placeholder="简要描述商品" />
          </div>
          <div class="form-group">
            <label>状态</label>
            <div class="toggle-row">
              <label class="toggle">
                <input type="checkbox" v-model="formStatus" />
                <span class="toggle-slider"></span>
              </label>
              <span class="toggle-label">{{ formStatus ? '上架' : '下架' }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="btn-secondary" @click="formVisible = false">取消</button>
          <button class="btn-primary" @click="handleSave" :disabled="saving">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductList, createProduct, updateProduct, deleteProduct, type Product } from '@/api/admin'

const products = ref<Product[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const editingProduct = ref<Product | null>(null)

const form = reactive<Product>({
  name: '', category: 'title', icon: '', price: 0,
  stock: 999, valid_days: 30, description: '', status: 1,
})

const formStatus = computed({
  get: () => form.status === 1,
  set: (v: boolean) => { form.status = v ? 1 : 0 },
})

const categoryLabel = (c: string) => ({
  title: '称号', avatar_frame: '头像框', background: '背景', emotion: '表情',
}[c] || c)

const loadData = async () => {
  loading.value = true
  try {
    const resp = await getProductList()
    products.value = resp.list || []
  } catch {
    ElMessage.error('加载商品列表失败')
  } finally {
    loading.value = false
  }
}

const openForm = (product?: Product) => {
  if (product) {
    editingProduct.value = product
    Object.assign(form, product)
  } else {
    editingProduct.value = null
    Object.assign(form, { name: '', category: 'title', icon: '', price: 0, stock: 999, valid_days: 30, description: '', status: 1 })
  }
  formVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.icon) {
    ElMessage.warning('请填写商品名称和图标')
    return
  }
  saving.value = true
  try {
    if (editingProduct.value?.id) {
      await updateProduct(editingProduct.value.id, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createProduct({ ...form })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadData()
  } catch {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (product: Product) => {
  try {
    await ElMessageBox.confirm(`确认删除商品「${product.name}」？`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (product.id) {
      await deleteProduct(product.id)
      ElMessage.success('删除成功')
      loadData()
    }
  } catch { /* cancel */ }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 600; color: #E8EAF0; }
.card { background: #1A1D27; border: 1px solid #2D3348; border-radius: 12px; padding: 20px; }
.loading-state, .empty-state { text-align: center; color: #8B90A0; padding: 40px 0; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; font-size: 12px; font-weight: 600; color: #8B90A0; padding: 8px 12px; border-bottom: 1px solid #2D3348; }
.data-table td { padding: 12px; border-bottom: 1px solid #2D3348; font-size: 14px; color: #E8EAF0; vertical-align: middle; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #22263A; }
.product-icon { font-size: 28px; width: 44px; height: 44px; display: inline-flex; align-items: center; justify-content: center; background: #22263A; border-radius: 8px; }
.name-cell { font-weight: 600; }
.product-desc { font-size: 12px; color: #8B90A0; font-weight: 400; margin-top: 2px; }
.category-tag { padding: 3px 10px; border-radius: 10px; font-size: 12px; }
.cat-title { background: rgba(91,127,255,0.15); color: #5B7FFF; }
.cat-avatar_frame { background: rgba(255,215,0,0.15); color: #FFD700; }
.cat-background { background: rgba(74,222,128,0.15); color: #4ADE80; }
.cat-emotion { background: rgba(185,242,255,0.15); color: #B9F2FF; }
.num-cell { color: #FFD700; font-family: monospace; }
.text-danger { color: #FF6B6B; }
.text-warning { color: #FFA500; }
.text-secondary { color: #8B90A0; font-size: 13px; }
.status-tag { padding: 3px 10px; border-radius: 10px; font-size: 12px; }
.status-on { background: rgba(74,222,128,0.15); color: #4ADE80; }
.status-off { background: rgba(255,107,107,0.15); color: #FF6B6B; }
.action-btns { display: flex; gap: 8px; }
.btn-text { background: none; border: none; color: #5B7FFF; font-size: 13px; cursor: pointer; padding: 2px 6px; border-radius: 4px; transition: background 0.2s; }
.btn-text:hover { background: rgba(91,127,255,0.1); }
.btn-text.danger { color: #FF6B6B; }
.btn-text.danger:hover { background: rgba(255,107,107,0.1); }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.7); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: #1A1D27; border: 1px solid #2D3348; border-radius: 16px; width: 560px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.5); }
.dialog-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid #2D3348; }
.dialog-header h3 { font-size: 16px; font-weight: 600; color: #E8EAF0; }
.dialog-close { background: none; border: none; color: #8B90A0; font-size: 22px; cursor: pointer; }
.dialog-close:hover { color: #E8EAF0; }
.dialog-body { padding: 24px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 16px 24px; border-top: 1px solid #2D3348; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-group:last-child { margin-bottom: 0; }
.form-group label { font-size: 13px; font-weight: 600; color: #8B90A0; }
.required { color: #FF6B6B; }
.form-input { background: #22263A; border: 1px solid #2D3348; border-radius: 8px; padding: 10px 12px; font-size: 14px; color: #E8EAF0; outline: none; transition: border-color 0.2s; }
.form-input:focus { border-color: #5B7FFF; }
select.form-input { cursor: pointer; }
.icon-picker { display: flex; align-items: center; gap: 12px; }
.icon-input { flex: 1; }
.icon-preview { font-size: 32px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: #22263A; border-radius: 8px; }
.toggle-row { display: flex; align-items: center; gap: 10px; }
.toggle { position: relative; width: 44px; height: 24px; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: #2D3348; border-radius: 12px; transition: 0.3s; }
.toggle-slider::before { content: ''; position: absolute; width: 18px; height: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: 0.3s; }
.toggle input:checked + .toggle-slider { background: #5B7FFF; }
.toggle input:checked + .toggle-slider::before { transform: translateX(20px); }
.toggle-label { font-size: 14px; color: #E8EAF0; }

.btn-primary { background: #5B7FFF; color: #fff; border: none; border-radius: 8px; padding: 10px 20px; font-size: 14px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
.btn-primary:hover { background: #7B9FFF; }
.btn-primary:disabled { background: #2D3348; color: #555A6E; cursor: not-allowed; }
.btn-secondary { background: #22263A; color: #8B90A0; border: 1px solid #2D3348; border-radius: 8px; padding: 10px 20px; font-size: 14px; font-weight: 600; cursor: pointer; }
.btn-secondary:hover { background: #2D3348; color: #E8EAF0; }
</style>
