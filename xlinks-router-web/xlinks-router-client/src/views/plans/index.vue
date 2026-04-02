<script setup>
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Check, Box, CreditCard, Calendar, Clock, ChevronDown, Ticket, History, Eye } from 'lucide-vue-next'
import { usePlans } from '@/composables/usePlans'
import { formatCurrency } from '@/utils/formatters'

const { t } = useI18n()

const {
  codexPlans,
  activeSubscriptions,
  historicalSubscriptions,
  currentSubscriptionIndex,
  isDropdownOpen,
  selectedPayment,
  isCheckoutModalOpen,
  isActivationCodeModalOpen,
  isHistoryDetailModalOpen,
  selectedHistoricalSub,
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
  { id: 'alipay', name: t('plans.alipay'), icon: '💳', enabled: false, isDefault: false },
  { id: 'wechat', name: t('plans.wechat'), icon: '💚', enabled: false, isDefault: false },
  { id: 'third-party', name: t('plans.thirdParty'), icon: '🌐', enabled: true, isDefault: true },
]

const historyStatusMeta = {
  expired: { label: t('plans.status.expired'), badgeClass: 'bg-slate-100 text-slate-700' },
  success: { label: t('plans.status.success'), badgeClass: 'bg-emerald-100 text-emerald-700' },
  cancelled: { label: t('plans.status.cancelled'), badgeClass: 'bg-orange-100 text-orange-700' },
}

const getHistoryStatusLabel = (status) => {
  return historyStatusMeta?.[status]?.label || status || '-'
}

const getHistoryStatusBadgeClass = (status) => {
  return historyStatusMeta?.[status]?.badgeClass || 'bg-slate-100 text-slate-700'
}

onMounted(() => {
  const defaultMethod = paymentMethods.find(m => m.isDefault && m.enabled)
  if (defaultMethod) {
    selectedPayment.value = defaultMethod.id
  }
})

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
    <div class="mb-8 bg-gradient-hero rounded-3xl p-6 md:p-8 text-white shadow-xl relative z-10">
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
                        ? 'bg-primary/10 border-primary/20'
                        : 'hover:bg-slate-50 border-transparent'
                    ]"
                  >
                    <div class="flex items-center justify-between mb-1">
                      <span class="font-semibold" :class="[index === currentSubscriptionIndex ? 'text-primary' : 'text-slate-900']">
                        {{ sub.planName }}
                      </span>
                      <div v-if="index === currentSubscriptionIndex" class="w-5 h-5 bg-gradient-button rounded-full flex items-center justify-center">
                        <Check class="w-3 h-3 text-white" />
                      </div>
                    </div>
                    <div class="flex items-center gap-3 text-xs">
                      <span class="flex items-center gap-1" :class="[index === currentSubscriptionIndex ? 'text-primary' : 'text-slate-500']">
                        <Clock class="w-3 h-3" />
                        {{ t('plans.remainingDays') }} {{ sub.daysRemaining }} {{ t('plans.dayUnit') }}
                      </span>
                      <span :class="[index === currentSubscriptionIndex ? 'text-primary' : 'text-slate-500']">
                        •
                      </span>
                      <span :class="[index === currentSubscriptionIndex ? 'text-primary' : 'text-slate-500']">
                        ${{ sub.remainingQuota.toFixed(2) }} / ${{ sub.totalQuota }}
                      </span>
                    </div>
                  </button>
                </div>
              </div>
            </template>
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
            <p class="text-xl font-bold">${{ currentSubscription.remainingQuota.toFixed(2) }} / ${{ currentSubscription.totalQuota }}</p>
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

    <!-- 重要提示 -->
    <div class="bg-gradient-to-r from-amber-50 to-yellow-50 border-2 border-amber-300 rounded-2xl p-4 mb-6 shadow-sm">
      <div class="flex items-start gap-3">
        <div class="flex-shrink-0 w-6 h-6 bg-amber-400 rounded-full flex items-center justify-center mt-0.5">
          <span class="text-white text-sm font-bold">!</span>
        </div>
        <div class="flex-1">
          <p class="text-amber-900 font-medium text-sm md:text-base">
            {{ t('plans.importantNote') }}
          </p>
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
        class="relative bg-white rounded-3xl border-2 border-slate-200 shadow-sm hover:shadow-lg overflow-hidden transition-all duration-200"
      >
        <div class="bg-gradient-to-br from-slate-900 to-slate-800 p-8">
          <div class="flex items-center justify-between mb-4">
            <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg">
              <Box class="w-6 h-6 text-white" />
            </div>
            <div class="text-right">
              <div class="text-white text-opacity-80 text-sm">{{ t('plans.dailyQuota') }}</div>
              <div class="text-white text-2xl font-bold">${{ plan.dailyLimit }}</div>
            </div>
          </div>
          <h3 class="text-xl font-bold text-white mb-2">{{ plan.name }}</h3>
          <div class="flex items-baseline text-white mb-1">
            <span class="text-4xl font-bold">{{ formatCurrency(plan.price, 'CNY') }}</span>
            <span class="ml-2 text-white text-opacity-80">/{{ plan.durationDays }}{{ t('plans.dayUnit') }}</span>
          </div>
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
            class="w-full py-3 rounded-xl font-semibold transition-all duration-200 bg-gradient-button text-white hover:shadow-lg hover:shadow-primary/25"
          >
            {{ t('plans.buyNow') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 历史订阅区域 -->
    <div class="mt-12 bg-white rounded-3xl border-2 border-slate-200 shadow-sm overflow-hidden">
      <div class="bg-gradient-hero p-6">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 bg-white/25 rounded-xl flex items-center justify-center backdrop-blur-sm border border-white/30">
            <History class="w-5 h-5 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-white">{{ t('plans.historyTitle') }}</h2>
          </div>
        </div>
      </div>

      <div class="p-6">
        <!-- 桌面端表格视图 -->
        <div class="hidden md:block overflow-x-auto">
          <table class="w-full">
            <thead>
              <tr class="border-b-2 border-slate-200">
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.planName') }}</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.purchaseDate') }}</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.expiryDate') }}</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.totalQuota') }}</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.usage') }}</th>
                <th class="text-left py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.status') }}</th>
                <th class="text-right py-3 px-4 text-sm font-semibold text-slate-700">{{ t('plans.table.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="sub in historicalSubscriptions" :key="sub.id" class="border-b border-slate-100 hover:bg-slate-50 transition-colors">
                <td class="py-4 px-4">
                  <div class="flex items-center gap-2">
                    <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                      <Box class="w-4 h-4 text-white" />
                    </div>
                    <span class="font-medium text-slate-900">{{ sub.planName }}</span>
                  </div>
                </td>
                <td class="py-4 px-4 text-sm text-slate-600">{{ sub.purchaseDate }}</td>
                <td class="py-4 px-4 text-sm text-slate-600">{{ sub.expiryDate }}</td>
                <td class="py-4 px-4 text-sm font-semibold text-slate-900">${{ sub.totalQuota }}</td>
                <td class="py-4 px-4">
                  <div class="flex items-center gap-2">
                    <div class="flex-1 bg-slate-200 rounded-full h-2 max-w-[80px]">
                      <div 
                        class="bg-gradient-button rounded-full h-2 transition-all duration-300"
                        :style="{ width: `${sub.usedPercentage}%` }"
                      />
                    </div>
                    <span class="text-sm font-medium text-slate-700">{{ sub.usedPercentage }}%</span>
                  </div>
                </td>
                <td class="py-4 px-4">
                  <span 
                    class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                    :class="getHistoryStatusBadgeClass(sub.status)"
                  >
                    {{ getHistoryStatusLabel(sub.status) }}
                  </span>
                </td>
                <td class="py-4 px-4 text-right">
                  <button 
                    @click="() => { selectedHistoricalSub = sub; isHistoryDetailModalOpen = true }"
                    class="inline-flex items-center gap-1 text-sm text-primary hover:text-primary font-medium transition-colors"
                  >
                    <Eye class="w-4 h-4" />
                    {{ t('plans.viewDetail') }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 移动端卡片视图 -->
        <div class="md:hidden space-y-4">
          <div v-for="sub in historicalSubscriptions" :key="sub.id" class="bg-slate-50 rounded-2xl p-4 border border-slate-200">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-8 h-8 bg-gradient-icon rounded-lg flex items-center justify-center">
                  <Box class="w-4 h-4 text-white" />
                </div>
                <span class="font-semibold text-slate-900">{{ sub.planName }}</span>
              </div>
              <span 
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                :class="getHistoryStatusBadgeClass(sub.status)"
              >
                {{ getHistoryStatusLabel(sub.status) }}
              </span>
            </div>

            <div class="space-y-2 mb-3">
              <div class="flex items-center justify-between text-sm">
                <span class="text-slate-600">{{ t('plans.table.purchaseDate') }}</span>
                <span class="text-slate-900">{{ sub.purchaseDate }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-slate-600">{{ t('plans.table.expiryDate') }}</span>
                <span class="text-slate-900">{{ sub.expiryDate }}</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <span class="text-slate-600">{{ t('plans.table.totalQuota') }}</span>
                <span class="font-semibold text-slate-900">${{ sub.totalQuota }}</span>
              </div>
            </div>

            <div class="mb-3">
              <div class="flex items-center justify-between text-sm mb-1">
                <span class="text-slate-600">{{ t('plans.table.usage') }}</span>
                <span class="font-medium text-slate-700">{{ sub.usedPercentage }}%</span>
              </div>
              <div class="w-full bg-slate-200 rounded-full h-2">
                <div 
                  class="bg-gradient-button rounded-full h-2 transition-all duration-300"
                  :style="{ width: `${sub.usedPercentage}%` }"
                />
              </div>
            </div>

            <button 
              @click="() => { selectedHistoricalSub = sub; isHistoryDetailModalOpen = true }"
              class="w-full flex items-center justify-center gap-2 text-sm text-primary hover:text-primary font-medium transition-colors py-2"
            >
              <Eye class="w-4 h-4" />
              {{ t('plans.viewDetail') }}
            </button>
          </div>
        </div>

        <div v-if="historicalSubscriptions.length === 0" class="text-center py-12">
          <div class="w-16 h-16 bg-slate-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <History class="w-8 h-8 text-slate-400" />
          </div>
          <p class="text-slate-500">{{ t('common.noData') }}</p>
        </div>
      </div>
    </div>

    <!-- 结账模态框 -->
    <div v-if="isCheckoutModalOpen" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg">
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
              <span class="text-2xl font-bold text-slate-900">{{ formatCurrency(selectedPlanData.price, 'CNY') }}</span>
            </div>
          </template>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">{{ t('plans.paymentMethod') }}</h3>
          <div class="space-y-2">
            <label
              v-for="method in paymentMethods"
              :key="method.id"
              class="flex items-center gap-3 p-3 border-2 rounded-xl transition-colors"
              :class="[
                method.enabled 
                  ? (selectedPayment === method.id ? 'border-primary bg-primary/5 cursor-pointer' : 'border-slate-200 hover:border-slate-300 cursor-pointer')
                  : 'border-slate-100 bg-slate-50 opacity-60 cursor-not-allowed'
              ]"
            >
              <input
                type="radio"
                name="payment"
                :value="method.id"
                v-model="selectedPayment"
                :disabled="!method.enabled"
                class="w-4 h-4 text-primary disabled:opacity-50"
              />
              <span class="text-2xl">{{ method.icon }}</span>
              <span class="font-medium" :class="method.enabled ? 'text-slate-900' : 'text-slate-400'">{{ method.name }}</span>
              <span v-if="!method.enabled" class="ml-auto text-[10px] bg-slate-200 text-slate-500 px-1.5 py-0.5 rounded-full uppercase tracking-wider">
                {{ t('models.status.unavailable') }}
              </span>
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
            class="flex-1 px-4 py-3 bg-gradient-button text-white rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium"
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
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg">
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
            class="w-full px-4 py-3 border border-slate-300 text-slate-700 rounded-xl focus:outline-none focus:border-primary focus:ring-2 focus:ring-ring"
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
            class="flex-1 px-4 py-3 bg-gradient-button text-white rounded-xl hover:shadow-lg hover:shadow-primary/25 transition-all duration-200 font-medium disabled:opacity-50"
          >
            {{ submitting ? t('common.submitting') || '处理中...' : t('plans.confirmActivation') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 历史订阅详情模态框 -->
    <div v-if="isHistoryDetailModalOpen && selectedHistoricalSub" class="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-[100] p-4">
      <div class="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl">
        <div class="flex items-center gap-3 mb-6">
          <div class="w-12 h-12 bg-gradient-icon rounded-2xl flex items-center justify-center shadow-lg">
            <History class="w-6 h-6 text-white" />
          </div>
          <div>
            <h2 class="text-xl font-bold text-slate-900">{{ t('plans.historyDetailTitle') }}</h2>
            <p class="text-sm text-slate-500">{{ selectedHistoricalSub.planName }}</p>
          </div>
        </div>

        <div class="bg-slate-50 rounded-2xl p-4 mb-6">
          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600">{{ t('plans.table.planName') }}</span>
            <span class="font-semibold text-slate-900">{{ selectedHistoricalSub.planName }}</span>
          </div>
          <div class="flex items-center justify-between mb-2">
            <span class="text-slate-600">{{ t('plans.table.totalQuota') }}</span>
            <span class="font-semibold text-slate-900">${{ selectedHistoricalSub.totalQuota }}</span>
          </div>
          <div class="flex items-center justify-between pt-2 border-t border-slate-200">
            <span class="text-slate-900 font-semibold">{{ t('plans.total') }}</span>
            <span class="text-2xl font-bold text-slate-900">
              ${{ selectedHistoricalSub.usedQuota.toFixed(2) }} / ${{ selectedHistoricalSub.totalQuota }}
            </span>
          </div>
        </div>

        <div class="mb-6">
          <h3 class="text-sm font-semibold text-slate-900 mb-3">{{ t('plans.subscriptionInfo') }}</h3>
          <div class="space-y-2">
            <div class="flex items-center gap-3 p-3 border-2 rounded-xl border-slate-100">
              <div class="w-8 h-8 bg-slate-100 rounded-full flex items-center justify-center">
                <Calendar class="w-4 h-4 text-slate-500" />
              </div>
              <div>
                <p class="text-xs text-slate-500">{{ t('plans.table.purchaseDate') }}</p>
                <p class="text-sm font-semibold text-slate-900">{{ selectedHistoricalSub.purchaseDate }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3 p-3 border-2 rounded-xl border-slate-100">
              <div class="w-8 h-8 bg-slate-100 rounded-full flex items-center justify-center">
                <Calendar class="w-4 h-4 text-slate-500" />
              </div>
              <div>
                <p class="text-xs text-slate-500">{{ t('plans.table.expiryDate') }}</p>
                <p class="text-sm font-semibold text-slate-900">{{ selectedHistoricalSub.expiryDate }}</p>
              </div>
            </div>
            <div class="flex items-center gap-3 p-3 border-2 rounded-xl border-slate-100">
              <div class="w-8 h-8 bg-slate-100 rounded-full flex items-center justify-center">
                <Eye class="w-4 h-4 text-slate-500" />
              </div>
              <div>
                <p class="text-xs text-slate-500">{{ t('plans.table.status') }}</p>
                <p class="text-sm font-semibold text-slate-900">
                  {{ getHistoryStatusLabel(selectedHistoricalSub.status) }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="flex gap-3">
          <button
            @click="isHistoryDetailModalOpen = false"
            class="flex-1 px-4 py-3 bg-slate-100 text-slate-700 rounded-xl hover:bg-slate-200 transition-colors font-medium"
          >
            {{ t('plans.close') }}
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
