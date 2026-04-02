<script setup>
import { useI18n } from 'vue-i18n'
import { Key, Shield, Lock, Phone, Sparkles } from 'lucide-vue-next'
import { useForgotPassword } from '@/composables/useForgotPassword'

const { t } = useI18n()
const {
  formData,
  isSubmitting,
  isSendingCode,
  countdown,
  handleSendCode,
  handleSubmit,
} = useForgotPassword()
</script>

<template>
  <div class="min-h-screen bg-gradient-main flex">
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
          {{ t('forgotPassword.heroSubtitle') }}
        </p>
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-primary" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('forgotPassword.feature1Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('forgotPassword.feature1Desc') }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-secondary/10 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-secondary" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('forgotPassword.feature2Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('forgotPassword.feature2Desc') }}</p>
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
            {{ t('forgotPassword.title') }}
          </h1>
          <p class="text-slate-500 mb-8">
            {{ t('forgotPassword.subtitle') }}
          </p>

          <form @submit.prevent="handleSubmit" class="space-y-5">
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('forgotPassword.account') }}
              </label>
              <div class="relative">
                <Phone class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.account"
                  type="text"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('forgotPassword.accountPlaceholder')"
                  required
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('forgotPassword.verificationCode') }}
              </label>
              <div class="relative flex gap-2">
                <div class="relative flex-1">
                  <Shield class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                  <input
                    v-model="formData.verificationCode"
                    type="text"
                    class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                    :placeholder="t('forgotPassword.verificationCodePlaceholder')"
                    required
                  />
                </div>
                <button
                  type="button"
                  @click="handleSendCode"
                  :disabled="isSendingCode || countdown > 0"
                  class="px-4 py-3 bg-primary/10 text-primary rounded-xl hover:bg-primary/20 transition-colors font-medium whitespace-nowrap text-sm disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {{ isSendingCode ? t('common.loading') : (countdown > 0 ? t('register.sendCodeCountdown', { countdown }) : t('forgotPassword.sendCode')) }}
                </button>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('forgotPassword.newPassword') }}
              </label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.newPassword"
                  type="password"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('forgotPassword.newPasswordPlaceholder')"
                  required
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('forgotPassword.confirmPassword') }}
              </label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.confirmPassword"
                  type="password"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-ring focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('forgotPassword.confirmPasswordPlaceholder')"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="w-full bg-gradient-to-r from-orange-500 to-pink-500 text-white py-3.5 rounded-xl hover:shadow-lg hover:shadow-orange-500/50 transition-all duration-200 font-semibold text-base disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {{ isSubmitting ? t('common.loading') : t('forgotPassword.submit') }}
            </button>
          </form>

          <div class="mt-6 text-center text-sm">
            <span class="text-slate-600">{{ t('forgotPassword.backToLoginText') }}</span>
            <router-link
              to="/login"
              class="ml-1 text-primary hover:text-primary font-semibold"
            >
              {{ t('forgotPassword.backToLogin') }}
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
