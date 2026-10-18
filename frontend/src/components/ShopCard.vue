<template>
  <RouterLink :to="`/shop/${shop.id}`" class="shop-card card">
    <div class="cover" :style="coverStyle">
      <span v-if="!cover">{{ initial }}</span>
      <span v-if="shop.openStatus === 0" class="closed-tag">休息中</span>
    </div>
    <div class="body">
      <div class="title-row">
        <h3 class="name">{{ shop.name }}</h3>
        <span class="score">{{ shop.score?.toFixed(1) || '—' }}</span>
      </div>
      <div class="meta">
        <span>{{ typeName }}</span>
        <span class="dot">·</span>
        <span>{{ shop.area }}</span>
      </div>
      <div class="sub">
        <span class="price">¥{{ shop.avgPrice }}/人</span>
        <span class="sold">已售 {{ shop.sold }}</span>
        <span class="comments">{{ shop.comments }} 条点评</span>
      </div>
    </div>
  </RouterLink>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  shop: { type: Object, required: true },
  typeName: { type: String, default: '' }
})

const palette = ['#ff9a3c', '#ff6a00', '#ffb000', '#ff7a85', '#5ac8fa', '#7ed957']
const cover = computed(() => (props.shop.images && props.shop.images[0]) || '')
const initial = computed(() => props.shop.name?.charAt(0) || '店')
const coverStyle = computed(() => {
  if (cover.value) return { backgroundImage: `url(${cover.value})` }
  const idx = (props.shop.id || 0) % palette.length
  return { background: `linear-gradient(135deg, ${palette[idx]}, ${palette[(idx + 2) % palette.length]})` }
})
</script>

<style scoped>
.shop-card {
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}
.shop-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
}
.cover {
  position: relative;
  height: 150px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 42px;
  font-weight: 800;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
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
.body {
  padding: 12px 14px 16px;
}
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.name {
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.score {
  color: var(--js-star);
  font-weight: 800;
  font-size: 16px;
  flex-shrink: 0;
}
.meta {
  margin-top: 6px;
  color: var(--js-text-3);
  font-size: 13px;
  display: flex;
  gap: 6px;
}
.sub {
  margin-top: 10px;
  font-size: 13px;
  color: var(--js-text-2);
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.price {
  color: var(--js-primary);
  font-weight: 700;
}
</style>
