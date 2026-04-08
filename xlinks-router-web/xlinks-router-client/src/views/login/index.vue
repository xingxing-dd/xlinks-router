<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Key, Phone, Lock, Sparkles } from 'lucide-vue-next'
import { postAuth } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'
import LocaleSwitch from '@/components/common/LocaleSwitch.vue'

const REMEMBER_ACCOUNT_KEY = 'xlinks-remember-account'
const REMEMBER_PHONE_KEY = 'xlinks-remember-phone'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const phone = ref(localStorage.getItem(REMEMBER_PHONE_KEY) || '')
const password = ref('')
const rememberMe = ref(localStorage.getItem(REMEMBER_ACCOUNT_KEY) !== 'false')
const isSubmitting = ref(false)
const hintMessage = ref('')

function getRedirectPath() {
  const redirect = router.currentRoute.value.query.redirect
  return typeof redirect === 'string' && redirect ? redirect : '/tokens'
}

const handleSubmit = async () => {
  hintMessage.value = ''
  isSubmitting.value = true

  try {
    const rsaData = await postAuth('/rsa-public-key')
    hintMessage.value = rsaData?.algorithm ? `已获取公钥算法：${rsaData.algorithm}` : ''

    const loginData = await postAuth('/login', {
      username: phone.value,
      password: password.value,
    })

    authStore.setAccessToken(loginData?.accessToken, { remember: rememberMe.value })
    if (rememberMe.value) {
      localStorage.setItem(REMEMBER_ACCOUNT_KEY, 'true')
      localStorage.setItem(REMEMBER_PHONE_KEY, phone.value)
    } else {
      localStorage.setItem(REMEMBER_ACCOUNT_KEY, 'false')
      localStorage.removeItem(REMEMBER_PHONE_KEY)
    }
    toast.success(t('common.success'))
    router.push(getRedirectPath())
  } catch (error) {
    toast.error(t('common.error'), error.message)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="relative min-h-screen bg-gradient-main flex">
    <div class="absolute right-4 top-4 z-20">
      <LocaleSwitch />
    </div>

    <div class="hidden lg:flex lg:w-1/2 items-center justify-center p-12">
      <div class="max-w-xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-14 h-14 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-xl shadow-primary/20">
            <Key class="w-7 h-7 text-white" />
          </div>
          <div>
            <h1 class="text-5xl font-bold bg-gradient-icon bg-clip-text text-transparent">
              {{ t('auth.brandName') }}
            </h1>
          </div>
        </div>
        <p class="text-2xl text-slate-700 mb-8 font-medium">
          {{ t('auth.brandSlogan') }}
        </p>
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-primary" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('auth.feature1Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('auth.feature1Desc') }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-secondary/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-secondary" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('auth.feature2Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('auth.feature2Desc') }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-primary" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('auth.feature3Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('auth.feature3Desc') }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="w-full lg:w-1/2 flex items-center justify-center p-6 lg:p-12">
      <div class="w-full max-w-md">
        <div class="bg-white rounded-3xl shadow-2xl p-8 lg:p-10">
          <div class="flex lg:hidden items-center justify-center mb-6">
            <div class="w-14 h-14 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
              <Key class="w-7 h-7 text-white" />
            </div>
          </div>

          <h1 class="text-3xl font-bold text-slate-900 mb-2">
            {{ t('login.title') }}
          </h1>
          <p class="text-slate-500 mb-8">
            {{ t('login.subtitle') }}
          </p>

          <!-- <p v-if="hintMessage" class="mb-4 rounded-xl border border-sky-200 bg-sky-50 px-4 py-3 text-sm text-sky-700">
            {{ hintMessage }}
          </p> -->

          <form @submit.prevent="handleSubmit" class="space-y-5">
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('login.phone') }}
              </label>
              <div class="relative">
                <Phone class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="phone"
                  type="tel"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('login.phonePlaceholder')"
                  required
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('login.password') }}
              </label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="password"
                  type="password"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('login.passwordPlaceholder')"
                  required
                />
              </div>
            </div>

            <div class="flex items-center justify-between text-sm">
              <label class="flex items-center cursor-pointer">
                <input
                  v-model="rememberMe"
                  type="checkbox"
                  class="w-4 h-4 text-primary border-slate-300 rounded focus:ring-ring"
                />
                <span class="ml-2 text-slate-600">{{ t('login.rememberMe') }}</span>
              </label>
              <router-link to="/forgot-password" class="text-primary hover:text-primary font-medium">
                {{ t('login.forgotPassword') }}
              </router-link>
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="w-full bg-gradient-to-r from-orange-500 to-pink-500 text-white py-3.5 rounded-xl hover:shadow-lg hover:shadow-orange-500/50 transition-all duration-200 font-semibold text-base"
            >
              {{ isSubmitting ? t('common.loading') : t('login.submit') }}
            </button>

            <div class="relative my-6">
              <div class="absolute inset-0 flex items-center">
                <div class="w-full border-t border-slate-200" />
              </div>
              <div class="relative flex justify-center text-sm">
                <span class="px-4 bg-white text-slate-500"></span>
              </div>
            </div>

            <div class="text-center text-sm">
              <span class="text-slate-600">{{ t('login.noAccount') }}</span>
              <router-link
                to="/register"
                class="ml-1 text-primary hover:text-primary font-semibold"
              >
                {{ t('login.register') }}
              </router-link>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>
