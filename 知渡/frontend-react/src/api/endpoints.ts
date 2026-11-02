/**
 * 后端接口清单。所有读接口都接受可选 AbortSignal，
 * 让页面可以在参数变化 / 卸载时取消上一轮请求。
 */

import { request, upload } from './client'
import type {
  AiResult,
  Comment,
  LoginResponse,
  MyComment,
  Notification,
  PageResult,
  Post,
  PostInteraction,
  PostPayload,
  UploadResult,
  UserProfile,
  UserStats,
  UserSummary
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

/** 用户公开摘要（昵称、帖子数、粉丝/关注数、我是否已关注） */
export const fetchUserSummary = (id: string | number, signal?: AbortSignal) =>
  request<UserSummary>(`/user/${id}`, { signal })

export const fetchFollowers = (
  id: string | number,
  current = 1,
  size = 20,
  signal?: AbortSignal
) => request<PageResult<UserSummary>>(`/user/${id}/followers?current=${current}&size=${size}`, { signal })

export const fetchFollowees = (
  id: string | number,
  current = 1,
  size = 20,
  signal?: AbortSignal
) => request<PageResult<UserSummary>>(`/user/${id}/followees?current=${current}&size=${size}`, { signal })

export const followUser = (id: string | number) =>
  request<void>(`/user/${id}/follow`, { method: 'POST' })

export const unfollowUser = (id: string | number) =>
  request<void>(`/user/${id}/follow`, { method: 'DELETE' })

// ---------- 个人中心 ----------
export const fetchMyStats = (signal?: AbortSignal) => request<UserStats>('/me/stats', { signal })

export const fetchMyPosts = (
  params: { current: number; size: number; includePrivate?: boolean },
  signal?: AbortSignal
) => {
  const q = new URLSearchParams({
    current: String(params.current),
    size: String(params.size),
    includePrivate: String(params.includePrivate ?? true)
  })
  return request<PageResult<Post>>(`/me/posts?${q.toString()}`, { signal })
}

export const fetchMyComments = (current = 1, size = 20, signal?: AbortSignal) =>
  request<PageResult<MyComment>>(`/me/comments?current=${current}&size=${size}`, { signal })

export const fetchMyLikes = (current = 1, size = 10, signal?: AbortSignal) =>
  request<PageResult<Post>>(`/me/likes?current=${current}&size=${size}`, { signal })

export const fetchMyFavorites = (current = 1, size = 10, signal?: AbortSignal) =>
  request<PageResult<Post>>(`/me/favorites?current=${current}&size=${size}`, { signal })

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

/** 收藏 / 取消收藏，返回切换后的状态 */
export const toggleFavorite = (id: string | number, signal?: AbortSignal) =>
  request<boolean>(`/posts/${id}/favorite`, { method: 'POST', signal })

/** 当前用户与该帖的交互状态（未登录返回全 false） */
export const fetchInteraction = (id: string | number, signal?: AbortSignal) =>
  request<PostInteraction>(`/posts/${id}/interaction`, { signal })

/** AI 摘要增强：后端调 DeepSeek，失败会规则降级，不会抛 5xx */
export const enhancePost = (id: string | number, signal?: AbortSignal) =>
  request<AiResult>(`/ai/posts/${id}/enhance`, { method: 'POST', signal, timeout: 60_000 })

// ---------- 评论 ----------
/** 后端返回的是分页结构，不是裸数组 */
export const fetchComments = (postId: string | number, signal?: AbortSignal) =>
  request<PageResult<Comment>>(`/comments/posts/${postId}?current=1&size=100`, { signal })

export const addComment = (
  postId: number,
  content: string,
  parentId: number | null = null,
  signal?: AbortSignal
) => request<number>('/comments', { method: 'POST', body: { postId, content, parentId }, signal })

// ---------- 通知 ----------
/** 与评论同理：后端返回分页结构，不是裸数组 */
export const fetchNotifications = (current = 1, size = 50, signal?: AbortSignal) =>
  request<PageResult<Notification>>(`/notifications?current=${current}&size=${size}`, { signal })

export const fetchUnreadCount = (signal?: AbortSignal) =>
  request<number>('/notifications/unread-count', { signal })

export const readNotification = (id: number, signal?: AbortSignal) =>
  request<void>(`/notifications/${id}/read`, { method: 'POST', signal })

export const readAllNotifications = (signal?: AbortSignal) =>
  request<void>('/notifications/read-all', { method: 'POST', signal })

// ---------- 文件 ----------
export const uploadImage = (file: File, signal?: AbortSignal) =>
  upload<UploadResult>('/files/upload', file, signal)
