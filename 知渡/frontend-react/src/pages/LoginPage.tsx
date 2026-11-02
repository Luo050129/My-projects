import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { login, register } from '../api/endpoints'
import { useAuth } from '../auth/useAuth'
import styles from './LoginPage.module.css'

type Mode = 'login' | 'register'

export default function LoginPage() {
  const { isLoggedIn, signIn } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [mode, setMode] = useState<Mode>('login')
  const [form, setForm] = useState({ username: '', password: '', nickname: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  // 已登录再访问 /login：直接回列表（对应 Vue 版 beforeEach 的第二条规则）
  if (isLoggedIn) return <Navigate to="/posts" replace />

  const switchMode = (next: Mode) => {
    setMode(next)
    setError('')
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setError('')

    if (!form.username || !form.password) {
      setError('用户名和密码不能为空')
      return
    }

    setLoading(true)
    try {
      const resp =
        mode === 'login'
          ? await login(form.username, form.password)
          : await register(form.username, form.password, form.nickname || form.username)

      signIn(resp)
      // 登录成功后回到来源页；没有来源就去列表
      const redirect = searchParams.get('redirect')
      navigate(redirect ? decodeURIComponent(redirect) : '/posts', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <div className={`panel ${styles.panel}`}>
        <h1 className={styles.title}>知渡</h1>
        <p className={`muted small ${styles.sub}`}>内容 / 知识社区</p>

        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab} ${mode === 'login' ? styles.active : ''}`}
            onClick={() => switchMode('login')}
          >
            登录
          </button>
          <button
            type="button"
            className={`${styles.tab} ${mode === 'register' ? styles.active : ''}`}
            onClick={() => switchMode('register')}
          >
            注册
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="username">用户名</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              placeholder="admin"
              value={form.username}
              onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
            />
          </div>

          <div className="field">
            <label htmlFor="password">密码</label>
            <input
              id="password"
              type="password"
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              placeholder="admin123"
              value={form.password}
              onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
            />
          </div>

          {mode === 'register' && (
            <div className="field">
              <label htmlFor="nickname">昵称（可选）</label>
              <input
                id="nickname"
                type="text"
                placeholder="展示给别人看的名字"
                value={form.nickname}
                onChange={(event) => setForm((prev) => ({ ...prev, nickname: event.target.value }))}
              />
            </div>
          )}

          <button className={`btn btn-primary ${styles.full}`} type="submit" disabled={loading}>
            {loading ? '处理中…' : mode === 'login' ? '登录' : '注册并登录'}
          </button>

          {error && <p className="error-text">{error}</p>}
        </form>

        <p className={`muted small ${styles.hint}`}>演示账号：admin / admin123、alice / alice123</p>
      </div>
    </div>
  )
}
