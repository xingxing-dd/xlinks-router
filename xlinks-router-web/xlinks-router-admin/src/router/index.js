import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login/index.vue'
import Dashboard from '../views/dashboard/index.vue'
import Merchants from '../views/merchants/index.vue'
import Providers from '../views/providers/index.vue'
import Tokens from '../views/tokens/index.vue'
import Models from '../views/models/index.vue'
import Plans from '../views/plans/index.vue'
import Trades from '../views/trades/index.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: Dashboard },
      { path: 'merchants', name: 'Merchants', component: Merchants },
      { path: 'providers', name: 'Providers', component: Providers },
      { path: 'tokens', name: 'Tokens', component: Tokens },
      { path: 'models', name: 'Models', component: Models },
      { path: 'plans', name: 'Plans', component: Plans },
      { path: 'trades', name: 'Trades', component: Trades },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  const publicPaths = ['/login']

  if (!publicPaths.includes(to.path) && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (publicPaths.includes(to.path) && authStore.isAuthenticated) {
    return '/dashboard'
  }

  return true
})

export default router
