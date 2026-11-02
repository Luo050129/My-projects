import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  addComment,
  deletePost,
  enhancePost,
  fetchComments,
  fetchInteraction,
  fetchPost,
  fetchUserSummary,
  followUser,
  toggleFavorite,
  toggleLike,
  unfollowUser
} from '../api/endpoints'
import type { Comment, Post, PostInteraction, UserSummary } from '../api/types'
import { VISIBILITY_PRIVATE } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useToast } from '../components/ToastProvider'
import { useAsync } from '../hooks/useAsync'
import { formatTime, splitTags } from '../utils/format'
import styles from './PostDetailPage.module.css'

interface DetailData {
  post: Post
  comments: Comment[]
  interaction: PostInteraction
  author: UserSummary | null
}

const ANON_INTERACTION: PostInteraction = { liked: false, favorited: false }

export default function PostDetailPage() {
  const { id = '' } = useParams()
  const navigate = useNavigate()
  const { isLoggedIn, user } = useAuth()
  const { showToast } = useToast()

  const [commentText, setCommentText] = useState('')
  const [posting, setPosting] = useState(false)
  const [enhancing, setEnhancing] = useState(false)

  const { data, loading, error, reload, setData, setError } = useAsync<DetailData>(
    async (signal) => {
      const post = await fetchPost(id, signal)
      // 帖子拿到后再并发取评论、交互状态、作者摘要
      const [commentsPage, interaction, author] = await Promise.all([
        fetchComments(id, signal),
        isLoggedIn ? fetchInteraction(id, signal) : Promise.resolve(ANON_INTERACTION),
        fetchUserSummary(post.authorId, signal).catch(() => null)
      ])
      return { post, comments: commentsPage.records ?? [], interaction, author }
    },
    [id, isLoggedIn]
  )

  const post = data?.post ?? null
  const comments = data?.comments ?? []
  const interaction = data?.interaction ?? ANON_INTERACTION
  const author = data?.author ?? null

  const isOwner = Boolean(user && post && String(post.authorId) === String(user.userId))
  const isPrivate = post?.visibility === VISIBILITY_PRIVATE
  const tags = useMemo(() => splitTags(post?.tags), [post?.tags])

  const handleLike = async () => {
    if (!post || !data) return
    const snapshot = data
    const nextLiked = !interaction.liked

    // 乐观更新：状态与计数一起改，点击立刻有反馈
    setData((prev) =>
      prev
        ? {
            ...prev,
            post: {
              ...prev.post,
              likeCount: Math.max(0, prev.post.likeCount + (nextLiked ? 1 : -1))
            },
            interaction: { ...prev.interaction, liked: nextLiked }
          }
        : prev
    )

    try {
      await toggleLike(post.id)
      // 计数以服务端为准（并发点赞时本地加减可能偏），重新拉一次对齐
      reload()
    } catch (err) {
      setData(() => snapshot)
      setError(err instanceof Error ? err.message : '点赞失败')
    }
  }

  const handleFavorite = async () => {
    if (!post || !data) return
    const snapshot = data
    const nextFavorited = !interaction.favorited

    setData((prev) =>
      prev ? { ...prev, interaction: { ...prev.interaction, favorited: nextFavorited } } : prev
    )

    try {
      const result = await toggleFavorite(post.id)
      showToast(result ? '已收藏' : '已取消收藏', result ? 'ok' : 'info')
      if (result !== nextFavorited) reload()
    } catch (err) {
      setData(() => snapshot)
      setError(err instanceof Error ? err.message : '收藏失败')
    }
  }

  const handleFollowAuthor = async () => {
    if (!author) return
    try {
      if (author.following) {
        await unfollowUser(author.id)
        showToast(`已取消关注 ${author.nickname}`, 'info')
      } else {
        await followUser(author.id)
        showToast(`已关注 ${author.nickname}`, 'ok')
      }
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败')
    }
  }

  const handleEnhance = async () => {
    if (!post) return
    setEnhancing(true)
    try {
      const result = await enhancePost(post.id)
      showToast(result.safe ? 'AI 摘要已生成' : '内容判定为不合规，未生成摘要', result.safe ? 'ok' : 'bad')
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'AI 摘要生成失败')
    } finally {
      setEnhancing(false)
    }
  }

  const handleSubmitComment = async (event: FormEvent) => {
    event.preventDefault()
    if (!post || !commentText.trim()) return

    setPosting(true)
    try {
      await addComment(post.id, commentText.trim())
      setCommentText('')
      reload()
      showToast('评论已发布', 'ok')
    } catch (err) {
      setError(err instanceof Error ? err.message : '评论失败')
    } finally {
      setPosting(false)
    }
  }

  const handleDelete = async () => {
    if (!post) return
    if (!window.confirm('确定删除这篇帖子？')) return
    try {
      await deletePost(post.id)
      showToast('帖子已删除', 'ok')
      navigate('/posts', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : '删除失败')
    }
  }

  // 用 navigate(-1) 而不是固定回 /posts：能带回列表页的页码与搜索词
  const goBack = () => {
    if (window.history.length > 1) navigate(-1)
    else navigate('/posts')
  }

  return (
    <div>
      <button type="button" className={`muted small ${styles.back}`} onClick={goBack}>
        ← 返回列表
      </button>

      {error && <p className="error-text">{error}</p>}

      {loading && !data && (
        <div aria-busy="true" aria-label="加载帖子">
          <div className="skeleton" style={{ height: 220, marginBottom: 16 }} />
          <div className="skeleton" style={{ height: 120 }} />
        </div>
      )}

      {post && (
        <>
          {isPrivate && (
            <p className={styles.privateBanner}>
              这是一篇私密帖子，只有你自己能看到，不会出现在首页列表
            </p>
          )}

          <article className="panel">
            <h1 className={styles.title}>{post.title}</h1>
            <div className={styles.meta}>
              <span>{post.authorName}</span>
              {author && !isOwner && isLoggedIn && (
                <button type="button" className={styles.followBtn} onClick={handleFollowAuthor}>
                  {author.following ? '已关注' : '+ 关注'}
                </button>
              )}
              <span className={styles.dot}>·</span>
              <span>{formatTime(post.createTime)}</span>
              <span className={styles.dot}>·</span>
              <span>{post.viewCount} 浏览</span>
              {post.postType === 'IMAGE' && (
                <span className={`tag ${styles.typeTag}`}>图文</span>
              )}
            </div>

            {post.coverImage && (
              <img src={post.coverImage} alt={post.title} className={styles.cover} />
            )}

            {tags.length > 0 && (
              <div className={styles.tags}>
                {tags.map((tag) => (
                  <span key={tag} className="tag">
                    {tag}
                  </span>
                ))}
              </div>
            )}

            <div className={styles.content}>{post.content || post.summary || '（无正文）'}</div>

            <div className={styles.actions}>
              <button
                className={`btn ${interaction.liked ? styles.btnActive : ''}`}
                disabled={!isLoggedIn}
                onClick={handleLike}
                title={isLoggedIn ? '点赞' : '登录后可点赞'}
              >
                {interaction.liked ? '已赞' : isLoggedIn ? '点赞' : '登录后可点赞'} ·{' '}
                {post.likeCount}
              </button>
              <button
                className={`btn ${interaction.favorited ? styles.btnActive : ''}`}
                disabled={!isLoggedIn}
                onClick={handleFavorite}
                title={isLoggedIn ? '收藏' : '登录后可收藏'}
              >
                {interaction.favorited ? '已收藏' : '收藏'}
              </button>
              {isOwner && (
                <>
                  <button className="btn" onClick={handleEnhance} disabled={enhancing}>
                    {enhancing ? '生成中…' : 'AI 生成摘要'}
                  </button>
                  <Link to={`/posts/${post.id}/edit`} className="btn">
                    编辑
                  </Link>
                  <button className="btn btn-danger" onClick={handleDelete}>
                    删除
                  </button>
                </>
              )}
            </div>

            {post.aiEnhanced === 1 && post.summary && (
              <p className={styles.aiSummary}>
                <strong>AI 摘要：</strong>
                {post.summary}
              </p>
            )}
          </article>

          <section className="panel">
            <h2 className={styles.sectionTitle}>评论 · {comments.length}</h2>

            {isLoggedIn ? (
              <form onSubmit={handleSubmitComment} className={styles.commentForm}>
                <textarea
                  value={commentText}
                  onChange={(event) => setCommentText(event.target.value)}
                  placeholder="说点什么…"
                  maxLength={500}
                />
                <div className={styles.commentActions}>
                  <button className="btn btn-primary btn-sm" type="submit" disabled={posting}>
                    {posting ? '发送中…' : '发表评论'}
                  </button>
                </div>
              </form>
            ) : (
              <p className="muted small">
                登录后可以评论。<Link to="/login">去登录</Link>
              </p>
            )}

            {comments.length === 0 ? (
              <div className="empty">还没有评论</div>
            ) : (
              <ul className={styles.commentList}>
                {comments.map((comment) => (
                  <li key={comment.id} className={styles.comment}>
                    <div className={styles.commentHead}>
                      <span className={styles.authorName}>{comment.authorName}</span>
                      <span className="muted small">{formatTime(comment.createTime)}</span>
                    </div>
                    <p className={styles.commentBody}>{comment.content}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </div>
  )
}
