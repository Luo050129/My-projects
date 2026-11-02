/**
 * 统一请求层。
 *
 * 相比 Vue 版增强的三点：
 * 1. 泛型返回类型 —— 调用方拿到的是 `PageResult<Post>` 而不是 `any`；
 * 2. `AbortSignal` 透传 —— 路由切换 / 组件卸载时取消在途请求，避免竞态覆盖新状态；
 * 3. 超时兜底 —— 后端卡住时前端不会无限转圈，15s 后给出明确错误。
 *
 * token 与 401 处理通过 `configureAuth` 由 AuthProvider 注入，
 * 这样 api 层不 import React，也不会和 Context 形成循环依赖。
 */

import type { ApiEnvelope } from './types'

const BASE = '/api'
const DEFAULT_TIMEOUT = 15_000

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: number
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface AuthBridge {
  getToken: () => string
  onUnauthorized: () => void
}

let bridge: AuthBridge = { getToken: () => '', onUnauthorized: () => {} }

/** AuthProvider 挂载时注入；离开页面时不需要还原，桥接对象本身无状态 */
export function configureAuth(next: AuthBridge): void {
  bridge = next
}

export function isAbortError(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError'
}

async function parseResponse<T>(res: Response): Promise<T> {
  let payload: ApiEnvelope<T> | null = null
  try {
    payload = (await res.json()) as ApiEnvelope<T>
  } catch {
    payload = null
  }

  if (res.status === 401) {
    bridge.onUnauthorized()
    throw new ApiError(payload?.message || '登录已过期，请重新登录', 401)
  }

  if (!res.ok) {
    throw new ApiError(payload?.message || `请求失败（HTTP ${res.status}）`, res.status)
  }

  if (payload && typeof payload.code === 'number' && payload.code !== 0) {
    throw new ApiError(payload.message || `请求失败（${payload.code}）`, res.status, payload.code)
  }

  return payload?.data as T
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
  timeout?: number
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, signal, timeout = DEFAULT_TIMEOUT } = options

  const headers: Record<string, string> = {}
  const token = bridge.getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  // 外部的取消信号 + 内部超时，合并成一个 controller
  const controller = new AbortController()
  let timedOut = false
  const forwardAbort = () => controller.abort()
  if (signal) {
    if (signal.aborted) controller.abort()
    else signal.addEventListener('abort', forwardAbort, { once: true })
  }
  const timer = window.setTimeout(() => {
    timedOut = true
    controller.abort()
  }, timeout)

  try {
    const res = await fetch(BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal
    })
    return await parseResponse<T>(res)
  } catch (err) {
    if (timedOut) {
      throw new ApiError('请求超时，请检查网络或后端服务', 0)
    }
    // 主动取消：原样抛出，交给 useAsync 静默忽略
    if (isAbortError(err)) throw err
    if (err instanceof ApiError) throw err
    throw new ApiError('网络异常，无法连接服务', 0)
  } finally {
    window.clearTimeout(timer)
    signal?.removeEventListener('abort', forwardAbort)
  }
}

/**
 * 文件上传必须走 FormData，且不能手动指定 Content-Type：
 * 浏览器要自己带 multipart boundary，覆盖掉后端会报「not a multipart request」。
 */
export async function upload<T>(path: string, file: File, signal?: AbortSignal): Promise<T> {
  const form = new FormData()
  form.append('file', file)

  const headers: Record<string, string> = {}
  const token = bridge.getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(BASE + path, { method: 'POST', headers, body: form, signal })
  return parseResponse<T>(res)
}
