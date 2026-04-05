import request from './index'

export interface TrendPoint {
  date: string
  total: number
  completed: number
  rate: number
}

export interface MemberStat {
  userId: number
  username: string
  completedCount: number
  overdueCount: number
  completionRate: number
  rank: number
}

export interface DashboardStats {
  todayTotal: number
  todayCompleted: number
  todayOverdue: number
  pendingCount: number
  completedCount: number
  completionRate: number
  totalMembers: number
  totalSOPs: number
  trend: TrendPoint[]
  topMembers: MemberStat[]
}

export const getDashboardStats = () =>
  request.get<any, { data: DashboardStats }>('/stats/dashboard')

export const getTrend = (days = 7) =>
  request.get<any, { data: TrendPoint[] }>(`/stats/trend?days=${days}`)

export const getLeaderboard = (limit = 10) =>
  request.get<any, { data: MemberStat[] }>(`/stats/leaderboard?limit=${limit}`)
