import { useState, useEffect, useCallback } from 'react'

export type Theme = 'dark' | 'light'

const STORAGE_KEY = 'tessera_theme'

function getInitialTheme(): Theme {
  if (typeof window === 'undefined') return 'dark'
  try {
    // Se aberto dentro do Telegram, respeita o tema nativo do Telegram
    if (window.Telegram?.WebApp?.colorScheme) {
      return window.Telegram.WebApp.colorScheme === 'light' ? 'light' : 'dark'
    }
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved === 'dark' || saved === 'light') return saved
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches) {
      return 'light'
    }
  } catch {
    // ignore
  }
  return 'dark'
}

function applyTheme(theme: Theme) {
  if (typeof document === 'undefined') return
  document.documentElement.setAttribute('data-theme', theme)

  // Atualiza meta theme-color para navegadores móveis e barra de status
  const metaThemeColor = document.querySelector('meta[name="theme-color"]')
  if (metaThemeColor) {
    metaThemeColor.setAttribute('content', theme === 'dark' ? '#0B0D13' : '#F4F6F9')
  }

  // Atualiza cores do cabeçalho e fundo no Telegram WebApp
  if (typeof window !== 'undefined' && window.Telegram?.WebApp) {
    try {
      const color = theme === 'dark' ? '#0B0D13' : '#F4F6F9'
      if (typeof window.Telegram.WebApp.setHeaderColor === 'function') {
        window.Telegram.WebApp.setHeaderColor(color)
      }
      if (typeof window.Telegram.WebApp.setBackgroundColor === 'function') {
        window.Telegram.WebApp.setBackgroundColor(color)
      }
    } catch {}
  }
}

export function useTheme() {
  const [theme, setThemeState] = useState<Theme>(getInitialTheme)

  useEffect(() => {
    applyTheme(theme)
    try {
      localStorage.setItem(STORAGE_KEY, theme)
    } catch {
      // ignore
    }
  }, [theme])

  // Escuta alterações de outras abas ou componentes
  useEffect(() => {
    const handleStorage = (e: StorageEvent) => {
      if (e.key === STORAGE_KEY && (e.newValue === 'dark' || e.newValue === 'light')) {
        setThemeState(e.newValue)
      }
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  // Escuta alteração dinâmica de tema disparada pelo Telegram
  useEffect(() => {
    if (typeof window === 'undefined' || !window.Telegram?.WebApp) return

    const handleTgTheme = () => {
      const tgScheme = window.Telegram?.WebApp?.colorScheme
      if (tgScheme === 'light' || tgScheme === 'dark') {
        setThemeState(tgScheme)
      }
    }

    try {
      window.Telegram.WebApp.onEvent('themeChanged', handleTgTheme)
    } catch {}

    return () => {
      try {
        window.Telegram?.WebApp?.offEvent('themeChanged', handleTgTheme)
      } catch {}
    }
  }, [])

  const toggleTheme = useCallback(() => {
    setThemeState(prev => (prev === 'dark' ? 'light' : 'dark'))
  }, [])

  const setTheme = useCallback((newTheme: Theme) => {
    setThemeState(newTheme)
  }, [])

  return { theme, toggleTheme, setTheme }
}
