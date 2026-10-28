<template>
  <div>
    <div class="toolbar">
      <form class="search" @submit.prevent="reload">
        <input v-model.trim="keyword" type="text" placeholder="搜索标题…" />
        <button class="btn" type="submit">搜索</button>
      </form>
      <router-link v-if="logged" to="/posts/new" class="btn btn-primary">发帖</router-link>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>

    <div v-if="loading" class="empty">加载中…</div>

    <template v-else>
      <div v-if="!posts.length" class="empty">还没有帖子</div>

      <article v-for="post in posts" :key="post.id" class="panel post-card">
        <router-link v-if="post.coverImage" :to="`/posts/${post.id}`" class="thumb">
          <img :src="post.coverImage" alt="封面" loading="lazy" />
        </router-link>
        <div class="post-main">
          <router-link :to="`/posts/${post.id}`" class="post-title">{{ post.title }}</router-link>
          <p v-if="post.summary" class="summary">{{ post.summary }}</p>
          <div class="meta">
            <span>{{ post.authorName }}</span>
            <span class="dot">·</span>
            <span>{{ formatTime(post.createTime) }}</span>
            <span class="dot">·</span>
            <span>{{ post.viewCount }} 浏览</span>
            <span class="dot">·</span>
            <span>{{ post.likeCount }} 赞</span>
            <span class="dot">·</span>
            <span>{{ post.commentCount }} 评论</span>
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
import { computed, onMounted, ref } from 'vue'
import { fetchPosts } from '../api'
import { auth } from '../auth'
import { formatTime } from '../utils'

const PAGE_SIZE = 10

const posts = ref([])
const total = ref(0)
const current = ref(1)
const keyword = ref('')
const loading = ref(false)
const error = ref('')
const logged = computed(() => Boolean(auth.token))
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const page = await fetchPosts(current.value, PAGE_SIZE, keyword.value)
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
  load()
}

function go(page) {
  if (page < 1 || page > totalPages.value) return
  current.value = page
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.search {
  display: flex;
  gap: 8px;
  flex: 1;
}
.search input {
  flex: 1;
}
.post-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  transition: border-color 0.15s;
}
.post-card:hover {
  border-color: #3a4150;
}
.thumb {
  flex: 0 0 auto;
  width: 112px;
  height: 78px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid var(--border);
}
.thumb img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.post-main {
  flex: 1;
  min-width: 0;
}
.post-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}
.post-title:hover {
  color: var(--accent);
  text-decoration: none;
}
.summary {
  color: var(--muted);
  font-size: 13px;
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
  color: var(--muted);
}
.dot {
  opacity: 0.5;
}
.ai-tag {
  margin-left: auto;
  border-color: #2a4a3a;
  color: var(--ok);
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 20px;
}
</style>
