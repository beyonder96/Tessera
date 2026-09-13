import { useState, useEffect } from 'react'
import { MarketSharePage } from './pages/MarketSharePage'
import { FinanceSharePage } from './pages/FinanceSharePage'
import { TaskSharePage } from './pages/TaskSharePage'
import { WishSharePage } from './pages/WishSharePage'
import { HomePage } from './pages/HomePage'
import { getLastActiveRoute } from './utils/recentStorage'
import { initTelegramWebApp, setupTelegramBackButton, tgHaptic } from './utils/telegram'
import { useTheme } from './hooks/useTheme'

export type RouteType = 'market' | 'finance' | 'tasks' | 'wishes' | 'home'

export interface RouteInfo {
  type: RouteType
  id: string
}

function parseRoute(): RouteInfo {
  if (typeof window === 'undefined') return { type: 'home', id: '' }

  const path = window.location.pathname
  const hash = window.location.hash
  const params = new URLSearchParams(window.location.search)

  // 1. Direct path /market/:id, /finance/:id, /tasks/:id or /wishes/:id
  const marketMatch = path.match(/^\/market(?:\/([^/]+))?/)
  if (marketMatch) return { type: 'market', id: marketMatch[1] ? decodeURIComponent(marketMatch[1]) : 'market_default' }

  const financeMatch = path.match(/^\/finance(?:\/([^/]+))?/)
  if (financeMatch) return { type: 'finance', id: financeMatch[1] ? decodeURIComponent(financeMatch[1]) : 'finance_default' }

  const tasksMatch = path.match(/^\/tasks(?:\/([^/]+))?/)
  if (tasksMatch) return { type: 'tasks', id: tasksMatch[1] ? decodeURIComponent(tasksMatch[1]) : 'tasks_default' }

  const wishesMatch = path.match(/^\/wishes(?:\/([^/]+))?/)
  if (wishesMatch) return { type: 'wishes', id: wishesMatch[1] ? decodeURIComponent(wishesMatch[1]) : 'wishes_default' }

  // 2. Hash routes #/market/:id, #/finance/:id, #/tasks or #/wishes
  const hashMarket = hash.match(/^#\/?market(?:\/([^/]+))?/)
  if (hashMarket) return { type: 'market', id: hashMarket[1] ? decodeURIComponent(hashMarket[1]) : 'market_default' }

  const hashFinance = hash.match(/^#\/?finance(?:\/([^/]+))?/)
  if (hashFinance) return { type: 'finance', id: hashFinance[1] ? decodeURIComponent(hashFinance[1]) : 'finance_default' }

  const hashTasks = hash.match(/^#\/?tasks(?:\/([^/]+))?/)
  if (hashTasks) return { type: 'tasks', id: hashTasks[1] ? decodeURIComponent(hashTasks[1]) : 'tasks_default' }

  const hashWishes = hash.match(/^#\/?wishes(?:\/([^/]+))?/)
  if (hashWishes) return { type: 'wishes', id: hashWishes[1] ? decodeURIComponent(hashWishes[1]) : 'wishes_default' }

  // 3. Query params
  const listId = params.get('listId') || params.get('marketId')
  if (listId) return { type: 'market', id: listId }

  const financeId = params.get('financeId') || params.get('dashboardId')
  if (financeId) return { type: 'finance', id: financeId }

  const taskId = params.get('taskId') || params.get('hubId')
  if (taskId) return { type: 'tasks', id: taskId }

  const wishesId = params.get('wishesId') || params.get('wishId')
  if (wishesId) return { type: 'wishes', id: wishesId }

  const typeParam = params.get('type')
  const idParam = params.get('id')
  if (typeParam === 'finance') return { type: 'finance', id: idParam || 'finance_default' }
  if (typeParam === 'market') return { type: 'market', id: idParam || 'market_default' }
  if (typeParam === 'tasks') return { type: 'tasks', id: idParam || 'tasks_default' }
  if (typeParam === 'wishes') return { type: 'wishes', id: idParam || 'wishes_default' }
  if (idParam) return { type: 'market', id: idParam }

  return { type: 'home', id: '' }
}

function getInitialRoute(): RouteInfo {
  const currentRoute = parseRoute()
  if (currentRoute.type !== 'home') return currentRoute

  // Se o usuário solicitou explicitamente a Home, respeita a intenção
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search)
    if (params.get('home') === 'true') return currentRoute

    const skipRedirect = sessionStorage.getItem('tessera_skip_autoredirect')
    if (skipRedirect === 'true') return currentRoute

    // Se estiver na raiz e houver um último acesso registrado, auto-redireciona
    const lastActive = getLastActiveRoute()
    if (lastActive) {
      const targetPath = `/${lastActive.type}/${lastActive.id}`
      window.history.replaceState(null, '', targetPath)
      return { type: lastActive.type, id: lastActive.id }
    }
  }

  return currentRoute
}

export function App() {
  const { theme } = useTheme()
  const [routeInfo, setRouteInfo] = useState<RouteInfo>(getInitialRoute)

  // Inicializa o Telegram WebApp (ready, expand, status bar colors)
  useEffect(() => {
    initTelegramWebApp(theme)
  }, [theme])

  // Vincula o botão Voltar nativo do Telegram à navegação para Home
  useEffect(() => {
    if (routeInfo.type !== 'home') {
      return setupTelegramBackButton(() => {
        if (typeof window !== 'undefined') {
          sessionStorage.setItem('tessera_skip_autoredirect', 'true')
          window.history.pushState(null, '', '/?home=true')
        }
        setRouteInfo({ type: 'home', id: '' })
      })
    }
  }, [routeInfo.type])

  useEffect(() => {
    const handleLocationChange = () => setRouteInfo(parseRoute())
    window.addEventListener('popstate', handleLocationChange)
    window.addEventListener('hashchange', handleLocationChange)
    return () => {
      window.removeEventListener('popstate', handleLocationChange)
      window.removeEventListener('hashchange', handleLocationChange)
    }
  }, [])

  const handleNavigate = (type: 'market' | 'finance' | 'tasks' | 'wishes', id: string) => {
    tgHaptic('light')
    if (typeof window !== 'undefined') {
      sessionStorage.removeItem('tessera_skip_autoredirect')
      const targetPath = `/${type}/${id}`
      window.history.pushState(null, '', targetPath)
    }
    setRouteInfo({ type, id })
  }

  if (routeInfo.type === 'market' && routeInfo.id) {
    return <MarketSharePage listId={routeInfo.id} />
  }

  if (routeInfo.type === 'finance' && routeInfo.id) {
    return <FinanceSharePage dashboardId={routeInfo.id} />
  }

  if (routeInfo.type === 'tasks' && routeInfo.id) {
    return <TaskSharePage hubId={routeInfo.id} />
  }

  if (routeInfo.type === 'wishes' && routeInfo.id) {
    return <WishSharePage hubId={routeInfo.id} />
  }

  return <HomePage onNavigate={handleNavigate} />
}
