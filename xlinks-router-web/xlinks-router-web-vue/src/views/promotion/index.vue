<script setup>
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Gift, Copy, Users, DollarSign, TrendingUp, Award } from 'lucide-vue-next'
import { usePromotion } from '@/composables/usePromotion'
import { formatCurrency } from '@/utils/formatters'

const { t } = useI18n()

const {
  stats,
  records,
  referralCode,
  referralLink,
  rules,
  loading,
  copyToClipboard,
  getStatusColor,
  getStatusText,
  loadPromotionData,
} = usePromotion()

onMounted(loadPromotionData)
</script>

<template>
  <div class="p-4 md:p-8 max-w-7xl mx-auto">

    <!-- 推广统计 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center shadow-lg">
            <Users class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">{{ t('promotion.totalReferrals') }}</h3>
        <p class="text-2xl font-bold text-slate-900">{{ stats.totalReferrals }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center shadow-lg">
            <TrendingUp class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">{{ t('promotion.activeReferrals') }}</h3>
        <p class="text-2xl font-bold text-slate-900">{{ stats.activeReferrals }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center shadow-lg">
            <DollarSign class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">{{ t('promotion.totalEarnings') }}</h3>
        <p class="text-2xl font-bold text-slate-900">{{ formatCurrency(stats.totalEarnings) }}</p>
      </div>

      <div class="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <div class="flex items-center justify-between mb-4">
          <div class="w-12 h-12 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center shadow-lg">
            <Award class="w-6 h-6 text-white" />
          </div>
        </div>
        <h3 class="text-slate-500 text-sm mb-1">{{ t('promotion.pendingEarnings') }}</h3>
        <p class="text-2xl font-bold text-slate-900">{{ formatCurrency(stats.pendingEarnings) }}</p>
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
            <h2 class="text-xl font-bold text-white">{{ t('promotion.myLink') }}</h2>
            <p class="text-sm text-white/95">{{ t('promotion.linkSubtitle') }}</p>
          </div>
        </div>
        <div class="bg-white/20 rounded-2xl p-4 mb-4 backdrop-blur-md border-2 border-white/30 shadow-lg">
          <p class="text-sm text-white/95 mb-2 font-medium">{{ t('promotion.myLink') }}</p>
          <div class="flex items-center gap-2">
            <code class="flex-1 text-sm bg-white/25 px-3 py-2 rounded-xl font-mono truncate backdrop-blur-sm text-white border border-white/20">
              {{ referralLink }}
            </code>
            <button
              @click="copyToClipboard(referralLink)"
              class="bg-white text-violet-600 px-4 py-2 rounded-xl hover:bg-white/90 transition-all shadow-lg flex items-center gap-2 flex-shrink-0 font-medium"
            >
              <Copy class="w-4 h-4" />
              <span>{{ t('promotion.copy') }}</span>
            </button>
          </div>
        </div>
        <div class="bg-white/20 rounded-2xl p-4 backdrop-blur-md border-2 border-white/30 shadow-lg">
          <p class="text-sm text-white/95 mb-2 font-medium">{{ t('promotion.inviteCode') }}</p>
          <div class="flex items-center gap-2">
            <code class="flex-1 text-xl font-bold tracking-wider text-white">
              {{ referralCode }}
            </code>
            <button
              @click="copyToClipboard(referralCode)"
              class="bg-white text-violet-600 px-4 py-2 rounded-xl hover:bg-white/90 transition-all shadow-lg flex items-center gap-2 font-medium"
            >
              <Copy class="w-4 h-4" />
              <span>{{ t('promotion.copy') }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- 奖励规则 -->
      <div class="bg-white rounded-3xl p-8 border border-slate-200 shadow-sm hover:shadow-lg transition-shadow">
        <h2 class="text-xl font-bold text-slate-900 mb-6">{{ t('promotion.rules') }}</h2>
        <div class="space-y-6">
          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">1</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">{{ t('promotion.rule1Title') }}</h3>
              <p class="text-sm text-slate-600">
                {{ t('promotion.rule1Desc', { amount: formatCurrency(rules?.registerReward || 0) }) }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-green-500 to-emerald-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">2</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">{{ t('promotion.rule2Title') }}</h3>
              <p class="text-sm text-slate-600">
                {{ t('promotion.rule2Desc', { rate: rules?.firstRechargeRate || 0 }) }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-violet-500 to-purple-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">3</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">{{ t('promotion.rule3Title') }}</h3>
              <p class="text-sm text-slate-600">
                {{ t('promotion.rule3Desc', { rate: rules?.consumptionRate || 0 }) }}
              </p>
            </div>
          </div>

          <div class="flex items-start gap-4">
            <div class="w-10 h-10 bg-gradient-to-br from-orange-500 to-amber-500 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg">
              <span class="text-white font-bold">4</span>
            </div>
            <div>
              <h3 class="font-semibold text-slate-900 mb-1">{{ t('promotion.rule4Title') }}</h3>
              <p class="text-sm text-slate-600">
                {{ t('promotion.rule4Desc', { day: rules?.settlementDay || 1 }) }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 推广记录 -->
    <div class="bg-white rounded-3xl border border-slate-200 shadow-sm overflow-hidden hover:shadow-lg transition-shadow">
      <div class="p-6 border-b border-slate-200">
        <h2 class="text-lg font-semibold text-slate-900">{{ t('promotion.records') }}</h2>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full">
          <thead class="bg-slate-50">
            <tr>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('promotion.table.user') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('promotion.table.email') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('promotion.table.joinDate') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('promotion.table.status') }}</th>
              <th class="px-6 py-4 text-left text-sm font-semibold text-slate-900">{{ t('promotion.table.earnings') }}</th>
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
                <span class="font-semibold text-green-600">{{ formatCurrency(record.earnings) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!loading && records.length === 0" class="text-center py-12">
        <Gift class="w-12 h-12 text-slate-300 mx-auto mb-3" />
        <p class="text-slate-500">{{ t('promotion.noRecords') }}</p>
        <p class="text-sm text-slate-400 mt-1">{{ t('promotion.noRecordsDesc') }}</p>
      </div>
    </div>
  </div>
</template>
