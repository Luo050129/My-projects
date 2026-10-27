<template>
  <NavBar />
  <main class="container">
    <router-view v-slot="{ Component }">
      <component :is="Component" />
    </router-view>
  </main>
  <footer class="footer">知渡 · 知识社区</footer>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>
