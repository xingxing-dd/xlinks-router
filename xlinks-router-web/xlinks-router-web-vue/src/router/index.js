import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/login/index.vue'
import Register from '../views/register/index.vue'
import Dashboard from '../views/dashboard/index.vue'
import Tokens from '../views/tokens/index.vue'
import Models from '../views/models/index.vue'
import Plans from '../views/plans/index.vue'
import Promotion from '../views/promotion/index.vue'
import Contact from '../views/contact/index.vue'
import Docs from '../views/docs/index.vue'
import DefaultLayout from '../layouts/DefaultLayout.vue'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
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
        component: Promotion
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

export default router
