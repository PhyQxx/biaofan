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
            <th>徽章ID</th>
            <th>稀有度</th>
            <th>条件类型</th>
            <th>EXP奖励</th>
            <th>积分奖励</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="badge in badges" :key="badge.id">
            <td><span class="badge-icon">{{ badge.icon }}</span></td>
            <td class="name-cell">{{ badge.name }}</td>
            <td><code class="badge-id">{{ badge.badge_id }}</code></td>
            <td>
              <span class="rarity-tag" :class="'rarity-' + badge.rarity">
                {{ rarityLabel(badge.rarity) }}
              </span>
            </td>
            <td class="cond-cell">{{ conditionLabel(badge.condition_type) }}</td>
            <td class="num-cell">+{{ badge.exp_reward }}</td>
            <td class="num-cell">+{{ badge.score_reward }}</td>
            <td>
              <span class="status-tag" :class="badge.status === 1 ? 'status-on' : 'status-off'">
                {{ badge.status === 1 ? '上线' : '下线' }}
              </span>
            </td>
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
              <input v-model="form.name" class="form-input" placeholder="如：连续7天之星" />
            </div>
            <div class="form-group">
              <label>徽章ID <span class="required">*</span></label>
              <input v-model="form.badge_id" class="form-input" placeholder="如：streak_7d" :disabled="!!editingBadge" />
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
                <option value="common">普通（铜色）</option>
                <option value="rare">稀有（银色）</option>
                <option value="legendary">传奇（金色）</option>
              </select>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>条件类型 <span class="required">*</span></label>
              <select v-model="form.condition_type" class="form-input">
                <option value="first_complete">首次完成</option>
                <option value="streak_7d">连续7天</option>
                <option value="streak_30d">连续30天</option>
                <option value="speed">速度之星</option>
                <option value="troubleshoot">异常处理</option>
                <option value="sop_master">SOP大师</option>
                <option value="time_range">时间段完成</option>
                <option value="perfect">完美执行</option>
                <option value="assist">协助他人</option>
                <option value="category_count">分类完成数</option>
                <option value="top_weekly">周榜冠军</option>
                <option value="top_monthly">月榜冠军</option>
                <option value="comeback">逆袭达人</option>
              </select>
            </div>
            <div class="form-group">
              <label>条件参数</label>
              <input v-model="form.condition_param" class="form-input" placeholder="如：7（连续天数）或 50（提前百分比）" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>EXP 奖励</label>
              <input v-model.number="form.exp_reward" type="number" class="form-input" placeholder="如：200" min="0" />
            </div>
            <div class="form-group">
              <label>积分奖励</label>
              <input v-model.number="form.score_reward" type="number" class="form-input" placeholder="如：80" min="0" />
            </div>
          </div>
          <div class="form-group">
            <label>状态</label>
            <div class="toggle-row">
              <label class="toggle">
                <input type="checkbox" v-model="formStatus" />
                <span class="toggle-slider"></span>
              </label>
              <span class="toggle-label">{{ formStatus ? '上线' : '下线' }}</span>
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
 * 管理后台 - 徽章管理页
 * - 徽章列表（图标、名称、描述、所需积分）
 * - 新建 / 编辑 / 删除徽章
 * - 启用/禁用徽章
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBadgeList, createBadge, updateBadge, deleteBadge, type Badge } from '@/api/admin'

const badges = ref<Badge[]>([])
const loading = ref(false)
const saving = ref(false)
const formVisible = ref(false)
const editingBadge = ref<Badge | null>(null)

const form = reactive<Badge>({
  badge_id: '',
  name: '',
  icon: '',
  rarity: 'common',
  condition_type: 'first_complete',
  condition_param: '',
  exp_reward: 0,
  score_reward: 0,
  status: 1,
})

const formStatus = computed({
  get: () => form.status === 1,
  set: (v: boolean) => { form.status = v ? 1 : 0 },
})

const rarityLabel = (r: string) => ({ common: '普通', rare: '稀有', legendary: '传奇' }[r] || r)
const conditionLabel = (c: string) => ({
  first_complete: '首次完成', streak_7d: '连续7天', streak_30d: '连续30天',
  speed: '速度之星', troubleshoot: '异常处理', sop_master: 'SOP大师',
  time_range: '时间段', perfect: '完美执行', assist: '协助他人',
  category_count: '分类完成数', top_weekly: '周榜冠军', top_monthly: '月榜冠军',
  comeback: '逆袭达人',
}[c] || c)

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
      badge_id: '', name: '', icon: '', rarity: 'common',
      condition_type: 'first_complete', condition_param: '',
      exp_reward: 0, score_reward: 0, status: 1,
    })
  }
  formVisible.value = true
}

const handleSave = async () => {
  if (!form.name || !form.badge_id) {
    ElMessage.warning('请填写徽章名称和徽章ID')
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
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #E8EAF0;
}
.card {
  background: #1A1D27;
  border: 1px solid #2D3348;
  border-radius: 12px;
  padding: 20px;
}
.loading-state, .empty-state {
  text-align: center;
  color: #8B90A0;
  padding: 40px 0;
}
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th {
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  color: #8B90A0;
  padding: 8px 12px;
  border-bottom: 1px solid #2D3348;
}
.data-table td {
  padding: 12px;
  border-bottom: 1px solid #2D3348;
  font-size: 14px;
  color: #E8EAF0;
  vertical-align: middle;
}
.data-table tr:last-child td { border-bottom: none; }
.data-table tr:hover td { background: #22263A; }

.badge-icon {
  font-size: 28px;
  display: block;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #22263A;
  border-radius: 8px;
}
.name-cell { font-weight: 600; }
.badge-id {
  font-family: monospace;
  font-size: 12px;
  background: #22263A;
  padding: 2px 6px;
  border-radius: 4px;
  color: #8B90A0;
}
.rarity-tag {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.rarity-common { background: rgba(205,127,50,0.15); color: #CD7F32; border: 1px solid #CD7F32; }
.rarity-rare { background: rgba(192,192,192,0.15); color: #C0C0C0; border: 1px solid #C0C0C0; }
.rarity-legendary { background: rgba(255,215,0,0.15); color: #FFD700; border: 1px solid #FFD700; }
.cond-cell { color: #8B90A0; font-size: 13px; }
.num-cell { color: #4ADE80; font-family: monospace; }
.status-tag {
  padding: 3px 10px;
  border-radius: 10px;
  font-size: 12px;
}
.status-on { background: rgba(74,222,128,0.15); color: #4ADE80; }
.status-off { background: rgba(255,107,107,0.15); color: #FF6B6B; }
.action-btns { display: flex; gap: 8px; }
.btn-text {
  background: none;
  border: none;
  color: #5B7FFF;
  font-size: 13px;
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: background 0.2s;
}
.btn-text:hover { background: rgba(91,127,255,0.1); }
.btn-text.danger { color: #FF6B6B; }
.btn-text.danger:hover { background: rgba(255,107,107,0.1); }

/* Form Dialog */
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
  border: 1px solid #2D3348;
  border-radius: 16px;
  width: 560px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 60px rgba(0,0,0,0.5);
}
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #2D3348;
}
.dialog-header h3 { font-size: 16px; font-weight: 600; color: #E8EAF0; }
.dialog-close {
  background: none;
  border: none;
  color: #8B90A0;
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
}
.dialog-close:hover { color: #E8EAF0; }
.dialog-body { padding: 24px; }
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #2D3348;
}
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-group { display: flex; flex-direction: column; gap: 6px; margin-bottom: 16px; }
.form-group:last-child { margin-bottom: 0; }
.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #8B90A0;
}
.required { color: #FF6B6B; }
.form-input {
  background: #22263A;
  border: 1px solid #2D3348;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 14px;
  color: #E8EAF0;
  outline: none;
  transition: border-color 0.2s;
}
.form-input:focus { border-color: #5B7FFF; }
.form-input:disabled { opacity: 0.5; cursor: not-allowed; }
select.form-input { cursor: pointer; }
.icon-picker { display: flex; align-items: center; gap: 12px; }
.icon-input { flex: 1; }
.icon-preview { font-size: 32px; width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; background: #22263A; border-radius: 8px; }
.toggle-row { display: flex; align-items: center; gap: 10px; }
.toggle { position: relative; width: 44px; height: 24px; }
.toggle input { opacity: 0; width: 0; height: 0; }
.toggle-slider {
  position: absolute;
  cursor: pointer;
  inset: 0;
  background: #2D3348;
  border-radius: 12px;
  transition: 0.3s;
}
.toggle-slider::before {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  bottom: 3px;
  background: white;
  border-radius: 50%;
  transition: 0.3s;
}
.toggle input:checked + .toggle-slider { background: #5B7FFF; }
.toggle input:checked + .toggle-slider::before { transform: translateX(20px); }
.toggle-label { font-size: 14px; color: #E8EAF0; }

/* Buttons */
.btn-primary {
  background: #5B7FFF;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-primary:hover { background: #7B9FFF; }
.btn-primary:disabled { background: #2D3348; color: #555A6E; cursor: not-allowed; }
.btn-secondary {
  background: #22263A;
  color: #8B90A0;
  border: 1px solid #2D3348;
  border-radius: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}
.btn-secondary:hover { background: #2D3348; color: #E8EAF0; }
</style>
