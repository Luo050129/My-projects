import { auth, signOut } from './auth'

const BASE = '/api'

/** 401 回跳由路由层注入，避免 api 层直接依赖 router 造成循环引用 */
let unauthorizedHandler = null
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn
}

/** 统一解析响应：HTTP 层与业务层的错误都在这里收敛成 Error */
async function parseResponse(res) {
  let payload = null
  try {
    payload = await res.json()
  } catch {
    payload = null
  }

  if (res.status === 401) {
    // token 过期或被篡改：清登录态并立刻回跳，不再等下一次导航
    signOut()
    if (unauthorizedHandler) {
      try {
        unauthorizedHandler(window.location.pathname + window.location.search)
      } catch {
        // 跳转失败不能吞掉原始的 401 错误
      }
    }
    throw new Error(payload?.message || '登录已过期，请重新登录')
  }

  if (!res.ok) {
    throw new Error(payload?.message || `请求失败（HTTP ${res.status}）`)
  }

  if (payload && typeof payload.code === 'number' && payload.code !== 0) {
    throw new Error(payload.message || `请求失败（${payload.code}）`)
  }

  return payload?.data
}

/**
 * 统一请求封装：
 * - 自动带 JWT
 * - 后端约定 code=0 为成功，非 0 抛业务错误
 * - 401 清登录态并回跳登录页
 */
export async function request(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth.token) {
    headers.Authorization = `Bearer ${auth.token}`
  }

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  return parseResponse(res)
}

// ---------- 认证 ----------
export const login = (username, password) =>
  request('/auth/login', { method: 'POST', body: { username, password } })

export const register = (username, password, nickname) =>
  request('/auth/register', { method: 'POST', body: { username, password, nickname } })

// ---------- 用户 ----------
export const fetchMe = () => request('/user/me')
export const fetchUsers = (current = 1, size = 10) =>
  request(`/user/page?current=${current}&size=${size}`)

// ---------- 搜索 ----------
// 输入联想：多词包含匹配，后端按点赞数降序返回
export const fetchSuggestions = (keyword, limit = 8) => {
  const q = new URLSearchParams({ keyword, limit })
  return request(`/search/suggest?${q.toString()}`)
}

// ---------- 帖子 ----------
export const fetchPosts = (current = 1, size = 10, keyword = '') => {
  const q = new URLSearchParams({ current, size })
  if (keyword) q.set('keyword', keyword)
  return request(`/posts?${q.toString()}`)
}

export const fetchPost = (id) => request(`/posts/${id}`)

export const createPost = (payload) =>
  request('/posts', { method: 'POST', body: payload })

export const updatePost = (id, payload) =>
  request(`/posts/${id}`, { method: 'PUT', body: payload })

export const deletePost = (id) => request(`/posts/${id}`, { method: 'DELETE' })

export const toggleLike = (id) => request(`/posts/${id}/like`, { method: 'POST' })

export const enhancePost = (id) =>
  request(`/ai/posts/${id}/enhance`, { method: 'POST' })

// ---------- 评论 ----------
// 后端返回 {records,total,current,size} 分页对象，调用方需取 records
export const fetchComments = (postId) => request(`/comments/posts/${postId}`)

export const addComment = (postId, content, parentId = null) =>
  request('/comments', { method: 'POST', body: { postId, content, parentId } })

// ---------- 通知 ----------
// 同样返回分页对象
export const fetchNotifications = () => request('/notifications')
export const fetchUnreadCount = () => request('/notifications/unread-count')
export const readNotification = (id) =>
  request(`/notifications/${id}/read`, { method: 'POST' })
export const readAllNotifications = () =>
  request('/notifications/read-all', { method: 'POST' })

// ---------- 文件 ----------
/**
 * 上传图片。必须走 FormData：手动设 Content-Type 会覆盖掉浏览器生成的
 * multipart boundary，后端会报「Current request is not a multipart request」。
 */
export async function uploadImage(file) {
  const fd = new FormData()
  fd.append('file', file)

  const headers = {}
  if (auth.token) {
    headers.Authorization = `Bearer ${auth.token}`
  }

  const res = await fetch(BASE + '/files/upload', { method: 'POST', headers, body: fd })
  return parseResponse(res)
}
