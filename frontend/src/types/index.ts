// ============ Auth / User Types ============
export interface UserInfo {
  id: number
  username: string
  phone?: string
  role: 'admin' | 'user'
  avatar?: string
  createdAt?: string
}

// ============ Execution Types ============
export interface Execution {
  id: number
  sopId: number
  sopTitle: string
  status: 'pending' | 'in_progress' | 'completed' | 'paused'
  currentStep: number
  startedAt: string
  completedAt?: string
  userId: number
}

export interface Sop {
  id: number
  title: string
  content: string
  status: string
  createdAt: string
  updatedAt: string
}

export interface CheckItem {
  label: string
  itemType: 'checkbox' | 'text' | 'number' | 'date' | 'select'
  isRequired?: boolean
  placeholder?: string
  options?: string[]
}

export interface StepData {
  title: string
  description?: string
  duration?: string
  checkItems?: CheckItem[] | string
  stepOrder?: number
}

// ============ API Response Types ============
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp?: number
  success?: boolean
}
