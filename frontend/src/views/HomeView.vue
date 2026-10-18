<template>
  <div class="container">
    <!-- 主视觉 -->
    <section class="hero" :style="heroStyle">
      <div class="hero-text">
        <h1>君山点评</h1>
        <p>发现城市里的好味道、好去处。真实点评，帮你选得安心。</p>
        <div class="hero-actions">
          <RouterLink to="/shops" class="btn btn-primary">逛逛商户</RouterLink>
          <a class="btn btn-ghost" @click.prevent="scrollToReviews">看热门点评</a>
        </div>
      </div>
    </section>

    <!-- 分类导航 -->
    <section v-if="types.length">
      <div class="section-title"><h2>逛分类</h2></div>
      <div class="cat-grid">
        <RouterLink
          v-for="t in types"
          :key="t.id"
          :to="`/shops?type=${t.id}`"
          class="cat card"
        >
          <span class="cat-icon">{{ t.icon }}</span>
          <span class="cat-name">{{ t.name }}</span>
        </RouterLink>
      </div>
    </section>

    <!-- 人气美食 -->
    <section v-if="foodShops.length">
      <div class="section-title">
        <h2>人气美食</h2>
        <RouterLink to="/shops?type=1" class="more">查看全部 ›</RouterLink>
      </div>
      <div class="grid">
        <ShopCard
          v-for="s in foodShops"
          :key="s.id"
          :shop="s"
          type-name="美食"
        />
      </div>
    </section>

    <!-- 热门点评 -->
    <section id="reviews" v-if="reviews.length">
      <div class="section-title"><h2>热门点评</h2></div>
      <div class="review-grid">
        <ReviewCard v-for="r in reviews" :key="r.id" :review="r" />
      </div>
    </section>

    <p v-if="loading" class="hint">加载中…</p>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import ShopCard from '@/components/ShopCard.vue'
import ReviewCard from '@/components/ReviewCard.vue'
import { fetchShopTypes, fetchShops, fetchHotReviews } from '@/api'

const types = ref([])
const foodShops = ref([])
const reviews = ref([])
const loading = ref(true)

const heroStyle = computed(() => ({
  backgroundImage:
    'linear-gradient(120deg, rgba(255,241,230,0.94), rgba(255,227,207,0.82)), url(/images/hero.png)'
}))

function scrollToReviews() {
  document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(async () => {
  const [t, f, r] = await Promise.all([
    fetchShopTypes(),
    fetchShops({ typeId: 1, current: 1 }),
    fetchHotReviews()
  ])
  types.value = t || []
  foodShops.value = (f || []).slice(0, 4)
  reviews.value = (r || []).slice(0, 4)
  loading.value = false
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background-size: cover;
  background-position: center;
  border-radius: 18px;
  padding: 48px 36px;
  margin-bottom: 8px;
}
.hero-text h1 {
  font-size: 34px;
  color: var(--js-primary-dark);
  letter-spacing: 2px;
}
.hero-text p {
  margin: 10px 0 18px;
  color: var(--js-text-2);
  max-width: 520px;
}
.hero-actions {
  display: flex;
  gap: 12px;
}
.cat-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
}
.cat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  transition: transform 0.18s ease;
}
.cat:hover {
  transform: translateY(-3px);
}
.cat-icon {
  font-size: 30px;
}
.cat-name {
  font-size: 13px;
  color: var(--js-text-2);
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.review-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.hint {
  text-align: center;
  color: var(--js-text-3);
  margin-top: 20px;
}
@media (max-width: 920px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .cat-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  .review-grid {
    grid-template-columns: 1fr;
  }
}
</style>
