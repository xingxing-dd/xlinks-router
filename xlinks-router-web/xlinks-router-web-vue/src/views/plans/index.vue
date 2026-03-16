<script setup>
import { ref, computed } from 'vue'
import { Check, Box, CreditCard, DollarSign, X } from 'lucide-vue-next'

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

const claudeModels = [
  { name: 'claude-3-7-sonnet', inputPrice: '$3.00/M', outputPrice: '$15.00/M', status: '可用' },
  { name: 'claude-3-7-sonnet-20250219', inputPrice: '$3.00/M', outputPrice: '$15.00/M', status: '可用' },
  { name: 'claude-3-7-sonnet-20250219-thinking', inputPrice: '$3.00/M', outputPrice: '$15.00/M', status: '可用' },
  { name: 'claude-haiku-4-5', inputPrice: '$1.00/M', outputPrice: '$5.00/M', status: '可用' },
  { name: 'claude-haiku-4-5-20251001', inputPrice: '$1.00/M', outputPrice: '$5.00/M', status: '可用' },
  { name: 'claude-opus-4', inputPrice: '$15.00/M', outputPrice: '$75.00/M', status: '可用' },
  { name: 'claude-opus-4-1', inputPrice: '$15.00/M', outputPrice: '$75.00/M', status: '可用' },
  { name: 'claude-opus-4-1-20250805', inputPrice: '$15.00/M', outputPrice: '$75.00/M', status: '可用' },
]

const rechargeOptions = [
  { usd: 100, cny: 20 },
  { usd: 200, cny: 40 },
  { usd: 500, cny: 100 },
  { usd: 1000, cny: 200 },
  { usd: 2000, cny: 400 },
  { usd: 5000, cny: 1000 },
]

const paymentMethods = [
  { id: 'alipay', name: '支付宝', icon: '💳' },
  { id: 'wechat', name: '微信支付', icon: '💚' },
]

const selectedPlanId = ref(null)
const selectedRechargeAmount = ref(null)
const selectedPayment = ref('alipay')
const isCheckoutModalOpen = ref(false)
const checkoutType = ref('plan')

const handlePurchasePlan = (planId) => {
  selectedPlanId.value = planId
  checkoutType.value = 'plan'
  isCheckoutModalOpen.value = true
}

const handleRecharge = (cnyAmount) => {
  selectedRechargeAmount.value = cnyAmount
  checkoutType.value = 'recharge'
  isCheckoutModalOpen.value = true
}

const handleConfirmPurchase = () => {
  alert('正在跳转到支付页面...')
  isCheckoutModalOpen.value = false
}

const selectedPlanData = computed(() => codexPlans.find(p => p.id === selectedPlanId.value))
const selectedRechargeData = computed(() => rechargeOptions.find(r => r.cny === selectedRechargeAmount.value))
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto space-y-16">
    <!-- 包月套餐 -->
    <div>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div
          v-for="(plan, index) in codexPlans"
          :key="plan.id"
          class="relative bg-white rounded-3xl border-2 transition-all duration-300"
          :class="[
            index === 1
              ? 'border-violet-500 shadow-2xl md:scale-105 shadow-violet-500/50'
              : 'border-slate-200 shadow-sm hover:shadow-lg'
          ]"
        >
          <div v-if="index === 1" class="absolute top-0 left-0 right-0 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white text-center py-2 text-sm font-semibold rounded-t-[22px]">
            🌟 推荐套餐
          </div>

          <div 
            class="bg-gradient-to-br from-slate-900 to-slate-800 p-8 rounded-t-3xl"
            :class="{ 'pt-14': index === 1 }"
          >
            <div class="flex items-center justify-between mb-4">
              <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg">
                <Box class="w-6 h-6 text-white" />
              </div>
              <div class="text-right">
                <div class="text-white text-opacity-80 text-sm">每日额度</div>
                <div class="text-white text-2xl font-bold tabular-nums">${{ plan.dailyLimit }}</div>
              </div>
            </div>
            <h3 class="text-xl font-bold text-white mb-2">{{ plan.name }}</h3>
            <div class="flex items-baseline text-white mb-1">
              <span class="text-4xl font-bold tabular-nums">¥{{ plan.price }}</span>
              <span class="ml-2 text-white text-opacity-80">/30天</span>
            </div>
            <p class="text-white text-opacity-80 text-sm">月度额度 ${{ plan.monthlyQuota }}</p>
          </div>

          <div class="p-6">
            <ul class="space-y-3 mb-8">
              <li v-for="(feature, idx) in plan.features" :key="idx" class="flex items-start gap-3">
                <div class="w-5 h-5 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                  <Check class="w-3 h-3 text-green-600" />
                </div>
                <span class="text-sm text-slate-700 font-medium">{{ feature }}</span>
              </li>
            </ul>

            <button
              @click="handlePurchasePlan(plan.id)"
              class="w-full py-3.5 rounded-xl font-bold transition-all duration-200 active:scale-[0.98]"
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
    </div>

    <!-- 充值优惠 -->
    <div>
      <div class="mb-8">
        <h1 class="text-3xl font-bold text-slate-900 mb-2 tracking-tight">充值优惠</h1>
        <p class="text-slate-500">按需充值，支持所有 AI 模型</p>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- 充值选项 -->
        <div class="bg-gradient-to-br from-violet-600 via-purple-600 to-fuchsia-600 rounded-3xl p-8 text-white shadow-2xl">
          <div class="flex items-center gap-4 mb-8">
            <div class="w-14 h-14 bg-white/30 rounded-2xl flex items-center justify-center backdrop-blur-sm shadow-lg border border-white/20">
              <DollarSign class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-2xl font-bold text-white">余额充值</h2>
              <p class="text-sm text-white/90 font-medium">1美元 = 0.2人民币</p>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <button
              v-for="option in rechargeOptions"
              :key="option.usd"
              @click="handleRecharge(option.cny)"
              class="bg-white/10 backdrop-blur-md border-2 border-white/20 rounded-2xl p-4 hover:bg-white/20 hover:border-white/40 transition-all shadow-lg active:scale-[0.98] text-left"
            >
              <div class="text-white/80 text-sm mb-1 font-bold">${{ option.usd }}</div>
              <div class="text-white text-2xl font-extrabold tabular-nums">¥{{ option.cny }}</div>
            </button>
          </div>

          <div class="mt-8 space-y-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-2xl p-5">
            <div class="flex items-center gap-3">
              <Check class="w-5 h-5 text-white" />
              <span class="text-sm text-white font-bold tracking-wide">余额永不过期</span>
            </div>
            <div class="flex items-center gap-3">
              <Check class="w-5 h-5 text-white" />
              <span class="text-sm text-white font-bold tracking-wide">按实际使用量扣费</span>
            </div>
          </div>
        </div>

        <!-- 模型列表预览 -->
        <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div class="bg-gradient-to-r from-slate-900 to-slate-800 p-6 text-white">
            <h3 class="text-xl font-bold mb-1 tracking-tight">支持的模型</h3>
            <p class="text-sm text-white/70 font-medium">充值后可使用以下所有 AI 模型</p>
          </div>
          <div class="p-6 flex-1 space-y-1">
            <div v-for="(model, idx) in claudeModels" :key="idx" class="flex items-center justify-between py-3 border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors px-2 rounded-lg group">
              <div class="flex-1">
                <code class="text-xs text-slate-900 font-bold font-mono bg-slate-100 px-2 py-1 rounded group-hover:text-primary transition-colors">
                  {{ model.name }}
                </code>
              </div>
              <div class="flex gap-2">
                <span class="text-[10px] font-bold bg-blue-50 text-blue-600 px-2 py-0.5 rounded-full uppercase tracking-tighter">输入 {{ model.inputPrice }}</span>
                <span class="text-[10px] font-bold bg-green-50 text-green-600 px-2 py-0.5 rounded-full uppercase tracking-tighter">输出 {{ model.outputPrice }}</span>
              </div>
            </div>
            <div class="mt-4 pt-4 text-center">
              <router-link to="/models" class="text-sm font-bold text-primary hover:text-primary/80 transition-colors flex items-center justify-center gap-1">
                查看更多模型规格
              </router-link>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 结账模态框 -->
    <div v-if="isCheckoutModalOpen" class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-300">
      <div class="bg-white rounded-[32px] max-w-md w-full p-8 shadow-2xl border border-white/20 animate-in slide-in-from-top-4 duration-300">
        <div class="flex items-center justify-between mb-8">
          <div class="flex items-center gap-4">
            <div class="w-14 h-14 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg shadow-primary/20">
              <CreditCard class="w-7 h-7 text-white" />
            </div>
            <div>
              <h2 class="text-xl font-bold text-slate-900 tracking-tight">确认支付</h2>
              <p class="text-sm text-slate-500 font-medium">
                {{ checkoutType === 'plan' ? selectedPlanData?.name : '余额充值' }}
              </p>
            </div>
          </div>
          <button @click="isCheckoutModalOpen = false" class="p-2 hover:bg-slate-100 rounded-full transition-colors">
            <X class="w-6 h-6 text-slate-400" />
          </button>
        </div>

        <div class="bg-slate-50/80 rounded-2xl p-6 mb-8 border border-slate-100">
          <template v-if="checkoutType === 'plan' && selectedPlanData">
            <div class="flex items-center justify-between mb-3">
              <span class="text-slate-500 font-medium">套餐类型</span>
              <span class="font-bold text-slate-900">{{ selectedPlanData.name }}</span>
            </div>
            <div class="flex items-center justify-between mb-3">
              <span class="text-slate-500 font-medium">月度额度</span>
              <span class="font-bold text-slate-900">${{ selectedPlanData.monthlyQuota }}</span>
            </div>
            <div class="flex items-center justify-between pt-4 border-t border-slate-200">
              <span class="text-slate-900 font-extrabold text-lg">实付金额</span>
              <span class="text-3xl font-black text-slate-900 tabular-nums">¥{{ selectedPlanData.price }}</span>
            </div>
          </template>
          <template v-if="checkoutType === 'recharge' && selectedRechargeData">
            <div class="flex items-center justify-between mb-3">
              <span class="text-slate-500 font-medium">充值额度</span>
              <span class="font-bold text-slate-900">${{ selectedRechargeData.usd }}</span>
            </div>
            <div class="flex items-center justify-between pt-4 border-t border-slate-200">
              <span class="text-slate-900 font-extrabold text-lg">支付金额</span>
              <span class="text-3xl font-black text-slate-900 tabular-nums">¥{{ selectedRechargeData.cny }}</span>
            </div>
          </template>
        </div>

        <div class="mb-8">
          <h3 class="text-sm font-bold text-slate-900 mb-4 uppercase tracking-widest">支付方式</h3>
          <div class="space-y-3">
            <label
              v-for="method in paymentMethods"
              :key="method.id"
              class="flex items-center gap-4 p-4 border-2 rounded-2xl cursor-pointer transition-all group"
              :class="[
                selectedPayment === method.id
                  ? 'border-violet-500 bg-violet-50 shadow-md shadow-violet-500/10'
                  : 'border-slate-100 hover:border-slate-200 bg-white'
              ]"
            >
              <input
                type="radio"
                name="payment"
                :value="method.id"
                v-model="selectedPayment"
                class="w-5 h-5 text-violet-600 focus:ring-violet-500 border-slate-300"
              />
              <span class="text-3xl group-hover:scale-110 transition-transform">{{ method.icon }}</span>
              <span class="font-bold text-slate-900">{{ method.name }}</span>
            </label>
          </div>
        </div>

        <div class="flex gap-4">
          <button
            @click="isCheckoutModalOpen = false"
            class="flex-1 h-14 border border-slate-200 text-slate-600 font-bold rounded-2xl hover:bg-slate-50 transition-all"
          >
            取消
          </button>
          <button
            @click="handleConfirmPurchase"
            class="flex-1 h-14 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white font-bold rounded-2xl hover:shadow-xl hover:shadow-violet-500/40 transition-all active:scale-[0.98]"
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
  animation-duration: 0.3s;
  animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
  animation-fill-mode: forwards;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slide-in-from-top-4 {
  from { transform: translateY(-1rem); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.fade-in { animation-name: fade-in; }
.slide-in-from-top-4 { animation-name: slide-in-from-top-4; }
</style>
