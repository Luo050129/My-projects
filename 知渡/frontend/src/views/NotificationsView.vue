<template>
  <div>
    <div class="head">
      <h1 class="title">通知</h1>
      <button class="btn btn-sm" :disabled="!unreadCount || marking" @click="markAll">
        {{ marking ? '处理中…' : `全部标为已读（${unreadCount}）` }}
      </button>
    </div>

    <p v-if="error" class="error-text">{{ error }}</p>
    <div v-if="loading" class="empty">加载中…</div>

    <template v-else>
      <div v-if="!items.length" class="empty">暂无通知</div>

      <ul v-else class="list">
        <li
          v-for="n in items"
          :key="n.id"
          :class="['panel', 'item', { unread: n.readStatus === 0 }]"
        >
          <div class="item-main">
            <span class="type">{{ typeLabel(n.type) }}</span>
            <span class="content">{{ n.content }}</span>
          </div>
          <div class="item-side">
            <span class="muted small">{{ formatTime(n.createTime) }}</span>
            <button
              v-if="n.readStatus === 0"
              class="btn btn-sm"
              @click="markOne(n)"
            >
              标为已读
            </button>
            <router-link v-else-if="n.refId" :to="`/posts/${n.refId}`" class="small">查看</router-link>
          </div>
        </li>
      </ul>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import {
  fetchNotifications,
  fetchUnreadCount,
  readAllNotifications,
  readNotification
} from '../api'
import { formatTime } from '../utils'

const items = ref([])
const unreadCount = ref(0)
const loading = ref(true)
const marking = ref(false)
const error = ref('')

function typeLabel(type) {
  const map = { comment: '评论', like: '点赞', system: '系统' }
  return map[type] || type || '通知'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = (await fetchNotifications()) || []
    unreadCount.value = Number(await fetchUnreadCount()) || 0
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

async function markOne(n) {
  try {
    await readNotification(n.id)
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function markAll() {
  marking.value = true
  try {
    await readAllNotifications()
    await load()
  } catch (e) {
    error.value = e.message
  } finally {
    marking.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.title {
  font-size: 18px;
  margin: 0;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
}
.item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.item.unread {
  border-left: 3px solid var(--accent);
}
.item-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}
.type {
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--accent-dim);
  color: var(--accent);
  white-space: nowrap;
}
.content {
  word-break: break-word;
}
.item-side {
  display: flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}
</style>
