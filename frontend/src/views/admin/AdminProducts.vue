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


/**
 * 管理后台 - 积分商品管理页
 * - 商品列表（名称、积分价格、库存）
 * - 上架 / 下架 / 编辑
 */
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
    products.value = resp.data?.list || []
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
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--space-xl); }
.page-title { font-size: var(--font-size-3xl); font-weight: 600; color: var(--color-text-primary); }
.card { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: var(--space-xl); }
.loading-state, .empty-state { text-align: center; color: var(--color-text-secondary); padding: 40px 0; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th { text-align: left; font-size: var(--font-size-xs); font-weight: 600; color: var(--color-text-secondary); padding: var(--space-sm) var(--space-md); border-bottom: 1px solid var(--color-border); }
.data-table td { padding: var(--space-md); border-bottom: 1px solid var(--color-border); font-size: var(--font-size-base); color: var(--color-text-primary); vertical-align: middle; }
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: var(--color-bg-surface); }
.product-icon { font-size: 28px; width: 44px; height: 44px; display: inline-flex; align-items: center; justify-content: center; background: var(--color-bg-surface); border-radius: var(--radius-md); }
.name-cell { font-weight: 600; }
.product-desc { font-size: var(--font-size-xs); color: var(--color-text-secondary); font-weight: 400; margin-top: 2px; }
.category-tag { padding: 3px 10px; border-radius: 10px; font-size: var(--font-size-xs); }
.cat-title { background: rgba(91,127,255,0.15); color: var(--color-primary); }
.cat-avatar_frame { background: rgba(255,215,0,0.15); color: #FFD700; }
.cat-background { background: rgba(74,222,128,0.15); color: #4ADE80; }
.cat-emotion { background: rgba(185,242,255,0.15); color: #B9F2FF; }
.num-cell { color: #FFD700; font-family: monospace; }
.text-danger { color: #FF6B6B; }
.text-warning { color: #FFA500; }
.text-secondary { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.status-tag { padding: 3px 10px; border-radius: 10px; font-size: var(--font-size-xs); }
.status-on { background: rgba(74,222,128,0.15); color: #4ADE80; }
.status-off { background: rgba(255,107,107,0.15); color: #FF6B6B; }
.action-btns { display: flex; gap: var(--space-sm); }
.btn-text { background: none; border: none; color: var(--color-primary); font-size: var(--font-size-sm); cursor: pointer; padding: 2px 6px; border-radius: var(--radius-sm); transition: background var(--transition-normal); }
.btn-text:hover { background: rgba(91,127,255,0.1); }
.btn-text.danger { color: #FF6B6B; }
.btn-text.danger:hover { background: rgba(255,107,107,0.1); }

.dialog-overlay { position: fixed; inset: 0; background: var(--color-bg-overlay); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.dialog { background: var(--color-bg-elevated); border: 1px solid var(--color-border); border-radius: var(--radius-xl); width: 560px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 60px rgba(0,0,0,0.5); }
.dialog-header { display: flex; align-items: center; justify-content: space-between; padding: var(--space-xl) var(--space-2xl); border-bottom: 1px solid var(--color-border); }
.dialog-header h3 { font-size: var(--font-size-xl); font-weight: 600; color: var(--color-text-primary); }
.dialog-close { background: none; border: none; color: var(--color-text-secondary); font-size: 22px; cursor: pointer; }
.dialog-close:hover { color: var(--color-text-primary); }
.dialog-body { padding: var(--space-2xl); }
.dialog-footer { display: flex; justify-content: flex-end; gap: var(--space-md); padding: var(--space-lg) var(--space-2xl); border-top: 1px solid var(--color-border); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-lg); }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: var(--space-lg); }
.form-group:last-child { margin-bottom: 0; }
.form-group label { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-secondary); }
.required { color: #FF6B6B; }
.form-input { background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px var(--space-md); font-size: var(--font-size-base); color: var(--color-text-primary); outline: none; transition: border-color var(--transition-normal); }
.form-input:focus { border-color: var(--color-primary); }
select.form-input { cursor: pointer; }
.icon-picker { display: flex; align-items: center; gap: var(--space-md); }
.icon-input { flex: 1; }
.icon-preview { font-size: 32px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: var(--color-bg-surface); border-radius: var(--radius-md); }
.toggle-row { display: flex; align-items: center; gap: 10px; }
.toggle { position: relative; width: 44px; height: 24px; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: var(--color-border); border-radius: 12px; transition: 0.3s; }
.toggle-slider::before { content: ''; position: absolute; width: 18px; height: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: 0.3s; }
.toggle input:checked + .toggle-slider { background: var(--color-primary); }
.toggle input:checked + .toggle-slider::before { transform: translateX(20px); }
.toggle-label { font-size: var(--font-size-base); color: var(--color-text-primary); }

.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-md); padding: 10px var(--space-xl); font-size: var(--font-size-base); font-weight: 600; cursor: pointer; transition: background var(--transition-normal); }
.btn-primary:hover { background: var(--color-primary-hover); }
.btn-primary:disabled { background: var(--color-border); color: var(--color-text-muted); cursor: not-allowed; }
.btn-secondary { background: var(--color-bg-surface); color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px var(--space-xl); font-size: var(--font-size-base); font-weight: 600; cursor: pointer; }
.btn-secondary:hover { background: var(--color-border); color: var(--color-text-primary); }
</style>
