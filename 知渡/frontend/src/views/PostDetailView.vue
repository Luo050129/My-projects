<template>
  <div>
    <router-link to="/posts" class="back muted small">← 返回列表</router-link>

    <p v-if="error" class="error-text">{{ error }}</p>
    <div v-if="loading" class="empty">加载中…</div>

    <template v-else-if="post">
      <article class="panel">
        <h1 class="title">{{ post.title }}</h1>
        <div class="meta">
          <span>{{ post.authorName }}</span>
          <span class="dot">·</span>
          <span>{{ formatTime(post.createTime) }}</span>
          <span class="dot">·</span>
          <span>{{ post.viewCount }} 浏览</span>
        </div>

        <img v-if="post.coverImage" :src="post.coverImage" :alt="post.title" class="cover" />

        <div v-if="tags.length" class="tags">
          <span v-for="t in tags" :key="t" class="tag">{{ t }}</span>
        </div>

        <div class="content">{{ post.content || post.summary || '（无正文）' }}</div>

        <div class="actions">
          <button class="btn" :disabled="!logged" @click="like">
            {{ logged ? '点赞' : '登录后可点赞' }} · {{ post.likeCount }}
          </button>
          <button v-if="isOwner" class="btn" @click="enhance" :disabled="enhancing">
            {{ enhancing ? '生成中…' : 'AI 生成摘要' }}
          </button>
          <router-link v-if="isOwner" :to="`/posts/${post.id}/edit`" class="btn">编辑</router-link>
          <button v-if="isOwner" class="btn btn-danger" @click="remove">删除</button>
        </div>

        <p v-if="post.aiEnhanced === 1 && post.summary" class="ai-summary">
          <strong>AI 摘要：</strong>{{ post.summary }}
        </p>
      </article>

      <section class="panel">
        <h2 class="section-title">评论 · {{ comments.length }}</h2>

        <form v-if="logged" class="comment-form" @submit.prevent="submitComment">
          <textarea v-model.trim="commentText" placeholder="说点什么…" maxlength="500" />
          <div class="comment-actions">
            <button class="btn btn-primary btn-sm" type="submit" :disabled="posting">
              {{ posting ? '发送中…' : '发表评论' }}
            </button>
          </div>
        </form>
        <p v-else class="muted small">
          登录后可以评论。<router-link to="/login">去登录</router-link>
        </p>

        <div v-if="!comments.length" class="empty">还没有评论</div>

        <ul v-else class="comment-list">
          <li v-for="c in comments" :key="c.id" class="comment">
            <div class="comment-head">
              <span class="author">{{ c.authorName }}</span>
              <span class="muted small">{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="comment-body">{{ c.content }}</p>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  addComment,
  deletePost,
  enhancePost,
  fetchComments,
  fetchPost,
  toggleLike
} from '../api'
import { auth } from '../auth'
import { formatTime } from '../utils'

const route = useRoute()
const router = useRouter()

const post = ref(null)
const comments = ref([])
const loading = ref(true)
const error = ref('')
const commentText = ref('')
const posting = ref(false)
const enhancing = ref(false)

const logged = computed(() => Boolean(auth.token))
const isOwner = computed(
  () => auth.user && post.value && String(post.value.authorId) === String(auth.user.userId)
)
const tags = computed(() =>
  (post.value?.tags || '')
    .split(',')
    .map((t) => t.trim())
    .filter(Boolean)
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    post.value = await fetchPost(route.params.id)
    // 后端返回分页对象 {records,total,...}，不能当数组用
    const page = await fetchComments(route.params.id)
    comments.value = page?.records ?? []
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function like() {
  if (!logged.value) return
  try {
    await toggleLike(route.params.id)
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function submitComment() {
  if (!commentText.value) return
  posting.value = true
  try {
    await addComment(Number(route.params.id), commentText.value)
    commentText.value = ''
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    posting.value = false
  }
}

async function enhance() {
  enhancing.value = true
  try {
    await enhancePost(route.params.id)
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    enhancing.value = false
  }
}

async function remove() {
  if (!window.confirm('确定删除这篇帖子？')) return
  try {
    await deletePost(route.params.id)
    router.replace({ name: 'posts' })
  } catch (e) {
    error.value = e.message
  }
}

onMounted(load)
</script>

<style scoped>
.back {
  display: inline-block;
  margin-bottom: 12px;
}
.title {
  font-size: 22px;
  margin: 0 0 8px;
}
.meta {
  display: flex;
  gap: 6px;
  font-size: 12px;
  color: var(--muted);
}
.dot {
  opacity: 0.5;
}
.cover {
  display: block;
  width: 100%;
  max-height: 420px;
  object-fit: cover;
  margin: 14px 0 0;
  border: 1px solid var(--border);
  border-radius: 8px;
}
.tags {
  margin: 12px 0 0;
}
.content {
  margin: 16px 0 20px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
}
.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}
.ai-summary {
  margin: 14px 0 0;
  font-size: 13px;
  color: var(--muted);
  background: var(--panel-2);
  border-left: 3px solid var(--ok);
  padding: 10px 12px;
  border-radius: 0 6px 6px 0;
}
.section-title {
  font-size: 14px;
  color: var(--muted);
  margin: 0 0 14px;
  font-weight: 600;
}
.comment-form textarea {
  min-height: 80px;
}
.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
.comment-list {
  list-style: none;
  margin: 18px 0 0;
  padding: 0;
}
.comment {
  padding: 12px 0;
  border-top: 1px solid var(--line);
}
.comment-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}
.author {
  font-size: 13px;
  color: var(--accent);
}
.comment-body {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
