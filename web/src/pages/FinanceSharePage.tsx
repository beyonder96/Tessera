import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { TrendingUp, TrendingDown, Wallet, Eye, EyeOff, Share2, AlertCircle, RefreshCw, ArrowUpRight, ArrowDownLeft, Home, Plus, X, Clock, CheckCircle2 } from 'lucide-react'

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

export interface FinanceSuggestionDoc {
  id: string
  title: string
  amount: number
  type: 'expense' | 'income'
  category: string
  date: string
  created_at: string
  status: 'pending' | 'approved' | 'rejected'
}

interface FinanceDashboardDoc {
  id: string
  title: string
  month_label: string
  total_balance: number
  spendable_balance?: number
  salary_value?: number
  committed_value?: number
  committed_percentage?: number
  monthly_income?: number
  monthly_expense?: number
  categories: CategoryBreakdown[]
  transactions: TransactionItem[]
  suggestions?: FinanceSuggestionDoc[]
  is_live: boolean
  updated_at: string
}

export const FinanceSharePage: React.FC<{ dashboardId: string }> = ({ dashboardId }) => {
  const [doc, setDoc] = useState<FinanceDashboardDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [isPrivacyMode, setIsPrivacyMode] = useState(false)
  const [copied, setCopied] = useState(false)

  // Suggestion Modal state
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [suggestionType, setSuggestionType] = useState<'expense' | 'income'>('expense')
  const [suggestionTitle, setSuggestionTitle] = useState('')
  const [suggestionAmount, setSuggestionAmount] = useState('')
  const [suggestionCategory, setSuggestionCategory] = useState('Alimentação')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  useEffect(() => {
    async function loadDashboard() {
      setLoading(true)
      setError(null)
      try {
        const { data, error: sbError } = await supabase
          .from('shared_finance_dashboards')
          .select('*')
          .eq('id', dashboardId)
          .single()

        if (sbError) {
          throw sbError
        }

        if (data) {
          setDoc(data as FinanceDashboardDoc)
          localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(data))
        }
      } catch (err: unknown) {
        console.error('Error fetching finance dashboard:', err)
        const cached = localStorage.getItem(`tessera_finance_${dashboardId}`)
        if (cached) {
          try {
            setDoc(JSON.parse(cached) as FinanceDashboardDoc)
            setError(null)
            setLoading(false)
            return
          } catch {
            // ignore
          }
        }
        const message = err instanceof Error ? err.message : 'Resumo financeiro não encontrado ou link expirado.'
        setError(message)
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
        (payload) => {
          if (payload.new && typeof payload.new === 'object') {
            const newDoc = payload.new as unknown as FinanceDashboardDoc
            setDoc(newDoc)
            localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(newDoc))
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

  const formatDate = (rawDate: unknown): string => {
    if (!rawDate) return ''
    try {
      if (typeof rawDate === 'number' || (!isNaN(Number(rawDate)) && String(rawDate).length > 8)) {
        const d = new Date(Number(rawDate))
        return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
      }
      return String(rawDate)
    } catch {
      return String(rawDate)
    }
  }

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // Handle submitting a suggestion from Web
  const handleSuggestTransaction = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!doc || !suggestionTitle.trim()) return

    const amount = parseFloat(suggestionAmount.replace(',', '.'))
    if (!amount || amount <= 0) return

    setIsSubmitting(true)

    const newSuggestion: FinanceSuggestionDoc = {
      id: `sug_${Date.now()}`,
      title: suggestionTitle.trim(),
      amount,
      type: suggestionType,
      category: suggestionCategory,
      date: new Date().toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      created_at: new Date().toISOString(),
      status: 'pending',
    }

    const currentSuggestions = doc.suggestions || []
    const updatedSuggestions = [newSuggestion, ...currentSuggestions]
    const updatedDoc: FinanceDashboardDoc = { ...doc, suggestions: updatedSuggestions, updated_at: new Date().toISOString() }

    // Optimistic local update
    setDoc(updatedDoc)
    localStorage.setItem(`tessera_finance_${dashboardId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_finance_dashboards')
        .update({ suggestions: updatedSuggestions, updated_at: new Date().toISOString() })
        .eq('id', dashboardId)

      setSuccessMessage(`Sugestão de ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount)} enviada! Ela aparecerá no aplicativo para aprovação.`)
      setSuggestionTitle('')
      setSuggestionAmount('')
      setIsModalOpen(false)
      setTimeout(() => setSuccessMessage(null), 5000)
    } catch (err: unknown) {
      console.error('Failed to submit suggestion:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div className="skeleton" style={{ width: 160, height: 28 }} />
          <div className="skeleton" style={{ width: 90, height: 24, borderRadius: 999 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 160, borderRadius: 20, marginBottom: 16 }} />
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
        <div className="card" style={{ maxWidth: 460, margin: '0 auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--danger-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <AlertCircle size={24} color="var(--danger)" />
          </div>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>Resumo indisponível</h2>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            O painel financeiro (<code>{dashboardId}</code>) não foi localizado no servidor.
          </p>
          <div style={{ background: 'var(--bg-surface)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', width: '100%', textAlign: 'left', fontSize: 12, color: 'var(--text-muted)' }}>
            💡 <strong>Dica:</strong> Abra o aplicativo Tessera no celular, acesse a aba <em>Finanças</em> e toque em <em>Compartilhar / Gerar Novo Link</em> para atualizar os dados compartilhados.
          </div>
          <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
            <button className="btn btn-outline" onClick={() => window.location.reload()}>
              <RefreshCw size={14} /> Tentar novamente
            </button>
            <button className="btn btn-outline" onClick={() => window.location.href = '/'}>
              <Home size={14} /> Início
            </button>
          </div>
        </div>
      </div>
    )
  }

  const parseNum = (val: unknown, fallback: number = 0): number => {
    if (val === undefined || val === null) return fallback
    const n = typeof val === 'number' ? val : parseFloat(String(val))
    return isNaN(n) ? fallback : n
  }

  const spendableValue = parseNum(doc.spendable_balance, parseNum(doc.total_balance, 0))
  const salaryValue = parseNum(doc.salary_value, parseNum(doc.monthly_income, 0))
  const committedValue = parseNum(doc.committed_value, parseNum(doc.monthly_expense, 0))
  const committedPercent = parseNum(doc.committed_percentage, salaryValue > 0 ? (committedValue / salaryValue) * 100 : 0)
  const isSpendableNegative = spendableValue < 0
  const pendingSuggestions = (doc.suggestions || []).filter(s => s.status === 'pending')

  return (
    <div className="container">
      {/* Success Notification */}
      {successMessage && (
        <div style={{
          background: 'var(--accent-subtle)',
          border: '1px solid var(--border-active)',
          borderRadius: 'var(--radius-md)',
          padding: '12px 16px',
          marginBottom: 16,
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          fontSize: 13,
          color: 'var(--text-primary)'
        }}>
          <CheckCircle2 size={18} color="var(--accent)" />
          <span>{successMessage}</span>
        </div>
      )}

      {/* Header */}
      <div style={{ marginBottom: 20 }}>
        {/* Top bar com Badge e Botões de Controle */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <div className="live-badge">
            <div className="live-dot" />
            Ao Vivo
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button 
              className="btn btn-outline" 
              onClick={() => setIsPrivacyMode(!isPrivacyMode)}
              style={{ width: 36, height: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-md)' }}
              title={isPrivacyMode ? 'Mostrar valores' : 'Ocultar valores'}
              aria-label="Alternar privacidade"
            >
              {isPrivacyMode ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
            <button 
              className="btn btn-outline" 
              onClick={handleShare} 
              style={{ width: 36, height: 36, padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-md)' }}
              title={copied ? 'Link copiado!' : 'Copiar link'}
              aria-label="Compartilhar"
            >
              {copied ? <CheckCircle2 size={16} color="var(--accent)" /> : <Share2 size={16} />}
            </button>
          </div>
        </div>

        {/* Título e Subtítulo sem quebras forçadas */}
        <div>
          <h1 className="header-title" style={{ fontSize: 24, fontWeight: 700, letterSpacing: -0.5, color: 'var(--text-primary)', marginBottom: 4 }}>
            {doc.title || 'Resumo Financeiro'}
          </h1>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
            {doc.month_label} • Atualizado em tempo real
          </p>
        </div>

        {/* Botão de Sugerir Lançamento */}
        <button 
          className="btn btn-primary"
          onClick={() => setIsModalOpen(true)}
          style={{ width: '100%', marginTop: 14, height: 42 }}
        >
          <Plus size={16} /> Sugerir Lançamento
        </button>
      </div>

      {/* 1. HERO CARD: DISPONÍVEL PARA GASTAR (Harmônico em Dark/Light Mode) */}
      <div className="card" style={{ marginBottom: 16, padding: '24px 20px', background: 'var(--bg-card)' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Wallet size={16} color="var(--accent)" />
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1.2, color: 'var(--accent)', textTransform: 'uppercase' }}>
              Disponível para Gastar
            </span>
          </div>
          <span style={{ fontSize: 11, fontWeight: 500, color: 'var(--text-muted)' }}>Livre no mês</span>
        </div>

        <div style={{ 
          fontSize: 36, 
          fontWeight: 700, 
          letterSpacing: -0.8, 
          color: isSpendableNegative && !isPrivacyMode ? 'var(--danger)' : 'var(--text-primary)',
          marginBottom: 16,
          fontVariantNumeric: 'tabular-nums'
        }}>
          {formatCurrency(spendableValue)}
        </div>

        {/* Budget Commitment Progress */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 12, marginBottom: 6 }}>
            <span style={{ color: 'var(--text-secondary)', fontWeight: 500 }}>Orçamento Comprometido</span>
            <span style={{ fontWeight: 700, color: 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
              {isPrivacyMode ? '•••' : `${Math.round(committedPercent)}%`}
            </span>
          </div>
          <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
            <div 
              style={{ 
                width: `${Math.min(committedPercent, 100)}%`, 
                height: '100%', 
                background: committedPercent > 90 ? 'var(--danger)' : 'var(--accent)', 
                borderRadius: 999,
                transition: 'width 300ms ease-out' 
              }} 
            />
          </div>
        </div>
      </div>

      {/* Income & Expense Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 20 }}>
        <div className="card" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingUp size={14} color="var(--success)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Receitas do Mês</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--success)', fontVariantNumeric: 'tabular-nums' }}>
            {formatCurrency(salaryValue)}
          </div>
        </div>

        <div className="card" style={{ padding: '16px 18px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
            <TrendingDown size={14} color="var(--danger)" />
            <span style={{ fontSize: 11, fontWeight: 600, color: 'var(--text-secondary)' }}>Despesas Comprometidas</span>
          </div>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--danger)', fontVariantNumeric: 'tabular-nums' }}>
            {formatCurrency(committedValue)}
          </div>
        </div>
      </div>

      {/* Pending Suggestions Waiting for Approval */}
      {pendingSuggestions.length > 0 && (
        <div className="card" style={{ marginBottom: 20, border: '1px solid var(--border-active)', background: 'var(--bg-surface)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
            <Clock size={16} color="var(--accent)" />
            <h2 style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--accent)' }}>
              Sugestões Enviadas ({pendingSuggestions.length} aguardando aprovação no app)
            </h2>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {pendingSuggestions.map(sug => {
              const isIncome = sug.type === 'income'
              return (
                <div 
                  key={sug.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '10px 14px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-sm)',
                  }}
                >
                  <div>
                    <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>{sug.title}</div>
                    <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{sug.category} • {sug.date} • Aguardando aprovação</div>
                  </div>
                  <div style={{ fontSize: 13, fontWeight: 700, color: isIncome ? 'var(--success)' : 'var(--text-primary)', fontVariantNumeric: 'tabular-nums' }}>
                    {isIncome ? '+' : '-'} {formatCurrency(sug.amount)}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Category Breakdown */}
      {doc.categories && doc.categories.length > 0 && (
        <div className="card" style={{ marginBottom: 20 }}>
          <h2 style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--text-muted)', marginBottom: 16 }}>
            Gastos por Categoria
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {doc.categories.map((cat, idx) => {
              const roundedPct = Math.round(cat.percentage || 0)
              return (
                <div key={idx}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 13, marginBottom: 6 }}>
                    <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{cat.name}</span>
                    <span style={{ fontWeight: 600, color: 'var(--text-secondary)', fontVariantNumeric: 'tabular-nums' }}>
                      {formatCurrency(cat.amount)} <span style={{ color: 'var(--text-muted)', fontSize: 12, fontWeight: 500 }}>({roundedPct}%)</span>
                    </span>
                  </div>
                  <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
                    <div 
                      style={{ 
                        width: `${Math.min(cat.percentage, 100)}%`, 
                        height: '100%', 
                        background: 'var(--accent)', 
                        borderRadius: 999 
                      }} 
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Recent Transactions List */}
      {doc.transactions && doc.transactions.length > 0 && (
        <div>
          <h2 style={{ fontSize: 12, fontWeight: 700, textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--text-muted)', marginBottom: 12 }}>
            Últimas Movimentações
          </h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {doc.transactions.map((tx) => {
              const isIncome = tx.type?.toUpperCase() === 'INCOME'
              return (
                <div 
                  key={tx.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 12,
                    padding: '12px 14px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderLeft: isIncome ? '3px solid var(--success)' : '3px solid var(--danger)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1 }}>
                    <div style={{ 
                      width: 32, 
                      height: 32, 
                      flexShrink: 0,
                      borderRadius: 'var(--radius-sm)', 
                      background: isIncome ? 'var(--success-subtle)' : 'var(--danger-subtle)',
                      border: `1px solid ${isIncome ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)'}`,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}>
                      {isIncome ? <ArrowDownLeft size={16} color="var(--success)" /> : <ArrowUpRight size={16} color="var(--danger)" />}
                    </div>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div style={{ 
                        fontSize: 13, 
                        fontWeight: 600, 
                        color: 'var(--text-primary)',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        {tx.title}
                      </div>
                      <div style={{ 
                        fontSize: 11, 
                        color: 'var(--text-muted)', 
                        marginTop: 2,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        <span style={{ fontWeight: 500, color: 'var(--text-secondary)' }}>{tx.category}</span>
                        {tx.date ? ` • ${formatDate(tx.date)}` : ''}
                      </div>
                    </div>
                  </div>

                  <div style={{ 
                    fontSize: 13, 
                    fontWeight: 700, 
                    color: isIncome ? 'var(--success)' : 'var(--danger)',
                    whiteSpace: 'nowrap',
                    flexShrink: 0,
                    textAlign: 'right',
                    fontVariantNumeric: 'tabular-nums'
                  }}>
                    {isIncome ? '+ ' : '- '}{formatCurrency(tx.amount)}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Suggestion Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>Sugerir Lançamento</h2>
              <button 
                className="btn btn-outline" 
                onClick={() => setIsModalOpen(false)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32 }}
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleSuggestTransaction} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Type Switcher */}
              <div>
                <label className="input-label">Tipo de Lançamento</label>
                <div className="segmented-control">
                  <button 
                    type="button"
                    className={`segmented-btn ${suggestionType === 'expense' ? 'active' : ''}`}
                    onClick={() => setSuggestionType('expense')}
                  >
                    💸 Despesa
                  </button>
                  <button 
                    type="button"
                    className={`segmented-btn ${suggestionType === 'income' ? 'active' : ''}`}
                    onClick={() => setSuggestionType('income')}
                  >
                    💰 Receita
                  </button>
                </div>
              </div>

              {/* Title */}
              <div>
                <label className="input-label">Descrição / Título</label>
                <input 
                  type="text"
                  className="input-field"
                  placeholder="Ex: Jantar, Supermercado, Freela, Farmácia..."
                  value={suggestionTitle}
                  onChange={e => setSuggestionTitle(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Amount & Category */}
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label className="input-label">Valor (R$)</label>
                  <input 
                    type="number"
                    step="0.01"
                    min="0.01"
                    placeholder="0,00"
                    className="input-field"
                    value={suggestionAmount}
                    onChange={e => setSuggestionAmount(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label className="input-label">Categoria</label>
                  <select 
                    className="input-field"
                    value={suggestionCategory}
                    onChange={e => setSuggestionCategory(e.target.value)}
                    style={{ cursor: 'pointer' }}
                  >
                    <option value="Alimentação">Alimentação</option>
                    <option value="Mercado">Mercado</option>
                    <option value="Transporte">Transporte</option>
                    <option value="Moradia">Moradia</option>
                    <option value="Lazer">Lazer</option>
                    <option value="Saúde">Saúde</option>
                    <option value="Educação">Educação</option>
                    <option value="Salário">Salário</option>
                    <option value="Outros">Outros</option>
                  </select>
                </div>
              </div>

              <div style={{ 
                background: 'var(--bg-surface)', 
                border: '1px solid var(--border)', 
                borderRadius: 'var(--radius-sm)', 
                padding: '10px 14px', 
                fontSize: 12, 
                color: 'var(--text-muted)',
                lineHeight: 1.4
              }}>
                ℹ️ Esta transação será enviada como uma sugestão para o aplicativo do Kenned e só será efetivada após aprovação no celular.
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
                <button 
                  type="button" 
                  className="btn btn-outline" 
                  onClick={() => setIsModalOpen(false)}
                  style={{ flex: 1 }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={!suggestionTitle.trim() || !suggestionAmount || isSubmitting}
                  style={{ flex: 1 }}
                >
                  {isSubmitting ? 'Enviando...' : 'Enviar Sugestão'}
                </button>
              </div>
            </form>
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

