/** 后端返回 ISO 时间（2026-10-21T10:00:00），这里统一显示到分钟 */
export function formatTime(value) {
  if (!value) return ''
  const s = String(value).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}
