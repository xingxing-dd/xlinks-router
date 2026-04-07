<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const orderNo = computed(() => String(route.query.orderNo || '-'))

const goPlans = () => {
  router.push('/plans')
}

const goTokens = () => {
  router.push('/tokens')
}
</script>

<template>
  <div class="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,197,94,0.16),_transparent_35%),linear-gradient(135deg,_#f8fffb_0%,_#eefbf3_45%,_#f8fafc_100%)] px-4 py-10 md:px-6">
    <div class="mx-auto max-w-2xl">
      <div class="overflow-hidden rounded-[32px] border border-emerald-100 bg-white shadow-[0_30px_100px_-40px_rgba(16,185,129,0.45)]">
        <div class="bg-gradient-to-r from-emerald-500 via-green-500 to-teal-500 px-8 py-10 text-white">
          <div class="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-white/20 text-4xl shadow-lg backdrop-blur-sm">
            ✓
          </div>
          <p class="text-sm font-semibold uppercase tracking-[0.28em] text-white/80">Payment Result</p>
          <h1 class="mt-3 text-3xl font-black md:text-4xl">支付提交成功</h1>
          <p class="mt-3 max-w-xl text-sm text-white/85 md:text-base">
            你已从支付宝完成回跳，最终是否到账请以服务端异步通知处理结果为准。
          </p>
        </div>

        <div class="px-8 py-8">
          <div class="rounded-3xl border border-emerald-100 bg-emerald-50/70 p-6">
            <div class="flex items-center justify-between gap-4 border-b border-emerald-100 pb-4">
              <span class="text-sm font-medium text-slate-500">订单号</span>
              <span class="break-all text-right text-sm font-semibold text-slate-900">{{ orderNo }}</span>
            </div>
            <div class="mt-4 text-sm leading-7 text-slate-600">
              <p>1. 如果这是同步回调页面，说明支付宝前端跳转已经完成。</p>
              <p>2. 套餐是否真正生效，请以后端异步回调更新订单状态为准。</p>
              <p>3. 如果长时间未到账，请检查异步通知、订单状态和支付宝沙箱配置。</p>
            </div>
          </div>

          <div class="mt-6 flex flex-col gap-3 sm:flex-row">
            <button
              class="flex-1 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-emerald-500/20 transition-transform hover:-translate-y-0.5"
              @click="goPlans"
            >
              返回订阅计划
            </button>
            <button
              class="flex-1 rounded-2xl border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 transition-colors hover:bg-slate-50"
              @click="goTokens"
            >
              去我的令牌
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>