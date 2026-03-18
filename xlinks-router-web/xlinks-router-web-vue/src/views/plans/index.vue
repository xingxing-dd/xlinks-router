<script setup>
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Box, CreditCard, Calendar, Clock, ChevronDown, Ticket } from 'lucide-vue-next'
import { usePlans } from '@/composables/usePlans'
import { formatCurrency } from '@/utils/formatters'

const { t } = useI18n()

const {
  codexPlans,
  activeSubscriptions,
  currentSubscriptionIndex,
  isDropdownOpen,
  selectedPayment,
  isCheckoutModalOpen,
  isActivationCodeModalOpen,
  activationCode,
  loading,
  submitting,
  actionMessage,
  currentSubscription,
  selectedPlanData,
  loadPlans,
  handlePurchasePlan,
  createOrder,
  handleActivateCode,
} = usePlans()

const paymentMethods = [
  { id: 'alipay', name: t('plans.alipay'), icon: '💳' },
  { id: 'wechat', name: t('plans.wechat'), icon: '💚' },
]

const handleConfirmPurchase = () => {
  createOrder()
}

onMounted(loadPlans)
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">
    <div v-if="actionMessage" class="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700 break-all">
      {{ actionMessage }}
    </div>

    <!-- 我的订阅卡片 -->
    <div class="mb-8 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-3xl p-6 md:p-8 text-white shadow-xl relative z-10">
      <div class="flex items-center justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold mb-1">{{ t('plans.mySubscription') }}</h2>
        </div>
        
        <!-- 右上角按钮区域 -->
        <div class="flex items-center gap-3">
          <!-- 使用激活码按钮 -->
          <button
            @click="isActivationCodeModalOpen = true"
            class="flex items-center gap-2 bg-white/20 backdrop-blur-md border border-white/30 text-white font-medium px-4 py-2.5 rounded-xl hover:bg-white/30 transition-all focus:outline-none focus:ring-2 focus:ring-white/50"
          >
            <Ticket class="w-4 h-4" />
            <span class="text-sm hidden sm:inline">{{ t('plans.useActivationCode') }}</span>
          </button>
          
          <!-- 切换套餐下拉选择器 -->
          <div v-if="activeSubscriptions.length > 1" class="relative">
            <button
              @click="isDropdownOpen = !isDropdownOpen"
              class="flex items-center gap-2 bg-white/20 backdrop-blur-md border border-white/30 text-white font-medium px-4 py-2.5 rounded-xl hover:bg-white/30 transition-all focus:outline-none focus:ring-2 focus:ring-white/50"
            >
              <span class="text-sm">
                {{ currentSubscription.planName.replace('Codex', '') }} · {{ currentSubscription.daysRemaining }}{{ t('plans.dayUnit') }}
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
                        {{ t('plans.remainingDays') }} {{ sub.daysRemaining }} {{ t('plans.dayUnit') }}
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
      </div>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Clock class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">{{ t('plans.remainingDays') }}</p>
          </div>
          <p class="text-2xl font-bold">{{ currentSubscription.daysRemaining }} {{ t('plans.dayUnit') }}</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Calendar class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">{{ t('plans.purchaseDate') }}</p>
          </div>
          <p class="text-sm font-semibold">{{ currentSubscription.purchaseDate }}</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Calendar class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">{{ t('plans.expiryDate') }}</p>
          </div>
          <p class="text-sm font-semibold">{{ currentSubscription.expiryDate }}</p>
        </div>

        <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
          <div class="flex items-center gap-2 mb-2">
            <Check class="w-4 h-4 text-white/80" />
            <p class="text-sm text-white/80">{{ t('plans.dailyReset') }}</p>
          </div>
          <p class="text-lg font-bold">{{ currentSubscription.dailyReset ? t('plans.reset') : t('plans.notReset') }}</p>
        </div>
      </div>

      <div class="bg-white/15 backdrop-blur-md border border-white/20 rounded-2xl p-4">
        <div class="flex items-center justify-between mb-3">
          <div>
            <p class="text-sm text-white/80 mb-1">{{ t('plans.availableEndpoints') }}</p>
            <p class="text-sm text-white/80">{{ t('plans.remainingQuota') }}</p>
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
          <span class="text-white/80">{{ t('plans.usedQuota') }}</span>
          <span class="font-semibold">{{ currentSubscription.usedPercentage }}%</span>
        </div>
      </div>
    </div>

    <!-- 套餐列表 -->
    <div v-if="loading" class="rounded-2xl border border-slate-200 bg-white py-12 text-center text-slate-500">
      {{ t('common.loading') }}
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-3 gap-6 relative z-0">
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
          🌟 {{ t('plans.recommended') }}
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
              <div class="text-white text-opacity-80 text-sm">{{ t('plans.dailyQuota') }}</div>
              <div class="text-white text-2xl font-bold">${{ plan.dailyLimit }}</div>
            </div>
          </div>
          <h3 class="text-xl font-bold text-white mb-2">{{ plan.name }}</h3>
          <div class="flex items-baseline text-white mb-1">
            <span class="text-4xl font-bold">{{ formatCurrency(plan.price) }}</span>
            <span class="ml-2 text-white text-opacity-80">{{ t('plans.perMonth') }}</span>
          </div>
          <p class="text-white text-opacity-80 text-sm">{{ t('plans.monthlyQuota') }} ${{ plan.monthlyQuota }}</p>
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
            {{ t('plans.buyNow') }}
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
            <h2 class="text-xl font-bold text-slate-900">{{ t('plans.confirmPurchase') }}</h2>
            <p class="text-sm text-slate-500">{{ selectedPlanData?.name }}</p>
          </div>
        </div>

        <div class="bg-slate-50 rounded-2xl p-4 mb-6">
          <template v-if="selectedPlanData">
            <div class="flex items-center justify-between mb-2">
              <span class="text-slate-600">{{ t('common.name') || '套餐' }}</span>
              <span class="font-semibold text-slate-900">{{ selectedPlanData.name }}</span>
            </div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-slate-600">{{ t('plans.monthlyQuota') }}</span>
              <span class="font-semibold text-slate-900">${{ selectedPlanData.monthlyQuota }}</span>
            </div>
            <div class="flex items-center justify-between pt-2 border-t border-slate-200">
              <span class="text-slate-900 font-semibold">{{ t('plans.total') }}</span>
              <span class="text-2xl font-bold text-slate-900">{{ formatCurrency(selectedPlanData.price) }}</span>
            </div>
          </template>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">{{ t('plans.paymentMethod') }}</h3>
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
            {{ t('common.cancel') }}
          </button>
          <button
            @click="handleConfirmPurchase"
            :disabled="submitting"
            class="flex-1 px-4 py-3 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium"
          >
            {{ submitting ? t('plans.creatingOrder') : t('plans.confirmPay') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 激活码模态框 -->
    <div v-if="isActivationCodeModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-fuchsia-500 rounded-2xl flex items-center justify-center shadow-lg">
            <Ticket class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">
              {{ t('plans.useActivationCode') }}
            </h2>
            <p class="text-sm text-slate-500">
              {{ t('plans.activationCodeSubtitle') }}
            </p>
          </div>
        </div>

        <div class="mb-6">
          <input
            type="text"
            v-model="activationCode"
            class="w-full px-4 py-3 border border-slate-300 text-slate-700 rounded-xl focus:outline-none focus:border-violet-500 focus:ring-2 focus:ring-violet-500/50"
            :placeholder="t('plans.activationCodePlaceholder')"
          />
        </div>

        <div class="flex gap-3">
          <button
            @click="isActivationCodeModalOpen = false"
            class="flex-1 px-4 py-3 border border-slate-300 text-slate-700 rounded-xl hover:bg-slate-50 transition-colors font-medium"
          >
            {{ t('common.cancel') }}
          </button>
          <button
            @click="handleActivateCode"
            :disabled="submitting"
            class="flex-1 px-4 py-3 bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white rounded-xl hover:shadow-lg hover:shadow-violet-500/50 transition-all duration-200 font-medium disabled:opacity-50"
          >
            {{ submitting ? t('common.submitting') || '处理中...' : t('plans.confirmActivation') }}
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
