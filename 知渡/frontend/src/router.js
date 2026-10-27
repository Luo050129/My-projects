import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from './auth'

const routes = [
  { path: '/', redirect: '/posts' },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue') },
  { path: '/posts', name: 'posts', component: () => import('./views/PostListView.vue') },
  {
    path: '/posts/new',
    name: 'post-new',
    component: () => import('./views/PostEditorView.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/posts/:id', name: 'post-detail', component: () => import('./views/PostDetailView.vue') },
  {
    path: '/posts/:id/edit',
    name: 'post-edit',
    component: () => import('./views/PostEditorView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'notifications',
    component: () => import('./views/NotificationsView.vue'),
    meta: { requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/posts' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && isLoggedIn()) {
    return { name: 'posts' }
  }
})

export default router
