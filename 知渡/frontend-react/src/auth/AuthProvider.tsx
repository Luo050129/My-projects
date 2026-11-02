import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import type { ReactNode } from 'react'
import { configureAuth } from '../api/client'
import type { AuthUser, LoginResponse } from '../api/types'
import { AuthContext } from './auth-context'
import type { AuthContextValue } from './auth-context'

const TOKEN_KEY = 'zhidu_token'
const USER_KEY = 'zhidu_user'

interface AuthState {
  token: string
  user: AuthUser | null
}

type AuthAction =
  | { type: 'sign-in'; payload: LoginResponse }
  | { type: 'sign-out' }

function readUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as AuthUser) : null
  } catch {
    return null
  }
}

function init(): AuthState {
  return {
    token: localStorage.getItem(TOKEN_KEY) ?? '',
    user: readUser()
  }
}

function reducer(_state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'sign-in': {
      const { token, userId, nickname, role } = action.payload
      return { token, user: { userId, nickname, role } }
    }
    case 'sign-out':
      return { token: '', user: null }
  }
}

/**
 * 登录态的唯一真相源。
 *
 * Vue 版是一个 `reactive` 单例 + 直接读写 localStorage；
 * 这里改成 Context + useReducer：状态变更只能由 action 触发，
 * 持久化集中在一处 effect 里，页面组件只读不写。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, undefined, init)

  // 给非组件层（api/client）一个稳定的读取入口，避免每次 token 变化都重新配置
  const stateRef = useRef(state)
  stateRef.current = state

  useEffect(() => {
    configureAuth({
      getToken: () => stateRef.current.token,
      // 任何接口返回 401 都走这里：清空登录态，路由守卫会把用户送回登录页
      onUnauthorized: () => dispatch({ type: 'sign-out' })
    })
  }, [])

  // 持久化：只在状态真的变化时写，且 token / user 一起写，避免出现半套登录态
  useEffect(() => {
    if (state.token) {
      localStorage.setItem(TOKEN_KEY, state.token)
      if (state.user) localStorage.setItem(USER_KEY, JSON.stringify(state.user))
    } else {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }, [state])

  const signIn = useCallback((resp: LoginResponse) => {
    dispatch({ type: 'sign-in', payload: resp })
  }, [])

  const signOut = useCallback(() => {
    dispatch({ type: 'sign-out' })
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      token: state.token,
      user: state.user,
      isLoggedIn: Boolean(state.token),
      signIn,
      signOut
    }),
    [state, signIn, signOut]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
