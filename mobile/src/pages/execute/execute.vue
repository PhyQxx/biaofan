<template>
  <view class="container">
    <!-- SOP 详情头部 -->
    <view class="sop-header">
      <view class="title-row">
        <text class="title">{{ executionDetail.sopTitle || executionDetail.templateName }}</text>
        <text class="status-tag" :class="getStatusTagClass(executionDetail.status)">
          {{ getStatusText(executionDetail.status) }}
        </text>
      </view>
      <view class="info-row">
        <text class="info-item">📅 {{ formatDateTime(executionDetail.deadline, 'MM-DD HH:mm') }} 截止</text>
      </view>
      <view class="progress-row">
        <text class="progress-label">执行进度</text>
        <view class="progress-bar">
          <view class="progress-fill" :style="{ width: progressPercent + '%' }"></view>
        </view>
        <text class="progress-text">{{ executionDetail.currentStep || 0 }} / {{ executionDetail.totalSteps || executionDetail.steps?.length }}</text>
      </view>
    </view>
    
    <!-- 步骤列表 -->
    <view class="steps-section">
      <view 
        v-for="(step, index) in executionDetail.steps" 
        :key="step.id || index"
        class="step-item"
        :class="{ 
          'step-completed': step.status === 'completed',
          'step-current': step.index === currentStepIndex,
          'step-overdue': step.index < currentStepIndex && step.status !== 'completed'
        }"
      >
        <!-- 步骤节点 -->
        <view class="step-node">
          <view class="node-circle">
            <text v-if="step.status === 'completed'" class="node-check">✓</text>
            <text v-else>{{ step.index }}</text>
          </view>
          <view class="node-line" v-if="index < executionDetail.steps.length - 1"></view>
        </view>
        
        <!-- 步骤内容 -->
        <view class="step-content">
          <view class="step-header">
            <text class="step-title">{{ step.title }}</text>
            <text class="step-time" v-if="step.completedAt">
              {{ formatDateTime(step.completedAt, 'HH:mm') }}
            </text>
          </view>
          <view class="step-desc" v-if="step.description">{{ step.description }}</view>
          
          <!-- 打卡操作 -->
          <view class="step-action" v-if="step.index === currentStepIndex && executionDetail.status !== 'completed'">
            <view class="action-card">
              <view class="action-title">📸 打卡备注（可选）</view>
              <textarea 
                class="note-input" 
                v-model="stepNote" 
                placeholder="可输入执行说明或备注..."
                maxlength="200"
              />
              
              <!-- 拍照上传 -->
              <view class="photo-row">
                <view 
                  v-for="(photo, pIndex) in stepPhotos[step.index]" 
                  :key="pIndex"
                  class="photo-item"
                >
                  <image :src="photo" mode="aspectFill" class="photo-img"></image>
                  <view class="photo-del" @click="removePhoto(step.index, pIndex)">×</view>
                </view>
                <view class="photo-add" @click="takePhoto(step.index)" v-if="stepPhotos[step.index]?.length < 3">
                  <text class="add-icon">+</text>
                  <text class="add-text">拍照</text>
                </view>
              </view>
              
              <!-- 打卡按钮 -->
              <button 
                class="checkin-btn" 
                :disabled="checkingIn"
                @click="handleCheckIn(step)"
              >
                {{ checkingIn ? '打卡中...' : '确认打卡' }}
              </button>
            </view>
            
            <!-- 异常上报按钮 -->
            <button class="exception-btn" @click="showExceptionModal(step)">
              ⚠️ 上报异常
            </button>
          </view>
          
          <!-- 已完成标记 -->
          <view class="completed-tip" v-if="step.status === 'completed'">
            <text>✅ 已完成</text>
            <text v-if="step.note" class="step-note">备注：{{ step.note }}</text>
          </view>
        </view>
      </view>
    </view>
    
    <!-- 完成执行按钮 -->
    <view class="finish-section" v-if="canFinish">
      <button class="finish-btn" @click="handleFinish">完成执行</button>
    </view>
    
    <!-- 异常上报弹窗 -->
    <view class="modal-mask" v-if="showException" @click="showException = false">
      <view class="modal-content" @click.stop>
        <view class="modal-title">上报异常</view>
        <view class="modal-body">
          <textarea 
            class="exception-input" 
            v-model="exceptionDesc" 
            placeholder="请详细描述异常情况..."
            maxlength="500"
          />
          
          <view class="photo-row">
            <view 
              v-for="(photo, pIndex) in exceptionPhotos" 
              :key="pIndex"
              class="photo-item"
            >
              <image :src="photo" mode="aspectFill" class="photo-img"></image>
              <view class="photo-del" @click="exceptionPhotos.splice(pIndex, 1)">×</view>
            </view>
            <view class="photo-add" @click="takeExceptionPhoto" v-if="exceptionPhotos.length < 5">
              <text class="add-icon">+</text>
              <text class="add-text">拍照</text>
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @click="showException = false">取消</button>
          <button class="submit-btn" @click="submitException">提交</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import api from '../../api'
import { useAuthStore } from '../../store/auth'
import { useDraftStore } from '../../store/draft'
import { 
  formatDateTime, 
  getStatusText, 
  getStatusTagClass, 
  isOverdue,
  checkNetwork 
} from '../../common/utils'

export default {
  data() {
    return {
      executionId: null,
      instanceId: null,
      sopId: null,
      isInstanceMode: false,
      executionDetail: {
        steps: []
      },
      currentStepIndex: 1,
      stepNote: '',
      stepPhotos: {},
      checkingIn: false,
      showException: false,
      exceptionStep: null,
      exceptionDesc: '',
      exceptionPhotos: []
    }
  },
  
  computed: {
    progressPercent() {
      if (!this.executionDetail.totalSteps && !this.executionDetail.steps?.length) return 0
      const total = this.executionDetail.totalSteps || this.executionDetail.steps.length
      return Math.round((this.currentStepIndex / total) * 100)
    },
    canFinish() {
      if (this.executionDetail.status === 'completed') return false
      return this.executionDetail.steps?.every(s => s.status === 'completed')
    }
  },
  
  onLoad(options) {
      if (options.instanceId) {
        this.instanceId = parseInt(options.instanceId)
        this.sopId = parseInt(options.sopId)
        this.isInstanceMode = true
        this.loadInstanceDetail()
      } else if (options.id) {
        this.executionId = parseInt(options.id)
        this.loadDetail()
      }
    },
  
  methods: {
    async loadDetail() {
      uni.showLoading({ title: '加载中...' })
      try {
        const res = await api.execution.getSteps(this.executionId)
        this.executionDetail = res.data || res
        
        const pendingStep = this.executionDetail.steps?.find(s => s.status === 'pending')
        this.currentStepIndex = pendingStep?.index || this.executionDetail.steps?.length || 1
        
        this.stepPhotos = {}
        this.executionDetail.steps?.forEach(s => {
          this.stepPhotos[s.index] = []
        })
        
        if (this.executionDetail.status === 'pending') {
          await this.startExecution()
        }
      } catch (e) {
        console.error('加载执行详情失败:', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    
    async loadInstanceDetail() {
      uni.showLoading({ title: '加载中...' })
      try {
        const instRes = await api.instance.detail(this.instanceId)
        const instData = instRes.data?.instance || instRes.data
        this.executionDetail = { steps: [], sopTitle: '' }
        
        if (instData.status === 'pending') {
          await api.instance.activate(this.instanceId)
          instData.status = 'in_progress'
          instData.currentStep = 1
        }
        
        this.currentStepIndex = instData.currentStep || 1
        
        const sopRes = await api.sop.detail(this.sopId)
        if (sopRes.code === 200 && sopRes.data) {
          this.executionDetail.sopTitle = sopRes.data.title
          let steps = []
          try {
            const raw = sopRes.data.content
            steps = (raw && raw !== 'null') ? JSON.parse(raw) : []
          } catch {}
          this.executionDetail.steps = steps.map((s, i) => ({
            ...s,
            index: i + 1,
            status: i + 1 < this.currentStepIndex ? 'completed' : (i + 1 === this.currentStepIndex ? 'pending' : 'pending')
          }))
          this.executionDetail.totalSteps = steps.length
        }
        
        this.stepPhotos = {}
        this.executionDetail.steps?.forEach(s => {
          this.stepPhotos[s.index] = []
        })
      } catch (e) {
        console.error('加载实例详情失败:', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    
    async startExecution() {
      try {
        await api.execution.start(this.executionId)
        this.executionDetail.status = 'in_progress'
      } catch (e) {
        console.error('开始执行失败:', e)
      }
    },
    
    async handleCheckIn(step) {
      this.checkingIn = true
      const isOnline = await checkNetwork()
      
      try {
        if (isOnline) {
          if (this.isInstanceMode) {
            await api.instance.completeStep(this.instanceId, step.index, {
              notes: this.stepNote
            })
          } else {
            let photoUrl = ''
            if (this.stepPhotos[step.index]?.length > 0) {
              const uploadRes = await api.upload.image(this.stepPhotos[step.index][0])
              photoUrl = uploadRes.data?.url || ''
            }
            await api.execution.completeStep(this.executionId, step.id || step.index, {
              note: this.stepNote,
              photoUrl
            })
          }
          uni.showToast({ title: '打卡成功', icon: 'success' })
        } else {
          // 离线：保存到本地草稿
          const draftStore = useDraftStore()
          draftStore.addDraft(
            this.executionId,
            step.index,
            this.stepPhotos[step.index]?.[0] || '',
            this.stepNote
          )
          uni.showToast({ title: '已保存草稿，网络恢复后自动同步', icon: 'success' })
        }
        
        // 更新本地状态
        step.status = 'completed'
        step.completedAt = new Date().toISOString()
        step.note = this.stepNote
        
        // 清理当前步骤数据
        this.stepNote = ''
        this.stepPhotos[step.index] = []
        
        // 更新当前步骤索引
        const nextStep = this.executionDetail.steps?.find(s => s.status === 'pending')
        this.currentStepIndex = nextStep?.index || this.executionDetail.steps?.length + 1
        
      } catch (e) {
        console.error('打卡失败:', e)
        // 即使在线失败，也保存草稿
        const draftStore = useDraftStore()
        draftStore.addDraft(
          this.executionId,
          step.index,
          this.stepPhotos[step.index]?.[0] || '',
          this.stepNote
        )
        uni.showToast({ title: '已保存草稿', icon: 'success' })
      } finally {
        this.checkingIn = false
      }
    },
    
    async handleFinish() {
      if (!this.canFinish) return
      
      uni.showModal({
        title: '确认完成',
        content: '确认已完成所有步骤？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '提交中...' })
              await (this.isInstanceMode ? api.instance.finish(this.instanceId) : api.execution.finish(this.executionId))
              uni.showToast({ title: '执行完成', icon: 'success' })
              setTimeout(() => {
                uni.navigateBack()
              }, 1500)
            } catch (e) {
              uni.showToast({ title: '提交失败', icon: 'none' })
            } finally {
              uni.hideLoading()
            }
          }
        }
      })
    },
    
    takePhoto(stepIndex) {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        success: (res) => {
          if (!this.stepPhotos[stepIndex]) {
            this.$set(this.stepPhotos, stepIndex, [])
          }
          this.stepPhotos[stepIndex].push(res.tempFilePaths[0])
        }
      })
    },
    
    removePhoto(stepIndex, pIndex) {
      this.stepPhotos[stepIndex].splice(pIndex, 1)
    },
    
    showExceptionModal(step) {
      this.exceptionStep = step
      this.exceptionDesc = ''
      this.exceptionPhotos = []
      this.showException = true
    },
    
    takeExceptionPhoto() {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        success: (res) => {
          this.exceptionPhotos.push(res.tempFilePaths[0])
        }
      })
    },
    
    async submitException() {
      if (!this.exceptionDesc.trim()) {
        uni.showToast({ title: '请输入异常描述', icon: 'none' })
        return
      }
      
      uni.showLoading({ title: '提交中...' })
      try {
        let photoUrl = ''
        if (this.exceptionPhotos.length > 0) {
          const uploadRes = await api.upload.image(this.exceptionPhotos[0])
          photoUrl = uploadRes.data?.url || ''
        }
        
        await api.execution.reportException(this.executionId, {
          stepId: this.exceptionStep?.id || this.exceptionStep?.index,
          description: this.exceptionDesc,
          photoUrl
        })
        
        uni.showToast({ title: '异常已上报', icon: 'success' })
        this.showException = false
      } catch (e) {
        console.error('异常上报失败:', e)
        uni.showToast({ title: '上报失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
    
    formatDateTime,
    getStatusText,
    getStatusTagClass,
    isOverdue
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 150rpx;
}

.sop-header {
  background: linear-gradient(135deg, #4A90E2 0%, #67B3E8 100%);
  padding: 30rpx;
  color: #FFFFFF;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16rpx;
}

.title {
  font-size: 36rpx;
  font-weight: bold;
  flex: 1;
  margin-right: 20rpx;
}

.status-tag {
  padding: 8rpx 20rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.2);
}

.info-row {
  margin-bottom: 20rpx;
}

.info-item {
  font-size: 26rpx;
  opacity: 0.9;
}

.progress-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.progress-label {
  font-size: 24rpx;
  opacity: 0.9;
}

.progress-bar {
  flex: 1;
  height: 8rpx;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 4rpx;
}

.progress-fill {
  height: 100%;
  background: #FFFFFF;
  border-radius: 4rpx;
}

.progress-text {
  font-size: 24rpx;
  opacity: 0.9;
}

.steps-section {
  padding: 30rpx;
}

.step-item {
  display: flex;
  margin-bottom: 0;
}

.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-right: 20rpx;
}

.node-circle {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #E5E5E5;
  color: #999;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26rpx;
  font-weight: bold;
  flex-shrink: 0;
}

.step-current .node-circle {
  background: #4A90E2;
  color: #FFFFFF;
  box-shadow: 0 4rpx 12rpx rgba(74, 144, 226, 0.4);
}

.step-completed .node-circle {
  background: #52C41A;
  color: #FFFFFF;
}

.node-check {
  font-size: 28rpx;
}

.node-line {
  width: 4rpx;
  flex: 1;
  min-height: 80rpx;
  background: #E5E5E5;
  margin: 8rpx 0;
}

.step-completed .node-line {
  background: #52C41A;
}

.step-content {
  flex: 1;
  background: #FFFFFF;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12rpx;
}

.step-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.step-time {
  font-size: 24rpx;
  color: #999;
}

.step-desc {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin-bottom: 16rpx;
}

.step-action {
  margin-top: 20rpx;
  border-top: 1px solid #E5E5E5;
  padding-top: 20rpx;
}

.action-card {
  background: #F8F8F8;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 20rpx;
}

.action-title {
  font-size: 26rpx;
  color: #666;
  margin-bottom: 12rpx;
}

.note-input {
  width: 100%;
  height: 120rpx;
  background: #FFFFFF;
  border-radius: 8rpx;
  padding: 16rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  margin-bottom: 16rpx;
}

.photo-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-bottom: 20rpx;
}

.photo-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
}

.photo-img {
  width: 100%;
  height: 100%;
  border-radius: 8rpx;
}

.photo-del {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 40rpx;
  height: 40rpx;
  background: #F5222D;
  color: #FFFFFF;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
}

.photo-add {
  width: 160rpx;
  height: 160rpx;
  background: #F5F5F5;
  border: 2rpx dashed #D9D9D9;
  border-radius: 8rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.add-icon {
  font-size: 48rpx;
  color: #999;
  line-height: 1;
}

.add-text {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
}

.checkin-btn {
  width: 100%;
  height: 88rpx;
  background: linear-gradient(135deg, #4A90E2 0%, #67B3E8 100%);
  color: #FFFFFF;
  border-radius: 44rpx;
  font-size: 32rpx;
  font-weight: bold;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.checkin-btn[disabled] {
  background: #CCCCCC;
}

.exception-btn {
  width: 100%;
  height: 80rpx;
  background: #FFFFFF;
  color: #F5222D;
  border: 2rpx solid #F5222D;
  border-radius: 40rpx;
  font-size: 28rpx;
  border: none;
}

.completed-tip {
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: #52C41A;
  font-size: 26rpx;
  margin-top: 12rpx;
}

.step-note {
  color: #999;
  font-size: 24rpx;
}

.finish-section {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  background: #FFFFFF;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.finish-btn {
  width: 100%;
  height: 96rpx;
  background: linear-gradient(135deg, #52C41A 0%, #73D13D 100%);
  color: #FFFFFF;
  border-radius: 48rpx;
  font-size: 34rpx;
  font-weight: bold;
  border: none;
}

/* 异常上报弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  background: #FFFFFF;
  border-radius: 24rpx 24rpx 0 0;
  max-height: 80vh;
}

.modal-title {
  text-align: center;
  font-size: 34rpx;
  font-weight: bold;
  padding: 30rpx;
  border-bottom: 1px solid #E5E5E5;
}

.modal-body {
  padding: 30rpx;
  max-height: 50vh;
  overflow-y: auto;
}

.exception-input {
  width: 100%;
  height: 200rpx;
  background: #F5F5F5;
  border-radius: 12rpx;
  padding: 20rpx;
  font-size: 28rpx;
  box-sizing: border-box;
  margin-bottom: 20rpx;
}

.modal-footer {
  display: flex;
  gap: 20rpx;
  padding: 20rpx 30rpx 50rpx;
}

.cancel-btn, .submit-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  font-size: 32rpx;
  border: none;
}

.cancel-btn {
  background: #F5F5F5;
  color: #666;
}

.submit-btn {
  background: #F5222D;
  color: #FFFFFF;
}
</style>
