/**
 * 后端 DTO 的镜像类型。
 *
 * 与 Java record（PostVO / CommentVO / NotificationVO / LoginResponse / UploadResult /
 * UserSummaryVO / UserStatsVO / MyCommentVO）一一对应，字段名和可空性保持一致 ——
 * 类型写错在 `tsc --noEmit` 阶段就会报错，而不是等到运行时读到 undefined 才发现。
 */

/** 后端统一响应壳：code = 0 表示成功 */
export interface ApiEnvelope<T> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus 分页结构（后端 PageResult 只有这四个字段） */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/** 帖子形态：纯文字 / 图文 */
export type PostType = 'TEXT' | 'IMAGE'

export const VISIBILITY_PUBLIC = 0
export const VISIBILITY_PRIVATE = 1

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
  postType: PostType
  /** 0 公开 / 1 私密 */
  visibility: number
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  /** 1 = 已被 AI 增强过（有摘要） */
  aiEnhanced: number
  /** 作者声明 AI 生成 / 辅助创作：前端必须显式展示该标识 */
  aiGenerated: boolean
  /** 0 免审 / 1 待审 / 2 通过 / 3 驳回 */
  reviewStatus: number
  /** 后端 LocalDateTime，序列化为 "2026-10-21T10:00:00" */
  createTime: string
}

/** 搜索联想项：GET /search/suggest，按点赞数降序 */
export interface PostSuggestion {
  id: number
  title: string
  /** 命中词附近的上下文，来自摘要 / 正文 / 标签 */
  snippet: string
  authorName: string
  likeCount: number
  commentCount: number
  matchedWord: string
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

/** MyCommentVO：我的评论，带帖子标题 */
export interface MyComment {
  id: number
  postId: number
  postTitle: string
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

/** UserSummaryVO：粉丝 / 关注列表里的条目 */
export interface UserSummary {
  id: number
  nickname: string
  role: string
  postCount: number
  followerCount: number
  followeeCount: number
  following: boolean
}

/** UserStatsVO：个人中心顶部统计 */
export interface UserStats {
  postCount: number
  privatePostCount: number
  commentCount: number
  likedCount: number
  favoriteCount: number
  followerCount: number
  followeeCount: number
  likeReceivedCount: number
}

/** UploadResult */
export interface UploadResult {
  url: string
  name: string
  size: number
  contentType: string
}

/** 后端 DeepSeekDto.AiResult：safe 为 false 表示内容被判为不合规 */
export interface AiResult {
  summary: string
  tags: string
  safe: boolean
}

/** PostInteractionVO：当前用户与帖子的交互状态 */
export interface PostInteraction {
  liked: boolean
  favorited: boolean
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
  postType: PostType
  visibility: number
  /** 作者声明 AI 生成 / 辅助创作；不传按 false */
  aiGenerated?: boolean
}

export interface GovernanceInfo {
  postId: number
  aiGenerated: boolean
  reviewStatus: number
  aiDeclaration: string | null
}

export interface ReportItem {
  id: number
  postId: number
  title: string
  reason: string
  status: number
  handleNote: string | null
  createTime: string
}

export interface PostQuery {
  current: number
  size: number
  keyword: string
}
