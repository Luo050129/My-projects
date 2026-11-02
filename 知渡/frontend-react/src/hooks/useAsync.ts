import { useCallback, useEffect, useRef, useState } from 'react'
import type { DependencyList } from 'react'
import { isAbortError } from '../api/client'

export interface AsyncResult<T> {
  data: T | null
  loading: boolean
  error: string
  /** 手动重跑（如提交表单后刷新列表） */
  reload: () => void
  /** 局部更新数据，用于乐观更新与回滚 */
  setData: (next: T | ((prev: T | null) => T | null)) => void
  setError: (message: string) => void
}

interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string
}

/**
 * 统一请求生命周期：加载态、错误态、重跑、取消。
 *
 * Vue 版的五个视图里，`loading / error / try-catch-finally` 这组样板各写了一遍；
 * 这里是唯一实现，页面只需要提供"怎么取数据 + 什么时候重取"。
 *
 * 取消策略：deps 变化或组件卸载时 abort 上一轮请求。
 * 这样快速切页 / 连续翻页时，晚到的旧响应不会覆盖新状态（Vue 版存在这个竞态）。
 */
export function useAsync<T>(
  task: (signal: AbortSignal) => Promise<T>,
  deps: DependencyList,
  initialData: T | null = null
): AsyncResult<T> {
  const taskRef = useRef(task)
  taskRef.current = task

  const [state, setState] = useState<AsyncState<T>>({
    data: initialData,
    loading: true,
    error: ''
  })
  const [nonce, setNonce] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    let alive = true

    setState((prev) => ({ ...prev, loading: true, error: '' }))

    taskRef
      .current(controller.signal)
      .then((data) => {
        if (!alive) return
        setState({ data, loading: false, error: '' })
      })
      .catch((err: unknown) => {
        // 主动取消不算错误，保留上一份数据，界面上不会闪一下错误提示
        if (!alive || isAbortError(err)) return
        const message = err instanceof Error ? err.message : '加载失败'
        setState((prev) => ({ ...prev, loading: false, error: message }))
      })

    return () => {
      alive = false
      controller.abort()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, nonce])

  const reload = useCallback(() => setNonce((n) => n + 1), [])

  const setData = useCallback((next: T | ((prev: T | null) => T | null)) => {
    setState((prev) => ({
      ...prev,
      data: typeof next === 'function' ? (next as (p: T | null) => T | null)(prev.data) : next
    }))
  }, [])

  const setError = useCallback((message: string) => {
    setState((prev) => ({ ...prev, error: message }))
  }, [])

  return { ...state, reload, setData, setError }
}
