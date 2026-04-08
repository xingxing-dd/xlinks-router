<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Globe } from 'lucide-vue-next'
import { getLocale, setLocale } from '@/locales'

const props = defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
  dark: {
    type: Boolean,
    default: false,
  },
  header: {
    type: Boolean,
    default: false,
  },
})

const { t, locale } = useI18n()

const isZh = computed(() => String(locale.value || getLocale()).startsWith('zh'))

const buttonClass = computed(() => {
  if (props.compact) {
    return props.dark
      ? 'inline-flex items-center justify-center p-2 rounded-lg border border-white/30 bg-white/10 text-white hover:bg-white/20 transition-colors'
      : 'inline-flex items-center justify-center p-2 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-colors'
  }

  if (props.header) {
    return 'flex items-center gap-2 px-4 py-2 h-12 bg-primary/10 hover:bg-primary/15 rounded-xl border border-primary/15 transition-all group'
  }

  return props.dark
    ? 'inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/30 bg-white/10 text-white hover:bg-white/20 transition-colors'
    : 'inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 transition-colors'
})

const label = computed(() => (isZh.value ? t('common.chinese') : t('common.english')))
const iconClass = computed(() => (
  props.dark
    ? 'w-4 h-4 text-white'
    : 'w-4 h-4 text-slate-500 group-hover:scale-110 transition-transform duration-300'
))
const labelClass = computed(() => (
  props.dark
    ? 'text-sm font-medium text-white'
    : 'text-sm font-medium text-slate-700'
))

const toggleLocale = () => {
  const next = isZh.value ? 'en-US' : 'zh-CN'
  setLocale(next)
}
</script>

<template>
  <button :class="buttonClass" @click="toggleLocale">
    <Globe :class="iconClass" />
    <span v-if="!compact" :class="labelClass">
      {{ label }}
    </span>
  </button>
</template>
