/**
 * 通用工具函数
 */
import dayjs from 'dayjs'

/**
 * 格式化日期时间
 */
export function formatDateTime(date, format = 'YYYY-MM-DD HH:mm') {
  if (!date) return ''
  return dayjs(date).format(format)
}

/**
 * 格式化日期
 */
export function formatDate(date, format = 'YYYY-MM-DD') {
  if (!date) return ''
  return dayjs(date).format(format)
}

/**
 * 相对时间（几小时前、几天前）
 */
export function relativeTime(date) {
  if (!date) return ''
  const now = dayjs()
  const target = dayjs(date)
  const diffMinutes = now.diff(target, 'minute')
  
  if (diffMinutes < 1) return '刚刚'
  if (diffMinutes < 60) return `${diffMinutes}分钟前`
  
  const diffHours = now.diff(target, 'hour')
  if (diffHours < 24) return `${diffHours}小时前`
  
  const diffDays = now.diff(target, 'day')
  if (diffDays < 7) return `${diffDays}天前`
  
  return formatDate(date)
}

/**
 * 判断是否过期
 */
export function isOverdue(deadline) {
  if (!deadline) return false
  return dayjs().isAfter(dayjs(deadline))
}

/**
 * 距离截止时间
 */
export function deadlineText(deadline) {
  if (!deadline) return ''
  
  const now = dayjs()
  const end = dayjs(deadline)
  const diffMinutes = end.diff(now, 'minute')
  
  if (diffMinutes < 0) {
    const overdueMinutes = Math.abs(diffMinutes)
    if (overdueMinutes < 60) return `已超时${overdueMinutes}分钟`
    const overdueHours = Math.floor(overdueMinutes / 60)
    if (overdueHours < 24) return `已超时${overdueHours}小时`
    return `已超时${Math.floor(overdueHours / 24)}天`
  }
  
  if (diffMinutes < 60) return `还剩${diffMinutes}分钟`
  
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) return `还剩${diffHours}小时`
  
  return `还剩${Math.floor(diffHours / 24)}天`
}

/**
 * 获取状态文本
 */
export function getStatusText(status) {
  const statusMap = {
    pending: '待开始',
    in_progress: '进行中',
    completed: '已完成',
    overdue: '已超时',
    cancelled: '已取消'
  }
  return statusMap[status] || status
}

/**
 * 获取状态标签样式
 */
export function getStatusTagClass(status) {
  const classMap = {
    pending: 'tag-pending',
    in_progress: 'tag-progress',
    completed: 'tag-completed',
    overdue: 'tag-overdue'
  }
  return classMap[status] || ''
}

/**
 * 显示加载状态
 */
export function showLoading(title = '加载中...') {
  uni.showLoading({ title, mask: true })
}

/**
 * 隐藏加载状态
 */
export function hideLoading() {
  uni.hideLoading()
}

/**
 * 检查网络状态
 */
export function checkNetwork() {
  return new Promise((resolve) => {
    uni.getNetworkType({
      success: (res) => {
        resolve(res.networkType !== 'none')
      },
      fail: () => resolve(false)
    })
  })
}

/**
 * 监听网络状态变化
 */
export function onNetworkChange(callback) {
  uni.onNetworkStatusChange((res) => {
    callback(res.isConnected)
  })
}
