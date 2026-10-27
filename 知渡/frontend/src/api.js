import { auth, signOut } from './auth'

const BASE = '/api'

/**
 * 统一请求封装：
 * - 自动带 JWT
 * - 后端约定 code=0 为成功，非 0 抛业务错误
 * - 401 清登录态
 */
export async function request(path, { method = 'GET', body, auth: needAuth = false } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth.token) {
    headers.Authorization = `Bearer ${auth.token}`
  }

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })

  let payload = null
  try {
    payload = await res.json()
  } catch {
    payload = null
  }

  if (res.status === 401) {
    // token 过期或被篡改
    signOut()
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

// ---------- 认证 ----------
export const login = (username, password) =>
  request('/auth/login', { method: 'POST', body: { username, password } })

export const register = (username, password, nickname) =>
  request('/auth/register', { method: 'POST', body: { username, password, nickname } })

// ---------- 用户 ----------
export const fetchMe = () => request('/user/me')
export const fetchUsers = (current = 1, size = 10) =>
  request(`/user/page?current=${current}&size=${size}`)

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
export const fetchComments = (postId) => request(`/comments/posts/${postId}`)

export const addComment = (postId, content, parentId = null) =>
  request('/comments', { method: 'POST', body: { postId, content, parentId } })

// ---------- 通知 ----------
export const fetchNotifications = () => request('/notifications')
export const fetchUnreadCount = () => request('/notifications/unread-count')
export const readNotification = (id) =>
  request(`/notifications/${id}/read`, { method: 'POST' })
export const readAllNotifications = () =>
  request('/notifications/read-all', { method: 'POST' })
