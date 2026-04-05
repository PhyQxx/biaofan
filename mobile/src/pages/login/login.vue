<template>
  <view class="login-container">
    <view class="logo-section">
      <view class="logo">标帆</view>
      <view class="subtitle">SOP 移动执行台</view>
    </view>
    
    <view class="form-section">
      <view class="form-item">
        <view class="label">手机号</view>
        <input 
          class="input" 
          type="number" 
          v-model="phone" 
          placeholder="请输入手机号"
          maxlength="11"
        />
      </view>
      
      <view class="form-item">
        <view class="label">验证码</view>
        <view class="code-row">
          <input 
            class="input code-input" 
            type="number" 
            v-model="code" 
            placeholder="请输入验证码"
            maxlength="6"
          />
          <button 
            class="code-btn" 
            :disabled="countdown > 0"
            @click="sendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>
      
      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </view>
    
    <view class="footer">
      登录即表示同意<text class="link">《用户协议》</text>和<text class="link">《隐私政策》</text>
    </view>
  </view>
</template>

<script>
import api from '../../api'
import { useAuthStore } from '../../store/auth'

export default {
  data() {
    return {
      phone: '',
      code: '',
      loading: false,
      countdown: 0
    }
  },
  methods: {
    async sendCode() {
      if (!this.phone || this.phone.length !== 11) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      
      try {
        await api.auth.sendCode(this.phone)
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        
        this.countdown = 60
        const timer = setInterval(() => {
          this.countdown--
          if (this.countdown <= 0) {
            clearInterval(timer)
          }
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      }
    },
    
    async handleLogin() {
      if (!this.phone || this.phone.length !== 11) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      if (!this.code || this.code.length < 4) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }
      
      this.loading = true
      try {
        const auth = useAuthStore()
        await auth.login(this.phone, this.code)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '登录失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #4A90E2 0%, #67B3E8 100%);
  padding: 100rpx 50rpx;
}

.logo-section {
  text-align: center;
  margin-bottom: 80rpx;
}

.logo {
  font-size: 80rpx;
  font-weight: bold;
  color: #FFFFFF;
  margin-bottom: 20rpx;
}

.subtitle {
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

.form-section {
  background: #FFFFFF;
  border-radius: 24rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 10rpx 40rpx rgba(0, 0, 0, 0.1);
}

.form-item {
  margin-bottom: 40rpx;
}

.label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
  font-weight: 500;
}

.input {
  height: 88rpx;
  background: #F5F5F5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 28rpx;
}

.code-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.code-input {
  flex: 1;
}

.code-btn {
  width: 220rpx;
  height: 88rpx;
  background: #4A90E2;
  color: #FFFFFF;
  border-radius: 12rpx;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  padding: 0;
}

.code-btn[disabled] {
  background: #CCCCCC;
}

.login-btn {
  width: 100%;
  height: 96rpx;
  background: #4A90E2;
  color: #FFFFFF;
  border-radius: 48rpx;
  font-size: 32rpx;
  font-weight: 500;
  border: none;
  margin-top: 20rpx;
}

.login-btn[disabled] {
  background: #CCCCCC;
}

.footer {
  text-align: center;
  margin-top: 50rpx;
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

.link {
  color: #FFFFFF;
  text-decoration: underline;
}
</style>
