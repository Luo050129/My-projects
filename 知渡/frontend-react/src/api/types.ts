/**
 * 后端 DTO 的镜像类型。
 *
 * 与 Java record（PostVO / CommentVO / NotificationVO / LoginResponse / UploadResult）
 * 一一对应，字段名和可空性保持一致 —— 类型写错在 `tsc --noEmit` 阶段就会报错，
 * 而不是等到运行时读到 undefined 才发现。
 */

/** 后端统一响应壳：code = 0 表示成功 */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** PostVO */
export interface Post {
  id: number
  authorId: number
  authorName: string
  title: string
  summary: string | null
  content: string | null
  tags: string | null
  coverImage: string | null
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  /** 1 = 已被 AI 增强过（有摘要） */
  aiEnhanced: number
  /** 后端 LocalDateTime，序列化为 "2026-10-21T10:00:00" */
  createTime: string
}

/** CommentVO */
export interface Comment {
  id: number
  postId: number
  userId: number
  authorName: string
  parentId: number | null
  content: string
  createTime: string
}

export type NotificationType = 'comment' | 'like' | 'system'

/** NotificationVO */
export interface Notification {
  id: number
  type: NotificationType | string
  content: string
  refId: number | null
  /** 0 = 未读 */
  readStatus: number
  createTime: string
}

/** LoginResponse */
export interface LoginResponse {
  userId: number
  nickname: string
  role: string
  token: string
}

/** UserVO */
export interface UserProfile {
  id: number
  username: string
  nickname: string
  role: string
  status: number
  createTime: string
}

/** UploadResult */
export interface UploadResult {
  url: string
  name: string
  size: number
  contentType: string
}

/** 后端 DeepSeekDto.AiResult：LLM 返回的摘要与标签，safe 为 false 表示内容被判为不合规 */
export interface AiResult {
  summary: string
  tags: string
  safe: boolean
}

/** 登录态里的当前用户（比 UserVO 窄，够用即可） */
export interface AuthUser {
  userId: number
  nickname: string
  role: string
}

export interface PostPayload {
  title: string
  content: string
  tags: string
  coverImage: string | null
}

export interface PostQuery {
  current: number
  size: number
  keyword: string
}
