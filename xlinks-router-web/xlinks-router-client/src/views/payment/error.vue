<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const errorMessage = computed(() => String(route.query.msg || '支付处理失败，请稍后重试'))

const retryPlans = () => {
  router.push('/plans')
}

const goContact = () => {
  router.push('/contact')
}
</script>

<template>
  <div class="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(244,63,94,0.14),_transparent_35%),linear-gradient(135deg,_#fff7f7_0%,_#fff1f2_40%,_#f8fafc_100%)] px-4 py-10 md:px-6">
    <div class="mx-auto max-w-2xl">
      <div class="overflow-hidden rounded-[32px] border border-rose-100 bg-white shadow-[0_30px_100px_-40px_rgba(244,63,94,0.45)]">
        <div class="bg-gradient-to-r from-rose-500 via-orange-500 to-amber-500 px-8 py-10 text-white">
          <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/20 text-4xl shadow-lg backdrop-blur-sm">
            !
          </div>
          <p class="text-sm font-semibold uppercase tracking-[0.28em] text-white/80">Payment Result</p>
          <h1 class="mt-3 text-3xl font-black md:text-4xl">支付失败或未完成</h1>
          <p class="mt-3 max-w-xl text-sm text-white/85 md:text-base">
            这是支付失败页，通常由同步回调跳转而来，便于给用户展示错误信息。
          </p>
        </div>

        <div class="px-8 py-8">
          <div class="rounded-3xl border border-rose-100 bg-rose-50/80 p-6">
            <div class="text-sm font-medium text-slate-500">错误信息</div>
            <div class="mt-3 break-all text-base font-semibold text-slate-900">{{ errorMessage }}</div>
            <div class="mt-5 text-sm leading-7 text-slate-600">
              <p>1. 如果是签名失败，优先检查 `appId`、私钥和支付宝公钥是否一一对应。</p>
              <p>2. 如果是扫码过期，检查是否使用了支付宝沙箱买家账号，以及沙箱订单是否可支付。</p>
              <p>3. 如仍失败，可回到订阅页重新发起支付，或联系支持排查日志。</p>
            </div>
          </div>

          <div class="mt-6 flex flex-col gap-3 sm:flex-row">
            <button
              class="flex-1 rounded-2xl bg-gradient-to-r from-rose-500 to-orange-500 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-rose-500/20 transition-transform hover:-translate-y-0.5"
              @click="retryPlans"
            >
              返回重新支付
            </button>
            <button
              class="flex-1 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
              @click="goContact"
            >
              联系支持
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>