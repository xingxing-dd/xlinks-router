<script setup>
import { ref } from 'vue'
import { Gift, Copy, Users, DollarSign, TrendingUp, Award } from 'lucide-vue-next'

const stats = ref({
  totalReferrals: 23,
  activeReferrals: 18,
  totalEarnings: 1580,
  pendingEarnings: 320,
})

const records = ref([
  {
    id: '1',
    userName: '张三',
    email: 'zhang***@example.com',
    joinDate: '2026-03-01',
    status: 'active',
    earnings: 150,
  },
  {
    id: '2',
    userName: '李四',
    email: 'li***@example.com',
    joinDate: '2026-03-05',
    status: 'active',
    earnings: 200,
  },
  {
    id: '3',
    userName: '王五',
    email: 'wang***@example.com',
    joinDate: '2026-03-08',
    status: 'pending',
    earnings: 50,
  },
])

const referralCode = 'INVITE2026ABC'
const referralLink = `https://token-hub.com/register?ref=${referralCode}`

const copyToClipboard = (text) => {
  const textArea = document.createElement('textarea')
  textArea.value = text
  textArea.style.position = 'fixed'
  textArea.style.left = '-999999px'
  textArea.style.top = '-999999px'
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()
  
  try {
    document.execCommand('copy')
    alert('邀请链接已复制到剪贴板')
  } catch (err) {
    console.error('复制失败:', err)
    alert('复制失败，请手动复制')
  } finally {
    document.body.removeChild(textArea)
  }
}

const getStatusColor = (status) => {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-700'
    case 'pending':
      return 'bg-yellow-100 text-yellow-700'
    case 'inactive':
      return 'bg-gray-100 text-gray-700'
    default:
      return 'bg-gray-100 text-gray-700'
  }
}

const getStatusText = (status) => {
  switch (status) {
    case 'active':
      return '已激活'
    case 'pending':
      return '待激活'
    case 'inactive':
      return '未激活'
    default:
      return '未知'
  }
}
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <div class="mb-8">
      <h1 class="text-3xl font-bold text-slate-900">推广有礼</h1>
      <p class="text-slate-500 mt-1">邀请好友注册，共享推广奖励</p>
    </div>

    <!-- 推广统计 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg">
            <Users class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">累计邀请</h3>
        <p class="text-2xl font-bold text-slate-900">{{ stats.totalReferrals }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center shadow-lg">
            <TrendingUp class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">活跃用户</h3>
        <p class="text-2xl font-bold text-slate-900">{{ stats.activeReferrals }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center shadow-lg">
            <DollarSign class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">累计收益</h3>
        <p class="text-2xl font-bold text-slate-900">¥{{ stats.totalEarnings }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center shadow-lg">
            <Award class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">待结算</h3>
        <p class="text-2xl font-bold text-slate-900">¥{{ stats.pendingEarnings }}</p>
      </div>
    </div>

    <!-- 推广链接和邀请码 -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
      <div class="bg-gradient-to-br from-violet-600 via-purple-600 to-fuchsia-600 rounded-3xl p-8 text-white shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-white/30 rounded-2xl flex items-center justify-center backdrop-blur-sm shadow-lg">
            <Gift class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-white">我的推广链接</h2>
            <p class="text-sm text-white/95">分享给好友快速注册</p>
          </div>
        </div>
        <div class="bg-white/20 rounded-2xl p-4 mb-4 backdrop-blur-md border-2 border-white/30 shadow-lg">
          <p class="text-sm text-white/95 mb-2 font-medium">推广链接</p>
          <div class="flex items-center gap-2">
            <code class="flex-1 text-sm bg-white/25 px-3 py-2 rounded-xl font-mono truncate backdrop-blur-sm text-white border border-white/20">
              {{ referralLink }}
            </code>
            <button
              @click="copyToClipboard(referralLink)"
              class="bg-white text-violet-600 px-4 py-2 rounded-xl hover:bg-white/90 transition-all shadow-lg flex items-center gap-2 flex-shrink-0 font-medium"
            >
              <Copy class="w-4 h-4" />
              <span>复制</span>
            </button>
          </div>
        </div>
        <div class="bg-white/20 rounded-2xl p-4 backdrop-blur-md border-2 border-white/30 shadow-lg">
          <p class="text-sm text-white/95 mb-2 font-medium">邀请码</p>
          <div class="flex items-center gap-2">
            <code class="flex-1 text-xl font-bold tracking-wider text-white">
              {{ referralCode }}
            </code>
            <button
              @click="copyToClipboard(referralCode)"
              class="bg-white text-violet-600 px-4 py-2 rounded-xl hover:bg-white/90 transition-all shadow-lg flex items-center gap-2 font-medium"
            >
              <Copy class="w-4 h-4" />
              <span>复制</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 奖励规则 -->
      <div class="bg-white rounded-3xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-xl font-bold text-slate-900 mb-6">奖励规则</h2>
        <div class="space-y-6">
          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">1</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">邀请注册奖励</h3>
              <p class="text-sm text-slate-600">
                好友通过您的链接注册，您将获得 <span class="font-semibold text-violet-600">¥10</span> 奖励
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">2</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">首次充值奖励</h3>
              <p class="text-sm text-slate-600">
                好友首次充值，您将获得充值金额 <span class="font-semibold text-green-600">10%</span> 的奖励
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">3</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">持续返佣</h3>
              <p class="text-sm text-slate-600">
                好友每次消费，您将获得消费金额 <span class="font-semibold text-violet-600">5%</span> 的返佣
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">4</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">结算周期</h3>
              <p class="text-sm text-slate-600">
                每月 1 日自动结算上月收益，直接转入账户余额
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 推广记录 -->
    <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-lg transition-shadow">
      <div class="p-6 border-b border-slate-200">
        <h2 class="text-lg font-semibold text-slate-900">推广记录</h2>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">用户</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">邮箱</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">注册时间</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">状态</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">累计奖励</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-200">
            <tr v-for="record in records" :key="record.id" class="hover:bg-slate-50 transition-colors">
              <td class="px-6 py-4">
                <span class="font-medium text-slate-900">{{ record.userName }}</span>
              </td>
              <td class="px-6 py-4 text-sm text-slate-600">{{ record.email }}</td>
              <td class="px-6 py-4 text-sm text-slate-600">{{ record.joinDate }}</td>
              <td class="px-6 py-4">
                <span
                  class="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-medium"
                  :class="getStatusColor(record.status)"
                >
                  {{ getStatusText(record.status) }}
                </span>
              </td>
              <td class="px-6 py-4">
                <span class="font-semibold text-green-600">¥{{ record.earnings }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="records.length === 0" class="text-center py-12">
        <Gift class="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p class="text-slate-500">暂无推广记录</p>
        <p class="text-sm text-slate-400 mt-1">快去分享您的推广链接吧！</p>
      </div>
    </div>
  </div>
</template>
