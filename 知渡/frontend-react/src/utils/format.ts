/** 后端返回 ISO 时间（2026-10-21T10:00:00），统一显示到分钟 */
export function formatTime(value: string | null | undefined): string {
  if (!value) return ''
  const s = String(value).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}

/** "Redis, 缓存, 并发" → ["Redis", "缓存", "并发"] */
export function splitTags(tags: string | null | undefined): string[] {
  if (!tags) return []
  return tags
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
}

export const MAX_COVER_SIZE = 5 * 1024 * 1024
export const ALLOWED_COVER_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']

/** 前端预校验：只为了少发无效请求，服务端仍会独立校验，不能当作信任边界 */
export function validateCover(file: File): string {
  if (!ALLOWED_COVER_TYPES.includes(file.type)) {
    return '仅支持 PNG / JPG / GIF / WEBP 图片'
  }
  if (file.size > MAX_COVER_SIZE) {
    return `图片超过 5MB（当前 ${(file.size / 1024 / 1024).toFixed(1)}MB）`
  }
  return ''
}
