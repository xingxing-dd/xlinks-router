import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login/index.vue'
import Dashboard from '../views/dashboard/index.vue'
import Merchants from '../views/merchants/index.vue'
import Providers from '../views/providers/index.vue'
import ProviderTokens from '../views/provider-tokens/index.vue'
import CustomerTokens from '../views/customer-tokens/index.vue'
import Models from '../views/models/index.vue'
import Plans from '../views/plans/index.vue'
import Subscriptions from '../views/subscriptions/index.vue'
import ActivationCodes from '../views/activation-codes/index.vue'
import ActivationUsage from '../views/activation-usage/index.vue'
import PaymentMethods from '../views/payment-methods/index.vue'
import PayLinks from '../views/pay-links/index.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: Login },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: Dashboard },
      { path: 'merchants', name: 'Merchants', component: Merchants },
      { path: 'providers', name: 'Providers', component: Providers },
      { path: 'provider-tokens', name: 'ProviderTokens', component: ProviderTokens },
      { path: 'customer-tokens', name: 'CustomerTokens', component: CustomerTokens },
      { path: 'models', name: 'Models', component: Models },
      { path: 'plans', name: 'Plans', component: Plans },
      { path: 'subscriptions', name: 'Subscriptions', component: Subscriptions },
      { path: 'activation-codes', name: 'ActivationCodes', component: ActivationCodes },
      { path: 'activation-usage', name: 'ActivationUsage', component: ActivationUsage },
      { path: 'payment-methods', name: 'PaymentMethods', component: PaymentMethods },
      { path: 'pay-links', name: 'PayLinks', component: PayLinks },
      { path: 'tokens', redirect: '/customer-tokens' },
      { path: 'trades', redirect: '/dashboard' },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
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
