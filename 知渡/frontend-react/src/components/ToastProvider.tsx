import { useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { ToastContext } from './toast-context'
import type { ToastApi, ToastTone } from './toast-context'
import styles from './ToastProvider.module.css'

interface ToastItem {
  id: number
  message: string
  tone: ToastTone
}

let seq = 0

/** 全局轻提示：点赞、已读、发布成功等"瞬时反馈"集中在这里 */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const showToast = useCallback((message: string, tone: ToastTone = 'info') => {
    const id = ++seq
    setItems((prev) => [...prev, { id, message, tone }])
    window.setTimeout(() => {
      setItems((prev) => prev.filter((item) => item.id !== id))
    }, 2400)
  }, [])

  const value = useMemo<ToastApi>(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className={styles.stack} role="status" aria-live="polite">
        {items.map((item) => (
          <div key={item.id} className={`${styles.toast} ${styles[item.tone]}`}>
            {item.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast 必须在 <ToastProvider> 内使用')
  return ctx
}
