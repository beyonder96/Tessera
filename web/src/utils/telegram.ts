declare global {
  interface Window {
    Telegram?: {
      WebApp?: {
        ready: () => void
        expand: () => void
        close: () => void
        colorScheme: 'light' | 'dark'
        themeParams: Record<string, string>
        isExpanded: boolean
        viewportHeight: number
        viewportStableHeight: number
        headerColor: string
        backgroundColor: string
        setHeaderColor: (color: string) => void
        setBackgroundColor: (color: string) => void
        enableClosingConfirmation: () => void
        disableClosingConfirmation: () => void
        BackButton: {
          isVisible: boolean
          show: () => void
          hide: () => void
          onClick: (callback: () => void) => void
          offClick: (callback: () => void) => void
        }
        HapticFeedback: {
          impactOccurred: (style: 'light' | 'medium' | 'heavy' | 'rigid' | 'soft') => void
          notificationOccurred: (type: 'error' | 'success' | 'warning') => void
          selectionChanged: () => void
        }
        initData: string
        initDataUnsafe?: {
          user?: {
            id: number
            first_name: string
            last_name?: string
            username?: string
            language_code?: string
          }
        }
        onEvent: (eventType: string, eventHandler: () => void) => void
        offEvent: (eventType: string, eventHandler: () => void) => void
      }
    }
  }
}

/**
 * Retorna se a aplicação está rodando dentro do Telegram WebApp
 */
export function isTelegramWebApp(): boolean {
  if (typeof window === 'undefined') return false
  const tg = window.Telegram?.WebApp
  if (!tg) return false
  // Verifica se temos initData ou plataforma Telegram
  return Boolean(tg.initData || (window as any).TelegramWebviewProxy || ((tg as any).platform && (tg as any).platform !== 'unknown'))
}

/**
 * Inicializa o Telegram WebApp (ready, expand e cores)
 */
export function initTelegramWebApp(currentTheme: 'dark' | 'light'): void {
  if (typeof window === 'undefined' || !window.Telegram?.WebApp) return

  try {
    const tg = window.Telegram.WebApp
    tg.ready()
    tg.expand()

    const color = currentTheme === 'dark' ? '#0B0D13' : '#F4F6F9'
    if (typeof tg.setHeaderColor === 'function') {
      tg.setHeaderColor(color)
    }
    if (typeof tg.setBackgroundColor === 'function') {
      tg.setBackgroundColor(color)
    }
  } catch (err) {
    console.warn('Aviso na inicialização do Telegram WebApp:', err)
  }
}

/**
 * Dispara vibração háptica tátil nativa no aparelho do usuário
 */
export function tgHaptic(
  type: 'light' | 'medium' | 'heavy' | 'selection' | 'success' | 'warning' | 'error' = 'medium'
): void {
  if (typeof window === 'undefined' || !window.Telegram?.WebApp?.HapticFeedback) return

  try {
    const haptic = window.Telegram.WebApp.HapticFeedback
    if (type === 'success' || type === 'warning' || type === 'error') {
      haptic.notificationOccurred(type)
    } else if (type === 'selection') {
      haptic.selectionChanged()
    } else {
      haptic.impactOccurred(type)
    }
  } catch {
    // Silencia em plataformas sem motor de vibração háptica
  }
}

/**
 * Conecta e exibe o botão nativo de voltar do Telegram na barra superior
 */
export function setupTelegramBackButton(onBack: () => void): () => void {
  if (typeof window === 'undefined' || !window.Telegram?.WebApp?.BackButton) {
    return () => {}
  }

  const bb = window.Telegram.WebApp.BackButton
  try {
    bb.show()
    bb.onClick(onBack)
  } catch (err) {
    console.warn('Erro ao configurar BackButton do Telegram:', err)
  }

  return () => {
    try {
      bb.offClick(onBack)
      bb.hide()
    } catch {}
  }
}

/**
 * Retorna os dados do usuário autenticado no Telegram se disponíveis
 */
export function getTelegramUser() {
  if (typeof window === 'undefined') return null
  return window.Telegram?.WebApp?.initDataUnsafe?.user || null
}
