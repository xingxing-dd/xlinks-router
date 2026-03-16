<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Key, Mail, Lock } from 'lucide-vue-next'

const router = useRouter()
const email = ref('')
const password = ref('')

const handleSubmit = () => {
  // 模拟登录
  console.log('Logging in with:', email.value, password.value)
  router.push('/')
}
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
          欢迎回来
        </h1>
        <p class="text-center text-slate-500 mb-8">
          登录到 Xlinks Ai
        </p>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              邮箱地址
            </label>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="email"
                type="email"
                class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                placeholder="your@email.com"
                required
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              密码
            </label>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="password"
                type="password"
                class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                placeholder="••••••••"
                required
              />
            </div>
          </div>

          <div class="flex items-center justify-between">
            <label class="flex items-center cursor-pointer group">
              <input
                type="checkbox"
                class="w-4 h-4 text-violet-600 border-slate-300 rounded focus:ring-violet-500 cursor-pointer"
              />
              <span class="ml-2 text-sm text-slate-600 group-hover:text-slate-900 transition-colors">记住我</span>
            </label>
            <a href="#" class="text-sm text-violet-600 hover:text-violet-700 font-medium transition-colors">
              忘记密码？
            </a>
          </div>

          <button
            type="submit"
            class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium active:scale-[0.98]"
          >
            登录
          </button>
        </form>

        <div class="mt-6 text-center">
          <span class="text-slate-600">还没有账号？</span>
          <router-link
            to="/register"
            class="ml-1 text-violet-600 hover:text-violet-700 font-medium transition-colors"
          >
            立即注册
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Ensure standard Tailwind classes work as expected */
</style>
