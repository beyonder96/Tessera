import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { TrendingUp, TrendingDown, Wallet, Eye, EyeOff, Share2, AlertCircle, RefreshCw, ArrowUpRight, ArrowDownLeft } from 'lucide-react'

interface CategoryBreakdown {
  name: string
  amount: number
  percentage: number
}

interface TransactionItem {
  id: number
  title: string
  category: string
  amount: number
  type: string
  date: string
}

interface FinanceDashboardDoc {
  id: string
  title: string
  month_label: string
  total_balance: number
  monthly_income: number
  monthly_expense: number
  categories: CategoryBreakdown[]
  transactions: TransactionItem[]
  is_live: boolean
  updated_at: string
}

export const FinanceSharePage: React.FC<{ dashboardId: string }> = ({ dashboardId }) => {
  const [doc, setDoc] = useState<FinanceDashboardDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isPrivacyMode, setIsPrivacyMode] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    async function loadDashboard() {
      setLoading(true)
      setError(null)
      try {
        const { data, error } = await supabase
          .from('shared_finance_dashboards')
          .select('*')
          .eq('id', dashboardId)
          .single()

        if (error) {
          throw error
        }

        setDoc(data)
      } catch (err: any) {
        console.error('Error fetching finance dashboard:', err)
        setError('Resumo financeiro não encontrado ou link expirado.')
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()

    // Realtime Subscription
    const channel = supabase
      .channel(`finance-dashboard-${dashboardId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'shared_finance_dashboards',
          filter: `id=eq.${dashboardId}`,
        },
        (payload: any) => {
          if (payload.new) {
            setDoc(payload.new as FinanceDashboardDoc)
          }
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [dashboardId])

  const formatCurrency = (val: number) => {
    if (isPrivacyMode) return 'R$ •••••'
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val)
  }

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  if (loading) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div className="skeleton" style={{ width: 160, height: 28 }} />
          <div className="skeleton" style={{ width: 90, height: 24, borderRadius: 999 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 140, borderRadius: 20, marginBottom: 16 }} />
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 24 }}>
          <div className="skeleton" style={{ height: 90, borderRadius: 16 }} />
          <div className="skeleton" style={{ height: 90, borderRadius: 16 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 200, borderRadius: 20 }} />
      </div>
    )
  }

  if (error || !doc) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: 80 }}>
        <div className="card" style={{ maxWidth: 420, margin: '0 auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--danger-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <AlertCircle size={24} color="var(--danger)" />
          </div>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>Resumo indisponível</h2>
          <p style={{ fontSize: 14, color: 'var(--text-secondary)' }}>{error || 'Não encontramos este resumo financeiro.'}</p>
          <button className="btn btn-outline" onClick={() => window.location.reload()}>
            <RefreshCw size={14} /> Tentar novamente
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="container">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <div className="live-badge" style={{ marginBottom: 8 }}>
            <div className="live-dot" />
            Ao Vivo
          </div>
          <h1 className="header-title">{doc.title || 'Resumo Financeiro'}</h1>
          <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
            {doc.month_label} • Atualizado em tempo real
          </p>
        </div>

        <div style={{ display: 'flex', gap: 8 }}>
          <button 
            className="btn btn-outline" 
            onClick={() => setIsPrivacyMode(!isPrivacyMode)}
            style={{ padding: '8px 10px' }}
            title="Alternar privacidade"
          >
            {isPrivacyMode ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
          <button className="btn btn-outline" onClick={handleShare} style={{ padding: '8px 12px' }}>
            <Share2 size={14} />
            {copied ? 'Copiado!' : 'Compartilhar'}
          </button>
        </div>
      </div>

      {/* Hero Balance Card */}
      <div className="card" style={{ marginBottom: 16 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <Wallet size={16} color="var(--accent)" />
          <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1.5, color: 'var(--accent)', textTransform: 'uppercase' }}>
            Saldo Consolidado
          </span>
        </div>
        <div style={{ fontSize: 32, fontWeight: 700, letterSpacing: -0.5, color: 'var(--text-primary)' }}>
          {formatCurrency(doc.total_balance)}
        </div>
      </div>

      {/* Income & Expense Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 24 }}>
        <div className="card" style={{ padding: 18 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingUp size={14} color="var(--success)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Receitas</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--success)' }}>
            {formatCurrency(doc.monthly_income)}
          </div>
        </div>

        <div className="card" style={{ padding: 18 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingDown size={14} color="var(--danger)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Despesas</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--danger)' }}>
            {formatCurrency(doc.monthly_expense)}
          </div>
        </div>
      </div>

      {/* Categories Breakdown */}
      {doc.categories && doc.categories.length > 0 && (
        <div className="card" style={{ marginBottom: 24 }}>
          <div style={{ fontSize: 13, fontWeight: 700, letterSpacing: 1, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: 16 }}>
            Gastos por Categoria
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {doc.categories.map((cat, idx) => (
              <div key={idx}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, fontWeight: 600, marginBottom: 6 }}>
                  <span>{cat.name}</span>
                  <span style={{ color: 'var(--text-secondary)' }}>
                    {formatCurrency(cat.amount)} <small style={{ opacity: 0.7 }}>({cat.percentage.toFixed(0)}%)</small>
                  </span>
                </div>
                <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                  <div 
                    style={{ 
                      width: `${Math.min(cat.percentage, 100)}%`, 
                      height: '100%', 
                      background: 'var(--accent)', 
                      borderRadius: 999,
                      opacity: 0.85
                    }} 
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Transactions List */}
      {doc.transactions && doc.transactions.length > 0 && (
        <div>
          <div style={{ fontSize: 13, fontWeight: 700, letterSpacing: 1, color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: 12, paddingLeft: 4 }}>
            Últimas Movimentações
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {doc.transactions.map((tx) => {
              const isIncome = tx.type?.toLowerCase() === 'income' || tx.type?.toLowerCase() === 'receita'
              return (
                <div 
                  key={tx.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-md)'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <div style={{ 
                      width: 32, 
                      height: 32, 
                      borderRadius: '50%', 
                      background: isIncome ? 'var(--success-subtle)' : 'var(--bg-surface)', 
                      display: 'flex', 
                      alignItems: 'center', 
                      justifyContent: 'center' 
                    }}>
                      {isIncome ? <ArrowDownLeft size={16} color="var(--success)" /> : <ArrowUpRight size={16} color="var(--text-muted)" />}
                    </div>
                    <div>
                      <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{tx.title}</div>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{tx.category || 'Geral'}</div>
                    </div>
                  </div>

                  <div style={{ 
                    fontSize: 13, 
                    fontWeight: 700, 
                    color: isIncome ? 'var(--success)' : 'var(--text-primary)' 
                  }}>
                    {isIncome ? '+ ' : '- '}
                    {formatCurrency(tx.amount)}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Footer Branding */}
      <div style={{ textAlign: 'center', marginTop: 40, marginBottom: 20, fontSize: 11, color: 'var(--text-muted)' }}>
        Desenvolvido por <strong style={{ color: 'var(--text-primary)' }}>Tessera</strong> • Conexão Web
      </div>
    </div>
  )
}
