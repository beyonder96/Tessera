import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { ShoppingCart, RefreshCw, AlertCircle, Share2, Home } from 'lucide-react'

interface MarketItem {
  id: number
  name: string
  isChecked: boolean
  isBought: boolean
  price: number
  quantity: number
  unit: string
  category: string
}

interface MarketListDoc {
  id: string
  title: string
  items: MarketItem[]
  updated_at: string
}

export const MarketSharePage: React.FC<{ listId: string }> = ({ listId }) => {
  const [list, setList] = useState<MarketListDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  // Fetch initial list
  useEffect(() => {
    async function loadList() {
      setLoading(true)
      setError(null)
      try {
        const { data, error: sbError } = await supabase
          .from('shared_market_lists')
          .select('*')
          .eq('id', listId)
          .single()

        if (sbError) {
          throw sbError
        }

        if (data) {
          setList(data)
          localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(data))
        }
      } catch (err: any) {
        console.error('Error fetching market list:', err)
        // Check localStorage cache as fallback
        const cached = localStorage.getItem(`tessera_market_${listId}`)
        if (cached) {
          try {
            setList(JSON.parse(cached))
            setError(null)
            setLoading(false)
            return
          } catch (e) {
            // ignore
          }
        }
        setError(err.message || 'Lista de compras não encontrada ou o link expirou.')
      } finally {
        setLoading(false)
      }
    }

    loadList()

    // Realtime Subscription
    const channel = supabase
      .channel(`market-list-${listId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'shared_market_lists',
          filter: `id=eq.${listId}`,
        },
        (payload: any) => {
          if (payload.new) {
            setList(payload.new as MarketListDoc)
            localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(payload.new))
          }
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [listId])

  // Toggle item check
  const toggleItem = async (index: number) => {
    if (!list) return
    const updatedItems = [...list.items]
    updatedItems[index].isChecked = !updatedItems[index].isChecked
    
    // Optimistic UI update
    const updatedDoc = { ...list, items: updatedItems, updated_at: new Date().toISOString() }
    setList(updatedDoc)
    localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_market_lists')
        .update({ items: updatedItems, updated_at: new Date().toISOString() })
        .eq('id', listId)
    } catch (err) {
      console.error('Failed to update item:', err)
    }
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
          <div className="skeleton" style={{ width: 140, height: 28 }} />
          <div className="skeleton" style={{ width: 100, height: 24, borderRadius: 999 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 120, borderRadius: 20, marginBottom: 24 }} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="skeleton" style={{ width: '100%', height: 60, borderRadius: 14 }} />
          ))}
        </div>
      </div>
    )
  }

  if (error || !list) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: 80 }}>
        <div className="card" style={{ maxWidth: 460, margin: '0 auto', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
          <div style={{ width: 48, height: 48, borderRadius: '50%', background: 'var(--danger-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <AlertCircle size={24} color="var(--danger)" />
          </div>
          <h2 style={{ fontSize: 18, fontWeight: 700 }}>Lista não encontrada</h2>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
            O identificador da lista (<code>{listId}</code>) não foi localizado no servidor.
          </p>
          <div style={{ background: 'var(--bg-surface)', padding: '12px 16px', borderRadius: 'var(--radius-sm)', width: '100%', textAlign: 'left', fontSize: 12, color: 'var(--text-muted)' }}>
            💡 <strong>Dica:</strong> Abra o aplicativo Tessera no celular, acesse a aba <em>Mercado</em> e toque em <em>Compartilhar / Gerar Link</em> para atualizar a sincronização.
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

  const totalItems = list.items.length
  const checkedItems = list.items.filter(i => i.isChecked)
  const checkedCount = checkedItems.length
  const cartTotal = checkedItems.reduce((acc, item) => acc + (item.price * (item.quantity || 1)), 0)
  const progressPercent = totalItems > 0 ? (checkedCount / totalItems) * 100 : 0

  return (
    <div className="container">
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <div className="live-badge" style={{ marginBottom: 8 }}>
            <div className="live-dot" />
            Ao Vivo
          </div>
          <h1 className="header-title">{list.title || 'Lista de Compras'}</h1>
          <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
            Sincronizado em tempo real com o Tessera
          </p>
        </div>

        <button className="btn btn-outline" onClick={handleShare} style={{ padding: '8px 12px' }}>
          <Share2 size={14} />
          {copied ? 'Copiado!' : 'Compartilhar'}
        </button>
      </div>

      {/* Progress & Total Card */}
      <div className="card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1, color: 'var(--accent)', textTransform: 'uppercase' }}>
              Total no Carrinho
            </span>
            <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--text-primary)', marginTop: 2 }}>
              {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(cartTotal)}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Itens Pegos</span>
            <div style={{ fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', marginTop: 2 }}>
              {checkedCount} / {totalItems}
            </div>
          </div>
        </div>

        {/* Progress Bar */}
        <div style={{ width: '100%', height: 6, background: 'var(--bg-surface-hover)', borderRadius: 999, overflow: 'hidden' }}>
          <div 
            style={{ 
              width: `${progressPercent}%`, 
              height: '100%', 
              background: 'var(--accent)', 
              borderRadius: 999, 
              transition: 'width 250ms ease-out' 
            }} 
          />
        </div>
      </div>

      {/* Items List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {list.items.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--text-secondary)' }}>
            <ShoppingCart size={32} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
            <p style={{ fontSize: 14 }}>Nenhum item na lista no momento.</p>
          </div>
        ) : (
          list.items.map((item, idx) => (
            <div 
              key={item.id || idx}
              onClick={() => toggleItem(idx)}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '14px 16px',
                background: item.isChecked ? 'var(--bg-surface)' : 'var(--bg-card)',
                border: '1px solid var(--border)',
                borderColor: item.isChecked ? 'transparent' : 'var(--border)',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
                transition: 'all var(--transition)',
                opacity: item.isChecked ? 0.65 : 1
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <input 
                  type="checkbox"
                  checked={item.isChecked}
                  onChange={() => {}} // Controlled via card click
                  className="custom-checkbox"
                />
                <div>
                  <div style={{ 
                    fontSize: 14, 
                    fontWeight: 600, 
                    color: 'var(--text-primary)',
                    textDecoration: item.isChecked ? 'line-through' : 'none' 
                  }}>
                    {item.name}
                  </div>
                  <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                    {item.quantity} {item.unit || 'un'} {item.category ? `• ${item.category}` : ''}
                  </div>
                </div>
              </div>

              {item.price > 0 && (
                <div style={{ fontSize: 13, fontWeight: 700, color: item.isChecked ? 'var(--text-muted)' : 'var(--text-primary)' }}>
                  {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(item.price * (item.quantity || 1))}
                </div>
              )}
            </div>
          ))
        )}
      </div>

      {/* Footer Branding */}
      <div style={{ textAlign: 'center', marginTop: 40, marginBottom: 20, fontSize: 11, color: 'var(--text-muted)' }}>
        Desenvolvido por <strong style={{ color: 'var(--text-primary)' }}>Tessera</strong> • Conexão Web
      </div>
    </div>
  )
}
