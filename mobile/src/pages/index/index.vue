<template>
  <view class="container">
    <view class="offline-tip" v-if="isOffline">
      <text>📴 当前离线，草稿暂存本地，网络恢复后自动同步</text>
    </view>

    <view class="draft-tip" v-if="hasPendingDrafts" @click="syncDrafts">
      <text>📤 有 {{ pendingDraftsCount }} 条待同步草稿，点击同步</text>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: currentTab === 'pending' }" @click="switchTab('pending')">
        待执行({{ pendingList.length }})
      </view>
      <view class="tab" :class="{ active: currentTab === 'in_progress' }" @click="switchTab('in_progress')">
        执行中({{ inProgressList.length }})
      </view>
      <view class="tab" :class="{ active: currentTab === 'overdue' }" @click="switchTab('overdue')">
        逾期({{ overdueList.length }})
      </view>
    </view>

    <scroll-view
      scroll-y
      class="list-container"
      @refresherrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
    >
      <view v-if="currentList.length === 0" class="empty-state">
        <text class="icon">📋</text>
        <text>暂无{{ tabLabel }}任务</text>
      </view>

      <view
        v-for="item in currentList"
        :key="item.id"
        class="card"
        @click="goExecute(item)"
      >
        <view class="card-header">
          <text class="title">{{ item.sopTitle || 'SOP' }}</text>
          <text class="tag" :class="getStatusTagClass(item.status)">
            {{ getStatusText(item.status) }}
          </text>
        </view>

        <view class="card-body">
          <view class="info-row">
            <text class="label">周期：</text>
            <text class="value">{{ formatPeriod(item) }}</text>
          </view>
          <view class="info-row" v-if="item.currentStep">
            <text class="label">进度：</text>
            <text class="value">第 {{ item.currentStep }} / {{ item.totalSteps || 0 }} 步</text>
          </view>
        </view>

        <view class="card-footer">
          <view class="progress-bar">
            <view class="progress-fill" :style="{ width: getProgress(item) + '%' }"></view>
          </view>
          <text class="progress-text">{{ getProgress(item) }}%</text>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script>


/**
 * 移动端首页 - 执行单列表
 * - 三个 Tab：待执行 / 执行中 / 逾期
 * - 支持下拉刷新、下拉加载
 * - 离线提示 + 草稿同步入口
 * - 根据 instanceId 判断是周期实例模式
 */
import api from '../../api'
import { useAuthStore } from '../../store/auth'
import { useDraftStore } from '../../store/draft'
import { checkNetwork, onNetworkChange } from '../../common/utils'

export default {
  data() {
    return {
      allInstances: [],
      currentTab: 'pending',
      refreshing: false,
      isOffline: false,
      hasPendingDrafts: false,
      pendingDraftsCount: 0
    }
  },

  computed: {
    pendingList() {
      return this.allInstances.filter(e => e.status === 'pending')
    },
    inProgressList() {
      return this.allInstances.filter(e => e.status === 'in_progress')
    },
    overdueList() {
      return this.allInstances.filter(e => e.status === 'overdue')
    },
    completedList() {
      return this.allInstances.filter(e => e.status === 'completed')
    },
    currentList() {
      if (this.currentTab === 'pending') return this.pendingList
      if (this.currentTab === 'in_progress') return this.inProgressList
      if (this.currentTab === 'overdue') return this.overdueList
      return this.completedList
    },
    tabLabel() {
      const map = { pending: '待执行', in_progress: '执行中', overdue: '逾期', completed: '已完成' }
      return map[this.currentTab] || ''
    }
  },

  onLoad() {
    this.checkNetworkStatus()
    // UniApp 的 onNetworkStatusChange 没有 off 方法，
    // 只在页面第一次加载时注册一次，用标志位防止重复
    if (!this._networkRegistered) {
      this._networkRegistered = true
      this._networkChangeCallback = (isConnected) => {
        this.isOffline = !isConnected
        if (isConnected && this.hasPendingDrafts) {
          this.syncDrafts()
        }
      }
      onNetworkChange(this._networkChangeCallback)
    }
  },

  onShow() {
    this.loadData()
  },

  onPullDownRefresh() {
    this.loadData()
  },

  methods: {
    async loadData() {
      const auth = useAuthStore()
      if (!auth.isLoggedIn) return

      try {
        const res = await api.instance.myInstances()
        this.allInstances = res.data || []

        const sopIds = [...new Set(this.allInstances.map(e => e.sopId))]
        // 并发请求所有 SOP 详情，避免串行等待
        const sopMap = {}
        await Promise.all(
          sopIds.map(async (sopId) => {
            try {
              const r = await api.sop.detail(sopId)
              if (r.code === 200) sopMap[sopId] = r.data
            } catch {}
          })
        )

        for (const inst of this.allInstances) {
          const sop = sopMap[inst.sopId]
          if (sop) {
            inst.sopTitle = sop.title
            try {
              const steps = (sop.content && sop.content !== 'null') ? JSON.parse(sop.content) : []
              inst.totalSteps = steps.length
            } catch { inst.totalSteps = 0 }
          } else {
            inst.sopTitle = 'SOP'
            inst.totalSteps = 0
          }
        }

        const draftStore = useDraftStore()
        this.hasPendingDrafts = draftStore.hasPendingDrafts
        this.pendingDraftsCount = draftStore.drafts.filter(d => !d.synced).length
      } catch (e) {
        console.error('加载数据失败:', e)
      } finally {
        uni.stopPullDownRefresh()
        this.refreshing = false
      }
    },

    switchTab(tab) {
      this.currentTab = tab
    },

    goExecute(item) {
      uni.navigateTo({ url: `/pages/execute/execute?instanceId=${item.id}&sopId=${item.sopId}` })
    },

    onRefresh() {
      this.refreshing = true
      this.loadData()
    },

    async syncDrafts() {
      const draftStore = useDraftStore()
      await draftStore.syncAll()
      this.hasPendingDrafts = draftStore.hasPendingDrafts
      this.pendingDraftsCount = 0
      this.loadData()
    },

    async checkNetworkStatus() {
      this.isOffline = !(await checkNetwork())
    },

    getProgress(item) {
      if (!item.currentStep || !item.totalSteps) return 0
      return Math.round((item.currentStep / item.totalSteps) * 100)
    },

    formatPeriod(item) {
      if (!item.periodStart || !item.periodEnd) return ''
      const s = new Date(item.periodStart)
      const e = new Date(item.periodEnd)
      const fmt = (d) => `${d.getMonth() + 1}/${d.getDate()}`
      return `${fmt(s)} ~ ${fmt(e)}`
    },

    getStatusText(status) {
      const map = { pending: '待执行', in_progress: '执行中', completed: '已完成', overdue: '已逾期' }
      return map[status] || status
    },

    getStatusTagClass(status) {
      const map = { pending: 'tag-pending', in_progress: 'tag-progress', completed: 'tag-completed', overdue: 'tag-overdue' }
      return map[status] || ''
    }
  }
}
</script>

<style scoped>
.container { min-height: 100vh; background: #F5F5F5; }
.offline-tip { background: #FFF7E6; color: #FA8C16; padding: 16rpx 30rpx; font-size: 24rpx; text-align: center; }
.draft-tip { background: #E6F7FF; color: #1890FF; padding: 16rpx 30rpx; font-size: 24rpx; text-align: center; }
.tabs { display: flex; background: #FFFFFF; padding: 0 30rpx; border-bottom: 1px solid #E5E5E5; }
.tab { flex: 1; height: 88rpx; line-height: 88rpx; text-align: center; font-size: 28rpx; color: #666; position: relative; }
.tab.active { color: #4A90E2; font-weight: bold; }
.tab.active::after { content: ''; position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 60rpx; height: 4rpx; background: #4A90E2; border-radius: 2rpx; }
.list-container { height: calc(100vh - 88rpx - 80rpx); padding: 20rpx 30rpx; }
.card { background: #FFFFFF; border-radius: 16rpx; padding: 30rpx; margin-bottom: 20rpx; box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.title { font-size: 32rpx; font-weight: bold; color: #333; flex: 1; margin-right: 20rpx; }
.tag { padding: 6rpx 16rpx; border-radius: 20rpx; font-size: 22rpx; }
.tag-pending { background: #FFF7E6; color: #FA8C16; }
.tag-progress { background: #E6F7FF; color: #1890FF; }
.tag-completed { background: #F6FFED; color: #52C41A; }
.tag-overdue { background: #FFF1F0; color: #F5222D; }
.card-body { margin-bottom: 20rpx; }
.info-row { display: flex; font-size: 26rpx; margin-bottom: 10rpx; }
.label { color: #999; width: 140rpx; }
.value { color: #333; flex: 1; }
.card-footer { display: flex; align-items: center; gap: 20rpx; }
.progress-bar { flex: 1; height: 8rpx; background: #E5E5E5; border-radius: 4rpx; }
.progress-fill { height: 100%; background: linear-gradient(90deg, #4A90E2, #67B3E8); border-radius: 4rpx; transition: width 0.3s; }
.progress-text { font-size: 24rpx; color: #999; width: 70rpx; text-align: right; }
.empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 150rpx 0; color: #999; }
.empty-state .icon { font-size: 120rpx; margin-bottom: 20rpx; }
</style>
