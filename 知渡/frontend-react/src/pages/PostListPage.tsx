import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { fetchPosts } from '../api/endpoints'
import type { PageResult, Post } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { PostCard } from '../components/PostCard'
import { useAsync } from '../hooks/useAsync'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import styles from './PostListPage.module.css'

const PAGE_SIZE = 10

export default function PostListPage() {
  const { isLoggedIn } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()

  // 分页与搜索词放进 URL：刷新、后退、分享链接都能回到同一屏（Vue 版刷新即回第 1 页）
  const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1)
  const keyword = searchParams.get('q') ?? ''

  const [input, setInput] = useState(keyword)
  const debouncedInput = useDebouncedValue(input, 400)

  const { data, loading, error } = useAsync<PageResult<Post>>(
    (signal) => fetchPosts({ current: page, size: PAGE_SIZE, keyword }, signal),
    [page, keyword]
  )

  const writeQuery = useCallback(
    (next: { page?: number; q?: string }) => {
      setSearchParams(
        (prev) => {
          const params = new URLSearchParams(prev)
          if (next.page !== undefined) params.set('page', String(next.page))
          if (next.q !== undefined) {
            if (next.q) params.set('q', next.q)
            else params.delete('q')
          }
          return params
        },
        { replace: true }
      )
    },
    [setSearchParams]
  )

  // 输入停顿 400ms 后自动搜；与 URL 里已有的词相同时不产生导航，避免自激循环
  useEffect(() => {
    if (debouncedInput === keyword) return
    writeQuery({ page: 1, q: debouncedInput })
  }, [debouncedInput, keyword, writeQuery])

  // 地址栏被外部改动（前进/后退）时，把输入框同步回来
  useEffect(() => {
    setInput(keyword)
  }, [keyword])

  const posts = data?.records ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    writeQuery({ page: 1, q: input.trim() })
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <form className={styles.search} onSubmit={handleSubmit} role="search">
          <input
            type="text"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="搜索标题…（停顿 0.4 秒自动搜）"
            aria-label="搜索帖子标题"
          />
          <button className="btn" type="submit">
            搜索
          </button>
        </form>
        {isLoggedIn && (
          <Link to="/posts/new" className="btn btn-primary">
            发帖
          </Link>
        )}
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading && !data && (
        <div aria-busy="true" aria-label="加载帖子列表">
          <div className="skeleton" style={{ height: 96, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 96, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 96 }} />
        </div>
      )}

      {!loading && !error && posts.length === 0 && <div className="empty">还没有帖子</div>}

      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}

      {total > 0 && (
        <div className={styles.pager}>
          <button
            className="btn btn-sm"
            disabled={page <= 1 || loading}
            onClick={() => writeQuery({ page: page - 1 })}
          >
            上一页
          </button>
          <span className="muted small">
            第 {page} / {totalPages} 页 · 共 {total} 条
          </span>
          <button
            className="btn btn-sm"
            disabled={page >= totalPages || loading}
            onClick={() => writeQuery({ page: page + 1 })}
          >
            下一页
          </button>
        </div>
      )}
    </div>
  )
}
