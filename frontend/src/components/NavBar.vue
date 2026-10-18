<template>
  <header class="navbar">
    <div class="container nav-inner">
      <RouterLink to="/" class="brand">
        <img class="logo" src="/images/logo.png" alt="君山点评" />
        <span class="brand-name">君山点评</span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/" exact-active-class="active">首页</RouterLink>
        <RouterLink to="/shops" active-class="active">点评列表</RouterLink>
      </nav>

      <form class="search" @submit.prevent="onSearch">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索商户 / 美食 / KTV…"
        />
        <button type="submit" class="btn btn-primary">搜索</button>
      </form>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')

function onSearch() {
  const name = keyword.value.trim()
  router.push(name ? { path: '/shops', query: { name } } : '/shops')
}
</script>

<style scoped>
.navbar {
  position: sticky;
  top: 0;
  z-index: 50;
  background: #fff;
  border-bottom: 1px solid var(--js-line);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}
.nav-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 64px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
  font-size: 20px;
}
.logo {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  object-fit: cover;
}
.brand-name {
  color: var(--js-primary);
  letter-spacing: 1px;
}
.nav-links {
  display: flex;
  gap: 22px;
  font-size: 15px;
  font-weight: 600;
}
.nav-links a {
  color: var(--js-text-2);
  padding: 4px 2px;
  position: relative;
}
.nav-links a.active {
  color: var(--js-primary);
}
.nav-links a.active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -6px;
  height: 3px;
  border-radius: 3px;
  background: var(--js-primary);
}
.search {
  margin-left: auto;
  display: flex;
  gap: 8px;
}
.search input {
  width: 260px;
  max-width: 40vw;
  padding: 9px 14px;
  border: 1px solid var(--js-line);
  border-radius: 999px;
  outline: none;
  font-size: 14px;
  transition: border-color 0.18s ease;
}
.search input:focus {
  border-color: var(--js-primary);
}
@media (max-width: 720px) {
  .nav-links {
    display: none;
  }
  .search input {
    width: 140px;
  }
}
</style>
