/**
 * API 统一配置
 * 所有接口请求都通过这个模块
 */

const BASE_URL = 'http://192.168.31.104:8013'

/**
 * 通用请求封装
 */
function request(options) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        'Authorization': token ? `Bearer ${token}` : '',
        ...options.header
      },
      timeout: 30000,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data.code === 200 || res.data.code === 0) {
            resolve(res.data)
          } else if (res.data.code === 401) {
            // Token 过期，跳转登录
            uni.removeStorageSync('token')
            uni.reLaunch({ url: '/pages/login/login' })
            reject(new Error('登录已过期'))
          } else {
            uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未授权'))
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络请求失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default {
  baseUrl: BASE_URL,
  
  // ========== 认证相关 ==========
  auth: {
    login(data) {
      return request({ url: '/api/auth/login', method: 'POST', data })
    },
    sendCode(phone) {
      return request({ url: '/api/auth/send-code', method: 'POST', data: { phone } })
    },
    register(data) {
      return request({ url: '/api/auth/register', method: 'POST', data })
    },
    logout() {
      return request({ url: '/api/auth/logout', method: 'POST' })
    }
  },

  // ========== 执行单相关 ==========
  execution: {
    // 我的待执行列表
    myPending() {
      return request({ url: '/api/sop/executions', data: { status: 'pending', executorId: uni.getStorageSync('userId') } })
    },
    // 我的进行中
    myInProgress() {
      return request({ url: '/api/sop/executions', data: { status: 'in_progress', executorId: uni.getStorageSync('userId') } })
    },
    // 我的已完成
    myCompleted() {
      return request({ url: '/api/sop/executions', data: { status: 'completed', executorId: uni.getStorageSync('userId') } })
    },
    // 执行单详情
    detail(id) {
      return request({ url: `/api/sop/executions/${id}` })
    },
    // 开始执行
    start(id) {
      return request({ url: `/api/sop/executions/${id}/start`, method: 'POST' })
    },
    // 步骤打卡
    completeStep(executionId, stepId, data) {
      return request({ 
        url: `/api/sop/executions/${executionId}/steps/${stepId}/complete`, 
        method: 'POST', 
        data 
      })
    },
    // 完成执行
    finish(id) {
      return request({ url: `/api/sop/executions/${id}/finish`, method: 'POST' })
    },
    // 获取步骤详情（移动端专用，减少冗余数据）
    getSteps(id) {
      return request({ url: `/api/sop/executions/${id}/steps` })
    },
    // 执行记录
    record(id) {
      return request({ url: `/api/sop/executions/${id}/record` })
    },
    // 异常上报
    reportException(executionId, data) {
      return request({ url: `/api/sop/exceptions/${executionId}/report`, method: 'POST', data })
    }
  },

  // ========== 周期实例相关 ==========
  instance: {
    myInstances(status) {
      const params = {}
      if (status) params.status = status
      return request({ url: '/api/instance/my', data: params })
    },
    detail(id) {
      return request({ url: `/api/instance/${id}` })
    },
    activate(id) {
      return request({ url: `/api/instance/${id}/activate`, method: 'POST' })
    },
    completeStep(id, stepIndex, data) {
      return request({ url: `/api/instance/${id}/steps/${stepIndex}/complete`, method: 'POST', data })
    },
    finish(id) {
      return request({ url: `/api/instance/${id}/finish`, method: 'POST' })
    }
  },

  // ========== SOP 相关 ==========
  sop: {
    detail(id) {
      return request({ url: `/api/sop/${id}` })
    }
  },

  // ========== 离线草稿同步 ==========
  draft: {
    // 同步离线草稿
    sync(data) {
      return request({ url: '/api/sop/executions/draft', method: 'POST', data })
    }
  },

  // ========== 通知相关 ==========
  notification: {
    list() {
      return request({ url: '/api/notifications' })
    },
    markRead(id) {
      return request({ url: `/api/notifications/${id}/read`, method: 'PUT' })
    },
    markAllRead() {
      return request({ url: '/api/notifications/read-all', method: 'PUT' })
    }
  },

  // ========== 文件上传 ==========
  upload: {
    // 图片上传（用于异常拍照、步骤打卡拍照）
    image(filePath) {
      return new Promise((resolve, reject) => {
        const token = uni.getStorageSync('token')
        uni.uploadFile({
          url: BASE_URL + '/api/upload/image',
          filePath: filePath,
          name: 'file',
          header: { Authorization: token ? `Bearer ${token}` : '' },
          success: (res) => {
            const data = JSON.parse(res.data)
            if (data.code === 200) {
              resolve(data)
            } else {
              reject(new Error(data.message || '上传失败'))
            }
          },
          fail: reject
        })
      })
    }
  },

  // ========== 推送相关 ==========
  push: {
    // 注册客户端 CID
    registerCid(clientId) {
      return request({ url: '/api/push/register', method: 'POST', data: { clientId } })
    }
  },

  // ========== SOP 模板相关 ==========
  template: {
    list(params) {
      return request({ url: '/api/sop/templates', data: params })
    },
    detail(id) {
      return request({ url: `/api/sop/templates/${id}` })
    }
  }
}
