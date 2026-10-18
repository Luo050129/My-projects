<template>
  <div class="container">
    <div class="section-title">
      <h2>点评列表</h2>
      <span class="more">共 {{ shops.length }} 家商户</span>
    </div>

    <!-- 分类筛选 -->
    <div class="filters">
      <button
        class="chip"
        :class="{ on: !selectedType }"
        @click="selectType(null)"
      >
        全部
      </button>
      <button
        v-for="t in types"
        :key="t.id"
        class="chip"
        :class="{ on: selectedType === t.id }"
        @click="selectType(t.id)"
      >
        {{ t.icon }} {{ t.name }}
      </button>
    </div>

    <!-- 名称搜索 -->
    <div class="search-row">
      <input
        v-model="name"
        type="text"
        placeholder="输入商户名称筛选…"
        @keyup.enter="applyFilters"
      />
      <button class="btn btn-primary" @click="applyFilters">筛选</button>
      <button class="btn btn-ghost" @click="reset">重置</button>
    </div>

    <!-- 列表 -->
    <div v-if="shops.length" class="grid">
      <ShopCard
        v-for="s in shops"
        :key="s.id"
        :shop="s"
        :type-name="typeNameOf(s.typeId)"
      />
    </div>

    <p v-else-if="!loading" class="empty">没有找到符合条件的商户，换个关键词试试～</p>

    <div v-if="loading" class="hint">加载中…</div>

    <div v-if="shops.length && hasMore" class="more-wrap">
      <button class="btn btn-ghost" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中…' : '加载更多' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import ShopCard from '@/components/ShopCard.vue'
import { fetchShopTypes, fetchShops } from '@/api'

const route = useRoute()
const types = ref([])
const typeMap = ref({})
const shops = ref([])
const selectedType = ref(null)
const name = ref('')
const loading = ref(false)
const page = ref(1)
const hasMore = ref(false)
const PAGE_SIZE = 8

function typeNameOf(id) {
  return typeMap.value[id] || ''
}

function selectType(id) {
  selectedType.value = id
  applyFilters()
}

function reset() {
  selectedType.value = null
  name.value = ''
  applyFilters()
}

async function load(reset = true) {
  loading.value = true
  if (reset) {
    page.value = 1
    shops.value = []
  }
  const list = await fetchShops({
    typeId: selectedType.value || undefined,
    name: name.value.trim() || undefined,
    current: page.value
  })
  const arr = list || []
  if (reset) shops.value = arr
  else shops.value = shops.value.concat(arr)
  // 模拟分页：若返回数量不足一页，则视为没有更多
  hasMore.value = arr.length >= PAGE_SIZE
  loading.value = false
}

function applyFilters() {
  load(true)
}

function loadMore() {
  page.value += 1
  load(false)
}

onMounted(async () => {
  const t = await fetchShopTypes()
  types.value = t || []
  t?.forEach((x) => (typeMap.value[x.id] = x.name))
  // 支持从首页 /shops?type=1 或 ?name= 进入
  if (route.query.type) selectedType.value = Number(route.query.type)
  if (route.query.name) name.value = String(route.query.name)
  load(true)
})

watch(
  () => route.query,
  () => {
    if (route.query.type) selectedType.value = Number(route.query.type)
    if (route.query.name) name.value = String(route.query.name)
    load(true)
  }
)
</script>

<style scoped>
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.chip {
  border: 1px solid var(--js-line);
  background: #fff;
  border-radius: 999px;
  padding: 7px 16px;
  font-size: 14px;
  cursor: pointer;
  color: var(--js-text-2);
  transition: all 0.16s ease;
}
.chip.on {
  background: var(--js-primary);
  border-color: var(--js-primary);
  color: #fff;
  font-weight: 600;
}
.search-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.search-row input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--js-line);
  border-radius: 10px;
  outline: none;
  font-size: 14px;
}
.search-row input:focus {
  border-color: var(--js-primary);
}
.grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.empty {
  text-align: center;
  color: var(--js-text-3);
  padding: 40px 0;
}
.hint {
  text-align: center;
  color: var(--js-text-3);
  padding: 16px 0;
}
.more-wrap {
  text-align: center;
  margin: 22px 0;
}
@media (max-width: 920px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
