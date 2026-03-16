<script setup>
import { reactive } from 'vue'
import {
  Mail,
  MessageCircle,
  Send,
  Phone,
  MapPin,
  Clock,
  Github,
  Twitter,
  Linkedin,
} from 'lucide-vue-next'

const formData = reactive({
  name: '',
  email: '',
  subject: '',
  message: '',
})

const handleSubmit = () => {
  // 模拟提交
  alert('感谢您的反馈！我们会尽快回复您。')
  formData.name = ''
  formData.email = ''
  formData.subject = ''
  formData.message = ''
}
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-12">
      <!-- 联系方式卡片 -->
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <Mail class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          邮箱支持
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          我们会在 24 小时内回复您的邮件
        </p>
        <a
          href="mailto:support@token-hub.com"
          class="text-violet-600 hover:text-violet-700 font-medium"
        >
          support@token-hub.com
        </a>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <MessageCircle class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          在线客服
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          工作日 9:00 - 18:00 在线服务
        </p>
        <button class="text-green-600 hover:text-green-700 font-medium">
          启动在线对话
        </button>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center mb-4 shadow-lg">
          <Phone class="w-6 h-6 text-white" />
        </div>
        <h3 class="font-semibold text-slate-900 mb-2">
          电话支持
        </h3>
        <p class="text-sm text-slate-600 mb-3">
          企业版用户专享电话支持
        </p>
        <a
          href="tel:+8640012345678"
          class="text-violet-600 hover:text-violet-700 font-medium"
        >
          400-123-4567
        </a>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      <!-- 联系表单 -->
      <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-xl font-bold text-slate-900 mb-6">
          发送消息
        </h2>
        <form @submit.prevent="handleSubmit" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              姓名 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.name"
              type="text"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none"
              placeholder="请输入您的姓名"
              required
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              邮箱 <span class="text-red-500">*</span>
            </label>
            <input
              v-model="formData.email"
              type="email"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none"
              placeholder="your@email.com"
              required
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              主题 <span class="text-red-500">*</span>
            </label>
            <select
              v-model="formData.subject"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none bg-white"
              required
            >
              <option value="">请选择主题</option>
              <option value="technical">技术支持</option>
              <option value="billing">账单问题</option>
              <option value="feature">功能建议</option>
              <option value="bug">Bug 反馈</option>
              <option value="other">其他问题</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-medium text-slate-700 mb-2">
              消息内容 <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="formData.message"
              rows="6"
              class="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-violet-500 focus:border-transparent outline-none resize-none"
              placeholder="请详细描述您的问题或建议..."
              required
            />
          </div>

          <button
            type="submit"
            class="w-full bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white py-3 rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium flex items-center justify-center gap-2"
          >
            <Send class="w-5 h-5" />
            <span>发送消息</span>
          </button>
        </form>
      </div>

      <!-- 其他信息 -->
      <div class="space-y-6">
        <!-- 公司信息 -->
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <h2 class="text-xl font-bold text-slate-900 mb-6">
            公司信息
          </h2>
          <div class="space-y-4">
            <div class="flex items-start gap-3">
              <MapPin class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  地址
                </p>
                <p class="text-slate-600">
                  中国上海市浦东新区
                  <br />
                  张江高科技园区
                  <br />
                  科技路 123 号，200120
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <Clock class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  工作时间
                </p>
                <p class="text-slate-600">
                  周一至周五：9:00 - 18:00
                  <br />
                  周六至周日：休息
                </p>
              </div>
            </div>

            <div class="flex items-start gap-3">
              <Mail class="w-5 h-5 text-slate-400 mt-1 flex-shrink-0" />
              <div>
                <p class="font-medium text-slate-900 mb-1">
                  商务合作
                </p>
                <p class="text-slate-600">
                  business@token-hub.com
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- 社交媒体 -->
        <div class="bg-gradient-to-br from-violet-600 via-purple-600 to-fuchsia-600 rounded-2xl p-8 text-white shadow-2xl">
          <h2 class="text-xl font-bold mb-4 text-white">
            关注我们
          </h2>
          <p class="text-white/95 mb-6 font-medium">
            在社交媒体上关注我们，获取最新动态和技术资讯
          </p>
          <div class="flex gap-4">
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Github class="w-6 h-6 text-white" />
            </a>
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Twitter class="w-6 h-6 text-white" />
            </a>
            <a
              href="#"
              class="w-12 h-12 bg-white/30 rounded-xl flex items-center justify-center hover:bg-white/40 transition-all backdrop-blur-sm shadow-lg border-2 border-white/30"
            >
              <Linkedin class="w-6 h-6 text-white" />
            </a>
          </div>
        </div>

        <!-- FAQ 快速链接 -->
        <div class="bg-white rounded-2xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
          <h2 class="text-xl font-bold text-slate-900 mb-4">
            常见问题
          </h2>
          <div class="space-y-3">
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              如何获取 API Key？
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              套餐可以随时更改吗？
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              如何查看使用统计？
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              支持哪些支付方式？
            </a>
            <a
              href="#"
              class="block text-violet-600 hover:text-violet-700 hover:underline font-medium"
            >
              推广奖励如何结算？
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
