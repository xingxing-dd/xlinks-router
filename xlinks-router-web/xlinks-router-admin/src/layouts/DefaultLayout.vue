<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { logoutAdmin } from '@/api/admin'
import {
  LayoutDashboard,
  Cloud,
  KeyRound,
  KeySquare,
  Boxes,
  LogOut,
  Menu,
  X,
  Shield,
  ChevronDown,
  Package,
  Ticket,
  ReceiptText,
  History,
  Link2,
  Layers3,
  FolderKanban,
} from 'lucide-vue-next'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isMobileMenuOpen = ref(false)
const isUserMenuOpen = ref(false)
const loggingOut = ref(false)

const groupOpenState = reactive({
  overview: true,
  resources: true,
  operations: true,
  payments: true,
})

const navGroups = computed(() => [
  {
    key: 'overview',
    label: '总览',
    icon: Layers3,
    children: [
      { path: '/dashboard', label: t('nav.dashboard'), icon: LayoutDashboard },
      { path: '/merchants', label: t('nav.merchants'), icon: Shield },
    ],
  },
  {
    key: 'resources',
    label: '资源管理',
    icon: Cloud,
    children: [
      { path: '/providers', label: t('nav.providers'), icon: Cloud },
      { path: '/provider-tokens', label: t('nav.providerTokens'), icon: KeySquare },
      { path: '/customer-tokens', label: t('nav.customerTokens'), icon: KeyRound },
      { path: '/models', label: t('nav.models'), icon: Boxes },
    ],
  },
  {
    key: 'operations',
    label: '套餐运营',
    icon: FolderKanban,
    children: [
      { path: '/plans', label: t('nav.plans'), icon: Package },
      { path: '/subscriptions', label: t('nav.subscriptions'), icon: ReceiptText },
      { path: '/activation-codes', label: t('nav.activationCodes'), icon: Ticket },
      { path: '/activation-usage', label: t('nav.activationUsage'), icon: History },
      { path: '/usage-records', label: 'Token使用记录', icon: History },
    ],
  },
  {
    key: 'payments',
    label: '支付管理',
    icon: Link2,
    children: [
      { path: '/payment-methods', label: t('nav.paymentMethods'), icon: ReceiptText },
      { path: '/pay-links', label: t('nav.payLinks'), icon: Link2 },
    ],
  },
])

const flatNavItems = computed(() => navGroups.value.flatMap((group) => group.children))

const isActive = (path) => route.path.startsWith(path)
const isGroupActive = (group) => group.children.some((item) => isActive(item.path))
const isGroupExpanded = (group) => groupOpenState[group.key] || isGroupActive(group)

const toggleGroup = (groupKey) => {
  groupOpenState[groupKey] = !groupOpenState[groupKey]
}

const currentLabel = computed(() => {
  const item = flatNavItems.value.find((navItem) => isActive(navItem.path))
  return item ? item.label : t('nav.dashboard')
})

const currentUserName = computed(() => (
  authStore.user?.displayName
  || authStore.user?.name
  || authStore.user?.username
  || 'admin'
))

const handleLogout = async () => {
  if (loggingOut.value) {
    return
  }
  loggingOut.value = true
  isUserMenuOpen.value = false
  try {
    await logoutAdmin()
  } catch (error) {
    // Always clear local auth even if remote logout fails.
  } finally {
    authStore.clearAuth()
    loggingOut.value = false
    router.push('/login')
  }
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
          <div>
            <h1 class="text-xl font-bold bg-gradient-icon bg-clip-text text-transparent tracking-tight">Xlinks Admin</h1>
            <p class="text-xs text-slate-400 mt-1">中转平台运营后台</p>
          </div>
        </div>
      </div>

      <nav class="flex-1 p-4 space-y-3 overflow-y-auto">
        <section
          v-for="group in navGroups"
          :key="group.key"
          class="rounded-2xl border border-slate-200/80 bg-slate-50/70 overflow-hidden"
        >
          <button
            class="w-full flex items-center justify-between gap-3 px-4 py-3 text-left hover:bg-white/80 transition-colors"
            @click="toggleGroup(group.key)"
          >
            <div class="flex items-center gap-3">
              <div
                class="w-9 h-9 rounded-xl flex items-center justify-center"
                :class="isGroupActive(group) ? 'bg-primary text-white shadow-md shadow-primary/20' : 'bg-white text-slate-500 border border-slate-200'"
              >
                <component :is="group.icon" class="w-4 h-4" />
              </div>
              <div>
                <div class="text-sm font-semibold text-slate-800">{{ group.label }}</div>
                <div class="text-xs text-slate-400 mt-0.5">{{ group.children.length }} 个功能</div>
              </div>
            </div>
            <ChevronDown
              class="w-4 h-4 text-slate-400 transition-transform duration-200"
              :class="{ 'rotate-180': isGroupExpanded(group) }"
            />
          </button>

          <div v-if="isGroupExpanded(group)" class="px-3 pb-3 space-y-1.5">
            <router-link
              v-for="item in group.children"
              :key="item.path"
              :to="item.path"
              class="flex items-center gap-3 px-3 py-3 rounded-xl transition-all duration-200 group"
              :class="[
                isActive(item.path)
                  ? 'bg-gradient-button text-white shadow-lg shadow-primary/25'
                  : 'text-slate-600 hover:bg-white hover:text-slate-900'
              ]"
            >
              <component
                :is="item.icon"
                class="w-4 h-4 transition-transform duration-300"
                :class="[isActive(item.path) ? '' : 'group-hover:scale-110']"
              />
              <span class="font-medium text-sm">{{ item.label }}</span>
            </router-link>
          </div>
        </section>
      </nav>
    </aside>

    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="hidden md:flex items-center justify-between px-6 py-4 bg-white/90 backdrop-blur-lg border-b border-slate-200 shadow-sm z-10">
        <h2 class="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent tracking-tight">{{ currentLabel }}</h2>

        <div class="relative">
          <button
            @click="isUserMenuOpen = !isUserMenuOpen"
            class="flex items-center gap-3 px-4 py-2 h-12 bg-secondary/10 hover:bg-secondary/15 rounded-xl border border-secondary/15 transition-all"
          >
            <div class="w-8 h-8 bg-gradient-icon rounded-full flex items-center justify-center shadow-sm">
              <Shield class="w-4 h-4 text-white" />
            </div>
            <span class="text-sm font-semibold text-slate-700">{{ currentUserName }}</span>
            <ChevronDown class="w-4 h-4 text-slate-400 transition-transform duration-300" :class="{ 'rotate-180': isUserMenuOpen }" />
          </button>

          <div v-if="isUserMenuOpen" class="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-20 animate-in fade-in slide-in-from-top-2 duration-200">
            <button
              @click="handleLogout"
              class="w-full flex items-center gap-3 px-4 py-2.5 text-slate-600 hover:bg-slate-50 hover:text-primary transition-colors"
            >
              <LogOut class="w-4 h-4" />
              <span class="font-medium text-sm">{{ loggingOut ? '退出中...' : t('common.logout') }}</span>
            </button>
          </div>
          <div v-if="isUserMenuOpen" @click="isUserMenuOpen = false" class="fixed inset-0 z-10"></div>
        </div>
      </header>

      <div class="md:hidden fixed top-0 left-0 right-0 bg-white/95 backdrop-blur-lg border-b border-slate-200 z-50 shadow-sm">
        <div class="flex items-center justify-between p-4">
          <div class="flex items-center gap-2">
            <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
              <Shield class="w-4 h-4 text-white" />
            </div>
            <div>
              <h1 class="font-bold text-slate-900 tracking-tight">Xlinks Admin</h1>
              <p class="text-[11px] text-slate-400">中转平台运营后台</p>
            </div>
          </div>
          <button @click="isMobileMenuOpen = !isMobileMenuOpen" class="p-2 hover:bg-slate-50 rounded-xl transition-colors">
            <component :is="isMobileMenuOpen ? X : Menu" class="w-6 h-6 text-slate-600" />
          </button>
        </div>

        <div v-if="isMobileMenuOpen" class="border-t border-slate-200 p-4 space-y-3 bg-white animate-in fade-in slide-in-from-top-4 duration-300">
          <section
            v-for="group in navGroups"
            :key="group.key"
            class="rounded-2xl border border-slate-200 bg-slate-50/80 overflow-hidden"
          >
            <button
              class="w-full flex items-center justify-between gap-3 px-4 py-3 text-left"
              @click="toggleGroup(group.key)"
            >
              <div class="flex items-center gap-3">
                <component :is="group.icon" class="w-4 h-4 text-slate-500" />
                <span class="font-semibold text-sm text-slate-800">{{ group.label }}</span>
              </div>
              <ChevronDown
                class="w-4 h-4 text-slate-400 transition-transform duration-200"
                :class="{ 'rotate-180': isGroupExpanded(group) }"
              />
            </button>

            <div v-if="isGroupExpanded(group)" class="px-3 pb-3 space-y-1.5">
              <router-link
                v-for="item in group.children"
                :key="item.path"
                :to="item.path"
                @click="isMobileMenuOpen = false"
                class="flex items-center gap-3 px-4 py-3 rounded-xl transition-all"
                :class="[
                  isActive(item.path)
                    ? 'bg-gradient-button text-white shadow-md shadow-primary/25'
                    : 'text-slate-600 hover:bg-white'
                ]"
              >
                <component :is="item.icon" class="w-4 h-4" />
                <span class="font-medium text-sm">{{ item.label }}</span>
              </router-link>
            </div>
          </section>

          <button
            @click="handleLogout"
            class="flex items-center gap-3 px-4 py-3.5 w-full text-slate-600 hover:bg-red-50 hover:text-red-600 rounded-xl transition-all mt-2 border border-slate-200"
          >
            <LogOut class="w-5 h-5" />
            <span class="font-medium text-sm">{{ loggingOut ? '退出中...' : t('common.logout') }}</span>
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
