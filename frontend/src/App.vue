<template>
  <NavBar />
  <main class="main">
    <RouterView v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </main>
  <footer class="footer">
    <div class="container">
      <p class="f-brand">君山点评 · 发现身边好生活</p>
      <p class="f-meta">SpringBoot + Redis + MySQL 点评类应用 · 前端基于 Vue 3 + Vue Router</p>
      <p class="f-copy">© 2025 君山点评 Demo</p>
    </div>
  </footer>
</template>

<script setup>
import NavBar from '@/components/NavBar.vue'
</script>

<style scoped>
.main {
  flex: 1;
  padding: 18px 0 40px;
}
.footer {
  background: #fff;
  border-top: 1px solid var(--js-line);
  padding: 26px 0;
  text-align: center;
  color: var(--js-text-3);
}
.f-brand {
  font-weight: 700;
  color: var(--js-text-2);
}
.f-meta {
  font-size: 13px;
  margin-top: 4px;
}
.f-copy {
  font-size: 12px;
  margin-top: 6px;
}
</style>
