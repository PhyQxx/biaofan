import request from '@/api'

// ============ 类型定义 ============

/**
 * 徽章实体
 * 用户已解锁或可解锁的徽章
 */
export interface Badge {
  id: string
  name: string                           // 徽章名称
  icon: string                            // 徽章图标
  rarity: 'bronze' | 'silver' | 'gold'    // 稀有度：铜、银、金
  description: string                     // 徽章描述
  condition: string                       // 解锁条件
  rewardExp: number                        // 经验奖励
  rewardScore: number                      // 积分奖励
  unlockedAt?: string                      // 解锁时间
  progress?: number                        // 当前进度
  target?: number                          // 目标值
}

/**
 * 用户资料DTO
 * 包含用户个人中心所有信息
 */
export interface ProfileDTO {
  userId: number
  username: string
  avatar?: string
  level: number              // 当前等级
  exp: number                // 当前经验
  expToNext: number          // 升级所需经验
  rank: string               // 段位key
  rankName: string           // 段位名称
  totalScore: number         // 总积分
  streakDays: number         // 连续签到天数
  thisWeekCount: number      // 本周完成数
  thisMonthCount: number     // 本月完成数
  totalCount: number         // 总完成数
  badgeCount: number         // 已获得徽章数
  totalBadgeCount: number   // 徽章总数
  equippedTitle?: string      // 已装备称号
  equippedAvatarFrame?: string  // 已装备头像框
  equippedBackground?: string   // 已装备背景
  equippedEmotion?: string      // 已装备表情
  titleExpireAt?: string        // 称号过期时间
  recentActivities: Activity[]  // 最近活动
}

/**
 * 活动记录
 * 用户获得的积分/经验记录
 */
export interface Activity {
  id: number
  type: 'execution' | 'redeem' | 'badge' | 'level_up'  // 活动类型：执行/兑换/徽章/升级
  description: string           // 活动描述
  exp: number                   // 经验变化
  score: number                 // 积分变化
  createdAt: string             // 时间
}

/**
 * 排行榜项
 * 单个用户在排行榜中的信息
 */
export interface LeaderboardItem {
  rank: number             // 排名
  userId: number           // 用户ID
  username: string        // 用户名
  avatar?: string          // 头像
  count: number            // 完成数量
  streakDays: number       // 连续天数
  isMe?: boolean           // 是否当前用户
}

/**
 * 排行榜概览
 * 包含排行榜类型、时间范围及排名列表
 */
export interface LeaderboardOverview {
  type: string                 // 排行榜类型：weekly/monthly
  label: string                // 显示标签
  startDate: string            // 开始日期
  endDate: string              // 结束日期
  totalParticipants: number    // 总参与人数
  myRank?: number              // 我的排名
  myCount?: number             // 我的完成数
  items: LeaderboardItem[]     // 排行榜列表
}

/**
 * 商城商品
 * 用户可在商城购买的商品
 */
export interface StoreProduct {
  id: number
  name: string                                   // 商品名称
  icon: string                                    // 商品图标
  category: 'title' | 'avatar_frame' | 'background' | 'emotion'  // 类别
  price: number                                   // 价格
  stock: number                                   // 库存
  durationDays?: number                           // 有效期天数
  isPermanent?: boolean                           // 是否永久
  owned: boolean                                  // 是否已拥有
  equipped: boolean                              // 是否已装备
  expireAt?: string                               // 过期时间
}

/**
 * 积分历史
 * 用户积分变动记录
 */
export interface ScoreHistory {
  id: number
  type: 'execution' | 'redeem' | 'badge' | 'streak' | 'level_up'  // 变动类型
  description: string        // 描述
  change: number             // 变化量
  balance: number            // 余额
  createdAt: string           // 时间
}

/**
 * 兑换结果
 * 用户兑换商品后的结果
 */
export interface RedeemResult {
  success: boolean            // 是否成功
  productName: string         // 商品名称
  newBalance: number          // 新余额
  expireAt?: string           // 过期时间
}

// ============ API 接口 ============
export const gamificationApi = {
  // 获取个人中心资料
  getProfile: () => request.get<any, ProfileDTO>('/gamification/profile'),

  // 获取我的徽章列表
  getBadges: () => request.get<any, Badge[]>('/gamification/badges'),
  // 获取单个徽章详情
  getBadge: (badgeId: string) => request.get<any, Badge>(`/gamification/badges/${badgeId}`),

  // 获取排行榜
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
  // 获取排行榜概览
  getLeaderboardOverview: () => request.get<any, LeaderboardOverview>('/gamification/leaderboard/overview'),

  // 获取当前积分
  getScore: () => request.get<any, { total: number }>('/gamification/score'),
  // 获取积分历史记录
  getScoreHistory: (page = 1, pageSize = 20) =>
    request.get<any, { items: ScoreHistory[]; total: number; page: number; pageSize: number }>(
      `/gamification/score/history?page=${page}&pageSize=${pageSize}`
    ),
  // 兑换商品
  redeem: (productId: number) => request.post<any, RedeemResult>('/gamification/score/redeem', { productId }),

  // 获取商城商品列表
  getStore: (category?: string) =>
    request.get<any, StoreProduct[]>(`/gamification/store${category ? `?category=${category}` : ''}`),

  // 获取成长进度
  getProgress: () => request.get<any, any>('/gamification/progress'),
  // 获取等级排行榜
  getLevelRanking: () => request.get<any, any>('/gamification/level-ranking'),
}
