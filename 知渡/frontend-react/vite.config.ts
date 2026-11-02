import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // 与 Vue 版（5173）错开，两套前端可同时起来做对比
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8088',
        changeOrigin: true
      }
    }
  },
  build: {
    // 把 React 运行时单独拆包，业务代码变更时命中缓存的概率更高
    rollupOptions: {
      output: {
        manualChunks: {
          'react-vendor': ['react', 'react-dom', 'react-router-dom']
        }
      }
    }
  }
})
