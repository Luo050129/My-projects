<template>
  <article class="review card">
    <div class="head">
      <div class="avatar" :style="{ background: color }">
        <img v-if="review.avatar" :src="review.avatar" :alt="review.author" />
        <span v-else>{{ initial }}</span>
      </div>
      <div class="who">
        <div class="author">{{ review.author }}</div>
        <div class="date">{{ formatDate(review.date) }}</div>
      </div>
      <div class="stars">★★★★★</div>
    </div>
    <h4 class="title">{{ review.title }}</h4>
    <p class="content">{{ review.content }}</p>
    <div v-if="review.images && review.images.length" class="imgs">
      <div
        v-for="(img, i) in review.images"
        :key="i"
        class="img"
        :style="{ backgroundImage: `url(${img})` }"
      />
    </div>
    <div class="foot">
      <span>👍 {{ review.likes }}</span>
      <span>💬 {{ review.comments }}</span>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ review: { type: Object, required: true } })

const palette = ['#ff9a3c', '#ff6a00', '#ffb000', '#ff7a85', '#5ac8fa']
const initial = computed(() => props.review.author?.charAt(0) || '友')
const color = computed(() => palette[(props.review.id || 0) % palette.length])

function formatDate(d) {
  if (!d) return ''
  return String(d).replace('T', ' ').slice(0, 16)
}
</script>

<style scoped>
.review {
  padding: 16px 18px;
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #fff;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}
.who {
  flex: 1;
}
.author {
  font-weight: 700;
  font-size: 14px;
}
.date {
  font-size: 12px;
  color: var(--js-text-3);
}
.stars {
  color: var(--js-star);
  font-size: 13px;
  letter-spacing: 1px;
}
.title {
  margin: 12px 0 6px;
  font-size: 16px;
}
.content {
  color: var(--js-text-2);
  font-size: 14px;
}
.imgs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.img {
  width: 84px;
  height: 84px;
  border-radius: 10px;
  background-size: cover;
  background-position: center;
}
.foot {
  margin-top: 12px;
  display: flex;
  gap: 18px;
  color: var(--js-text-3);
  font-size: 13px;
}
</style>
