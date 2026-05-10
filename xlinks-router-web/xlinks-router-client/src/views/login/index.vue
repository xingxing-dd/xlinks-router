<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Check, CircleAlert, Copy, ExternalLink, Gift, Key, Lock, Phone, QrCode, Sparkles } from 'lucide-vue-next'
import { postApi, postAuth } from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'
import LocaleSwitch from '@/components/common/LocaleSwitch.vue'

const REMEMBER_ACCOUNT_KEY = 'xlinks-remember-account'
const REMEMBER_PHONE_KEY = 'xlinks-remember-phone'
const NEW_PLATFORM_URL = 'https://kr.xlinks.site'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const phone = ref(localStorage.getItem(REMEMBER_PHONE_KEY) || '')
const password = ref('')
const rememberMe = ref(localStorage.getItem(REMEMBER_ACCOUNT_KEY) !== 'false')
const isSubmitting = ref(false)
const hintMessage = ref('')
const showMigrationOverlay = ref(true)
const passwordCopied = ref(false)

const handleGoToNewSite = async () => {
  try {
    await postApi('/v1/public/stats/new-site-click')
  } catch (error) {
    console.error('Failed to count new site click', error)
  } finally {
    window.location.href = NEW_PLATFORM_URL
  }
}

const handleCopyPassword = async () => {
  try {
    await navigator.clipboard.writeText('Zxcv1234!')
    passwordCopied.value = true
    window.setTimeout(() => {
      passwordCopied.value = false
    }, 1800)
  } catch (error) {
    toast.error('复制失败', '请手动复制初始密码')
  }
}
function getRedirectPath() {
  const redirect = router.currentRoute.value.query.redirect
  return typeof redirect === 'string' && redirect ? redirect : '/tokens'
}

const handleSubmit = async () => {
  hintMessage.value = ''
  isSubmitting.value = true

  try {
    const rsaData = await postAuth('/rsa-public-key')
    hintMessage.value = rsaData?.algorithm ? `宸茶幏鍙栧叕閽ョ畻娉曪細${rsaData.algorithm}` : ''

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

    <div
      v-if="showMigrationOverlay"
      class="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/55 p-4 backdrop-blur-[6px]"
    >
      <div class="relative w-full max-w-5xl overflow-hidden rounded-[32px] border border-orange-200 bg-white shadow-[0_30px_120px_-30px_rgba(15,23,42,0.55)]">
        <div class="bg-[radial-gradient(circle_at_top_left,_rgba(251,146,60,0.22),_transparent_30%),linear-gradient(180deg,_#fff7ed_0%,_#ffffff_44%,_#fffaf5_100%)] px-6 py-6 md:px-8 md:py-8">

          <div class="mt-4 space-y-3">
            <h2 class="text-center text-2xl font-bold leading-tight text-slate-950 md:text-4xl">
              站点迁移公告
            </h2>
          </div>

          <div class="mt-5 rounded-3xl border-2 border-amber-300 bg-amber-50 p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]">
              &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;由于系统升级，需要迁移站点，特此发布迁移公告。请各位用户按尽快迁移新站，如有疑问请联系客服，本站会继续保留一段时间。对此次迁移给您带来的不便我们深表歉意，并感谢您的理解与支持。
          </div>

          <div class="mt-5 rounded-3xl border-2 border-amber-300 bg-amber-50 p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.7)]">
            <div class="grid gap-4 md:grid-cols-[1fr_190px] md:items-start">
              <div class="flex items-start gap-3">
                <div class="flex-1">
                  <p class="text-base font-bold text-slate-950 md:text-lg">新站点只能通过邮箱进行登录，请优先按此方式登录</p>
                  <div class="mt-3 space-y-3 text-sm leading-7 text-slate-700 md:text-[15px]">
                    <div class="rounded-2xl bg-white/80 px-4 py-3 ring-1 ring-amber-200">
                      <span class="font-bold text-slate-950">手机号注册用户：</span>
                      请在原手机号后补充
                      <span class="rounded-lg bg-white px-2 py-1 font-bold text-orange-600 ring-1 ring-amber-300">@qq.com</span>
                      作为登录邮箱。
                    </div>
                    <div class="rounded-2xl bg-white/80 px-4 py-3 ring-1 ring-amber-200">
                      <span class="font-bold text-slate-950">邮箱注册用户：</span>
                      可直接使用原邮箱登录。
                    </div>
                  </div>
                  <div class="mt-4 rounded-2xl bg-white/80 px-4 py-3 ring-1 ring-amber-200">
                    <div class="flex flex-wrap items-center justify-between gap-3">
                      <div class="font-bold text-slate-950">
                        <span>初始登录密码：</span>
                        <span class="text-orange-600">Zxcv1234!</span>
                      </div>
                      <button
                        type="button"
                        @click="handleCopyPassword"
                        class="inline-flex items-center gap-2 rounded-xl bg-slate-700 px-3 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
                      >
                        <component :is="passwordCopied ? Check : Copy" class="h-4 w-4" />
                        {{ passwordCopied ? '已复制' : '复制密码' }}
                      </button>
                    </div>
                  </div>
                  <div class="mt-3 space-y-3 text-sm leading-7 text-slate-700 md:text-[15px]">
                    <div class="rounded-2xl bg-white/80 px-4 py-3 ring-1 ring-amber-200">
                      <span class="font-bold text-slate-950">登录失败说明：</span>
                      本次只迁移了最近活跃的用户的账号和余额信息，如果尝试登录失败，请重新注册，联系客服恢复余额，领取补偿。
                    </div>
                  </div>
                </div>
              </div>

              <div class="rounded-2xl bg-slate-950 p-4 text-white">
                <div class="flex items-center gap-2 text-sm font-semibold text-orange-300">
                  <QrCode class="h-4 w-4" />
                  客服 QQ
                </div>
                <div class="mt-3 rounded-2xl bg-white p-3">
                  <img
                    src="/kefu.jpg"
                    alt="客服 QQ 二维码"
                    class="mx-auto aspect-square w-full max-w-[150px] rounded-xl object-cover"
                  />
                </div>
                <p class="mt-3 text-xs leading-6 text-white/70">如有任何问题，请联系客服处理。</p>
              </div>
            </div>
          </div>

          <div class="mt-5 rounded-2xl border border-slate-200 bg-white/90 p-4">
            <p class="text-sm font-semibold text-slate-900">迁移与补偿说明</p>
            <ul class="mt-3 space-y-2 text-sm leading-7 text-slate-600">
              <li>1. 原套餐余额直接迁移。</li>
              <li>2. 已消费过的用户（包含套餐和余额充值）补偿 <span class="font-bold text-orange-600 text-base">50 美元额度</span>，未消费的用户补偿 <span class="font-bold text-orange-600 text-base">20 美元额度</span>。</li>
              <li>3. 补偿请联系客服领取（人数较多，请见谅）。</li>
            </ul>
          </div>

          <div class="mt-6 grid gap-3 sm:grid-cols-2">
            <button
              type="button"
              @click="handleGoToNewSite"
              class="inline-flex items-center justify-center gap-2 rounded-2xl bg-gradient-to-r from-orange-500 to-pink-500 px-5 py-3.5 text-sm font-semibold text-white shadow-lg shadow-orange-500/30 transition hover:brightness-110"
            >
              跳转新站点
              <ExternalLink class="h-4 w-4" />
            </button>
            <button
              type="button"
              @click="showMigrationOverlay = false"
              class="inline-flex items-center justify-center gap-2 rounded-2xl border border-slate-300 bg-white px-5 py-3.5 text-sm font-semibold text-slate-800 transition hover:border-slate-400 hover:bg-slate-50"
            >
              登录老系统
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>














