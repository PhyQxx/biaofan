<template>
  <view class="container">
    <!-- 全部已读按钮 -->
    <view class="header-actions" v-if="notifications.length > 0">
      <text class="mark-all" @click="markAllRead">全部标为已读</text>
    </view>

    <!-- 通知列表 -->
    <scroll-view
      scroll-y
      class="list-container"
      @refresherrefresh="onRefresh"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
    >
      <view v-if="notifications.length === 0" class="empty-state">
        <text class="icon">🔔</text>
        <text>暂无通知</text>
      </view>

      <view
        v-for="item in notifications"
        :key="item.id"
        class="notification-card"
        :class="{ unread: !item.isRead }"
        @click="handleNotification(item)"
      >
        <view class="notif-icon">
          <text v-if="getNotifIcon(item.type) === 'task'" class="icon-text">📋</text>
          <text v-else-if="getNotifIcon(item.type) === 'alert'" class="icon-text">🚨</text>
          <text v-else-if="getNotifIcon(item.type) === 'exception'" class="icon-text">⚠️</text>
          <text v-else class="icon-text">🔔</text>
        </view>

        <view class="notif-content">
          <view class="notif-header">
            <text class="notif-title">{{ item.title }}</text>
            <text class="notif-time">{{ relativeTime(item.createdAt) }}</text>
          </view>
          <!--
            XSS 安全说明：
            Vue 的 {{ }} 插值默认会对内容进行 HTML 转义，防止 XSS 攻击。
            此处使用 {{ item.content }} 而非 v-html="item.content" 是正确的做法。
            如果未来需要支持富文本显示，请确保后端返回的内容已经过严格过滤。
          -->
          <view class="notif-body">{{ item.content }}</view>
          <view class="notif-action" v-if="item.actionUrl">
            <text>点击查看 ></text>
          </view>
        </view>

        <view class="unread-dot" v-if="!item.isRead"></view>
      </view>
    </scroll-view>
  </view>
</template>

<script>


/**
 * 移动端通知列表页
 * - 显示通知图标（任务/告警/异常）
 * - 未读红点标记，点击标记已读
 * - 全部标为已读功能
 */
import api from '../../api'
import { relativeTime } from '../../common/utils'

export default {
  data() {
    return {
      notifications: [],
      refreshing: false
    }
  },

  onShow() {
    this.loadNotifications()
  },

  onPullDownRefresh() {
    this.loadNotifications()
  },

  methods: {
    async loadNotifications() {
      try {
        const res = await api.notification.list()
        this.notifications = res.data || []
      } catch (e) {
        console.error('加载通知失败:', e)
      } finally {
        uni.stopPullDownRefresh()
        this.refreshing = false
      }
    },

    async handleNotification(item) {
      if (!item.isRead) {
        try {
          await api.notification.markRead(item.id)
          item.isRead = true
        } catch (e) {
          console.error('标记已读失败:', e)
        }
      }

      // item.instanceId 和 item.sopId 由后端通知数据填充
      // 如果没有这两个字段，则用 sourceId 作为 instanceId尝试加载
      if (item.sourceType === 'execution' || item.sourceType === 'instance') {
        const instanceId = item.instanceId || item.sourceId
        if (instanceId) {
          if (item.sopId) {
            uni.navigateTo({ url: `/pages/execute/execute?instanceId=${instanceId}&sopId=${item.sopId}` })
          } else {
            // 没有 sopId 时，先拉取实例详情获取 sopId，再跳转
            uni.navigateTo({ url: `/pages/execute/execute?instanceId=${instanceId}` })
          }
          return
        }
      }
    },

    async markAllRead() {
      try {
        await api.notification.markAllRead()
        this.notifications.forEach(n => {
          n.isRead = true
        })
        uni.showToast({ title: '已全部标为已读', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    onRefresh() {
      this.refreshing = true
      this.loadNotifications()
    },

    getNotifIcon(type) {
      const iconMap = {
        task: 'task',
        alert: 'alert',
        exception: 'exception',
        reminder: 'reminder'
      }
      return iconMap[type] || 'default'
    },

    relativeTime
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #F5F5F5;
}

.header-actions {
  display: flex;
  justify-content: flex-end;
  padding: 20rpx 30rpx;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E5E5;
}

.mark-all {
  font-size: 26rpx;
  color: #4A90E2;
}

.list-container {
  height: calc(100vh - 60rpx);
  padding: 20rpx 30rpx;
}

.notification-card {
  display: flex;
  background: #FFFFFF;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
  position: relative;
}

.notification-card.unread {
  background: #F0F7FF;
}

.notif-icon {
  width: 80rpx;
  height: 80rpx;
  background: #F5F5F5;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20rpx;
  flex-shrink: 0;
}

.icon-text {
  font-size: 36rpx;
}

.notif-content {
  flex: 1;
}

.notif-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10rpx;
}

.notif-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
  flex: 1;
  margin-right: 20rpx;
}

.notif-time {
  font-size: 22rpx;
  color: #999;
  flex-shrink: 0;
}

.notif-body {
  font-size: 26rpx;
  color: #666;
  line-height: 1.5;
  margin-bottom: 10rpx;
}

.notif-action {
  font-size: 24rpx;
  color: #4A90E2;
}

.unread-dot {
  position: absolute;
  top: 30rpx;
  right: 30rpx;
  width: 16rpx;
  height: 16rpx;
  background: #F5222D;
  border-radius: 50%;
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
