<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { loginAdmin } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { Shield } from 'lucide-vue-next'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toastStore = useToastStore()

const account = ref('admin')
const password = ref('')
const remember = ref(true)
const loading = ref(false)

const handleLogin = async () => {
  if (!account.value || !password.value) {
    toastStore.push('请输入账号与密码', 'warning')
    return
  }

  loading.value = true
  try {
    const result = await loginAdmin({
      username: account.value,
      password: password.value,
    })
    authStore.setAuth({
      ...result.user,
      name: result.user.displayName || result.user.username,
      remember: remember.value,
    }, result.accessToken)
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (error) {
    toastStore.push(error.message || '登录失败', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-main px-6 py-10">
    <div class="w-full max-w-5xl grid gap-10 md:grid-cols-2">
      <div class="hidden md:flex flex-col justify-center gap-6">
        <div class="flex items-center gap-3">
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
            <Shield class="w-6 h-6 text-white" />
          </div>
          <div>
            <h1 class="text-2xl font-bold text-slate-900">{{ t('login.title') }}</h1>
            <p class="text-slate-500 mt-1">统一管理服务商、模型、套餐和客户 Token 的后台工作台</p>
          </div>
        </div>
        <div class="card p-6 space-y-4">
          <h2 class="text-lg font-semibold text-slate-800">当前后台能力</h2>
          <p class="text-sm text-slate-600">- 管理服务商、服务商 Token、标准端点、标准模型和上游模型映射。</p>
          <p class="text-sm text-slate-600">- 维护客户 Token、套餐配置、第三方支付链接与激活码库存。</p>
        </div>
      </div>

      <div class="card p-8">
        <h2 class="text-xl font-semibold text-slate-800">{{ t('login.subtitle') }}</h2>
        <p class="text-sm text-slate-500 mt-2">请输入管理员账号密码登录后台。</p>
        <div class="mt-6 space-y-4">
          <div>
            <label class="text-sm text-slate-500">{{ t('login.account') }}</label>
            <input v-model.trim="account" class="input mt-2" />
          </div>
          <div>
            <label class="text-sm text-slate-500">{{ t('login.password') }}</label>
            <input v-model.trim="password" type="password" class="input mt-2"  />
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
