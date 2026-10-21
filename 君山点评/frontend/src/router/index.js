import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import ShopListView from '@/views/ShopListView.vue'
import ShopDetailView from '@/views/ShopDetailView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView, meta: { title: '首页' } },
  { path: '/shops', name: 'shops', component: ShopListView, meta: { title: '点评列表' } },
  {
    path: '/shop/:id',
    name: 'shop-detail',
    component: ShopDetailView,
    meta: { title: '商户详情' },
    props: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.afterEach((to) => {
  document.title = to.meta.title ? `君山点评 · ${to.meta.title}` : '君山点评'
})

export default router
