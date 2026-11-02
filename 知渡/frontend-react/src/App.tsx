import { lazy } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { Layout } from './components/Layout'
import { useAuth } from './auth/useAuth'

// 路由级代码分割：首屏只加载列表页，编辑器与通知页按需拉取
const PostListPage = lazy(() => import('./pages/PostListPage'))
const PostDetailPage = lazy(() => import('./pages/PostDetailPage'))
const PostEditorPage = lazy(() => import('./pages/PostEditorPage'))
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'))
const LoginPage = lazy(() => import('./pages/LoginPage'))

/**
 * 路由守卫。
 * 与 Vue 版的 `router.beforeEach` 等价，但这里是可组合的组件：
 * 需要保护的页面直接包一层，不用在全局注册表里维护 meta.requiresAuth。
 */
function RequireAuth({ children }: { children: ReactNode }) {
  const { isLoggedIn } = useAuth()
  const location = useLocation()

  if (!isLoggedIn) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/login?redirect=${redirect}`} replace />
  }
  return <>{children}</>
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/posts" replace />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="posts" element={<PostListPage />} />
        <Route
          path="posts/new"
          element={
            <RequireAuth>
              <PostEditorPage />
            </RequireAuth>
          }
        />
        <Route path="posts/:id" element={<PostDetailPage />} />
        <Route
          path="posts/:id/edit"
          element={
            <RequireAuth>
              <PostEditorPage />
            </RequireAuth>
          }
        />
        <Route
          path="notifications"
          element={
            <RequireAuth>
              <NotificationsPage />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/posts" replace />} />
      </Route>
    </Routes>
  )
}
