import { useState, useEffect } from 'react'
import { MarketSharePage } from './pages/MarketSharePage'
import { FinanceSharePage } from './pages/FinanceSharePage'
import { HomePage } from './pages/HomePage'
import { getLastActiveRoute } from './utils/recentStorage'

export type RouteType = 'market' | 'finance' | 'home'

export interface RouteInfo {
  type: RouteType
  id: string
}

function parseRoute(): RouteInfo {
  if (typeof window === 'undefined') return { type: 'home', id: '' }

  const path = window.location.pathname
  const hash = window.location.hash
  const params = new URLSearchParams(window.location.search)

  // 1. Direct path /market/:id or /finance/:id
  const marketMatch = path.match(/^\/market\/([^/]+)/)
  if (marketMatch) return { type: 'market', id: decodeURIComponent(marketMatch[1]) }

  const financeMatch = path.match(/^\/finance\/([^/]+)/)
  if (financeMatch) return { type: 'finance', id: decodeURIComponent(financeMatch[1]) }

  // 2. Hash routes #/market/:id or #/finance/:id
  const hashMarket = hash.match(/^#\/?market\/([^/]+)/)
  if (hashMarket) return { type: 'market', id: decodeURIComponent(hashMarket[1]) }

  const hashFinance = hash.match(/^#\/?finance\/([^/]+)/)
  if (hashFinance) return { type: 'finance', id: decodeURIComponent(hashFinance[1]) }

  // 3. Query params
  const listId = params.get('listId') || params.get('marketId')
  if (listId) return { type: 'market', id: listId }

  const financeId = params.get('financeId') || params.get('dashboardId')
  if (financeId) return { type: 'finance', id: financeId }

  const typeParam = params.get('type')
  const idParam = params.get('id')
  if (typeParam === 'finance' && idParam) return { type: 'finance', id: idParam }
  if (typeParam === 'market' && idParam) return { type: 'market', id: idParam }
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
  const [routeInfo, setRouteInfo] = useState<RouteInfo>(getInitialRoute)

  useEffect(() => {
    const handleLocationChange = () => setRouteInfo(parseRoute())
    window.addEventListener('popstate', handleLocationChange)
    window.addEventListener('hashchange', handleLocationChange)
    return () => {
      window.removeEventListener('popstate', handleLocationChange)
      window.removeEventListener('hashchange', handleLocationChange)
    }
  }, [])

  const handleNavigate = (type: 'market' | 'finance', id: string) => {
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

  return <HomePage onNavigate={handleNavigate} />
}
