// ============ 认证/用户类型 ============

/**
 * 用户信息
 * 基础用户数据结构
 */
export interface UserInfo {
  id: number
  username: string           // 用户名
  phone?: string             // 手机号
  role: 'admin' | 'user'     // 角色：管理员/普通用户
  avatar?: string             // 头像URL
  createdAt?: string          // 创建时间
}

// ============ 执行记录类型 ============

/**
 * 执行记录
 * 用户执行SOP的记录
 */
export interface Execution {
  id: number
  sopId: number                // SOP ID
  sopTitle: string            // SOP标题
  status: 'pending' | 'in_progress' | 'completed' | 'paused'  // 状态：待执行/进行中/已完成/已暂停
  currentStep: number         // 当前步骤
  startedAt: string           // 开始时间
  completedAt?: string         // 完成时间
  userId: number               // 用户ID
}

/**
 * SOP实体
 * 标准操作流程定义
 */
export interface Sop {
  id: number
  title: string              // SOP标题
  content: string           // SOP内容
  status: string            // 状态
  createdAt: string         // 创建时间
  updatedAt: string         // 更新时间
}

/**
 * 检查项
 * 步骤中的检查项定义
 */
export interface CheckItem {
  label: string                           // 标签/名称
  itemType: 'checkbox' | 'text' | 'number' | 'date' | 'select'  // 类型
  isRequired?: boolean                    // 是否必填
  placeholder?: string                    // 占位文本
  options?: string[]                       // 选项列表（select类型时）
}

/**
 * 步骤数据
 * SOP中的单个步骤
 */
export interface StepData {
  title: string             // 步骤标题
  description?: string       // 步骤描述
  duration?: string          // 预计时长
  checkItems?: CheckItem[] | string  // 检查项（支持对象数组或JSON字符串）
  stepOrder?: number          // 步骤顺序
}

// ============ API 响应类型 ============

/**
 * API 统一响应格式
 * 所有API接口的响应包装
 */
export interface ApiResponse<T = unknown> {
  code: number              // 状态码
  message: string          // 消息
  data: T                   // 响应数据
  timestamp?: number         // 时间戳
  success?: boolean          // 是否成功
}
