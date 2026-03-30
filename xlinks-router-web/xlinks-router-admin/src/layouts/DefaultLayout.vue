<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import {
  LayoutDashboard,
  Store,
  Users,
  KeySquare,
  Layers,
  CreditCard,
  Receipt,
  LogOut,
  Menu,
  X,
  Shield,
  ChevronDown,
} from 'lucide-vue-next'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isMobileMenuOpen = ref(false)
const isUserMenuOpen = ref(false)

const navItems = computed(() => [
  { path: '/dashboard', label: t('nav.dashboard'), icon: LayoutDashboard },
  { path: '/merchants', label: t('nav.merchants'), icon: Store },
  { path: '/providers', label: t('nav.providers'), icon: Users },
  { path: '/tokens', label: t('nav.tokens'), icon: KeySquare },
  { path: '/models', label: t('nav.models'), icon: Layers },
  { path: '/plans', label: t('nav.plans'), icon: CreditCard },
  { path: '/trades', label: t('nav.trades'), icon: Receipt },
])

const isActive = (path) => route.path.startsWith(path)

const currentLabel = computed(() => {
  const item = navItems.value.find(item => isActive(item.path))
  return item ? item.label : t('nav.dashboard')
})

const handleLogout = () => {
  isUserMenuOpen.value = false
  authStore.clearAuth()
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen bg-gradient-main">
    <aside class="hidden md:flex md:flex-col md:w-72 bg-white shadow-xl border-r border-slate-200 overflow-hidden">
      <div class="p-6 border-b border-slate-200">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-gradient-icon rounded-xl flex items-center justify-center shadow-lg shadow-primary/20">
            <Shield class="w-5 h-5 text-white" />
          </div>
          <h1 class="text-xl font-bold bg-gradient-icon bg-clip-text text-transparent tracking-tight">Xlinks Admin</h1>
        </div>
      </div>

      <nav class="flex-1 p-4 space-y-2 overflow-y-auto">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all duration-200 group"
          :class="[
            isActive(item.path)
              ? 'bg-gradient-button text-white shadow-lg shadow-primary/25'
              : 'text-slate-600 hover:bg-primary/5 hover:text-slate-900'
          ]"
        >
          <component
            :is="item.icon"
            class="w-5 h-5 transition-transform duration-300"
            :class="[isActive(item.path) ? '' : 'group-hover:scale-110']"
          />
          <span class="font-medium">{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>

    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="hidden md:flex items-center justify-between px-6 py-4 bg-white/90 backdrop-blur-lg border-b border-slate-200 shadow-sm z-10">
        <div class="flex items-center gap-4">
          <h2 class="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent tracking-tight">{{ currentLabel }}</h2>
        </div>

        <div class="flex items-center gap-4">
          <div class="relative">
            <button
              @click="isUserMenuOpen = !isUserMenuOpen"
              class="flex items-center gap-3 px-4 py-2 h-12 bg-secondary/10 hover:bg-secondary/15 rounded-xl border border-secondary/15 transition-all"
            >
              <div class="w-8 h-8 bg-gradient-icon rounded-full flex items-center justify-center shadow-sm">
                <Users class="w-4 h-4 text-white" />
              </div>
              <span class="text-sm font-semibold text-slate-700">admin</span>
              <ChevronDown class="w-4 h-4 text-slate-400 transition-transform duration-300" :class="{ 'rotate-180': isUserMenuOpen }" />
            </button>

            <div v-if="isUserMenuOpen" class="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-20 animate-in fade-in slide-in-from-top-2 duration-200">
              <button
                @click="handleLogout"
                class="w-full flex items-center gap-3 px-4 py-2.5 text-slate-600 hover:bg-slate-50 hover:text-primary transition-colors"
              >
                <LogOut class="w-4 h-4" />
                <span class="font-medium text-sm">{{ t('common.logout') }}</span>
              </button>
            </div>
            <div v-if="isUserMenuOpen" @click="isUserMenuOpen = false" class="fixed inset-0 z-10"></div>
          </div>
        </div>
      </header>

      <div class="md:hidden fixed top-0 left-0 right-0 bg-white/95 backdrop-blur-lg border-b border-slate-200 z-50 shadow-sm">
        <div class="flex items-center justify-between p-4">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
              <Shield class="w-4 h-4 text-white" />
            </div>
            <h1 class="font-bold text-slate-900 tracking-tight">Xlinks Admin</h1>
          </div>
          <button @click="isMobileMenuOpen = !isMobileMenuOpen" class="p-2 hover:bg-slate-50 rounded-xl transition-colors">
            <component :is="isMobileMenuOpen ? X : Menu" class="w-6 h-6 text-slate-600" />
          </button>
        </div>

        <div v-if="isMobileMenuOpen" class="border-t border-slate-200 p-4 space-y-1.5 bg-white animate-in fade-in slide-in-from-top-4 duration-300">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            @click="isMobileMenuOpen = false"
            class="flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all"
            :class="[
              isActive(item.path)
                ? 'bg-gradient-button text-white shadow-md shadow-primary/25'
                : 'text-slate-600 hover:bg-slate-50'
            ]"
          >
            <component :is="item.icon" class="w-5 h-5" />
            <span class="font-medium">{{ item.label }}</span>
          </router-link>
          <button
            @click="handleLogout"
            class="flex items-center gap-3 px-4 py-3.5 w-full text-slate-600 hover:bg-red-50 hover:text-red-600 rounded-xl transition-all mt-4 border-t border-slate-100 pt-6"
          >
            <LogOut class="w-5 h-5" />
            <span class="font-medium text-sm">{{ t('common.logout') }}</span>
          </button>
        </div>
      </div>

      <main class="flex-1 overflow-auto mt-[73px] md:mt-0">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.animate-in {
  animation-duration: 0.2s;
  animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  animation-fill-mode: forwards;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slide-in-from-top-2 {
  from { transform: translateY(-0.5rem); }
  to { transform: translateY(0); }
}

@keyframes slide-in-from-top-4 {
  from { transform: translateY(-1rem); }
  to { transform: translateY(0); }
}

.fade-in { animation-name: fade-in; }
.slide-in-from-top-2 { animation-name: slide-in-from-top-2; }
.slide-in-from-top-4 { animation-name: slide-in-from-top-4; }
</style>
