import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const Login = () => import('../views/login/index.vue')
const ForgotPassword = () => import('../views/forgot-password/index.vue')
const Register = () => import('../views/register/index.vue')
const Dashboard = () => import('../views/dashboard/index.vue')
const Tokens = () => import('../views/tokens/index.vue')
const Models = () => import('../views/models/index.vue')
const Plans = () => import('../views/plans/index.vue')
const Promotion = () => import('../views/promotion/index.vue')
const Contact = () => import('../views/contact/index.vue')
const Docs = () => import('../views/docs/index.vue')
const Landing = () => import('../views/landing/index.vue')
const PaymentSuccess = () => import('../views/payment/success.vue')
const PaymentError = () => import('../views/payment/error.vue')
const DefaultLayout = () => import('../layouts/DefaultLayout.vue')

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
    path: '/payment/success',
    name: 'PaymentSuccess',
    component: PaymentSuccess
  },
  {
    path: '/payment/error',
    name: 'PaymentError',
    component: PaymentError
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
  const publicPaths = ['/landing', '/login', '/register', '/forgot-password', '/promotion', '/contact', '/payment/success', '/payment/error']

  if (!publicPaths.includes(to.path) && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (publicPaths.includes(to.path) && authStore.isAuthenticated && !['/promotion', '/contact', '/payment/success', '/payment/error'].includes(to.path)) {
    return '/tokens'
  }

  return true
})

export default router
