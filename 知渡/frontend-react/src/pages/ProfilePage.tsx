import { useCallback, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  fetchFollowers,
  fetchFollowees,
  fetchMyComments,
  fetchMyFavorites,
  fetchMyLikes,
  fetchMyPosts,
  fetchMyStats,
  followUser,
  unfollowUser
} from '../api/endpoints'
import type { MyComment, PageResult, Post, UserStats, UserSummary } from '../api/types'
import { VISIBILITY_PRIVATE } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { Pager } from '../components/Pager'
import { PostCard } from '../components/PostCard'
import { useAsync } from '../hooks/useAsync'
import { useToast } from '../components/ToastProvider'
import { formatTime } from '../utils/format'
import styles from './ProfilePage.module.css'

type TabKey = 'posts' | 'comments' | 'likes' | 'favorites' | 'followees' | 'followers'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'posts', label: '我的帖子' },
  { key: 'comments', label: '我的评论' },
  { key: 'likes', label: '点赞' },
  { key: 'favorites', label: '收藏' },
  { key: 'followees', label: '关注' },
  { key: 'followers', label: '粉丝' }
]

const PAGE_SIZE = 10

type TabData =
  | { kind: 'posts'; page: PageResult<Post> }
  | { kind: 'comments'; page: PageResult<MyComment> }
  | { kind: 'users'; page: PageResult<UserSummary> }

export default function ProfilePage() {
  const { user } = useAuth()
  const { showToast } = useToast()

  const [tab, setTab] = useState<TabKey>('posts')
  const [page, setPage] = useState(1)

  const {
    data: stats,
    reload: reloadStats
  } = useAsync<UserStats>((signal) => fetchMyStats(signal), [])

  const { data, loading, error, reload, setError } = useAsync<TabData>(
    async (signal) => {
      const uid = user?.userId ?? 0
      switch (tab) {
        case 'comments':
          return { kind: 'comments', page: await fetchMyComments(page, PAGE_SIZE, signal) }
        // 点赞 / 收藏渲染成和帖子一样的卡片，所以统一归到 posts 这一支
        case 'likes':
          return { kind: 'posts', page: await fetchMyLikes(page, PAGE_SIZE, signal) }
        case 'favorites':
          return { kind: 'posts', page: await fetchMyFavorites(page, PAGE_SIZE, signal) }
        case 'followees':
          return { kind: 'users', page: await fetchFollowees(uid, page, PAGE_SIZE, signal) }
        case 'followers':
          return { kind: 'users', page: await fetchFollowers(uid, page, PAGE_SIZE, signal) }
        case 'posts':
        default:
          return { kind: 'posts', page: await fetchMyPosts({ current: page, size: PAGE_SIZE }, signal) }
      }
    },
    [tab, page, user?.userId]
  )

  const switchTab = useCallback((next: TabKey) => {
    setTab(next)
    setPage(1)
  }, [])

  const handleFollowToggle = async (target: UserSummary) => {
    try {
      if (target.following) {
        await unfollowUser(target.id)
        showToast(`已取消关注 ${target.nickname}`, 'info')
      } else {
        await followUser(target.id)
        showToast(`已关注 ${target.nickname}`, 'ok')
      }
      // 关注数会变，两个视图都重取，避免数字和列表状态不一致
      reload()
      reloadStats()
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败')
    }
  }

  const displayName = user?.nickname ?? '我'
  const avatarText = displayName.slice(0, 1)

  return (
    <div>
      <section className={`panel ${styles.header}`}>
        <div className={styles.avatar} aria-hidden="true">
          {avatarText}
        </div>
        <div className={styles.identity}>
          <div className={styles.nameRow}>
            <h1 className={styles.name}>{displayName}</h1>
            {user?.role === 'ADMIN' && <span className={`tag ${styles.roleTag}`}>管理员</span>}
          </div>
          <div className={styles.stats}>
            <button type="button" className={styles.stat} onClick={() => switchTab('posts')}>
              <strong>{stats?.postCount ?? '—'}</strong>
              <span>帖子</span>
            </button>
            <div className={styles.statStatic}>
              <strong>{stats?.likeReceivedCount ?? '—'}</strong>
              <span>获赞</span>
            </div>
            <button type="button" className={styles.stat} onClick={() => switchTab('followers')}>
              <strong>{stats?.followerCount ?? '—'}</strong>
              <span>粉丝</span>
            </button>
            <button type="button" className={styles.stat} onClick={() => switchTab('followees')}>
              <strong>{stats?.followeeCount ?? '—'}</strong>
              <span>关注</span>
            </button>
          </div>
          {stats && stats.privatePostCount > 0 && (
            <p className={`muted small ${styles.privateHint}`}>
              其中 {stats.privatePostCount} 篇私密帖，只有你自己能看到
            </p>
          )}
        </div>
        <Link to="/posts/new" className="btn btn-primary">
          写点什么
        </Link>
      </section>

      <nav className={styles.tabs} aria-label="个人中心分区">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            className={`${styles.tab} ${tab === item.key ? styles.tabActive : ''}`}
            onClick={() => switchTab(item.key)}
            aria-current={tab === item.key ? 'page' : undefined}
          >
            {item.label}
            {item.key === 'followers' && stats ? ` ${stats.followerCount}` : ''}
            {item.key === 'followees' && stats ? ` ${stats.followeeCount}` : ''}
          </button>
        ))}
      </nav>

      {error && <p className="error-text">{error}</p>}

      {loading && !data && (
        <div aria-busy="true" aria-label="加载中">
          <div className="skeleton" style={{ height: 96, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 96 }} />
        </div>
      )}

      {data?.kind === 'posts' &&
        (data.page.records.length === 0 ? (
          <div className="empty">还没有发过帖子</div>
        ) : (
          <>
            {data.page.records.map((post) => (
              <div key={post.id} className={styles.postWrap}>
                {post.visibility === VISIBILITY_PRIVATE && (
                  <span className={`tag ${styles.privateTag}`}>私密</span>
                )}
                <PostCard post={post} />
              </div>
            ))}
            <Pager
              page={page}
              total={data.page.total}
              size={PAGE_SIZE}
              loading={loading}
              unit="篇"
              onChange={setPage}
            />
          </>
        ))}

      {data?.kind === 'comments' &&
        (data.page.records.length === 0 ? (
          <div className="empty">还没有评论过</div>
        ) : (
          <>
            <ul className={styles.commentList}>
              {data.page.records.map((comment) => (
                <li key={comment.id} className={`panel ${styles.comment}`}>
                  <p className={styles.commentBody}>{comment.content}</p>
                  <div className={styles.commentMeta}>
                    <span className="muted small">评论于</span>
                    <Link to={`/posts/${comment.postId}`} className="small">
                      {comment.postTitle}
                    </Link>
                    <span className="muted small">{formatTime(comment.createTime)}</span>
                  </div>
                </li>
              ))}
            </ul>
            <Pager
              page={page}
              total={data.page.total}
              size={PAGE_SIZE}
              loading={loading}
              unit="条"
              onChange={setPage}
            />
          </>
        ))}

      {data?.kind === 'users' &&
        (data.page.records.length === 0 ? (
          <div className="empty">{tab === 'followers' ? '还没有粉丝' : '还没有关注任何人'}</div>
        ) : (
          <>
            <ul className={styles.userList}>
              {data.page.records.map((item) => (
                <li key={item.id} className={`panel ${styles.userRow}`}>
                  <div className={styles.userAvatar} aria-hidden="true">
                    {item.nickname.slice(0, 1)}
                  </div>
                  <div className={styles.userMain}>
                    <span className={styles.userName}>{item.nickname}</span>
                    <span className="muted small">
                      {item.postCount} 帖子 · {item.followerCount} 粉丝
                    </span>
                  </div>
                  <button
                    type="button"
                    className={`btn btn-sm ${item.following ? '' : 'btn-primary'}`}
                    onClick={() => handleFollowToggle(item)}
                  >
                    {item.following ? '已关注' : '关注'}
                  </button>
                </li>
              ))}
            </ul>
            <Pager
              page={page}
              total={data.page.total}
              size={PAGE_SIZE}
              loading={loading}
              unit="人"
              onChange={setPage}
            />
          </>
        ))}
    </div>
  )
}
