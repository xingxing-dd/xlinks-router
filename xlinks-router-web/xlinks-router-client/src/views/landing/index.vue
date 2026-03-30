<script setup>
import { ArrowRight, Sparkles, Shield, Gauge, Globe } from 'lucide-vue-next'
</script>

<template>
  <div class="min-h-screen bg-slate-950 text-white">
    <header class="border-b border-white/10">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <div class="flex items-center gap-3">
          <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/10 text-white">
            <Sparkles class="h-5 w-5" />
          </div>
          <div>
            <p class="text-lg font-semibold">XLinks Router</p>
            <p class="text-xs text-white/60">智能模型路由与统一接入</p>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <RouterLink
            to="/login"
            class="rounded-xl border border-white/20 px-4 py-2 text-sm font-medium text-white/90 hover:border-white/40 hover:text-white"
          >
            登录
          </RouterLink>
          <RouterLink
            to="/register"
            class="rounded-xl bg-white px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-white/90"
          >
            注册
          </RouterLink>
        </div>
      </div>
    </header>

    <main>
      <section class="mx-auto max-w-6xl px-6 py-16">
        <div class="grid gap-10 lg:grid-cols-2">
          <div class="space-y-6">
            <span class="inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/5 px-4 py-2 text-xs text-white/80">
              <Sparkles class="h-4 w-4" />
              一站式多模型接入平台
            </span>
            <h1 class="text-4xl font-bold leading-tight md:text-5xl">
              统一接入，智能路由，
              <span class="text-primary">让 AI 能力快速交付</span>
            </h1>
            <p class="text-base text-white/70">
              XLinks Router 帮助企业快速聚合主流模型服务，统一鉴权与额度管理，
              在性能、成本与稳定性之间实现最优路由策略。
            </p>
            <div class="flex flex-wrap gap-3">
              <RouterLink
                to="/register"
                class="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-white shadow-lg shadow-primary/25 hover:brightness-110"
              >
                立即体验
                <ArrowRight class="h-4 w-4" />
              </RouterLink>
            </div>
            <div class="flex flex-wrap gap-6 text-sm text-white/60">
              <div>✔ 秒级接入</div>
              <div>✔ 企业级安全</div>
              <div>✔ 灵活计费</div>
            </div>
          </div>
          <div class="rounded-3xl border border-white/10 bg-gradient-to-br from-white/10 via-white/5 to-transparent p-8">
            <div class="space-y-6">
              <div class="rounded-2xl bg-white/10 p-5">
                <p class="text-sm text-white/70">实时路由策略</p>
                <p class="mt-2 text-2xl font-semibold">自动选择最优模型</p>
              </div>
              <div class="grid gap-4 md:grid-cols-2">
                <div class="rounded-2xl bg-white/10 p-4">
                  <Shield class="h-5 w-5 text-primary" />
                  <p class="mt-3 text-sm font-semibold">安全鉴权</p>
                  <p class="mt-1 text-xs text-white/60">统一 Token 管理与权限控制</p>
                </div>
                <div class="rounded-2xl bg-white/10 p-4">
                  <Gauge class="h-5 w-5 text-primary" />
                  <p class="mt-3 text-sm font-semibold">性能监控</p>
                  <p class="mt-1 text-xs text-white/60">全链路调用观测与报表</p>
                </div>
                <div class="rounded-2xl bg-white/10 p-4">
                  <Globe class="h-5 w-5 text-primary" />
                  <p class="mt-3 text-sm font-semibold">多模型聚合</p>
                  <p class="mt-1 text-xs text-white/60">统一 API 适配主流模型</p>
                </div>
                <div class="rounded-2xl bg-white/10 p-4">
                  <Sparkles class="h-5 w-5 text-primary" />
                  <p class="mt-3 text-sm font-semibold">灵活计费</p>
                  <p class="mt-1 text-xs text-white/60">按量或套餐组合更省心</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="mx-auto max-w-6xl px-6 pb-16">
        <div class="grid gap-6 md:grid-cols-3">
          <div class="rounded-3xl border border-white/10 bg-white/5 p-6">
            <p class="text-sm text-white/60">场景覆盖</p>
            <p class="mt-2 text-lg font-semibold">对话、检索、代码、图像</p>
            <p class="mt-2 text-sm text-white/60">满足多业务的模型调用需求</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-white/5 p-6">
            <p class="text-sm text-white/60">稳定可靠</p>
            <p class="mt-2 text-lg font-semibold">多活容灾 + 智能降级</p>
            <p class="mt-2 text-sm text-white/60">保障调用稳定与响应速度</p>
          </div>
          <div class="rounded-3xl border border-white/10 bg-white/5 p-6">
            <p class="text-sm text-white/60">快速上手</p>
            <p class="mt-2 text-lg font-semibold">控制台 + API 文档</p>
            <p class="mt-2 text-sm text-white/60">开箱即用，分钟级接入</p>
          </div>
        </div>
      </section>

      <section class="mx-auto max-w-6xl px-6 pb-20">
        <div class="rounded-3xl bg-gradient-to-r from-primary/90 via-primary/70 to-indigo-500/90 px-8 py-10 text-center shadow-2xl shadow-primary/30">
          <h2 class="text-2xl font-bold">准备好开启你的模型路由之旅了吗？</h2>
          <p class="mt-2 text-sm text-white/90">立即注册，解锁完整控制台能力</p>
          <div class="mt-6 flex flex-wrap justify-center gap-3">
            <RouterLink
              to="/register"
              class="rounded-xl bg-white px-5 py-3 text-sm font-semibold text-primary"
            >
              免费注册
            </RouterLink>
            <RouterLink
              to="/login"
              class="rounded-xl border border-white/40 px-5 py-3 text-sm font-semibold text-white"
            >
              登录体验
            </RouterLink>
          </div>
        </div>
      </section>
    </main>

    <footer class="border-t border-white/10">
      <div class="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 px-6 py-6 text-xs text-white/50 md:flex-row">
        <span>© 2026 XLinks Router. All rights reserved.</span>
        <span class="flex items-center gap-3">
          <span>统一接入 · 智能路由 · 稳定可靠</span>
          <a
            href="https://beian.miit.gov.cn/"
            target="_blank"
            rel="noopener noreferrer"
            class="hover:text-white transition-colors"
          >
            鄂ICP备2025153632号-2
          </a>
        </span>
      </div>
    </footer>
  </div>
</template>
