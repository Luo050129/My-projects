import { createContext } from 'react'

export type ToastTone = 'info' | 'ok' | 'bad'

export interface ToastApi {
  showToast: (message: string, tone?: ToastTone) => void
}

export const ToastContext = createContext<ToastApi | null>(null)
