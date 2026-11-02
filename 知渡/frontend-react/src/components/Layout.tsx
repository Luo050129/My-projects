import { Suspense } from 'react'
import { Outlet } from 'react-router-dom'
import { NavBar } from './NavBar'
import styles from './Layout.module.css'

/** 懒加载路由的占位：骨架块比"加载中…"更接近最终布局，视觉跳动更小 */
function RouteFallback() {
  return (
    <div aria-busy="true" aria-label="页面加载中">
      <div className="skeleton" style={{ height: 44, marginBottom: 16 }} />
      <div className="skeleton" style={{ height: 96, marginBottom: 12 }} />
      <div className="skeleton" style={{ height: 96, marginBottom: 12 }} />
      <div className="skeleton" style={{ height: 96 }} />
    </div>
  )
}

export function Layout() {
  return (
    <>
      <NavBar />
      <main className={styles.container}>
        <Suspense fallback={<RouteFallback />}>
          <Outlet />
        </Suspense>
      </main>
      <footer className="footer">知渡 · 知识社区</footer>
    </>
  )
}
