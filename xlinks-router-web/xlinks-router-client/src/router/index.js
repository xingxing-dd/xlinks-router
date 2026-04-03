import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login/index.vue'
import ForgotPassword from '../views/forgot-password/index.vue'
import Register from '../views/register/index.vue'
import Dashboard from '../views/dashboard/index.vue'
import Tokens from '../views/tokens/index.vue'
import Models from '../views/models/index.vue'
import Plans from '../views/plans/index.vue'
import Promotion from '../views/promotion/index.vue'
import Contact from '../views/contact/index.vue'
import Docs from '../views/docs/index.vue'
import Landing from '../views/landing/index.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: '/landing'
  },
  {
    path: '/landing',
    name: 'Landing',
    component: Landing
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: ForgotPassword
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard
      },
      {
        path: 'tokens',
        name: 'Tokens',
        component: Tokens
      },
      {
        path: 'models',
        name: 'Models',
        component: Models
      },
      {
        path: 'plans',
        name: 'Plans',
        component: Plans
      },
      {
        path: 'promotion',
        name: 'Promotion',
        component: Promotion,
        props: (route) => ({ ref: route.query.ref || '' })
      },
      {
        path: 'contact',
        name: 'Contact',
        component: Contact
      },
      {
        path: 'docs',
        name: 'Docs',
        component: Docs
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  // Allow marketing, support, and auth pages to be visited without login.
  const publicPaths = ['/landing', '/login', '/register', '/forgot-password', '/promotion', '/contact']

  if (!publicPaths.includes(to.path) && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (publicPaths.includes(to.path) && authStore.isAuthenticated && !['/promotion', '/contact'].includes(to.path)) {
    return '/tokens'
  }

  return true
})

export default router
