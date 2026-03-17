<script setup>
import { ref, computed } from 'vue'
import { Check, Box, CreditCard, Calendar, Clock, ChevronDown } from 'lucide-vue-next'

const codexPlans = [
  {
    id: 'small',
    name: 'Codex小包套餐',
    price: 45,
    dailyLimit: 30,
    monthlyQuota: 900,
    concurrency: 8,
    features: [
      '有效期 30 天',
      '仅可用 Codex',
      '月度可用 $900 额度',
      '每日可用 $30 + 昨日未用完额度',
      '单套餐并发量为 8',
      '套餐多买只叠加额度，不叠加时间',
    ],
  },
  {
    id: 'medium',
    name: 'Codex中包套餐',
    price: 60,
    dailyLimit: 60,
    monthlyQuota: 1800,
    concurrency: 12,
    features: [
      '有效期 30 天',
      '仅可用 Codex',
      '月度可用 $1800 额度',
      '每日可用 $60 + 昨日未用完额度',
      '单套餐并发量为 12',
      '套餐多买只叠加额度，不叠加时间',
    ],
  },
  {
    id: 'large',
    name: 'Codex大包套餐',
    price: 75,
    dailyLimit: 90,
    monthlyQuota: 2700,
    concurrency: 16,
    features: [
      '有效期 30 天',
      '仅可用 Codex',
      '月度可用 $2700 额度',
      '每日可用 $90 + 昨日未用完额度',
      '单套餐并发量为 16',
      '套餐多买只叠加额度，不叠加时间',
    ],
  },
]

const paymentMethods = [
  { id: 'alipay', name: '支付宝', icon: '💳' },
  { id: 'wechat', name: '微信支付', icon: '💚' },
]

// 模拟多个订阅数据
const activeSubscriptions = [
  {
    id: "sub-1",
    planName: "Codex中包套餐",
    planType: "medium",
    daysRemaining: 24,
    purchaseDate: "2026-03-11 16:15",
    expiryDate: "2026-04-10 16:15",
    dailyReset: true,
    remainingQuota: 58.049198,
    totalQuota: 60,
    usedPercentage: 3,
  },
  {
    id: "sub-2",
    planName: "Codex大包套餐",
    planType: "large",
    daysRemaining: 15,
    purchaseDate: "2026-02-28 10:30",
    expiryDate: "2026-03-30 10:30",
    dailyReset: false,
    remainingQuota: 72.5,
    totalQuota: 90,
    usedPercentage: 19,
  },
  {
    id: "sub-3",
    planName: "Codex小包套餐",
    planType: "small",
    daysRemaining: 8,
    purchaseDate: "2026-03-05 14:20",
    expiryDate: "2026-03-25 14:20",
    dailyReset: true,
    remainingQuota: 28.3,
    totalQuota: 30,
    usedPercentage: 5,
  },
]

const currentSubscriptionIndex = ref(0)
const isDropdownOpen = ref(false)

const currentSubscription = computed(() => activeSubscriptions[currentSubscriptionIndex.value])

const selectedPlan = ref(null)
const selectedPayment = ref('alipay')
const isCheckoutModalOpen = ref(false)

const handlePurchasePlan = (planId) => {
  selectedPlan.value = planId
  isCheckoutModalOpen.value = true
}

const handleConfirmPurchase = () => {
  alert('正在跳转到支付页面...')
  isCheckoutModalOpen.value = false
}

const selectedPlanData = computed(() => codexPlans.find((p) => p.id === selectedPlan.value))
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <!-- 我的订阅卡片 -->
    <div class="mb-8 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-3xl p-6 md:p-8 text-white shadow-xl relative z-10">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1">我的订阅</h2>
        </div>
        
        <!-- 右上角下拉选择器 -->
        <div v-if="activeSubscriptions.length > 1" class="relative">
          <button
            @click="isDropdownOpen = !isDropdownOpen"
            class="flex items-center gap-2 bg-white/20 backdrop-blur-md border border-white/30 text-white font-medium px-4 py-2.5 rounded-xl hover:bg-white/30 transition-all focus:outline-none focus:ring-2 focus:ring-white/50"
          >
            <span class="text-sm">
              {{ currentSubscription.planName.replace('Codex', '') }} · {{ currentSubscription.daysRemaining }}天
            </span>
            <ChevronDown class="w-5 h-5 transition-transform duration-200" :class="{ 'rotate-180': isDropdownOpen }" />
          </button>

          <!-- 自定义下拉菜单 -->
          <template v-if="isDropdownOpen">
            <!-- 点击外部关闭下拉菜单 -->
            <div 
              class="fixed inset-0 z-10"
              @click="isDropdownOpen = false"
            />
            
            <div class="absolute right-0 top-full mt-2 w-72 bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden z-20 animate-in fade-in slide-in-from-top-2 duration-200">
              <div class="p-2">
                <button
                  v-for="(sub, index) in activeSubscriptions"
                  :key="sub.id"
                  @click="() => { currentSubscriptionIndex = index; isDropdownOpen = false }"
                  class="w-full text-left px-4 py-3 rounded-xl transition-all duration-150 border-2"
                  :class="[
                    index === currentSubscriptionIndex
                      ? 'bg-gradient-to-r from-indigo-50 to-purple-50 border-indigo-200'
                      : 'hover:bg-slate-50 border-transparent'
                  ]"
                >
                  <div class="flex items-center justify-between mb-1">
                    <span class="font-semibold" :class="[index === currentSubscriptionIndex ? 'text-indigo-700' : 'text-slate-900']">
                      {{ sub.planName }}
                    </span>
                    <div v-if="index === currentSubscriptionIndex" class="w-5 h-5 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-full flex items-center justify-center">
                      <Check class="w-3 h-3 text-white" />
                    </div>
                  </div>
                  <div class="flex items-center gap-3 text-xs">
                    <span class="flex items-center gap-1" :class="[index === currentSubscriptionIndex ? 'text-indigo-600' : 'text-slate-500']">
                      <Clock class="w-3 h-3" />
                      剩余 {{ sub.daysRemaining }} 天
                    </span>
                    <span :class="[index === currentSubscriptionIndex ? 'text-indigo-600' : 'text-slate-500']">
                      •
                    </span>
                    <span :class="[index === currentSubscriptionIndex ? 'text-indigo-600' : 'text-slate-500']">
                      ${{ sub.remainingQuota.toFixed(2) }} / ${{ sub.totalQuota }}
                    </span>
                  </div>
                </button>
              </div>
            </div>
          </template>
        </div>
        <div v-else class="w-16 h-16 bg-white/25 rounded-2xl flex items-center justify-center backdrop-blur-sm shadow-lg">
          <Box class="w-8 h-8 text-white" />
        </div>
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Clock class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">剩余天数</p>
          </div>
          <p class="text-2xl font-bold">{{ currentSubscription.daysRemaining }} 天</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Calendar class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">获取时间</p>
          </div>
          <p class="text-sm font-semibold">{{ currentSubscription.purchaseDate }}</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Calendar class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">到期时间</p>
          </div>
          <p class="text-sm font-semibold">{{ currentSubscription.expiryDate }}</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Check class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">今日重置</p>
          </div>
          <p class="text-lg font-bold">{{ currentSubscription.dailyReset ? '已重置' : '未重置' }}</p>
        </div>
      </div>

      <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
        <div class="flex items-center justify-between mb-3">
          <div>
            <p class="text-sm text-white/80 mb-1">可用端点</p>
            <p class="text-sm text-white/80">剩余额度</p>
          </div>
          <div class="text-right">
            <p class="text-xl font-bold">${{ currentSubscription.remainingQuota.toFixed(6) }} / ${{ currentSubscription.totalQuota }}</p>
          </div>
        </div>
        
        <div class="w-full bg-white/30 rounded-full h-3 mb-2">
          <div 
            class="bg-white rounded-full h-3 transition-all duration-300"
            :style="{ width: `${100 - currentSubscription.usedPercentage}%` }"
          />
        </div>
        
        <div class="flex items-center justify-between text-sm">
          <span class="text-white/80">已用额度</span>
          <span class="font-semibold">{{ currentSubscription.usedPercentage }}%</span>
        </div>
      </div>
    </div>

    <!-- 套餐列表 -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 relative z-0">
      <div
        v-for="(plan, index) in codexPlans"
        :key="plan.id"
        class="relative bg-white rounded-3xl border-2 overflow-hidden transition-all duration-200"
        :class="[
          index === 1
            ? 'border-violet-500 shadow-2xl md:scale-105 shadow-violet-500/50'
            : 'border-slate-200 shadow-sm hover:shadow-lg'
        ]"
      >
        <div v-if="index === 1" class="absolute top-0 left-0 right-0 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white text-center py-2 text-sm font-semibold">
          🌟 推荐套餐
        </div>

        <div 
          class="bg-gradient-to-br from-slate-900 to-slate-800 p-8"
          :class="{ 'pt-14': index === 1 }"
        >
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg">
              <Box class="w-6 h-6 text-white" />
            </div>
            <div class="text-right">
              <div class="text-white text-opacity-80 text-sm">每日额度</div>
              <div class="text-white text-2xl font-bold">${{ plan.dailyLimit }}</div>
            </div>
          </div>
          <h3 class="text-xl font-bold text-white mb-2">{{ plan.name }}</h3>
          <div class="flex items-baseline text-white mb-1">
            <span class="text-4xl font-bold">¥{{ plan.price }}</span>
            <span class="ml-2 text-white text-opacity-80">/30天</span>
          </div>
          <p class="text-white text-opacity-80 text-sm">月度额度 ${{ plan.monthlyQuota }}</p>
        </div>

        <div class="p-6">
          <ul class="space-y-3 mb-6">
            <li v-for="(feature, idx) in plan.features" :key="idx" class="flex items-start gap-2">
              <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                <Check class="w-3 h-3 text-green-600" />
              </div>
              <span class="text-sm text-slate-700">{{ feature }}</span>
            </li>
          </ul>

          <button
            @click="handlePurchasePlan(plan.id)"
            class="w-full py-3 rounded-xl font-semibold transition-all duration-200"
            :class="[
              index === 1
                ? 'bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white hover:shadow-lg hover:shadow-violet-500/50'
                : 'bg-slate-100 text-slate-900 hover:bg-slate-200'
            ]"
          >
            立即购买
          </button>
        </div>
      </div>
    </div>

    <!-- 结账模态框 -->
    <div v-if="isCheckoutModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg">
            <CreditCard class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">确认购买</h2>
            <p class="text-sm text-slate-500">{{ selectedPlanData?.name }}</p>
          </div>
        </div>

        <div class="bg-slate-50 rounded-2xl p-4 mb-6">
          <template v-if="selectedPlanData">
            <div class="flex items-center justify-between mb-2">
              <span class="text-slate-600">套餐</span>
              <span class="font-semibold text-slate-900">{{ selectedPlanData.name }}</span>
            </div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-slate-600">月度额度</span>
              <span class="font-semibold text-slate-900">${{ selectedPlanData.monthlyQuota }}</span>
            </div>
            <div class="flex items-center justify-between pt-2 border-t border-slate-200">
              <span class="text-slate-900 font-semibold">总计</span>
              <span class="text-2xl font-bold text-slate-900">¥{{ selectedPlanData.price }}</span>
            </div>
          </template>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">支付方式</h3>
          <div class="space-y-2">
            <label
              v-for="method in paymentMethods"
              :key="method.id"
              class="flex items-center gap-3 p-3 border-2 rounded-xl cursor-pointer transition-colors"
              :class="[
                selectedPayment === method.id
                  ? 'border-violet-500 bg-violet-50'
                  : 'border-slate-200 hover:border-slate-300'
              ]"
            >
              <input
                type="radio"
                name="payment"
                :value="method.id"
                v-model="selectedPayment"
                class="w-4 h-4 text-violet-600"
              />
              <span class="text-2xl">{{ method.icon }}</span>
              <span class="font-medium text-slate-900">{{ method.name }}</span>
            </label>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            @click="isCheckoutModalOpen = false"
            class="flex-1 px-4 py-3 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-colors font-medium"
          >
            取消
          </button>
          <button
            @click="handleConfirmPurchase"
            class="flex-1 px-4 py-3 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium"
          >
            确认支付
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-in {
  animation-duration: 0.2s;
  animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  animation-fill-mode: forwards;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slide-in-from-top-2 {
  from { transform: translateY(-0.5rem); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.fade-in { animation-name: fade-in; }
.slide-in-from-top-2 { animation-name: slide-in-from-top-2; }
</style>
