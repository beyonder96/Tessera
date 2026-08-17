import { useState, useEffect } from 'react'
import { MarketSharePage } from './pages/MarketSharePage'
import { FinanceSharePage } from './pages/FinanceSharePage'
import { Sparkles, Shield, Smartphone } from 'lucide-react'

export function App() {
  const [route, setRoute] = useState(window.location.pathname)

  useEffect(() => {
    const handlePopState = () => setRoute(window.location.pathname)
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  // Match /market/:id
  const marketMatch = route.match(/^\/market\/([^/]+)/)
  if (marketMatch) {
    return <MarketSharePage listId={marketMatch[1]} />
  }

  // Match /finance/:id
  const financeMatch = route.match(/^\/finance\/([^/]+)/)
  if (financeMatch) {
    return <FinanceSharePage dashboardId={financeMatch[1]} />
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
            <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 4 }}>App Android</div>
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
            <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 4 }}>Tempo Real</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>Atualização instantânea via Supabase WebSockets.</div>
          </div>
        </div>
      </div>
    </div>
  )
}
