import { Link } from 'react-router-dom'
import type { Post } from '../api/types'
import { formatTime } from '../utils/format'
import styles from './PostCard.module.css'

/**
 * 列表项。抽成独立组件后，列表页只剩"取数 + 分页"，
 * 卡片本身的展示逻辑可以单独改、单独测。
 */
export function PostCard({ post }: { post: Post }) {
  return (
    <article className={`panel ${styles.card}`}>
      {post.coverImage && (
        <Link to={`/posts/${post.id}`} className={styles.thumb}>
          <img src={post.coverImage} alt="封面" loading="lazy" />
        </Link>
      )}
      <div className={styles.main}>
        <Link to={`/posts/${post.id}`} className={styles.title}>
          {post.title}
        </Link>
        {post.summary && <p className={styles.summary}>{post.summary}</p>}
        <div className={styles.meta}>
          <span>{post.authorName}</span>
          <span className={styles.dot}>·</span>
          <span>{formatTime(post.createTime)}</span>
          <span className={styles.dot}>·</span>
          <span>{post.viewCount} 浏览</span>
          <span className={styles.dot}>·</span>
          <span>{post.likeCount} 赞</span>
          <span className={styles.dot}>·</span>
          <span>{post.commentCount} 评论</span>
          {post.aiGenerated && <span className={`tag ${styles.aiTag}`}>AI 辅助创作</span>}
          {post.aiEnhanced === 1 && <span className={`tag ${styles.aiTag}`}>AI 摘要</span>}
        </div>
      </div>
    </article>
  )
}
