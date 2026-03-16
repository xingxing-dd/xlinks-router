<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Key, Mail, Lock, Gift, Shield } from 'lucide-vue-next'

const router = useRouter()
const formData = reactive({
  email: '',
  password: '',
  verificationCode: '',
  inviteCode: '',
})

const handleSubmit = () => {
  // 模拟注册
  console.log('Registering with:', formData)
  router.push('/login')
}

const handleSendCode = () => {
  // 模拟发送验证码
  alert('验证码已发送到您的邮箱')
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
          创建账号
        </h1>
        <p class="text-center text-slate-500 mb-8">
          注册 Xlinks Ai 账号
        </p>

        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              邮箱地址
            </label>
            <div class="relative">
              <Mail class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="formData.email"
                type="email"
                class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                placeholder="your@email.com"
                required
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              验证码
            </label>
            <div class="relative flex gap-2">
              <div class="relative flex-1">
                <Shield class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input
                  v-model="formData.verificationCode"
                  type="text"
                  class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                  placeholder="请输入验证码"
                  required
                />
              </div>
              <button
                type="button"
                @click="handleSendCode"
                class="px-4 py-3 bg-violet-100 text-violet-600 rounded-xl hover:bg-violet-200 transition-colors font-medium whitespace-nowrap"
              >
                发送验证码
              </button>
            </div>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              密码
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
              邀请码（选填）
            </label>
            <div class="relative">
              <Gift class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
              <input
                v-model="formData.inviteCode"
                type="text"
                class="w-full pl-11 pr-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none transition-all bg-white text-slate-900"
                placeholder="请输入邀请码（可选）"
              />
            </div>
            <p class="text-xs text-slate-500 mt-1.5">
              填写邀请码可获得额外奖励
            </p>
          </div>

          <div class="flex items-start">
            <input
              type="checkbox"
              class="w-4 h-4 mt-1 text-violet-600 border-slate-300 rounded focus:ring-violet-500 cursor-pointer"
              required
            />
            <label class="ml-2 text-sm text-slate-600">
              我已阅读并同意
              <a href="#" class="text-violet-600 hover:text-violet-700 font-medium transition-colors">
                服务条款
              </a>
              和
              <a href="#" class="text-violet-600 hover:text-violet-700 font-medium transition-colors">
                隐私政策
              </a>
            </label>
          </div>

          <button
            type="submit"
            class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium active:scale-[0.98]"
          >
            注册
          </button>
        </form>

        <div class="mt-6 text-center">
          <span class="text-slate-600">已有账号？</span>
          <router-link
            to="/login"
            class="ml-1 text-violet-600 hover:text-violet-700 font-medium transition-colors"
          >
            立即登录
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>
