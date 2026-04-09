<template>
  <view class="container">
    <!-- 筛选 tabs -->
    <view class="filter-tabs">
      <view 
        class="tab" 
        :class="{ active: filterStatus === 'all' }"
        @click="filterStatus = 'all'"
      >全部</view>
      <view 
        class="tab" 
        :class="{ active: filterStatus === 'completed' }"
        @click="filterStatus = 'completed'"
      >已完成</view>
      <view 
        class="tab" 
        :class="{ active: filterStatus === 'abnormal' }"
        @click="filterStatus = 'abnormal'"
      >异常</view>
    </view>
    
    <!-- 记录列表 -->
    <scroll-view 
      scroll-y 
      class="list-container"
      @refresherrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
    >
      <view v-if="filteredList.length === 0" class="empty-state">
        <text class="icon">📜</text>
        <text>暂无执行记录</text>
      </view>
      
      <view 
        v-for="item in filteredList" 
        :key="item.id" 
        class="record-card"
        @click="viewDetail(item)"
      >
        <view class="card-header">
          <text class="title">{{ item.sopTitle || item.templateName }}</text>
          <text class="status-tag" :class="getStatusTagClass(item.status)">
            {{ item.status === 'abnormal' ? '异常' : getStatusText(item.status) }}
          </text>
        </view>
        
        <view class="card-body">
          <view class="info-row">
            <text class="label">执行时间：</text>
            <text class="value">{{ formatDateTime(item.startedAt, 'YYYY-MM-DD HH:mm') }}</text>
          </view>
          <view class="info-row">
            <text class="label">完成时间：</text>
            <text class="value">{{ item.completedAt ? formatDateTime(item.completedAt, 'YYYY-MM-DD HH:mm') : '-' }}</text>
          </view>
          <view class="info-row">
            <text class="label">执行时长：</text>
            <text class="value">{{ getDuration(item) }}</text>
          </view>
          <view class="info-row" v-if="item.exceptionNote">
            <text class="label">异常说明：</text>
            <text class="value text-error">{{ item.exceptionNote }}</text>
          </view>
        </view>
        
        <view class="card-footer">
          <text class="view-detail">查看详情 ></text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import api from '../../api'
import { formatDateTime, getStatusText, getStatusTagClass } from '../../common/utils'

export default {
  data() {
    return {
      recordsList: [],
      filterStatus: 'all',
      refreshing: false
    }
  },
  
  computed: {
    filteredList() {
      if (this.filterStatus === 'all') return this.recordsList
      if (this.filterStatus === 'completed') {
        return this.recordsList.filter(r => r.status === 'completed')
      }
      if (this.filterStatus === 'abnormal') {
        return this.recordsList.filter(r => r.status === 'abnormal' || r.hasException)
      }
      return this.recordsList
    }
  },
  
  onShow() {
    this.loadRecords()
  },
  
  onPullDownRefresh() {
    this.loadRecords()
  },
  
  methods: {
    async loadRecords() {
      try {
        const res = await api.instance.myInstances('completed')
        const records = res.data || []
        
        const sopIds = [...new Set(records.map(e => e.sopId))]
        const sopMap = {}
        for (const sopId of sopIds) {
          try {
            const r = await api.sop.detail(sopId)
            if (r.code === 200) sopMap[sopId] = r.data
          } catch {}
        }
        
        this.recordsList = records.map(r => {
          const sop = sopMap[r.sopId]
          return {
            ...r,
            sopTitle: sop?.title || 'SOP',
            startedAt: r.startedAt || r.createdAt,
          }
        })
      } catch (e) {
        console.error('加载记录失败:', e)
      } finally {
        uni.stopPullDownRefresh()
        this.refreshing = false
      }
    },
    
    viewDetail(item) {
      uni.navigateTo({ url: `/pages/execute/execute?instanceId=${item.id}&sopId=${item.sopId}` })
    },
    
    onRefresh() {
      this.refreshing = true
      this.loadRecords()
    },
    
    getDuration(item) {
      if (!item.startedAt || !item.completedAt) return '-'
      const start = new Date(item.startedAt)
      const end = new Date(item.completedAt)
      const diffMs = end - start
      const diffMins = Math.floor(diffMs / 60000)
      if (diffMins < 60) return `${diffMins}分钟`
      const hours = Math.floor(diffMins / 60)
      const mins = diffMins % 60
      return `${hours}小时${mins}分钟`
    },
    
    formatDateTime,
    getStatusText,
    getStatusTagClass
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #F5F5F5;
}

.filter-tabs {
  display: flex;
  background: #FFFFFF;
  padding: 0 30rpx;
  border-bottom: 1px solid #E5E5E5;
}

.tab {
  flex: 1;
  height: 88rpx;
  line-height: 88rpx;
  text-align: center;
  font-size: 28rpx;
  color: #666;
}

.tab.active {
  color: #4A90E2;
  font-weight: bold;
}

.tab.active::after {
  content: '';
  display: block;
  height: 4rpx;
  background: #4A90E2;
  margin: 0 30rpx;
}

.list-container {
  height: calc(100vh - 88rpx);
  padding: 20rpx 30rpx;
}

.record-card {
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
  margin-right: 20rpx;
}

.status-tag {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
}

.tag-completed { background: #F6FFED; color: #52C41A; }
.tag-overdue { background: #FFF1F0; color: #F5222D; }
.tag-progress { background: #E6F7FF; color: #1890FF; }

.card-body {
  margin-bottom: 16rpx;
}

.info-row {
  display: flex;
  font-size: 26rpx;
  margin-bottom: 10rpx;
}

.label {
  color: #999;
  width: 140rpx;
}

.value {
  color: #333;
  flex: 1;
}

.text-error {
  color: #F5222D;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16rpx;
  border-top: 1px solid #F0F0F0;
}

.view-detail {
  font-size: 26rpx;
  color: #4A90E2;
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
