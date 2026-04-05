<template>
  <view class="container">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="avatar">
        <image 
          v-if="userInfo.avatar" 
          :src="userInfo.avatar" 
          class="avatar-img"
        />
        <text v-else class="avatar-text">{{ userInfo.nickname?.[0] || userInfo.phone?.[0] || '我' }}</text>
      </view>
      <view class="user-info">
        <text class="nickname">{{ userInfo.nickname || '用户' + (userInfo.phone?.slice(-4) || '') }}</text>
        <text class="phone">{{ userInfo.phone || '' }}</text>
      </view>
      <text class="edit-btn" @click="goEditProfile">编辑 ></text>
    </view>
    
    <!-- 待同步草稿入口 -->
    <view class="menu-section" v-if="hasPendingDrafts">
      <view class="menu-item" @click="goDrafts">
        <view class="menu-left">
          <text class="menu-icon">📤</text>
          <text class="menu-text">待同步草稿</text>
        </view>
        <view class="menu-right">
          <text class="badge">{{ pendingDraftsCount }}</text>
          <text class="arrow">></text>
        </view>
      </view>
    </view>
    
    <!-- 功能菜单 -->
    <view class="menu-section">
      <view class="menu-item" @click="goSetting('notification')">
        <view class="menu-left">
          <text class="menu-icon">🔔</text>
          <text class="menu-text">通知设置</text>
        </view>
        <view class="menu-right">
          <switch 
            :checked="notifyEnabled" 
            @change="toggleNotify"
            color="#4A90E2"
          />
        </view>
      </view>
      
      <view class="menu-item" @click="goSetting('about')">
        <view class="menu-left">
          <text class="menu-icon">ℹ️</text>
          <text class="menu-text">关于我们</text>
        </view>
        <view class="menu-right">
          <text class="arrow">></text>
        </view>
      </view>
      
      <view class="menu-item" @click="goSetting('help')">
        <view class="menu-left">
          <text class="menu-icon">❓</text>
          <text class="menu-text">帮助与反馈</text>
        </view>
        <view class="menu-right">
          <text class="arrow">></text>
        </view>
      </view>
    </view>
    
    <!-- 退出登录 -->
    <view class="logout-section">
      <button class="logout-btn" @click="handleLogout">退出登录</button>
    </view>
    
    <!-- 版本信息 -->
    <view class="version-info">
      <text>标帆 SOP v1.0.0</text>
    </view>
  </view>
</template>

<script>
import { useAuthStore } from '../../store/auth'
import { useDraftStore } from '../../store/draft'

export default {
  data() {
    return {
      userInfo: {},
      notifyEnabled: true,
      hasPendingDrafts: false,
      pendingDraftsCount: 0
    }
  },
  
  onShow() {
    this.loadUserInfo()
    this.checkDrafts()
  },
  
  methods: {
    loadUserInfo() {
      const auth = useAuthStore()
      const stored = uni.getStorageSync('userInfo')
      if (stored) {
        try {
          this.userInfo = JSON.parse(stored)
        } catch (e) {
          this.userInfo = { phone: auth.userId }
        }
      } else {
        this.userInfo = { phone: auth.userId }
      }
    },
    
    checkDrafts() {
      const draftStore = useDraftStore()
      this.hasPendingDrafts = draftStore.hasPendingDrafts
      this.pendingDraftsCount = draftStore.drafts.filter(d => !d.synced).length
    },
    
    goEditProfile() {
      uni.showToast({ title: '功能开发中', icon: 'none' })
    },
    
    goDrafts() {
      uni.navigateTo({ url: '/pages/mine/drafts' })
    },
    
    goSetting(type) {
      if (type === 'about') {
        uni.showModal({
          title: '标帆 SOP',
          content: '标帆 SOP 移动端 v1.0.0\n\n让执行更高效，让管理更简单',
          showCancel: false
        })
      } else if (type === 'help') {
        uni.showToast({ title: '功能开发中', icon: 'none' })
      }
    },
    
    toggleNotify(e) {
      this.notifyEnabled = e.detail.value
      uni.setStorageSync('notifyEnabled', this.notifyEnabled)
      uni.showToast({
        title: this.notifyEnabled ? '已开启通知' : '已关闭通知',
        icon: 'success'
      })
    },
    
    handleLogout() {
      uni.showModal({
        title: '确认退出',
        content: '确定要退出登录吗？',
        success: (res) => {
          if (res.confirm) {
            const auth = useAuthStore()
            auth.logout()
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container {
  min-height: 100vh;
  background: #F5F5F5;
  padding: 20rpx 30rpx;
}

.user-card {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #4A90E2 0%, #67B3E8 100%);
  border-radius: 20rpx;
  padding: 40rpx 30rpx;
  margin-bottom: 30rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 24rpx;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
}

.avatar-text {
  font-size: 48rpx;
  color: #FFFFFF;
  font-weight: bold;
}

.user-info {
  flex: 1;
}

.nickname {
  display: block;
  font-size: 36rpx;
  font-weight: bold;
  color: #FFFFFF;
  margin-bottom: 8rpx;
}

.phone {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

.edit-btn {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.9);
}

.menu-section {
  background: #FFFFFF;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx;
  border-bottom: 1px solid #F0F0F0;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-left {
  display: flex;
  align-items: center;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 20rpx;
}

.menu-text {
  font-size: 30rpx;
  color: #333;
}

.menu-right {
  display: flex;
  align-items: center;
}

.badge {
  background: #F5222D;
  color: #FFFFFF;
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 20rpx;
  margin-right: 10rpx;
}

.arrow {
  font-size: 28rpx;
  color: #999;
}

.logout-section {
  margin-top: 40rpx;
}

.logout-btn {
  width: 100%;
  height: 96rpx;
  background: #FFFFFF;
  color: #F5222D;
  border-radius: 48rpx;
  font-size: 32rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
}

.version-info {
  text-align: center;
  margin-top: 40rpx;
  font-size: 24rpx;
  color: #999;
}
</style>
