import { useEffect, useState } from 'react'

/**
 * 输入防抖：Vue 版搜索必须点按钮或回车，
 * React 版在输入停顿 400ms 后自动搜一次，同时保留回车立即搜索。
 * 慢网络下连续输入不会打出一串请求。
 */
export function useDebouncedValue<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delay)
    return () => window.clearTimeout(timer)
  }, [value, delay])

  return debounced
}
