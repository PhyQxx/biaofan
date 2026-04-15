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
        <view class="label">密码</view>
        <input
          class="input"
          :password="!showPassword"
          v-model="password"
          placeholder="请输入密码"
        />
        <view class="password-toggle" @click="showPassword = !showPassword">
          {{ showPassword ? '🙈' : '👁️' }}
        </view>
      </view>

      <button class="login-btn" :disabled="loading" @click="handleLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>

      <view class="register-link" @click="goRegister">
        还没有账号？<text class="link">立即注册</text>
      </view>
    </view>

    <view class="footer">
      登录即表示同意<text class="link">《用户协议》</text>和<text class="link">《隐私政策》</text>
    </view>
  </view>
</template>

<script>


/**
 * 移动端登录页面
 * - 手机号 + 密码登录
 * - 与 PC 端保持一致
 */
import api from '../../api'
import { useAuthStore } from '../../store/auth'

export default {
  data() {
    return {
      phone: '',
      password: '',
      loading: false,
      showPassword: false
    }
  },
  methods: {
    validatePhone(phone) {
      return /^1[3-9]\d{9}$/.test(phone)
    },

    async handleLogin() {
      if (!this.phone || !this.validatePhone(this.phone)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return
      }
      if (!this.password || this.password.length < 6) {
        uni.showToast({ title: '请输入密码（至少6位）', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const auth = useAuthStore()
        await auth.login(this.phone, this.password)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 1000)
      } catch (e) {
        uni.showToast({ title: e.message || '登录失败', icon: 'none' })
      } finally {
        this.loading = false
      }
    },

    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
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

.password-toggle {
  position: absolute;
  right: 24rpx;
  bottom: 24rpx;
  font-size: 36rpx;
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

.register-link {
  text-align: center;
  margin-top: 30rpx;
  font-size: 26rpx;
  color: #666;
}

.link {
  color: #4A90E2;
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
