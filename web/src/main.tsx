import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import { App } from './App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)

// Registrar Service Worker para suporte a PWA
if ('serviceWorker' in navigator) {
  const registerSw = () => {
    navigator.serviceWorker.register('/sw.js').catch((err: unknown) => {
      console.warn('Falha ao registrar Service Worker:', err)
    })
  }

  if (document.readyState === 'complete') {
    registerSw()
  } else {
    window.addEventListener('load', registerSw)
  }
}

