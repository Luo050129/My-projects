<template>
  <div class="login-wrap">
    <div class="panel login-panel">
      <h1 class="title">知渡</h1>
      <p class="muted small sub">内容 / 知识社区</p>

      <div class="tabs">
        <button :class="['tab', { active: mode === 'login' }]" @click="switchMode('login')">登录</button>
        <button :class="['tab', { active: mode === 'register' }]" @click="switchMode('register')">注册</button>
      </div>

      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model.trim="form.username" type="text" autocomplete="username" placeholder="admin" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="form.password" type="password" autocomplete="current-password" placeholder="admin123" />
        </div>
        <div v-if="mode === 'register'" class="field">
          <label>昵称（可选）</label>
          <input v-model.trim="form.nickname" type="text" placeholder="展示给别人看的名字" />
        </div>

        <button class="btn btn-primary full" type="submit" :disabled="loading">
          {{ loading ? '处理中…' : mode === 'login' ? '登录' : '注册并登录' }}
        </button>

        <p v-if="error" class="error-text">{{ error }}</p>
      </form>

      <p class="muted small hint">演示账号：admin / admin123、alice / alice123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '../api'
import { signIn } from '../auth'

const route = useRoute()
const router = useRouter()

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const form = reactive({ username: '', password: '', nickname: '' })

function switchMode(next) {
  mode.value = next
  error.value = ''
}

async function submit() {
  error.value = ''
  if (!form.username || !form.password) {
    error.value = '用户名和密码不能为空'
    return
  }
  loading.value = true
  try {
    const resp =
      mode.value === 'login'
        ? await login(form.username, form.password)
        : await register(form.username, form.password, form.nickname || form.username)
    signIn(resp)
    router.replace(route.query.redirect || { name: 'posts' })
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding-top: 6vh;
}
.login-panel {
  width: 360px;
  padding: 28px 26px 22px;
}
.title {
  margin: 0;
  font-size: 26px;
  letter-spacing: 3px;
  text-align: center;
}
.sub {
  text-align: center;
  margin: 4px 0 20px;
}
.tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--line);
}
.tab {
  flex: 1;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--muted);
  padding: 8px 0;
  font: inherit;
  cursor: pointer;
}
.tab.active {
  color: var(--text);
  border-bottom-color: var(--accent);
}
.full {
  width: 100%;
  margin-top: 6px;
}
.hint {
  text-align: center;
  margin: 18px 0 0;
}
</style>
