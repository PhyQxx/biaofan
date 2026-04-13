/**
 * Token 管理工具
 */
const TOKEN_KEY = 'bf_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 解析 JWT payload（不验证签名，只提取数据）
 * @param token JWT token
 */
export function parseJwtPayload(token: string): { sub?: string; userid?: number; username?: string; exp?: number } | null {
  if (!token) return null
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

/**
 * 检查 token 是否可解析出 userId（不验证签名）
 * JWT 的 sub 字段存的是 userId
 */
export function isTokenValidatable(): boolean {
  const token = getToken()
  if (!token) return false
  const payload = parseJwtPayload(token)
  return payload != null && (payload.sub != null || payload.userid != null)
}
