import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true,
      }
    },
  },
  build: {
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }

          if (id.includes('node_modules/vue-echarts/')) {
            return 'charting-vue'
          }

          if (id.includes('node_modules/echarts/')) {
            return 'charting-echarts'
          }

          if (id.includes('node_modules/zrender/')) {
            return 'charting-zrender'
          }

          if (
            id.includes('node_modules/vue/') ||
            id.includes('node_modules/vue-router/') ||
            id.includes('node_modules/pinia/') ||
            id.includes('node_modules/vue-i18n/')
          ) {
            return 'vue'
          }

          if (
            id.includes('node_modules/lucide-vue-next/') ||
            id.includes('node_modules/radix-vue/') ||
            id.includes('node_modules/class-variance-authority/') ||
            id.includes('node_modules/clsx/') ||
            id.includes('node_modules/tailwind-merge/') ||
            id.includes('node_modules/tailwindcss-animate/')
          ) {
            return 'ui'
          }
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
})
