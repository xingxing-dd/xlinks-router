<script setup>
import { useI18n } from 'vue-i18n'
import { Key, Mail, Lock, Gift, Shield } from 'lucide-vue-next'
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
  <div class="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl p-8 border border-white/20">
        <!-- Logo -->
        <div class="flex items-center justify-center mb-8">
          <div class="w-16 h-16 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg shadow-violet-500/50">
            <Key class="w-8 h-8 text-white" />
          </div>
        </div>

        <h1 class="text-2xl font-bold text-center text-slate-900 mb-2">
          {{ t('register.title') }}
        </h1>
        <p class="text-center text-slate-500 mb-8">
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
                  :placeholder="t('register.verificationCode')"
                  required
                />
              </div>
              <button
                type="button"
                @click="handleSendCode"
                :disabled="isSendingCode"
                class="px-4 py-3 bg-violet-100 text-violet-600 rounded-xl hover:bg-violet-200 transition-colors font-medium whitespace-nowrap"
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
                placeholder="••••••••"
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
            class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium active:scale-[0.98]"
          >
            {{ isSubmitting ? t('common.loading') : t('register.submit') }}
          </button>
        </form>

        <div class="mt-6 text-center">
          <span class="text-slate-600">{{ t('register.hasAccount') }}</span>
          <router-link
            to="/login"
            class="ml-1 text-violet-600 hover:text-violet-700 font-medium transition-colors"
          >
            {{ t('register.login') }}
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
