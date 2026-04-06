import request from '@/api'

// Types
export interface Badge {
  id: string
  name: string
  icon: string
  rarity: 'bronze' | 'silver' | 'gold'
  description: string
  condition: string
  rewardExp: number
  rewardScore: number
  unlockedAt?: string
  progress?: number
  target?: number
}

export interface ProfileDTO {
  userId: number
  username: string
  avatar?: string
  level: number
  exp: number
  expToNext: number
  rank: string
  rankName: string
  totalScore: number
  streakDays: number
  thisWeekCount: number
  thisMonthCount: number
  totalCount: number
  badgeCount: number
  totalBadgeCount: number
  equippedTitle?: string
  equippedAvatarFrame?: string
  equippedBackground?: string
  equippedEmotion?: string
  titleExpireAt?: string
  recentActivities: Activity[]
}

export interface Activity {
  id: number
  type: 'execution' | 'redeem' | 'badge' | 'level_up'
  description: string
  exp: number
  score: number
  createdAt: string
}

export interface LeaderboardItem {
  rank: number
  userId: number
  username: string
  avatar?: string
  count: number
  streakDays: number
  isMe?: boolean
}

export interface LeaderboardOverview {
  type: string
  label: string
  startDate: string
  endDate: string
  totalParticipants: number
  myRank?: number
  myCount?: number
  items: LeaderboardItem[]
}

export interface StoreProduct {
  id: number
  name: string
  icon: string
  category: 'title' | 'avatar_frame' | 'background' | 'emotion'
  price: number
  stock: number
  durationDays?: number
  isPermanent?: boolean
  owned: boolean
  equipped: boolean
  expireAt?: string
}

export interface ScoreHistory {
  id: number
  type: 'execution' | 'redeem' | 'badge' | 'streak' | 'level_up'
  description: string
  change: number
  balance: number
  createdAt: string
}

export interface RedeemResult {
  success: boolean
  productName: string
  newBalance: number
  expireAt?: string
}

// API calls
export const gamificationApi = {
  // 个人中心
  getProfile: () => request.get<any, ProfileDTO>('/gamification/profile'),

  // 徽章
  getBadges: () => request.get<any, Badge[]>('/gamification/badges'),
  getBadge: (badgeId: string) => request.get<any, Badge>(`/gamification/badges/${badgeId}`),

  // 排行榜
  getLeaderboard: (type = 'weekly') => request.get<any, LeaderboardOverview>(`/gamification/leaderboard?type=${type}`).then((res: any) => {
      // res 是 { code, data: [...] } 格式，data 才是数组
      const arr = Array.isArray(res) ? res : (res?.data?.data ?? (Array.isArray(res?.data) ? res.data : []))
      return {
        type,
        label: type === 'weekly' ? '周榜' : type === 'monthly' ? '月榜' : type,
        startDate: '',
        endDate: '',
        totalParticipants: arr.length,
        items: arr.map((item: any) => ({
          ...item,
          rankTitle: item.rankTitle || 'bronze',
        })),
      }
    }),
  getLeaderboardOverview: () => request.get<any, LeaderboardOverview>('/gamification/leaderboard/overview'),

  // 积分
  getScore: () => request.get<any, { total: number }>('/gamification/score'),
  getScoreHistory: (page = 1, pageSize = 20) =>
    request.get<any, { items: ScoreHistory[]; total: number; page: number; pageSize: number }>(
      `/gamification/score/history?page=${page}&pageSize=${pageSize}`
    ),
  redeem: (productId: number) => request.post<any, RedeemResult>('/gamification/score/redeem', { productId }),

  // 商城
  getStore: (category?: string) =>
    request.get<any, StoreProduct[]>(`/gamification/store${category ? `?category=${category}` : ''}`),

  // 成长
  getProgress: () => request.get<any, any>('/gamification/progress'),
  getLevelRanking: () => request.get<any, any>('/gamification/level-ranking'),
}
