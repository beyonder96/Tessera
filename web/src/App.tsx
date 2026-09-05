import { useState, useEffect } from 'react'
import { MarketSharePage } from './pages/MarketSharePage'
import { FinanceSharePage } from './pages/FinanceSharePage'
import { Sparkles, Shield, Smartphone } from 'lucide-react'

function parseRoute(): { type: 'market' | 'finance' | 'home'; id: string } {
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

export function App() {
  const [routeInfo, setRouteInfo] = useState(parseRoute())

  useEffect(() => {
    const handleLocationChange = () => setRouteInfo(parseRoute())
    window.addEventListener('popstate', handleLocationChange)
    window.addEventListener('hashchange', handleLocationChange)
    return () => {
      window.removeEventListener('popstate', handleLocationChange)
      window.removeEventListener('hashchange', handleLocationChange)
    }
  }, [])

  if (routeInfo.type === 'market' && routeInfo.id) {
    return <MarketSharePage listId={routeInfo.id} />
  }

  if (routeInfo.type === 'finance' && routeInfo.id) {
    return <FinanceSharePage dashboardId={routeInfo.id} />
  }

  // Fallback Home / Landing
  return (
    <div className="container" style={{ textAlign: 'center', paddingTop: 80, maxWidth: 540 }}>
      <div className="card" style={{ padding: '40px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 20 }}>
        <div style={{ 
          width: 56, 
          height: 56, 
          borderRadius: '50%', 
          background: 'var(--accent-subtle)', 
          border: '1px solid var(--border-active)', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center' 
        }}>
          <Sparkles size={28} color="var(--accent)" />
        </div>

        <div>
          <h1 className="header-title" style={{ fontSize: 28, marginBottom: 8 }}>Tessera Live Web</h1>
          <p style={{ fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.6 }}>
            Compartilhe e acompanhe listas de compras e resumos financeiros em tempo real, sem necessidade de login ou instalação.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, width: '100%', marginTop: 12 }}>
          <div style={{ 
            background: 'var(--bg-surface)', 
            border: '1px solid var(--border)', 
            borderRadius: 'var(--radius-md)', 
            padding: 16,
            textAlign: 'left' 
          }}>
            <Smartphone size={20} color="var(--accent)" style={{ marginBottom: 8 }} />
            <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>App Android</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Gere um link no menu de Finanças ou Mercado do app.</div>
          </div>

          <div style={{ 
            background: 'var(--bg-surface)', 
            border: '1px solid var(--border)', 
            borderRadius: 'var(--radius-md)', 
            padding: 16,
            textAlign: 'left' 
          }}>
            <Shield size={20} color="var(--accent)" style={{ marginBottom: 8 }} />
            <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Tempo Real</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Atualização instantânea via Supabase WebSockets.</div>
          </div>
        </div>
      </div>
    </div>
  )
}
