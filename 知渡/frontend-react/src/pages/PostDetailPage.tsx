import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  addComment,
  deletePost,
  enhancePost,
  fetchComments,
  fetchPost,
  toggleLike
} from '../api/endpoints'
import type { Comment, Post } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { useToast } from '../components/ToastProvider'
import { useAsync } from '../hooks/useAsync'
import { formatTime, splitTags } from '../utils/format'
import styles from './PostDetailPage.module.css'

interface DetailData {
  post: Post
  comments: Comment[]
}

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
      // 详情与评论并行取，首屏等待时间取两者较大值而不是相加
      const [post, comments] = await Promise.all([
        fetchPost(id, signal),
        fetchComments(id, signal)
      ])
      return { post, comments }
    },
    [id]
  )

  const post = data?.post ?? null
  const comments = data?.comments ?? []
  const isOwner = Boolean(user && post && String(post.authorId) === String(user.userId))
  const tags = splitTags(post?.tags)

  const handleLike = async () => {
    if (!post) return
    const snapshot = data

    // 乐观更新：先加一，交互立刻有反馈；接口失败再整体回滚
    setData((prev) =>
      prev ? { ...prev, post: { ...prev.post, likeCount: prev.post.likeCount + 1 } } : prev
    )

    try {
      await toggleLike(post.id)
      // 接口没有返回最新计数（可能是取消赞），重取一次把真实数字对齐
      reload()
    } catch (err) {
      setData(() => snapshot)
      setError(err instanceof Error ? err.message : '点赞失败')
    }
  }

  const handleEnhance = async () => {
    if (!post) return
    setEnhancing(true)
    try {
      const result = await enhancePost(post.id)
      if (!result.safe) {
        showToast('内容判定为不合规，未生成摘要', 'bad')
      } else {
        showToast('AI 摘要已生成', 'ok')
      }
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
          <article className="panel">
            <h1 className={styles.title}>{post.title}</h1>
            <div className={styles.meta}>
              <span>{post.authorName}</span>
              <span className={styles.dot}>·</span>
              <span>{formatTime(post.createTime)}</span>
              <span className={styles.dot}>·</span>
              <span>{post.viewCount} 浏览</span>
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
                className="btn"
                disabled={!isLoggedIn}
                onClick={handleLike}
                title={isLoggedIn ? '点赞' : '登录后可点赞'}
              >
                {isLoggedIn ? '点赞' : '登录后可点赞'} · {post.likeCount}
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
                      <span className={styles.author}>{comment.authorName}</span>
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
