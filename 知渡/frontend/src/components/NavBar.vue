<template>
  <header class="nav">
    <div class="nav-inner">
      <router-link to="/posts" class="brand" aria-label="知渡 · 返回帖子首页">
        <img src="/icon.svg" alt="" class="brand-icon" width="30" height="30" />
        <span>知渡</span>
      </router-link>

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
  z-index: 30;
  /* 通栏只负责磨砂遮罩，真正的容器是下面那条细边框圆角导航条 */
  padding: 12px 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(14px) saturate(140%);
  -webkit-backdrop-filter: blur(14px) saturate(140%);
}
.nav-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 9px 16px;
  display: flex;
  align-items: center;
  gap: 18px;
  background: var(--bg-soft);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-nav);
}
.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  flex: 0 0 auto;
  font-family: var(--font-brand);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text);
}
.brand:hover {
  color: var(--text);
  text-decoration: none;
}
.brand-icon {
  display: block;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  box-shadow: var(--shadow-brand);
  transition: transform var(--dur) var(--ease);
}
.brand:hover .brand-icon {
  transform: rotate(-6deg) scale(1.06);
}
.links {
  display: flex;
  gap: 4px;
  flex: 1;
}
.links a {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border-radius: var(--radius-sm);
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  transition: color var(--dur) var(--ease), background var(--dur) var(--ease);
}
.links a:hover {
  color: var(--text);
  background: var(--panel-2);
  text-decoration: none;
}
.links a.router-link-active {
  color: var(--accent);
  background: var(--accent-soft);
}
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 17px;
  margin-left: 5px;
  background: var(--coral);
  color: var(--on-coral);
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  padding: 2px 5px;
  border-radius: var(--radius-pill);
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
@media (max-width: 640px) {
  .nav {
    padding: 8px 12px;
  }
  .nav-inner {
    padding: 8px 12px;
    gap: 10px;
  }
  .brand {
    font-size: 17px;
  }
  .brand-icon {
    width: 26px;
    height: 26px;
  }
  .links a {
    padding: 6px 8px;
  }
  .who {
    display: none;
  }
}
</style>
