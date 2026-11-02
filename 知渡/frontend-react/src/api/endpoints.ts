/**
 * 后端接口清单。所有读接口都接受可选 AbortSignal，
 * 让页面可以在参数变化 / 卸载时取消上一轮请求。
 */

import { request, upload } from './client'
import type {
  AiResult,
  Comment,
  LoginResponse,
  Notification,
  PageResult,
  Post,
  PostPayload,
  UploadResult,
  UserProfile
} from './types'

// ---------- 认证 ----------
export const login = (username: string, password: string, signal?: AbortSignal) =>
  request<LoginResponse>('/auth/login', { method: 'POST', body: { username, password }, signal })

export const register = (
  username: string,
  password: string,
  nickname: string,
  signal?: AbortSignal
) =>
  request<LoginResponse>('/auth/register', {
    method: 'POST',
    body: { username, password, nickname },
    signal
  })

// ---------- 用户 ----------
export const fetchMe = (signal?: AbortSignal) => request<UserProfile>('/user/me', { signal })

export const fetchUsers = (current = 1, size = 10, signal?: AbortSignal) =>
  request<PageResult<UserProfile>>(`/user/page?current=${current}&size=${size}`, { signal })

// ---------- 帖子 ----------
export const fetchPosts = (
  params: { current: number; size: number; keyword?: string },
  signal?: AbortSignal
) => {
  const q = new URLSearchParams({
    current: String(params.current),
    size: String(params.size)
  })
  if (params.keyword) q.set('keyword', params.keyword)
  return request<PageResult<Post>>(`/posts?${q.toString()}`, { signal })
}

export const fetchPost = (id: string | number, signal?: AbortSignal) =>
  request<Post>(`/posts/${id}`, { signal })

export const createPost = (payload: PostPayload, signal?: AbortSignal) =>
  request<number>('/posts', { method: 'POST', body: payload, signal })

export const updatePost = (id: string | number, payload: PostPayload, signal?: AbortSignal) =>
  request<void>(`/posts/${id}`, { method: 'PUT', body: payload, signal })

export const deletePost = (id: string | number, signal?: AbortSignal) =>
  request<void>(`/posts/${id}`, { method: 'DELETE', signal })

export const toggleLike = (id: string | number, signal?: AbortSignal) =>
  request<void>(`/posts/${id}/like`, { method: 'POST', signal })

/** AI 摘要增强：后端调 DeepSeek，失败会规则降级，不会抛 5xx */
export const enhancePost = (id: string | number, signal?: AbortSignal) =>
  request<AiResult>(`/ai/posts/${id}/enhance`, { method: 'POST', signal, timeout: 60_000 })

// ---------- 评论 ----------
export const fetchComments = (postId: string | number, signal?: AbortSignal) =>
  request<Comment[]>(`/comments/posts/${postId}`, { signal })

export const addComment = (
  postId: number,
  content: string,
  parentId: number | null = null,
  signal?: AbortSignal
) => request<number>('/comments', { method: 'POST', body: { postId, content, parentId }, signal })

// ---------- 通知 ----------
export const fetchNotifications = (signal?: AbortSignal) =>
  request<Notification[]>('/notifications', { signal })

export const fetchUnreadCount = (signal?: AbortSignal) =>
  request<number>('/notifications/unread-count', { signal })

export const readNotification = (id: number, signal?: AbortSignal) =>
  request<void>(`/notifications/${id}/read`, { method: 'POST', signal })

export const readAllNotifications = (signal?: AbortSignal) =>
  request<void>('/notifications/read-all', { method: 'POST', signal })

// ---------- 文件 ----------
export const uploadImage = (file: File, signal?: AbortSignal) =>
  upload<UploadResult>('/files/upload', file, signal)
