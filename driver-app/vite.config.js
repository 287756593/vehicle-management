import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const allowedHosts = [
  'localhost',
  '127.0.0.1'
]

const proxy = {
  '/api': {
    target: 'http://localhost:18231',
    changeOrigin: true
  },
  '/uploads': {
    target: 'http://localhost:18231',
    changeOrigin: true
  }
}

const serveOptions = {
  host: '0.0.0.0',
  allowedHosts,
  proxy
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3593,
    ...serveOptions,
    hmr: false,
    watch: {
      ignored: ['**/node_modules/**']
    }
  },
  preview: {
    port: 3593,
    ...serveOptions
  },
  optimizeDeps: {
    include: ['vue', 'vue-router', 'vant', 'dayjs']
  }
})
