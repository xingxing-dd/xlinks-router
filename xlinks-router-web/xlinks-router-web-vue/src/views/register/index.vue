<script setup>
import { useI18n } from 'vue-i18n'
import { Key, Mail, Lock, Gift, Shield, Sparkles } from 'lucide-vue-next'
import { useRegister } from '@/composables/useRegister'

const { t } = useI18n()
const {
  formData,
  isSubmitting,
  isSendingCode,
  feedback,
  handleSubmit,
  handleSendCode,
} = useRegister()
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-orange-100 via-pink-100 to-purple-200 flex">
    <div class="hidden lg:flex lg:w-1/2 items-center justify-center p-12">
      <div class="max-w-xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-14 h-14 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-xl shadow-violet-500/30">
            <Key class="w-7 h-7 text-white" />
          </div>
          <div>
            <h1 class="text-5xl font-bold bg-gradient-to-r from-violet-600 to-fuchsia-600 bg-clip-text text-transparent">
              {{ t('auth.brandName') }}
            </h1>
          </div>
        </div>
        <p class="text-2xl text-slate-700 mb-8 font-medium">
          {{ t('auth.brandSlogan') }}
        </p>
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-violet-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-violet-600" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('auth.feature1Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('auth.feature1Desc') }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-fuchsia-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-fuchsia-600" />
            </div>
            <div>
              <h3 class="font-semibold text-slate-800 mb-1">{{ t('auth.feature2Title') }}</h3>
              <p class="text-slate-600 text-sm">{{ t('auth.feature2Desc') }}</p>
            </div>
          </div>
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0 mt-1">
              <Sparkles class="w-5 h-5 text-purple-600" />
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
            <div class="w-14 h-14 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg shadow-violet-500/30">
              <Key class="w-7 h-7 text-white" />
            </div>
          </div>

          <h1 class="text-3xl font-bold text-slate-900 mb-2">
            {{ t('register.title') }}
          </h1>
          <p class="text-slate-500 mb-8">
            {{ t('register.subtitle') }}
          </p>

        <!-- <p v-if="feedback" class="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {{ feedback }}
        </p> -->

          <form @submit.prevent="handleSubmit" class="space-y-5">
            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('register.email') }}
              </label>
              <div class="relative">
                <Mail class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.email"
                  type="email"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('register.emailPlaceholder')"
                  required
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('register.verificationCode') }}
              </label>
              <div class="relative flex gap-2">
                <div class="relative flex-1">
                  <Shield class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                  <input
                    v-model="formData.verificationCode"
                    type="text"
                    class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                    :placeholder="t('register.verificationCodePlaceholder')"
                    required
                  />
                </div>
                <button
                  type="button"
                  @click="handleSendCode"
                  :disabled="isSendingCode"
                  class="px-4 py-3 bg-violet-100 text-violet-600 rounded-xl hover:bg-violet-200 transition-colors font-medium whitespace-nowrap text-sm"
                >
                  {{ isSendingCode ? t('common.loading') : t('register.sendCode') }}
                </button>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('register.password') }}
              </label>
              <div class="relative">
                <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.password"
                  type="password"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('register.passwordPlaceholder')"
                  required
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-slate-700 mb-2">
                {{ t('register.inviteCode') }}
              </label>
              <div class="relative">
                <Gift class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.inviteCode"
                  type="text"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  :placeholder="t('register.inviteCodePlaceholder')"
                />
              </div>
              <p class="text-xs text-slate-500 mt-1.5">
                {{ t('register.inviteCodeTip') }}
              </p>
            </div>

            <div class="flex items-start">
              <input
                type="checkbox"
                class="w-4 h-4 mt-1 text-violet-600 border-slate-300 rounded focus:ring-violet-500 cursor-pointer"
                required
              />
              <label class="ml-2 text-sm text-slate-600">
                {{ t('register.termsPrefix') }}
                <a href="#" class="text-violet-600 hover:text-violet-700 font-medium transition-colors">
                  {{ t('register.terms') }}
                </a>
                和
                <a href="#" class="text-violet-600 hover:text-violet-700 font-medium transition-colors">
                  {{ t('register.privacy') }}
                </a>
              </label>
            </div>

            <button
              type="submit"
              :disabled="isSubmitting"
              class="w-full bg-gradient-to-r from-orange-500 to-pink-500 text-white py-3.5 rounded-xl hover:shadow-lg hover:shadow-orange-500/50 transition-all duration-200 font-semibold text-base"
            >
              {{ isSubmitting ? t('common.loading') : t('register.submit') }}
            </button>
          </form>

          <div class="mt-6 text-center text-sm">
            <span class="text-slate-600">{{ t('register.hasAccount') }}</span>
            <router-link
              to="/login"
              class="ml-1 text-violet-600 hover:text-violet-700 font-semibold"
            >
              {{ t('register.login') }}
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
