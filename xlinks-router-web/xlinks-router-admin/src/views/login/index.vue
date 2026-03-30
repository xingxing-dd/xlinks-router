<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { Shield } from 'lucide-vue-next'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const account = ref('')
const password = ref('')
const remember = ref(true)
const loading = ref(false)

const handleLogin = () => {
  if (!account.value || !password.value) {
    toastStore.push('请输入账号与密码', 'warning')
    return
  }

  loading.value = true
  setTimeout(() => {
    authStore.setAuth({ name: account.value, role: '管理员' }, 'mock-token')
    loading.value = false
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  }, 600)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-main px-6">
    <div class="w-full max-w-4xl grid md:grid-cols-2 gap-10">
      <div class="hidden md:flex flex-col justify-center gap-6">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
            <Shield class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold text-slate-900">{{ t('login.title') }}</h1>
            <p class="text-slate-500 mt-1">运营端统一管理平台</p>
          </div>
        </div>
        <div class="card p-6">
          <h2 class="text-lg font-semibold text-slate-800">系统说明</h2>
          <ul class="mt-4 space-y-3 text-sm text-slate-600">
            <li>• 统一管理商户、服务商与模型资源</li>
            <li>• 监控 Token 与套餐使用情况</li>
            <li>• 跟踪交易流水与结算状态</li>
          </ul>
        </div>
      </div>

      <div class="card p-8">
        <h2 class="text-xl font-semibold text-slate-800">{{ t('login.subtitle') }}</h2>
        <div class="mt-6 space-y-4">
          <div>
            <label class="text-sm text-slate-500">{{ t('login.account') }}</label>
            <input v-model.trim="account" class="input mt-2" placeholder="admin@example.com" />
          </div>
          <div>
            <label class="text-sm text-slate-500">{{ t('login.password') }}</label>
            <input v-model.trim="password" type="password" class="input mt-2" placeholder="请输入密码" />
          </div>
          <label class="flex items-center gap-2 text-sm text-slate-500">
            <input v-model="remember" type="checkbox" class="accent-primary" />
            {{ t('login.remember') }}
          </label>
          <button class="btn-primary w-full" :disabled="loading" @click="handleLogin">
            {{ loading ? '登录中...' : t('login.signIn') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
