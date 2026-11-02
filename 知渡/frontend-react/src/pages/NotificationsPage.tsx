import { useState } from 'react'
import { Link } from 'react-router-dom'
import {
  fetchNotifications,
  fetchUnreadCount,
  readAllNotifications,
  readNotification
} from '../api/endpoints'
import type { Notification } from '../api/types'
import { useToast } from '../components/ToastProvider'
import { useAsync } from '../hooks/useAsync'
import { formatTime } from '../utils/format'
import styles from './NotificationsPage.module.css'

interface NotifyData {
  items: Notification[]
  unread: number
}

const TYPE_LABEL: Record<string, string> = {
  comment: '评论',
  like: '点赞',
  system: '系统'
}

export default function NotificationsPage() {
  const { showToast } = useToast()
  const [marking, setMarking] = useState(false)

  const { data, loading, error, reload, setData, setError } = useAsync<NotifyData>(
    async (signal) => {
      const [page, unread] = await Promise.all([
        fetchNotifications(1, 50, signal),
        fetchUnreadCount(signal)
      ])
      return { items: page?.records ?? [], unread: unread ?? 0 }
    },
    []
  )

  const items = data?.items ?? []
  const unread = data?.unread ?? 0

  const markOne = async (item: Notification) => {
    const snapshot = data
    // 乐观更新：先把这条状态改成已读，未读数减一，界面不用等接口回来
    setData((prev) =>
      prev
        ? {
            items: prev.items.map((n) => (n.id === item.id ? { ...n, readStatus: 1 } : n)),
            unread: Math.max(0, prev.unread - 1)
          }
        : prev
    )

    try {
      await readNotification(item.id)
    } catch (err) {
      setData(() => snapshot)
      setError(err instanceof Error ? err.message : '标记失败')
    }
  }

  const markAll = async () => {
    setMarking(true)
    try {
      await readAllNotifications()
      showToast('全部已标为已读', 'ok')
      reload()
    } catch (err) {
      setError(err instanceof Error ? err.message : '操作失败')
    } finally {
      setMarking(false)
    }
  }

  return (
    <div>
      <div className={styles.head}>
        <h1 className={styles.title}>通知</h1>
        <button className="btn btn-sm" disabled={!unread || marking} onClick={markAll}>
          {marking ? '处理中…' : `全部标为已读（${unread}）`}
        </button>
      </div>

      {error && <p className="error-text">{error}</p>}

      {loading && !data && (
        <div aria-busy="true" aria-label="加载通知">
          <div className="skeleton" style={{ height: 64, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 64, marginBottom: 12 }} />
          <div className="skeleton" style={{ height: 64 }} />
        </div>
      )}

      {!loading && items.length === 0 && <div className="empty">暂无通知</div>}

      <ul className={styles.list}>
        {items.map((item) => (
          <li
            key={item.id}
            className={`panel ${styles.item} ${item.readStatus === 0 ? styles.unread : ''}`}
          >
            <div className={styles.main}>
              <span className={styles.type}>{TYPE_LABEL[item.type] ?? item.type ?? '通知'}</span>
              <span className={styles.content}>{item.content}</span>
            </div>
            <div className={styles.side}>
              <span className="muted small">{formatTime(item.createTime)}</span>
              {item.readStatus === 0 ? (
                <button className="btn btn-sm" onClick={() => markOne(item)}>
                  标为已读
                </button>
              ) : (
                item.refId && (
                  <Link to={`/posts/${item.refId}`} className="small">
                    查看
                  </Link>
                )
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
