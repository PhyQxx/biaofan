<template>
  <div class="page-header">
    <h2 class="page-title">🏅 徽章管理</h2>
    <button class="btn-primary" @click="openForm()">+ 新增徽章</button>
  </div>

  <!-- Badge Table -->
  <div class="card">
    <div v-if="loading" class="loading-state">加载中...</div>
    <div v-else-if="badges.length === 0" class="empty-state">暂无徽章，点击上方按钮新增</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th>图标</th>
          <th>徽章名称</th>
          <th>徽章标识</th>
          <th>稀有度</th>
          <th>解锁条件</th>
          <th>EXP奖励</th>
          <th>积分奖励</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="badge in badges" :key="badge.id">
          <td><span class="badge-icon">{{ badge.icon }}</span></td>
          <td class="name-cell">{{ badge.name }}</td>
          <td><code class="badge-id">{{ badge.badgeKey }}</code></td>
          <td>
            <span class="rarity-tag" :class="'rarity-' + badge.rarity">
              {{ rarityLabel(badge.rarity) }}
            </span>
          </td>
          <td class="cond-cell">{{ badge.condition }}</td>
          <td class="num-cell">+{{ badge.rewardExp }}</td>
          <td class="num-cell">+{{ badge.rewardScore }}</td>
          <td>
            <div class="action-btns">
              <button class="btn-text" @click="openForm(badge)">编辑</button>
              <button class="btn-text danger" @click="handleDelete(badge)">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <!-- Badge Form Dialog -->
  <div class="dialog-overlay" v-if="formVisible" @click.self="formVisible = false">
    <div class="dialog">
      <div class="dialog-header">
        <h3>{{ editingBadge ? '编辑徽章' : '新增徽章' }}</h3>
        <button class="dialog-close" @click="formVisible = false">×</button>
      </div>
      <div class="dialog-body">
        <div class="form-row">
          <div class="form-group">
            <label>徽章名称 <span class="required">*</span></label>
            <input v-model="form.name" class="form-input" placeholder="如：首次完成" />
          </div>
          <div class="form-group">
            <label>徽章标识 <span class="required">*</span></label>
            <input v-model="form.badgeKey" class="form-input" placeholder="如：first_execution" :disabled="!!editingBadge" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>图标 Emoji <span class="required">*</span></label>
            <div class="icon-picker">
              <input v-model="form.icon" class="form-input icon-input" placeholder="如：🔥" maxlength="4" />
              <span class="icon-preview">{{ form.icon || '❓' }}</span>
            </div>
          </div>
          <div class="form-group">
            <label>稀有度 <span class="required">*</span></label>
            <select v-model="form.rarity" class="form-input">
              <option value="bronze">普通（铜色）</option>
              <option value="silver">稀有（银色）</option>
              <option value="gold">传奇（金色）</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>解锁条件</label>
          <input v-model="form.condition" class="form-input" placeholder="描述该徽章的获取条件" />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label>EXP 奖励</label>
            <input v-model.number="form.rewardExp" type="number" class="form-input" placeholder="如：200" min="0" />
          </div>
          <div class="form-group">
            <label>积分奖励</label>
            <input v-model.number="form.rewardScore" type="number" class="form-input" placeholder="如：80" min="0" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBadgeList, createBadge, updateBadge, deleteBadge, type Badge } from '@/api/admin'

const badges = ref<Badge[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const editingBadge = ref<Badge | null>(null)

const form = reactive<Badge>({
  badgeKey: '',
  name: '',
  icon: '',
  rarity: 'bronze',
  description: '',
  condition: '',
  rewardExp: 0,
  rewardScore: 0,
})

const rarityLabel = (r: string) => ({ bronze: '普通', silver: '稀有', gold: '传奇' }[r] || r)

const loadData = async () => {
  loading.value = true
  try {
    const resp = await getBadgeList()
    badges.value = resp.data?.list || []
  } catch {
    ElMessage.error('加载徽章列表失败')
  } finally {
    loading.value = false
  }
}

const openForm = (badge?: Badge) => {
  if (badge) {
    editingBadge.value = badge
    Object.assign(form, badge)
  } else {
    editingBadge.value = null
    Object.assign(form, {
      badgeKey: '', name: '', icon: '', rarity: 'bronze',
      condition: '', rewardExp: 0, rewardScore: 0,
    })
  }
  formVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.badgeKey) {
    ElMessage.warning('请填写徽章名称和徽章标识')
    return
  }
  saving.value = true
  try {
    if (editingBadge.value?.id) {
      await updateBadge(editingBadge.value.id, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createBadge({ ...form })
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

const handleDelete = async (badge: Badge) => {
  try {
    await ElMessageBox.confirm(`确认删除徽章「${badge.name}」？删除后用户将无法再获得此徽章。`, '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (badge.id) {
      await deleteBadge(badge.id)
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
.badge-icon { font-size: 28px; width: 44px; height: 44px; display: flex; align-items: center; justify-content: center; background: var(--color-bg-surface); border-radius: var(--radius-md); }
.name-cell { font-weight: 600; }
.badge-id { font-family: monospace; font-size: var(--font-size-xs); background: var(--color-bg-surface); padding: 2px 6px; border-radius: var(--radius-sm); color: var(--color-text-secondary); }
.rarity-tag { padding: 3px 10px; border-radius: 10px; font-size: var(--font-size-xs); font-weight: 600; }
.rarity-bronze { background: rgba(205,127,50,0.15); color: #CD7F32; border: 1px solid #CD7F32; }
.rarity-silver { background: rgba(192,192,192,0.15); color: #C0C0C0; border: 1px solid #C0C0C0; }
.rarity-gold { background: rgba(255,215,0,0.15); color: #FFD700; border: 1px solid #FFD700; }
.cond-cell { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.num-cell { color: #4ADE80; font-family: monospace; }
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
.dialog-body { padding: var(--space-2xl); }
.dialog-footer { display: flex; justify-content: flex-end; gap: var(--space-md); padding: var(--space-lg) var(--space-2xl); border-top: 1px solid var(--color-border); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-lg); }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: var(--space-lg); }
.form-group label { font-size: var(--font-size-sm); font-weight: 600; color: var(--color-text-secondary); }
.required { color: #FF6B6B; }
.form-input { background: var(--color-bg-surface); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px var(--space-md); font-size: var(--font-size-base); color: var(--color-text-primary); outline: none; transition: border-color var(--transition-normal); }
.form-input:focus { border-color: var(--color-primary); }
.icon-picker { display: flex; align-items: center; gap: var(--space-md); }
.icon-input { flex: 1; }
.icon-preview { font-size: 32px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: var(--color-bg-surface); border-radius: var(--radius-md); }
.btn-primary { background: var(--color-primary); color: #fff; border: none; border-radius: var(--radius-md); padding: 10px var(--space-xl); font-size: var(--font-size-base); font-weight: 600; cursor: pointer; }
.btn-secondary { background: var(--color-bg-surface); color: var(--color-text-secondary); border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px var(--space-xl); font-size: var(--font-size-base); font-weight: 600; cursor: pointer; }
</style>
