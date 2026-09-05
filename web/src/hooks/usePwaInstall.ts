import { useState, useEffect, useCallback } from 'react'

export interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

declare global {
  interface Window {
    deferredInstallPrompt?: BeforeInstallPromptEvent | null
  }
}

function checkIsStandalone(): boolean {
  if (typeof window === 'undefined') return false
  return (
    window.matchMedia('(display-mode: standalone)').matches ||
    (window.navigator as unknown as { standalone?: boolean }).standalone === true
  )
}

function detectIsIos(): boolean {
  if (typeof window === 'undefined') return false
  const userAgent = window.navigator.userAgent.toLowerCase()
  return /iphone|ipad|ipod/.test(userAgent)
}

export function usePwaInstall() {
  const [isInstalled, setIsInstalled] = useState<boolean>(checkIsStandalone)
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(() => {
    if (typeof window !== 'undefined' && window.deferredInstallPrompt) {
      return window.deferredInstallPrompt
    }
    return null
  })
  const [showHelpModal, setShowHelpModal] = useState(false)
  const [isIos] = useState<boolean>(detectIsIos)

  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault()
      const promptEvent = e as BeforeInstallPromptEvent
      window.deferredInstallPrompt = promptEvent
      setInstallPrompt(promptEvent)
    }

    const handleAppInstalled = () => {
      setIsInstalled(true)
      setInstallPrompt(null)
      window.deferredInstallPrompt = null
      setShowHelpModal(false)
    }

    const handlePromptReady = () => {
      if (window.deferredInstallPrompt) {
        setInstallPrompt(window.deferredInstallPrompt)
      }
    }

    window.addEventListener('beforeinstallprompt', handleBeforeInstall)
    window.addEventListener('appinstalled', handleAppInstalled)
    window.addEventListener('pwa-prompt-ready', handlePromptReady)

    if (window.deferredInstallPrompt && !installPrompt) {
      setInstallPrompt(window.deferredInstallPrompt)
    }

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall)
      window.removeEventListener('appinstalled', handleAppInstalled)
      window.removeEventListener('pwa-prompt-ready', handlePromptReady)
    }
  }, [installPrompt])

  const installApp = useCallback(async () => {
    const activePrompt = installPrompt || window.deferredInstallPrompt
    if (activePrompt) {
      try {
        await activePrompt.prompt()
        const { outcome } = await activePrompt.userChoice
        if (outcome === 'accepted') {
          setIsInstalled(true)
        }
        setInstallPrompt(null)
        window.deferredInstallPrompt = null
      } catch (err: unknown) {
        console.warn('Erro ao acionar prompt de instalação:', err)
        setShowHelpModal(true)
      }
    } else {
      setShowHelpModal(true)
    }
  }, [installPrompt])

  return {
    isInstalled,
    installApp,
    showHelpModal,
    setShowHelpModal,
    isIos,
    hasNativePrompt: Boolean(installPrompt || (typeof window !== 'undefined' && window.deferredInstallPrompt)),
  }
}
