import { createContext } from 'react'
import type { AuthUser, LoginResponse } from '../api/types'

export interface AuthContextValue {
  token: string
  user: AuthUser | null
  isLoggedIn: boolean
  signIn: (resp: LoginResponse) => void
  signOut: () => void
}

/**
 * Context 单独放在非组件文件里：
 * Provider 与 useAuth 各自独立 HMR，改 hook 不会让所有页面重置状态。
 */
export const AuthContext = createContext<AuthContextValue | null>(null)
