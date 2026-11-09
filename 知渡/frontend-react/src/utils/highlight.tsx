import type { ReactNode } from 'react'

/**
 * 搜索高亮：与后端 PostKeyword.split 保持同一套切词规则。
 * 后端做包含匹配，前端用同样的词把「命中的文字片段」标出来，两者才会对齐。
 */
const SEPARATOR = /[\s,，、;；|/\\+]+/
const NON_WORD = /[^\p{L}\p{N}]/gu
const MAX_WORDS = 5
const MAX_WORD_LENGTH = 40
const MAX_INPUT_LENGTH = 200

/** 把用户输入切成词：清洗标点 / LIKE 通配符、去重、限长限个数 */
export function splitWords(input: string | null | undefined): string[] {
  if (!input) return []
  const parts = input.trim().slice(0, MAX_INPUT_LENGTH).split(SEPARATOR)
  const words: string[] = []
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

export interface HighlightSegment {
  text: string
  hit: boolean
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/**
 * 把文本切成「普通片段 / 命中片段」序列。
 * 用捕获分组一次 split，避免多词嵌套区间；长词优先，防止短词抢先截断。
 * 返回的仍是纯字符串，由调用方以文本节点渲染 —— 不走 dangerouslySetInnerHTML，
 * 因此帖子标题里带 HTML 也不会造成 XSS。
 */
export function highlightSegments(
  text: string | null | undefined,
  words: string[]
): HighlightSegment[] {
  if (!text) return []
  const unique = [...new Set(words.filter(Boolean))].sort((a, b) => b.length - a.length)
  if (unique.length === 0) return [{ text, hit: false }]

  const pattern = new RegExp(`(${unique.map(escapeRegExp).join('|')})`, 'giu')
  const lowered = new Set(unique.map((w) => w.toLocaleLowerCase()))
  return text
    .split(pattern)
    .filter((piece) => piece.length > 0)
    .map((piece) => ({ text: piece, hit: lowered.has(piece.toLocaleLowerCase()) }))
}

/** 标题 / 摘要 / 联想项通用的高亮渲染 */
export function Highlight({
  text,
  words
}: {
  text: string | null | undefined
  words: string[]
}): ReactNode {
  if (!text) return null
  const segments = highlightSegments(text, words)
  if (segments.length === 0) return null
  return (
    <>
      {segments.map((segment, index) =>
        segment.hit ? (
          <mark key={index}>{segment.text}</mark>
        ) : (
          <span key={index}>{segment.text}</span>
        )
      )}
    </>
  )
}
