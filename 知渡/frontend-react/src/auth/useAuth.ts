import { useContext } from 'react'
import { AuthContext } from './auth-context'
import type { AuthContextValue } from './auth-context'

/** Provider 外调用直接抛错，比"读到 undefined 后页面静默不更新"好排查 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth 必须在 <AuthProvider> 内使用')
  return ctx
}
