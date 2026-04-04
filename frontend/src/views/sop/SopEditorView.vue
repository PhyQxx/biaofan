<template>
  <div class="editor-page">
    <!-- Topbar -->
    <div class="editor-topbar">
      <div class="topbar-left">
        <button class="btn-back" @click="router.back()">← 返回</button>
        <input v-model="form.title" class="title-input" placeholder="输入 SOP 标题..." />
      </div>
      <div class="topbar-right">
        <button v-if="isEdit" class="btn-versions" @click="router.push(`/sop/${route.params.id}/versions`)">📋 版本历史</button>
        <button class="btn-secondary" @click="handleSave('draft')">保存草稿</button>
        <button class="btn-primary" @click="handleSave('published')">发布 SOP</button>
      </div>
    </div>

    <div class="editor-body">
      <!-- Meta -->
      <div class="meta-row">
        <div class="meta-item">
          <label>分类</label>
          <select v-model="form.category" class="meta-select">
            <option value="工作">工作</option>
            <option value="生活">生活</option>
            <option value="学习">学习</option>
            <option value="健康">健康</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <div class="meta-item">
          <label>标签</label>
          <input v-model="tagsInput" class="meta-input" placeholder="多个标签用逗号分隔" />
        </div>
      </div>

      <div class="form-group">
        <label>描述</label>
        <textarea v-model="form.description" class="desc-input" placeholder="简要描述这个 SOP..."></textarea>
      </div>

      <!-- Steps -->
      <div class="steps-section">
        <div class="steps-header">
          <h3>步骤列表</h3>
          <button class="btn-add-step" @click="addStep">+ 添加步骤</button>
        </div>

        <div v-for="(step, index) in form.content" :key="index" class="step-card">
          <div class="step-num">{{ index + 1 }}</div>
          <div class="step-content">
            <input v-model="step.title" class="step-title-input" placeholder="步骤标题" />
            <textarea v-model="step.description" class="step-desc-input" placeholder="步骤说明..."></textarea>
            <div class="step-meta">
              <label>预计耗时（分钟）</label>
              <input v-model.number="step.duration" type="number" class="step-duration" placeholder="30" />
            </div>
          </div>
          <button class="btn-remove-step" @click="removeStep(index)">✕</button>
        </div>

        <div v-if="form.content.length === 0" class="empty-steps">
          <p>还没有步骤，点击上方"添加步骤"开始</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSopStore } from '@/stores/sop'

const route = useRoute()
const router = useRouter()
const sopStore = useSopStore()

const isEdit = !!route.params.id
const tagsInput = ref('')

const form = reactive({
  title: '',
  description: '',
  category: '工作',
  content: [] as { title: string; description: string; duration: number }[],
  status: 'draft',
})

const addStep = () => {
  form.content.push({ title: '', description: '', duration: 30 })
}

const removeStep = (index: number) => {
  form.content.splice(index, 1)
}

const handleSave = async (status: string) => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  const tags = tagsInput.value.split(',').map(t => t.trim()).filter(Boolean)
  const data = {
    ...form,
    tags,
    status,
  }
  try {
    if (isEdit) {
      await sopStore.updateSop(Number(route.params.id), data)
      ElMessage.success('保存成功')
    } else {
      await sopStore.createSop(data)
      ElMessage.success('创建成功')
    }
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

onMounted(async () => {
  if (isEdit) {
    const res: any = await sopStore.getSopById(Number(route.params.id))
    if (res.code === 200) {
      const sop = res.data
      form.title = sop.title
      form.description = sop.description || ''
      form.category = sop.category || '工作'
      form.content = sop.content ? JSON.parse(sop.content) : []
      tagsInput.value = sop.tags ? JSON.parse(sop.tags).join(',') : ''
    }
  }
})
</script>

<style scoped>
.editor-page { min-height: 100vh; background: #F5F7FA; }
.editor-topbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 24px; background: #fff;
  border-bottom: 1px solid #E8E8E8; gap: 16px;
}
.topbar-left { display: flex; align-items: center; gap: 12px; flex: 1; }
.topbar-right { display: flex; gap: 8px; }
.btn-back {
  background: none; border: none; font-size: 14px; color: #666;
  cursor: pointer; padding: 6px 12px; border-radius: 6px;
}
.btn-back:hover { background: #F5F7FA; }
.title-input {
  flex: 1; font-size: 18px; font-weight: 600; border: none; outline: none;
  background: transparent; color: #212121;
}
.title-input::placeholder { color: #BDBDBD; }
.btn-secondary {
  height: 36px; padding: 0 16px;
  background: #fff; color: #333;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 14px; font-weight: 500; cursor: pointer;
}
.btn-secondary:hover { background: #F5F7FA; }
.btn-primary {
  height: 36px; padding: 0 16px;
  background: #5B7FFF; color: white;
  border: none; border-radius: 8px;
  font-size: 14px; font-weight: 600; cursor: pointer;
}
.btn-primary:hover { background: #7994FF; }
.btn-versions {
  height: 36px; padding: 0 14px;
  background: #fff; color: #5B7FFF;
  border: 1.5px solid #5B7FFF; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-versions:hover { background: #E8ECFF; }
.editor-body { max-width: 860px; margin: 24px auto; padding: 0 24px; }
.meta-row { display: flex; gap: 16px; margin-bottom: 16px; }
.meta-item { flex: 1; }
.meta-item label, .form-group label { display: block; font-size: 13px; font-weight: 500; color: #666; margin-bottom: 6px; }
.meta-select, .meta-input, .desc-input {
  width: 100%; height: 40px; padding: 0 12px;
  border: 1.5px solid #E8E8E8; border-radius: 8px;
  font-size: 14px; outline: none; box-sizing: border-box;
  background: #fff;
}
.meta-select:focus, .meta-input:focus, .desc-input:focus {
  border-color: #5B7FFF;
  box-shadow: 0 0 0 3px rgba(91,127,255,0.10);
}
.desc-input { height: 80px; padding: 10px 12px; resize: vertical; }
.form-group { margin-bottom: 20px; }
.steps-section { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.steps-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.steps-header h3 { margin: 0; font-size: 15px; font-weight: 600; color: #212121; }
.btn-add-step {
  height: 32px; padding: 0 14px;
  background: #E8ECFF; color: #5B7FFF;
  border: none; border-radius: 8px;
  font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-add-step:hover { background: #D0D8FF; }
.step-card {
  display: flex; gap: 12px; margin-bottom: 12px;
  padding: 14px; background: #F5F7FA; border-radius: 8px;
}
.step-num {
  width: 28px; height: 28px; background: #5B7FFF; color: white;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.step-content { flex: 1; }
.step-title-input {
  width: 100%; height: 36px; padding: 0 10px;
  border: 1px solid #E8E8E8; border-radius: 6px;
  font-size: 14px; font-weight: 500; outline: none; margin-bottom: 8px; box-sizing: border-box;
  background: #fff;
}
.step-desc-input {
  width: 100%; height: 60px; padding: 8px 10px;
  border: 1px solid #E8E8E8; border-radius: 6px;
  font-size: 13px; outline: none; resize: vertical; margin-bottom: 8px; box-sizing: border-box;
  background: #fff;
}
.step-meta { display: flex; align-items: center; gap: 8px; }
.step-meta label { font-size: 12px; color: #999; margin: 0; }
.step-duration { width: 80px; height: 28px; padding: 0 8px; border: 1px solid #E8E8E8; border-radius: 6px; font-size: 13px; outline: none; background: #fff; }
.btn-remove-step {
  background: none; border: none; color: #FF4D4F;
  font-size: 16px; cursor: pointer; padding: 4px;
}
.empty-steps { text-align: center; padding: 24px; color: #999; }
</style>
