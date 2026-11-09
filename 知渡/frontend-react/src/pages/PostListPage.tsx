import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, KeyboardEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { fetchPosts, fetchSuggestions } from '../api/endpoints'
import type { PageResult, Post, PostSuggestion } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { PostCard } from '../components/PostCard'
import { useAsync } from '../hooks/useAsync'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import { Highlight, splitWords } from '../utils/highlight'
import styles from './PostListPage.module.css'

const PAGE_SIZE = 10
const SUGGEST_LIMIT = 8

/** 搜索框里的放大镜：纯 SVG，不引额外图标库 */
function SearchIcon() {
  return (
    <svg viewBox="0 0 20 20" width="17" height="17" aria-hidden="true" focusable="false">
      <circle cx="8.6" cy="8.6" r="5.6" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M12.8 12.8 L17 17"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
    </svg>
  )
}

export default function PostListPage() {
  const { isLoggedIn } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  // 分页与搜索词放进 URL：刷新、后退、分享链接都能回到同一屏
  const page = Math.max(1, Number(searchParams.get('page') ?? '1') || 1)
  const keyword = searchParams.get('q') ?? ''

  const [input, setInput] = useState(keyword)
  const debouncedInput = useDebouncedValue(input, 400)
  const suggestKeyword = useDebouncedValue(input, 200)

  const [suggestions, setSuggestions] = useState<PostSuggestion[]>([])
  const [suggestOpen, setSuggestOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)
  // 只有用户真正在输入时才自动弹联想，避免从分享链接进入就弹一脸
  const typingRef = useRef(false)

  // 联想面板按「正在输入的词」高亮；结果列表按「URL 里已提交的词」高亮。
  // 两者刻意分开：防抖期间用户还在打字，若共用 keyword，高亮会慢半拍。
  const suggestWords = useMemo(() => splitWords(suggestKeyword), [suggestKeyword])
  const resultWords = useMemo(() => splitWords(keyword), [keyword])

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

  // 联想：200ms 防抖 + AbortController 取消过期请求，避免慢网络下旧结果盖掉新结果
  useEffect(() => {
    const q = suggestKeyword.trim()
    if (!q) {
      setSuggestions([])
      setSuggestOpen(false)
      return
    }
    const controller = new AbortController()
    fetchSuggestions(q, SUGGEST_LIMIT, controller.signal)
      .then((list) => {
        const items = list ?? []
        setSuggestions(items)
        setActiveIndex(-1)
        // 无结果也弹面板，给出「没找到」的明确反馈而不是什么都不发生
        setSuggestOpen(typingRef.current)
      })
      .catch(() => {
        if (!controller.signal.aborted) setSuggestions([])
      })
    return () => controller.abort()
  }, [suggestKeyword])

  const posts = data?.records ?? []
  const total = data?.total ?? 0
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE))

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    typingRef.current = false
    setSuggestOpen(false)
    writeQuery({ page: 1, q: input.trim() })
  }

  const clearInput = () => {
    typingRef.current = false
    setInput('')
    setSuggestions([])
    setSuggestOpen(false)
    writeQuery({ page: 1, q: '' })
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (!suggestOpen || suggestions.length === 0) return
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActiveIndex((i) => (i + 1) % suggestions.length)
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActiveIndex((i) => (i <= 0 ? suggestions.length - 1 : i - 1))
    } else if (event.key === 'Escape') {
      setSuggestOpen(false)
    } else if (event.key === 'Enter' && activeIndex >= 0) {
      const picked = suggestions[activeIndex]
      if (!picked) return
      event.preventDefault()
      typingRef.current = false
      setSuggestOpen(false)
      navigate(`/posts/${picked.id}`)
    }
  }

  return (
    <div>
      <div className={styles.toolbar}>
        <form className={styles.search} onSubmit={handleSubmit} role="search">
          <div className={styles.inputWrap}>
            <span className={styles.searchIcon}>
              <SearchIcon />
            </span>
            <input
              type="text"
              value={input}
              onChange={(event) => {
                typingRef.current = true
                setInput(event.target.value)
              }}
              onFocus={() => {
                if (suggestions.length) setSuggestOpen(true)
              }}
              onBlur={() => {
                typingRef.current = false
                window.setTimeout(() => setSuggestOpen(false), 150)
              }}
              onKeyDown={handleKeyDown}
              placeholder="搜索帖子 · 空格分隔多个词"
              aria-label="搜索帖子标题、正文与标签"
              autoComplete="off"
              role="combobox"
              aria-expanded={suggestOpen}
              aria-controls="post-suggest-list"
            />
            {input && (
              <button
                type="button"
                className={styles.clearBtn}
                aria-label="清空搜索"
                onMouseDown={(event) => event.preventDefault()}
                onClick={clearInput}
              >
                ×
              </button>
            )}
            {suggestOpen && (
              <div className={styles.suggest} id="post-suggest-list">
                <div className={styles.suggestHead}>
                  <span>{suggestions.length ? '匹配帖子' : '没有匹配的帖子'}</span>
                  <span>按点赞数排序</span>
                </div>
                {suggestions.length > 0 ? (
                  <ul role="listbox" aria-label="搜索联想">
                    {suggestions.map((item, index) => (
                      <li key={item.id} role="option" aria-selected={index === activeIndex}>
                        <Link
                          to={`/posts/${item.id}`}
                          className={`${styles.suggestItem} ${index === activeIndex ? styles.suggestActive : ''}`}
                          onClick={() => {
                            typingRef.current = false
                            setSuggestOpen(false)
                          }}
                        >
                          <span className={styles.suggestTitle}>
                            <Highlight text={item.title} words={suggestWords} />
                          </span>
                          {item.snippet && (
                            <span className={styles.suggestSnippet}>
                              <Highlight text={item.snippet} words={suggestWords} />
                            </span>
                          )}
                          <span className={styles.suggestMeta}>
                            <span className={styles.suggestAuthor}>{item.authorName}</span>
                            <span>· {item.likeCount} 赞</span>
                            <span className={styles.hitWord}>命中「{item.matchedWord}」</span>
                          </span>
                        </Link>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className={styles.suggestEmpty}>换个关键词，或试试更短的词</p>
                )}
              </div>
            )}
          </div>
          <button className="btn btn-primary" type="submit">
            搜索
          </button>
        </form>
        {isLoggedIn && (
          <Link to="/posts/new" className="btn">
            写帖子
          </Link>
        )}
      </div>

      {keyword && (
        <div className={styles.resultBar}>
          <span className={styles.resultChip}>搜索「{keyword}」</span>
          <span className={styles.resultMeta}>任一词命中 · 按点赞数排序 · 共 {total} 条</span>
        </div>
      )}

      {error && <p className="error-text">{error}</p>}

      {loading && !data && (
        <div aria-busy="true" aria-label="加载帖子列表">
          <div className="skeleton" style={{ height: 104, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 104, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 104 }} />
        </div>
      )}

      {!loading && !error && posts.length === 0 && (
        <div className="empty">{keyword ? '没有匹配的帖子，换个词试试' : '还没有帖子'}</div>
      )}

      {posts.map((post) => (
        <PostCard key={post.id} post={post} highlightWords={resultWords} />
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
