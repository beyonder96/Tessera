import React, { useState, useEffect } from 'react'
import { 
  Sparkles, 
  ShoppingCart, 
  TrendingUp, 
  ArrowRight, 
  Trash2, 
  Link2, 
  AlertCircle, 
  Smartphone, 
  Shield, 
  Clock 
} from 'lucide-react'
import { 
  getRecentItems, 
  removeRecentItem, 
  clearRecentItems, 
  RecentItem 
} from '../utils/recentStorage'

interface HomePageProps {
  onNavigate: (type: 'market' | 'finance', id: string) => void
}

function parseInputLink(input: string): { type: 'market' | 'finance'; id: string } | null {
  const trimmed = input.trim()
  if (!trimmed) return null

  // 1. Detect market link/path
  const marketMatch = trimmed.match(/(?:market\/|[?&](?:listId|marketId)=)([^/?&#\s]+)/i)
  if (marketMatch && marketMatch[1]) {
    return { type: 'market', id: decodeURIComponent(marketMatch[1]) }
  }

  // 2. Detect finance link/path
  const financeMatch = trimmed.match(/(?:finance\/|[?&](?:financeId|dashboardId)=)([^/?&#\s]+)/i)
  if (financeMatch && financeMatch[1]) {
    return { type: 'finance', id: decodeURIComponent(financeMatch[1]) }
  }

  // 3. Fallback: ID direto (UUID ou alfanumérico)
  if (/^[a-zA-Z0-9_-]{6,}$/.test(trimmed)) {
    return { type: 'market', id: trimmed }
  }

  return null
}

function formatRelativeTime(timestamp: number): string {
  const diffMs = Date.now() - timestamp
  const diffMinutes = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMinutes / 60)
  const diffDays = Math.floor(diffHours / 24)

  if (diffMinutes < 1) return 'Agora há pouco'
  if (diffMinutes < 60) return `Há ${diffMinutes} min`
  if (diffHours < 24) return `Há ${diffHours}h`
  if (diffDays === 1) return 'Ontem'
  return `Há ${diffDays} dias`
}

export const HomePage: React.FC<HomePageProps> = ({ onNavigate }) => {
  const [recents, setRecents] = useState<RecentItem[]>([])
  const [inputValue, setInputValue] = useState('')
  const [inputError, setInputError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    setRecents(getRecentItems())
  }, [])

  const handleOpenItem = (type: 'market' | 'finance', id: string) => {
    onNavigate(type, id)
  }

  const handleRemove = (e: React.MouseEvent, type: 'market' | 'finance', id: string) => {
    e.stopPropagation()
    removeRecentItem(type, id)
    setRecents(getRecentItems())
  }

  const handleClearAll = () => {
    clearRecentItems()
    setRecents([])
  }

  const handleSubmitLink = (e: React.FormEvent) => {
    e.preventDefault()
    setInputError(null)

    const parsed = parseInputLink(inputValue)
    if (!parsed) {
      setInputError('Link ou código inválido. Cole a URL gerada pelo app Tessera.')
      return
    }

    setIsSubmitting(true)
    setTimeout(() => {
      onNavigate(parsed.type, parsed.id)
    }, 150)
  }

  return (
    <div className="container" style={{ paddingTop: 40, paddingBottom: 60, maxWidth: 560 }}>
      {/* Header Minimalista */}
      <div style={{ textAlign: 'center', marginBottom: 32 }}>
        <div 
          style={{ 
            width: 56, 
            height: 56, 
            borderRadius: '50%', 
            background: 'var(--accent-subtle)', 
            border: '1px solid var(--border-active)', 
            display: 'inline-flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            marginBottom: 16
          }}
        >
          <Sparkles size={28} color="var(--accent)" />
        </div>
        <h1 className="header-title" style={{ fontSize: 24, fontWeight: 600, marginBottom: 8 }}>
          Tessera Live Web
        </h1>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
          Sincronização em tempo real de listas de compras e resumos financeiros.
        </p>
      </div>

      {/* Seção 1: Acessos Recentes */}
      {recents.length > 0 ? (
        <div style={{ marginBottom: 28 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
            <span style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: 0.6 }}>
              Acessos Recentes
            </span>
            <button 
              type="button"
              onClick={handleClearAll}
              className="btn btn-outline"
              style={{ fontSize: 11, padding: '4px 8px', height: 26, border: 'none', background: 'transparent', color: 'var(--text-muted)' }}
            >
              Limpar tudo
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {recents.map((item) => {
              const isMarket = item.type === 'market'
              return (
                <div 
                  key={`${item.type}-${item.id}`}
                  onClick={() => handleOpenItem(item.type, item.id)}
                  className="card"
                  style={{ 
                    padding: '14px 16px', 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'space-between',
                    cursor: 'pointer',
                    transition: 'all var(--transition)'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, minWidth: 0 }}>
                    <div 
                      style={{ 
                        width: 36, 
                        height: 36, 
                        borderRadius: 'var(--radius-sm)', 
                        background: 'var(--bg-surface)', 
                        border: '1px solid var(--border)',
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center',
                        flexShrink: 0
                      }}
                    >
                      {isMarket ? (
                        <ShoppingCart size={18} color="var(--accent)" />
                      ) : (
                        <TrendingUp size={18} color="var(--accent)" />
                      )}
                    </div>

                    <div style={{ minWidth: 0 }}>
                      <div 
                        style={{ 
                          fontSize: 14, 
                          fontWeight: 500, 
                          color: 'var(--text-primary)', 
                          whiteSpace: 'nowrap', 
                          overflow: 'hidden', 
                          textOverflow: 'ellipsis' 
                        }}
                      >
                        {item.title}
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 2 }}>
                        <span 
                          style={{ 
                            fontSize: 10, 
                            fontWeight: 600, 
                            color: 'var(--accent)', 
                            background: 'var(--accent-subtle)',
                            padding: '1px 6px',
                            borderRadius: 'var(--radius-full)'
                          }}
                        >
                          {isMarket ? 'Mercado' : 'Finanças'}
                        </span>
                        <span style={{ fontSize: 11, color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: 4 }}>
                          <Clock size={11} /> {formatRelativeTime(item.updatedAt)}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                    <button 
                      type="button"
                      onClick={(e) => handleRemove(e, item.type, item.id)}
                      className="btn btn-outline"
                      style={{ width: 32, height: 32, padding: 0, border: 'none', background: 'transparent', color: 'var(--text-muted)' }}
                      title="Remover do histórico"
                      aria-label="Remover item"
                    >
                      <Trash2 size={15} />
                    </button>
                    <div 
                      style={{ 
                        width: 32, 
                        height: 32, 
                        borderRadius: 'var(--radius-sm)', 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center',
                        background: 'var(--bg-surface)'
                      }}
                    >
                      <ArrowRight size={16} color="var(--accent)" />
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      ) : null}

      {/* Seção 2: Acesso por Link ou ID */}
      <div className="card" style={{ padding: '20px 20px', marginBottom: 24 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
          <Link2 size={16} color="var(--accent)" />
          <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
            Acessar com Link Compartilhado
          </span>
        </div>

        <form onSubmit={handleSubmitLink}>
          <div style={{ marginBottom: 12 }}>
            <input 
              type="text"
              className="input-field"
              placeholder="Cole o link ou ID da lista..."
              value={inputValue}
              onChange={(e) => {
                setInputValue(e.target.value)
                if (inputError) setInputError(null)
              }}
              style={{ fontSize: 13 }}
            />
          </div>

          {inputError && (
            <div 
              style={{ 
                display: 'flex', 
                alignItems: 'center', 
                gap: 8, 
                fontSize: 12, 
                color: 'var(--danger)', 
                marginBottom: 12 
              }}
            >
              <AlertCircle size={14} />
              <span>{inputError}</span>
            </div>
          )}

          <button 
            type="submit"
            className="btn btn-primary"
            disabled={!inputValue.trim() || isSubmitting}
            style={{ 
              width: '100%', 
              padding: '10px 16px', 
              fontSize: 13,
              opacity: (!inputValue.trim() || isSubmitting) ? 0.45 : 1,
              cursor: (!inputValue.trim() || isSubmitting) ? 'not-allowed' : 'pointer'
            }}
          >
            {isSubmitting ? 'Abrindo...' : 'Abrir Lista ou Resumo'}
          </button>
        </form>
      </div>

      {/* Seção 3: Instruções / Estado Vazio */}
      {recents.length === 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <div 
            style={{ 
              background: 'var(--bg-surface)', 
              border: '1px solid var(--border)', 
              borderRadius: 'var(--radius-md)', 
              padding: 16,
              textAlign: 'left' 
            }}
          >
            <Smartphone size={18} color="var(--accent)" style={{ marginBottom: 8 }} />
            <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>App Android</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Gere um link no menu de Finanças ou Mercado do aplicativo.
            </div>
          </div>

          <div 
            style={{ 
              background: 'var(--bg-surface)', 
              border: '1px solid var(--border)', 
              borderRadius: 'var(--radius-md)', 
              padding: 16,
              textAlign: 'left' 
            }}
          >
            <Shield size={18} color="var(--accent)" style={{ marginBottom: 8 }} />
            <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 4 }}>Tempo Real</div>
            <div style={{ fontSize: 11, color: 'var(--text-muted)', lineHeight: 1.4 }}>
              Atualização contínua sem necessidade de recarregar a tela.
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
