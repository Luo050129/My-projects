<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/posts" class="brand">知渡</router-link>

      <nav class="links">
        <router-link to="/posts">帖子</router-link>
        <router-link v-if="logged" to="/posts/new">发帖</router-link>
        <router-link v-if="logged" to="/notifications" class="notif-link">
          通知
          <span v-if="unread > 0" class="badge">{{ unread > 99 ? '99+' : unread }}</span>
        </router-link>
      </nav>

      <div class="right">
        <template v-if="logged">
          <span class="who">{{ auth.user?.nickname || '用户' }}</span>
          <button class="btn btn-sm" @click="logout">退出</button>
        </template>
        <router-link v-else to="/login" class="btn btn-sm btn-primary">登录</router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, signOut } from '../auth'
import { fetchUnreadCount } from '../api'

const route = useRoute()
const router = useRouter()
const logged = computed(() => Boolean(auth.token))
const unread = ref(0)

async function refreshUnread() {
  if (!auth.token) {
    unread.value = 0
    return
  }
  try {
    unread.value = Number(await fetchUnreadCount()) || 0
  } catch {
    unread.value = 0
  }
}

function logout() {
  signOut()
  unread.value = 0
  router.push({ name: 'posts' })
}

onMounted(refreshUnread)
watch(() => route.fullPath, refreshUnread)
</script>

<style scoped>
.nav {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(15, 17, 21, 0.92);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
}
.nav-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.brand {
  font-size: 17px;
  font-weight: 700;
  color: var(--text);
  letter-spacing: 1px;
}
.links {
  display: flex;
  gap: 16px;
  flex: 1;
}
.links a {
  color: var(--muted);
  font-size: 14px;
}
.links a:hover {
  color: var(--text);
  text-decoration: none;
}
.links a.router-link-active {
  color: var(--text);
}
.badge {
  display: inline-block;
  margin-left: 4px;
  background: var(--bad);
  color: #fff;
  font-size: 11px;
  line-height: 1;
  padding: 2px 5px;
  border-radius: 999px;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.who {
  font-size: 13px;
  color: var(--muted);
}
</style>
