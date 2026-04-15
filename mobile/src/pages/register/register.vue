<template>
  <view class="register-container">
    <view class="logo-section">
      <view class="logo">标帆</view>
      <view class="subtitle">注册新账号</view>
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
            class="send-btn" 
            :disabled="countdown > 0 || sending"
            @click="handleSendCode"
          >
            {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
          </button>
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">密码</view>
        <input 
          class="input" 
          :password="!showPassword"
          v-model="password" 
          placeholder="请设置密码（至少6位）"
        />
        <view class="password-toggle" @click="showPassword = !showPassword">
          {{ showPassword ? '🙈' : '👁️' }}
        </view>
      </view>
      
      <view class="form-item">
        <view class="label">确认密码</view>
        <input 
          class="input" 
          :password="!showPassword"
          v-model="confirmPassword" 
          placeholder="请再次输入密码"
        />
      </view>
      
      <button class="register-btn" :disabled="loading" @click="handleRegister">
        {{ loading ? '注册中...' : '注册' }}
      </button>
      
      <view class="login-link" @click="goLogin">
        已有账号？<text class="link">立即登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import api from '../../api'

export default {
  data() {
    return {
      phone: '',
      code: '',
      password: '',
      confirmPassword: '',
      loading: false,
      sending: false,
      countdown: 0,
      showPassword: false
    }
  },
  
  methods: {
    validatePhone(phone) {
      return /^1[3-9]\d{9}$/.test(phone)
    },
    
    async handleSendCode() {
      if (!this.phone || !this.validatePhone(this.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      
      this.sending = true
      try {
        await api.auth.sendCode(this.phone)
        uni.showToast({ title: '验证码已发送', icon: 'success' })
        this.countdown = 60
        const timer = setInterval(() => {
          this.countdown--
          if (this.countdown <= 0) clearInterval(timer)
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '发送失败', icon: 'none' })
      } finally {
        this.sending = false
      }
    },
    
    async handleRegister() {
      if (!this.phone || !this.validatePhone(this.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      if (!this.code || this.code.length < 4) {
        uni.showToast({ title: '请输入验证码', icon: 'none' })
        return
      }
      if (!this.password || this.password.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (this.password !== this.confirmPassword) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }
      
      this.loading = true
      try {
        await api.auth.register({
          phone: this.phone,
          code: this.code,
          password: this.password
        })
        uni.showToast({ title: '注册成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1500)
      } catch (e) {
        uni.showToast({ title: e.message || '注册失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },
    
    goLogin() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #4A90E2 0%, #67B3E8 100%);
  padding: 100rpx 50rpx;
}

.logo-section {
  text-align: center;
  margin-bottom: 60rpx;
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
  margin-bottom: 36rpx;
  position: relative;
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
  gap: 16rpx;
}

.code-input {
  flex: 1;
}

.send-btn {
  width: 220rpx;
  height: 88rpx;
  background: #4A90E2;
  color: #FFFFFF;
  border-radius: 12rpx;
  font-size: 24rpx;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.send-btn[disabled] {
  background: #CCCCCC;
}

.password-toggle {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  font-size: 36rpx;
}

.register-btn {
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

.register-btn[disabled] {
  background: #CCCCCC;
}

.login-link {
  text-align: center;
  margin-top: 30rpx;
  font-size: 26rpx;
  color: #666;
}

.link {
  color: #4A90E2;
}
</style>
