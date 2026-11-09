/** 后端返回 ISO 时间（2026-10-21T10:00:00），这里统一显示到分钟 */
export function formatTime(value) {
  if (!value) return ''
  const s = String(value).replace('T', ' ')
  return s.length > 16 ? s.slice(0, 16) : s
}

/* ---------- 搜索高亮 ----------
 * 与后端 PostKeyword.split 保持同一套切词规则：清洗标点 / LIKE 通配符、
 * 去重、最多 5 词、每词 40 字。返回纯字符串片段，模板里用文本插值渲染，
 * 不使用 v-html，帖子标题含 HTML 也不会造成 XSS。
 */
const SEPARATOR = /[\s,，、;；|/\\+]+/
const NON_WORD = /[^\p{L}\p{N}]/gu
const MAX_WORDS = 5
const MAX_WORD_LENGTH = 40
const MAX_INPUT_LENGTH = 200

export function splitWords(input) {
  if (!input) return []
  const parts = input.trim().slice(0, MAX_INPUT_LENGTH).split(SEPARATOR)
  const words = []
  for (const part of parts) {
    const word = part.replace(NON_WORD, '').slice(0, MAX_WORD_LENGTH)
    if (!word) continue
    const lower = word.toLocaleLowerCase()
    if (words.some((w) => w.toLocaleLowerCase() === lower)) continue
    words.push(word)
    if (words.length >= MAX_WORDS) break
  }
  return words
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function highlightSegments(text, words) {
  if (!text) return []
  const unique = [...new Set((words || []).filter(Boolean))].sort((a, b) => b.length - a.length)
  if (!unique.length) return [{ text, hit: false }]
  const pattern = new RegExp(`(${unique.map(escapeRegExp).join('|')})`, 'giu')
  const lowered = new Set(unique.map((w) => w.toLocaleLowerCase()))
  return String(text)
    .split(pattern)
    .filter((piece) => piece.length > 0)
    .map((piece) => ({ text: piece, hit: lowered.has(piece.toLocaleLowerCase()) }))
}
