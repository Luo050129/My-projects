import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import { fetchUnreadCount } from '../api/endpoints'
import { useAuth } from '../auth/useAuth'
import { useAsync } from '../hooks/useAsync'
import styles from './NavBar.module.css'

export function NavBar() {
  const { isLoggedIn, user, signOut } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  // 未读数随「登录态 + 每次导航」重取；location.key 每次跳转都会变
  const { data: unread } = useAsync<number>(
    (signal) => (isLoggedIn ? fetchUnreadCount(signal) : Promise.resolve(0)),
    [isLoggedIn, location.key]
  )
  const unreadCount = unread ?? 0

  const handleLogout = (): void => {
    signOut()
    navigate('/posts')
  }

  return (
    <header className={styles.nav}>
      <div className={styles.inner}>
        <NavLink to="/posts" className={styles.brand}>
          知渡
        </NavLink>

        <nav className={styles.links}>
          <NavLink to="/posts" className={({ isActive }) => (isActive ? styles.active : '')}>
            帖子
          </NavLink>
          {isLoggedIn && (
            <NavLink to="/posts/new" className={({ isActive }) => (isActive ? styles.active : '')}>
              发帖
            </NavLink>
          )}
          {isLoggedIn && (
            <NavLink
              to="/notifications"
              className={({ isActive }) => (isActive ? styles.active : '')}
            >
              通知
              {unreadCount > 0 && (
                <span className={styles.badge}>{unreadCount > 99 ? '99+' : unreadCount}</span>
              )}
            </NavLink>
          )}
        </nav>

        <div className={styles.right}>
          {isLoggedIn ? (
            <>
              <NavLink
                to="/me"
                className={({ isActive }) =>
                  `${styles.who} ${isActive ? styles.whoActive : ''}`
                }
                title="个人中心"
              >
                <span className={styles.miniAvatar} aria-hidden="true">
                  {(user?.nickname || '我').slice(0, 1)}
                </span>
                {user?.nickname || '用户'}
              </NavLink>
              <button type="button" className="btn btn-sm" onClick={handleLogout}>
                退出
              </button>
            </>
          ) : (
            <NavLink to="/login" className="btn btn-sm btn-primary">
              登录
            </NavLink>
          )}
        </div>
      </div>
    </header>
  )
}
