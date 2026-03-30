<script setup>
import { onMounted, onUnmounted, computed } from 'vue'
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-vue-next'

const props = defineProps({
  id: { type: String, required: true },
  type: { type: String, required: true },
  title: { type: String, required: true },
  message: { type: String, default: '' },
  duration: { type: Number, default: 3000 }
})

const emit = defineEmits(['close'])

let timer = null

onMounted(() => {
  if (props.duration > 0) {
    timer = setTimeout(() => {
      emit('close', props.id)
    }, props.duration)
  }
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

const styles = computed(() => {
  switch (props.type) {
    case 'success':
      return {
        bg: 'bg-gradient-to-r from-green-500 to-emerald-500',
        icon: CheckCircle,
        progressBg: 'bg-green-300'
      }
    case 'error':
      return {
        bg: 'bg-gradient-to-r from-red-500 to-rose-500',
        icon: XCircle,
        progressBg: 'bg-red-300'
      }
    case 'warning':
      return {
        bg: 'bg-gradient-to-r from-yellow-500 to-orange-500',
        icon: AlertTriangle,
        progressBg: 'bg-yellow-300'
      }
    case 'info':
      return {
        bg: 'bg-gradient-button',
        icon: Info,
        progressBg: 'bg-primary/30'
      }
    default:
      return {
        bg: 'bg-gradient-to-r from-slate-500 to-slate-600',
        icon: Info,
        progressBg: 'bg-slate-300'
      }
  }
})
</script>

<template>
  <div
    class="relative w-full max-w-sm overflow-hidden rounded-2xl shadow-2xl pointer-events-auto"
    :class="styles.bg"
  >
    <div class="p-4">
      <div class="flex items-start gap-3 text-white">
        <div class="flex-shrink-0 mt-0.5">
          <component :is="styles.icon" class="w-6 h-6" />
        </div>
        <div class="flex-1 min-w-0">
          <h4 class="font-semibold text-base mb-0.5">
            {{ title }}
          </h4>
          <p v-if="message" class="text-white/90 text-sm leading-relaxed">
            {{ message }}
          </p>
        </div>
        <button
          @click="emit('close', id)"
          class="flex-shrink-0 text-white/80 hover:text-white transition-colors p-1 hover:bg-white/10 rounded-lg"
        >
          <X class="w-5 h-5" />
        </button>
      </div>
    </div>
    
    <!-- 进度条 -->
    <div v-if="duration > 0" class="h-1 bg-white/30">
      <div
        :class="['h-full', styles.progressBg]"
        :style="{
          animation: `toast-shrink ${duration}ms linear forwards`
        }"
      />
    </div>
  </div>
</template>

<style>
@keyframes toast-shrink {
  from { width: 100%; }
  to { width: 0%; }
}
</style>
