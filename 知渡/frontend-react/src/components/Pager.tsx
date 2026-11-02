import styles from './Pager.module.css'

interface Props {
  page: number
  total: number
  size: number
  loading?: boolean
  unit?: string
  onChange: (page: number) => void
}

/** 分页条。列表页与个人中心的各个 tab 共用，避免每处各写一套上一页/下一页。 */
export function Pager({ page, total, size, loading = false, unit = '条', onChange }: Props) {
  const totalPages = Math.max(1, Math.ceil(total / size))
  if (total <= size) {
    return <p className={styles.hint}>共 {total} {unit}</p>
  }

  return (
    <div className={styles.pager}>
      <button className="btn btn-sm" disabled={page <= 1 || loading} onClick={() => onChange(page - 1)}>
        上一页
      </button>
      <span className="muted small">
        第 {page} / {totalPages} 页 · 共 {total} {unit}
      </span>
      <button
        className="btn btn-sm"
        disabled={page >= totalPages || loading}
        onClick={() => onChange(page + 1)}
      >
        下一页
      </button>
    </div>
  )
}
