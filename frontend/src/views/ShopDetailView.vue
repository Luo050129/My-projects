<template>
  <div class="container">
    <RouterLink to="/shops" class="back">‹ 返回点评列表</RouterLink>

    <div v-if="loading" class="hint">加载中…</div>

    <div v-else-if="!shop" class="empty">
      <p>未找到该商户 😢</p>
      <RouterLink to="/shops" class="btn btn-primary">去点评列表看看</RouterLink>
    </div>

    <div v-else class="detail">
      <!-- 头部 -->
      <section class="head card">
        <div class="cover" :style="coverStyle">
          <span v-if="!cover">{{ initial }}</span>
          <span v-if="shop.openStatus === 0" class="closed-tag">休息中</span>
        </div>
        <div class="info">
          <h1>{{ shop.name }}</h1>
          <div class="score-line">
            <span class="score">{{ shop.score?.toFixed(1) || '—' }}</span>
            <span class="stars">★★★★★</span>
            <span class="count">{{ shop.comments }} 条点评</span>
          </div>
          <ul class="facts">
            <li><b>人均</b> ¥{{ shop.avgPrice }}</li>
            <li><b>类型</b> {{ typeName }}</li>
            <li><b>区域</b> {{ shop.area }}</li>
            <li><b>地址</b> {{ shop.address }}</li>
            <li><b>营业</b> {{ shop.businessHours }}</li>
            <li><b>已售</b> {{ shop.sold }}</li>
          </ul>
        </div>
      </section>

      <!-- 点评 -->
      <section>
        <div class="section-title">
          <h2>用户点评（{{ reviews.length }}）</h2>
        </div>
        <div v-if="reviews.length" class="review-list">
          <ReviewCard v-for="r in reviews" :key="r.id" :review="r" />
        </div>
        <p v-else class="empty-sm">这家店还没有点评，快来抢沙发～</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import ReviewCard from '@/components/ReviewCard.vue'
import { fetchShopById, fetchReviewsByShop, fetchShopTypes } from '@/api'

const props = defineProps({ id: { type: [String, Number], required: true } })

const shop = ref(null)
const reviews = ref([])
const typeMap = ref({})
const loading = ref(true)

const palette = ['#ff9a3c', '#ff6a00', '#ffb000', '#ff7a85', '#5ac8fa', '#7ed957']
const cover = computed(() => (shop.value?.images && shop.value.images[0]) || '')
const initial = computed(() => shop.value?.name?.charAt(0) || '店')
const typeName = computed(() => typeMap.value[shop.value?.typeId] || '商户')
const coverStyle = computed(() => {
  if (cover.value) return { backgroundImage: `url(${cover.value})` }
  const idx = (shop.value?.id || 0) % palette.length
  return {
    background: `linear-gradient(135deg, ${palette[idx]}, ${palette[(idx + 2) % palette.length]})`
  }
})

async function load() {
  loading.value = true
  const [s, r] = await Promise.all([
    fetchShopById(props.id),
    fetchReviewsByShop(props.id)
  ])
  shop.value = s
  reviews.value = r || []
  loading.value = false
}

onMounted(async () => {
  const t = await fetchShopTypes()
  t?.forEach((x) => (typeMap.value[x.id] = x.name))
  load()
})

watch(() => props.id, load)
</script>

<style scoped>
.back {
  display: inline-block;
  margin: 6px 0 14px;
  color: var(--js-text-2);
  font-size: 14px;
}
.head {
  display: flex;
  gap: 22px;
  padding: 20px;
  margin-bottom: 10px;
}
.cover {
  width: 220px;
  height: 220px;
  border-radius: 14px;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 64px;
  font-weight: 800;
  position: relative;
}
.closed-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 999px;
}
.info {
  flex: 1;
}
.info h1 {
  font-size: 24px;
}
.score-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0 14px;
}
.score {
  color: var(--js-star);
  font-weight: 800;
  font-size: 22px;
}
.stars {
  color: var(--js-star);
  letter-spacing: 2px;
}
.count {
  color: var(--js-text-3);
  font-size: 13px;
}
.facts {
  list-style: none;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px 24px;
  font-size: 14px;
  color: var(--js-text-2);
}
.facts b {
  color: var(--js-text-3);
  font-weight: 600;
  margin-right: 6px;
}
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.empty,
.empty-sm {
  text-align: center;
  color: var(--js-text-3);
  padding: 30px 0;
}
.empty-sm {
  padding: 16px 0;
}
.hint {
  text-align: center;
  padding: 40px 0;
  color: var(--js-text-3);
}
@media (max-width: 720px) {
  .head {
    flex-direction: column;
  }
  .cover {
    width: 100%;
    height: 180px;
  }
}
</style>
