<template>
  <div>
    <div class="toolbar">
      <form class="search" @submit.prevent="reload">
        <div class="input-wrap">
          <span class="search-icon" aria-hidden="true">
            <svg viewBox="0 0 20 20" width="17" height="17">
              <circle cx="8.6" cy="8.6" r="5.6" fill="none" stroke="currentColor" stroke-width="1.8" />
              <path d="M12.8 12.8 L17 17" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
            </svg>
          </span>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索帖子 · 空格分隔多个词"
            autocomplete="off"
            aria-label="搜索帖子标题、正文与标签"
            role="combobox"
            aria-autocomplete="list"
            :aria-expanded="suggestOpen"
            aria-controls="post-suggest-list"
            @focus="suggestOpen = suggestions.length > 0"
            @blur="closeSuggest"
            @keydown="onSearchKeydown"
          />
          <button
            v-if="keyword"
            type="button"
            class="clear-btn"
            aria-label="清空搜索"
            @mousedown.prevent
            @click="clearKeyword"
          >×</button>

          <div v-if="suggestOpen" id="post-suggest-list" class="suggest">
            <div class="suggest-head">
              <span>{{ suggestions.length ? '匹配帖子' : '没有匹配的帖子' }}</span>
              <span>按点赞数排序</span>
            </div>
            <ul v-if="suggestions.length" role="listbox" aria-label="搜索联想">
              <li
                v-for="(item, index) in suggestions"
                :key="item.id"
                role="option"
                :aria-selected="index === activeIndex"
              >
                <router-link
                  :to="`/posts/${item.id}`"
                  :class="{ 'suggest-active': index === activeIndex }"
                  @mouseenter="activeIndex = index"
                  @click="pickSuggestion"
                >
                  <span class="suggest-title">
                    <template v-for="(seg, i) in highlightSegments(item.title, suggestWords)" :key="i">
                      <mark v-if="seg.hit">{{ seg.text }}</mark>
                      <span v-else>{{ seg.text }}</span>
                    </template>
                  </span>
                  <span v-if="item.snippet" class="suggest-snippet">
                    <template v-for="(seg, i) in highlightSegments(item.snippet, suggestWords)" :key="i">
                      <mark v-if="seg.hit">{{ seg.text }}</mark>
                      <span v-else>{{ seg.text }}</span>
                    </template>
                  </span>
                  <span class="suggest-meta">
                    <span class="suggest-author">{{ item.authorName }}</span>
                    <span>· {{ item.likeCount }} 赞</span>
                    <span class="hit-word">命中「{{ item.matchedWord }}」</span>
                  </span>
                </router-link>
              </li>
            </ul>
            <p v-else class="suggest-empty">换个关键词，或试试更短的词</p>
          </div>
        </div>
        <button class="btn btn-primary" type="submit">搜索</button>
      </form>
      <router-link v-if="logged" to="/posts/new" class="btn">写帖子</router-link>
    </div>

    <div v-if="submittedKeyword" class="result-bar">
      <span class="result-chip">搜索「{{ submittedKeyword }}」</span>
      <span class="result-meta">任一词命中 · 按点赞数排序 · 共 {{ total }} 条</span>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-if="loading" class="empty">加载中…</div>

    <template v-else>
      <div v-if="!posts.length" class="empty">
        {{ submittedKeyword ? '没有匹配的帖子，换个词试试' : '还没有帖子' }}
      </div>

      <article v-for="post in posts" :key="post.id" class="panel post-card">
        <router-link v-if="post.coverImage" :to="`/posts/${post.id}`" class="thumb">
          <img :src="post.coverImage" alt="封面" loading="lazy" />
        </router-link>
        <div class="post-main">
          <router-link :to="`/posts/${post.id}`" class="post-title">
            <template v-for="(seg, i) in highlightSegments(post.title, resultWords)" :key="i">
              <mark v-if="seg.hit">{{ seg.text }}</mark>
              <span v-else>{{ seg.text }}</span>
            </template>
          </router-link>
          <p v-if="post.summary" class="summary">
            <template v-for="(seg, i) in highlightSegments(post.summary, resultWords)" :key="i">
              <mark v-if="seg.hit">{{ seg.text }}</mark>
              <span v-else>{{ seg.text }}</span>
            </template>
          </p>
          <div class="meta">
            <span class="author">{{ post.authorName }}</span>
            <span class="dot">·</span>
            <span>{{ formatTime(post.createTime) }}</span>
            <span class="dot">·</span>
            <span>{{ post.viewCount }} 浏览</span>
            <span class="dot">·</span>
            <span>{{ post.likeCount }} 赞</span>
            <span class="dot">·</span>
            <span>{{ post.commentCount }} 评论</span>
            <span v-if="post.aiGenerated" class="tag ai-tag">AI 辅助创作</span>
            <span v-if="post.aiEnhanced === 1" class="tag ai-tag">AI 摘要</span>
          </div>
        </div>
      </article>

      <div class="pager">
        <button class="btn btn-sm" :disabled="current <= 1" @click="go(current - 1)">上一页</button>
        <span class="muted small">第 {{ current }} / {{ totalPages }} 页 · 共 {{ total }} 条</span>
        <button class="btn btn-sm" :disabled="current >= totalPages" @click="go(current + 1)">下一页</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchPosts, fetchSuggestions } from '../api'
import { auth } from '../auth'
import { formatTime, highlightSegments, splitWords } from '../utils'

const PAGE_SIZE = 10
const router = useRouter()

const posts = ref([])
const total = ref(0)
const current = ref(1)
// 不用 v-model.trim：它会在每次输入时削掉末尾空格，导致打不出「词1 词2」这种多词查询
const keyword = ref('')
// 已提交到结果列表的词，与正在输入的词分开：防抖期间高亮不能慢半拍
const submittedKeyword = ref('')
const loading = ref(false)
const error = ref('')
const logged = computed(() => Boolean(auth.token))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

const suggestions = ref([])
const suggestOpen = ref(false)
const suggestQuery = ref('')
const activeIndex = ref(-1)
const suggestWords = computed(() => splitWords(suggestQuery.value))
const resultWords = computed(() => splitWords(submittedKeyword.value))
const typing = ref(false)
let suggestTimer = null
let suggestSeq = 0

watch(keyword, (value) => {
  typing.value = true
  activeIndex.value = -1
  clearTimeout(suggestTimer)
  const q = (value || '').trim()
  if (!q) {
    suggestions.value = []
    suggestOpen.value = false
    suggestQuery.value = ''
    return
  }
  // 250ms 防抖 + 序号比对：慢网络下旧响应不能盖掉新输入的结果
  suggestTimer = setTimeout(async () => {
    const seq = ++suggestSeq
    suggestQuery.value = q
    try {
      const list = await fetchSuggestions(q, 8)
      if (seq !== suggestSeq) return
      suggestions.value = list || []
      activeIndex.value = -1
      suggestOpen.value = typing.value
    } catch (e) {
      if (seq === suggestSeq) {
        suggestions.value = []
        activeIndex.value = -1
        suggestOpen.value = typing.value
      }
    }
  }, 250)
})

// 与 React 版保持一致的键盘操作：上下选择、Enter 直达、Esc 收起
function onSearchKeydown(event) {
  if (event.key === 'Escape') {
    suggestOpen.value = false
    return
  }
  if (!suggestOpen.value || !suggestions.value.length) return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % suggestions.value.length
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = activeIndex.value <= 0 ? suggestions.value.length - 1 : activeIndex.value - 1
  } else if (event.key === 'Enter' && activeIndex.value >= 0) {
    // 阻止表单提交，避免「键盘选中联想项」变成一次全量搜索
    event.preventDefault()
    const picked = suggestions.value[activeIndex.value]
    if (picked) {
      typing.value = false
      suggestOpen.value = false
      activeIndex.value = -1
      router.push(`/posts/${picked.id}`)
    }
  }
}

function pickSuggestion() {
  typing.value = false
  suggestOpen.value = false
  activeIndex.value = -1
}

function closeSuggest() {
  // 延迟关闭，否则点击下拉项之前链接就被移除了
  typing.value = false
  window.setTimeout(() => { suggestOpen.value = false }, 150)
}

function clearKeyword() {
  typing.value = false
  keyword.value = ''
  suggestions.value = []
  suggestQuery.value = ''
  suggestOpen.value = false
  activeIndex.value = -1
  current.value = 1
  load()
}

async function load() {
  loading.value = true
  error.value = ''
  submittedKeyword.value = keyword.value.trim()
  try {
    const page = await fetchPosts(current.value, PAGE_SIZE, submittedKeyword.value)
    posts.value = page?.records || []
    total.value = page?.total || 0
  } catch (e) {
    error.value = e.message || '加载失败'
    posts.value = []
  } finally {
    loading.value = false
  }
}

function reload() {
  current.value = 1
  typing.value = false
  suggestOpen.value = false
  activeIndex.value = -1
  load()
}

function go(page) {
  if (page < 1 || page > totalPages.value) return
  current.value = page
  load()
}

onMounted(load)
onBeforeUnmount(() => clearTimeout(suggestTimer))
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}
.search {
  display: flex;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.input-wrap {
  position: relative;
  flex: 1;
  min-width: 0;
}
.input-wrap input {
  width: 100%;
  height: 42px;
  padding-left: 39px;
  padding-right: 34px;
}
.search-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  color: var(--faint);
  pointer-events: none;
  z-index: 1;
}
.input-wrap:focus-within .search-icon {
  color: var(--accent);
}
.clear-btn {
  position: absolute;
  right: 9px;
  top: 50%;
  transform: translateY(-50%);
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: var(--panel-3);
  color: var(--muted);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  z-index: 1;
}
.clear-btn:hover {
  color: var(--text);
  background: var(--line-strong);
}

/* 联想下拉 */
.suggest {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  right: 0;
  z-index: 40;
  max-height: 380px;
  overflow-y: auto;
  padding: 6px;
  background: var(--panel);
  border: 1px solid var(--line-strong);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  animation: suggest-in 0.16s var(--ease);
}
@keyframes suggest-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
.suggest-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 10px 9px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--line);
  color: var(--faint);
  font-size: 11px;
  letter-spacing: 0.04em;
}
.suggest ul {
  list-style: none;
  margin: 0;
  padding: 0;
}
.suggest a {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  color: var(--text);
  text-decoration: none;
  transition: background var(--dur) var(--ease);
}
.suggest a:hover {
  background: var(--panel-2);
  color: var(--text);
  text-decoration: none;
}
.suggest a.suggest-active {
  background: var(--panel-2);
  color: var(--text);
  text-decoration: none;
  box-shadow: inset 2px 0 0 var(--accent);
}
.suggest-title {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.suggest-snippet {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.suggest-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: var(--faint);
}
.suggest-author {
  color: var(--muted);
}
.hit-word {
  margin-left: auto;
  flex: 0 0 auto;
  padding: 1px 8px;
  border-radius: var(--radius-pill);
  background: var(--hl-bg);
  color: var(--hl-text);
  font-size: 11px;
}
.suggest-empty {
  margin: 0;
  padding: 20px 12px 24px;
  text-align: center;
  color: var(--muted);
  font-size: 13px;
}

/* 结果页 */
.result-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: -4px 0 16px;
}
.result-chip {
  padding: 3px 11px;
  border: 1px solid var(--accent-line);
  border-radius: var(--radius-pill);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
}
.result-meta {
  font-size: 12px;
  color: var(--faint);
}
.post-card {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  transition: border-color var(--dur) var(--ease), box-shadow var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.post-card:hover {
  border-color: var(--line-strong);
  box-shadow: var(--shadow-card);
  transform: translateY(-2px);
}
.thumb {
  flex: 0 0 auto;
  width: 118px;
  height: 82px;
  border-radius: var(--radius-sm);
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--panel-2);
}
.thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s var(--ease);
}
.post-card:hover .thumb img {
  transform: scale(1.05);
}
.post-main {
  flex: 1;
  min-width: 0;
}
.post-title {
  display: inline-block;
  max-width: 100%;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text);
  overflow-wrap: anywhere;
}
.post-title:hover {
  color: var(--accent);
  text-decoration: none;
}
.summary {
  color: var(--muted);
  font-size: 13px;
  line-height: 1.65;
  margin: 6px 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--faint);
}
.author {
  color: var(--muted);
  font-weight: 500;
}
.dot {
  opacity: 0.5;
}
.ai-tag {
  margin-left: auto;
  border-color: var(--ok-line);
  color: var(--ok);
  background: var(--ok-soft);
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

@media (max-width: 640px) {
  .toolbar { flex-wrap: wrap; }
  .search { flex: 1 1 100%; }
  .suggest { max-height: 320px; }
  .post-card { gap: 12px; }
  .thumb { width: 84px; height: 66px; }
  .post-title { font-size: 15px; }
}
@media (prefers-reduced-motion: reduce) {
  .suggest { animation: none; }
  .post-card:hover { transform: none; }
}
</style>
