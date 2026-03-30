<script setup>
import { computed, onMounted } from 'vue'

const props = defineProps({
  message: { type: String, default: '' },
  type: { type: String, default: 'info' },
  duration: { type: Number, default: 2800 },
})

const emit = defineEmits(['close'])

const classes = computed(() => {
  const map = {
    success: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    warning: 'bg-amber-50 text-amber-700 border-amber-200',
    error: 'bg-rose-50 text-rose-700 border-rose-200',
    info: 'bg-slate-50 text-slate-700 border-slate-200',
  }
  return map[props.type] || map.info
})

onMounted(() => {
  if (props.duration > 0) {
    setTimeout(() => emit('close'), props.duration)
  }
})
</script>

<template>
  <div class="flex items-start gap-3 px-4 py-3 rounded-xl border shadow-sm" :class="classes">
    <div class="flex-1 text-sm font-medium">{{ message }}</div>
    <button class="text-xs text-slate-400 hover:text-slate-600" @click="emit('close')">关闭</button>
  </div>
</template>
