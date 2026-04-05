<template>
  <view class="container">
    <view class="header-tip">
      <text>📱 当前有 {{ pendingDrafts.length }} 条待同步草稿</text>
    </view>
    
    <view class="sync-all" v-if="pendingDrafts.length > 0">
      <button class="sync-btn" :disabled="syncing" @click="syncAll">
        {{ syncing ? '同步中...' : '全部同步' }}
      </button>
    </view>
    
    <scroll-view scroll-y class="list-container">
      <view v-if="allDrafts.length === 0" class="empty-state">
        <text class="icon">📝</text>
        <text>暂无草稿</text>
      </view>
      
      <view 
        v-for="draft in allDrafts" 
        :key="draft.id" 
        class="draft-card"
      >
        <view class="card-header">
          <text class="execution-id">执行单 #{{ draft.executionId }}</text>
          <text class="status" :class="draft.synced ? 'synced' : 'pending'">
            {{ draft.synced ? '已同步' : '待同步' }}
          </text>
        </view>
        
        <view class="card-body">
          <view class="info-row">
            <text class="label">步骤：</text>
            <text class="value">第 {{ draft.stepIndex }} 步</text>
          </view>
          <view class="info-row">
            <text class="label">备注：</text>
            <text class="value">{{ draft.note || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">本地时间：</text>
            <text class="value">{{ formatDateTime(draft.localTimestamp) }}</text>
          </view>
          <view class="info-row" v-if="draft.retryCount > 0">
            <text class="label">重试次数：</text>
            <text class="value text-error">{{ draft.retryCount }} 次</text>
          </view>
        </view>
        
        <view class="card-footer" v-if="!draft.synced">
          <button class="sync-one-btn" :disabled="syncing" @click="syncOne(draft.id)">
            立即同步
          </button>
          <button class="del-btn" @click="deleteDraft(draft.id)">删除</button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { useDraftStore } from '../../store/draft'
import { formatDateTime } from '../../common/utils'

export default {
  data() {
    return {
      syncing: false
    }
  },
  
  computed: {
    allDrafts() {
      const draftStore = useDraftStore()
      return draftStore.drafts
    },
    pendingDrafts() {
      return this.allDrafts.filter(d => !d.synced)
    }
  },
  
  methods: {
    async syncAll() {
      this.syncing = true
      const draftStore = useDraftStore()
      await draftStore.syncAll()
      this.syncing = false
    },
    
    async syncOne(draftId) {
      this.syncing = true
      const draftStore = useDraftStore()
      try {
        await draftStore.syncOne(draftId)
      } catch (e) {
        // 错误已由 store 处理
      }
      this.syncing = false
    },
    
    deleteDraft(draftId) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这条草稿吗？',
        success: (res) => {
          if (res.confirm) {
            const draftStore = useDraftStore()
            draftStore.removeDraft(draftId)
          }
        }
      })
    },
    
    formatDateTime
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #F5F5F5;
}

.header-tip {
  background: #E6F7FF;
  color: #1890FF;
  padding: 20rpx 30rpx;
  text-align: center;
  font-size: 26rpx;
}

.sync-all {
  padding: 20rpx 30rpx;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E5E5;
}

.sync-btn {
  width: 100%;
  height: 80rpx;
  background: #4A90E2;
  color: #FFFFFF;
  border-radius: 40rpx;
  font-size: 30rpx;
  border: none;
}

.sync-btn[disabled] {
  background: #CCCCCC;
}

.list-container {
  height: calc(100vh - 200rpx);
  padding: 20rpx 30rpx;
}

.draft-card {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
  padding-bottom: 16rpx;
  border-bottom: 1px solid #F0F0F0;
}

.execution-id {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}

.status {
  font-size: 24rpx;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
}

.status.synced {
  background: #F6FFED;
  color: #52C41A;
}

.status.pending {
  background: #FFF7E6;
  color: #FA8C16;
}

.card-body {
  margin-bottom: 16rpx;
}

.info-row {
  display: flex;
  font-size: 26rpx;
  margin-bottom: 8rpx;
}

.label {
  color: #999;
  width: 120rpx;
}

.value {
  flex: 1;
  color: #333;
}

.text-error {
  color: #F5222D;
}

.card-footer {
  display: flex;
  gap: 20rpx;
  padding-top: 16rpx;
  border-top: 1px solid #F0F0F0;
}

.sync-one-btn, .del-btn {
  flex: 1;
  height: 72rpx;
  border-radius: 36rpx;
  font-size: 26rpx;
  border: none;
}

.sync-one-btn {
  background: #4A90E2;
  color: #FFFFFF;
}

.del-btn {
  background: #F5F5F5;
  color: #666;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 150rpx 0;
  color: #999;
}

.empty-state .icon {
  font-size: 120rpx;
  margin-bottom: 20rpx;
}
</style>
