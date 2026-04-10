import request from './index'

/**
 * 趋势数据点
 * 单日统计数据
 */
export interface TrendPoint {
  date: string       // 日期
  total: number      // 总数
  completed: number  // 已完成数
  rate: number       // 完成率
}

/**
 * 成员统计
 * 单个用户的完成情况统计
 */
export interface MemberStat {
  userId: number           // 用户ID
  username: string        // 用户名
  completedCount: number   // 已完成数
  overdueCount: number     // 逾期数
  completionRate: number   // 完成率
  rank: number             // 排名
}

/**
 * 仪表盘统计数据
 * 仪表盘展示的所有统计数据
 */
export interface DashboardStats {
  todayTotal: number        // 今日总任务数
  todayCompleted: number    // 今日已完成
  todayOverdue: number      // 今日逾期
  pendingCount: number      // 待执行数
  completedCount: number    // 已完成总数
  completionRate: number    // 总完成率
  totalMembers: number      // 成员总数
  totalSOPs: number         // SOP总数
  trend: TrendPoint[]       // 趋势数据
  topMembers: MemberStat[]   // top成员列表
}

/**
 * 获取仪表盘统计数据
 * @returns 仪表盘所有统计数据
 */
export const getDashboardStats = () =>
  request.get<any, { data: DashboardStats }>('/stats/dashboard')

/**
 * 获取趋势数据
 * @param days 天数（默认7天）
 * @returns 每日完成率趋势数据
 */
export const getTrend = (days = 7) =>
  request.get<any, { data: TrendPoint[] }>(`/stats/trend?days=${days}`)

/**
 * 获取排行榜
 * @param limit 返回数量（默认10）
 * @returns 完成数排名前N的用户
 */
export const getLeaderboard = (limit = 10) =>
  request.get<any, { data: MemberStat[] }>(`/stats/leaderboard?limit=${limit}`)
