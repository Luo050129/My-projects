import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { AppRoutes } from './App'
import { AuthProvider } from './auth/AuthProvider'
import { ErrorBoundary } from './components/ErrorBoundary'
import { ToastProvider } from './components/ToastProvider'
import './styles/global.css'

const container = document.getElementById('root')
if (!container) throw new Error('找不到 #root 挂载点')

createRoot(container).render(
  <StrictMode>
    <ErrorBoundary>
      <AuthProvider>
        <ToastProvider>
          <BrowserRouter>
            <AppRoutes />
          </BrowserRouter>
        </ToastProvider>
      </AuthProvider>
    </ErrorBoundary>
  </StrictMode>
)
