import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { 
  ShoppingCart, 
  RefreshCw, 
  AlertCircle, 
  Share2, 
  Home, 
  Plus, 
  Sun, 
  Moon, 
  Download, 
  Clock, 
  CheckCircle2, 
  ArrowRight,
  RotateCcw,
  Sparkles
} from 'lucide-react'

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export interface MarketItem {
  id: number
  name: string
  isChecked: boolean
  isBought: boolean
  price: number
  quantity: number
  unit: string
  category: string
  inMarket?: boolean
  needsApproval?: boolean
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

  // Theme state
  const [theme, setTheme] = useState<'dark' | 'light'>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('tessera_theme')
      if (saved === 'light' || saved === 'dark') return saved
    }
    return 'dark'
  })

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('tessera_theme', theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme(prev => (prev === 'dark' ? 'light' : 'dark'))
  }

  // PWA Install state
  const [installPrompt, setInstallPrompt] = useState<BeforeInstallPromptEvent | null>(null)
  const [isInstalled, setIsInstalled] = useState(false)

  useEffect(() => {
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault()
      setInstallPrompt(e as BeforeInstallPromptEvent)
    }

    const handleAppInstalled = () => {
      setIsInstalled(true)
      setInstallPrompt(null)
    }

    window.addEventListener('beforeinstallprompt', handleBeforeInstall)
    window.addEventListener('appinstalled', handleAppInstalled)

    return () => {
      window.removeEventListener('beforeinstallprompt', handleBeforeInstall)
      window.removeEventListener('appinstalled', handleAppInstalled)
    }
  }, [])

  const handleInstallApp = async () => {
    if (!installPrompt) return
    await installPrompt.prompt()
    const { outcome } = await installPrompt.userChoice
    if (outcome === 'accepted') {
      setIsInstalled(true)
    }
    setInstallPrompt(null)
  }

  // Inline Add Item Form state (Zero Modal)
  const [newItemName, setNewItemName] = useState('')
  const [newItemQty, setNewItemQty] = useState('1')
  const [newItemUnit, setNewItemUnit] = useState('un')
  const [newItemPrice, setNewItemPrice] = useState('')
  const [newItemCategory, setNewItemCategory] = useState('Geral')
  const [isSavingItem, setIsSavingItem] = useState(false)

  // Fetch initial list & Realtime
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

        if (sbError) throw sbError

        if (data) {
          setList(data as MarketListDoc)
          localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(data))
        }
      } catch (err: unknown) {
        console.error('Error fetching market list:', err)
        const cached = localStorage.getItem(`tessera_market_${listId}`)
        if (cached) {
          try {
            setList(JSON.parse(cached) as MarketListDoc)
            setError(null)
            setLoading(false)
            return
          } catch {
            // ignore
          }
        }
        const message = err instanceof Error ? err.message : 'Lista de compras não encontrada ou o link expirou.'
        setError(message)
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
        (payload) => {
          if (payload.new && typeof payload.new === 'object') {
            const newDoc = payload.new as unknown as MarketListDoc
            setList(newDoc)
            localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(newDoc))
          }
        }
      )
      .subscribe()

    return () => {
      supabase.removeChannel(channel)
    }
  }, [listId])

  // Helper para persistir alterações no Supabase e no cache local
  const persistItems = async (updatedItems: MarketItem[]) => {
    if (!list) return
    const updatedDoc: MarketListDoc = { ...list, items: updatedItems, updated_at: new Date().toISOString() }
    setList(updatedDoc)
    localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_market_lists')
        .update({ items: updatedItems, updated_at: new Date().toISOString() })
        .eq('id', listId)
    } catch (err: unknown) {
      console.error('Erro ao sincronizar com Supabase:', err)
    }
  }

  // 1. Adicionar item inline: entra direto no PLANEJAMENTO (inMarket: false, isChecked: false)
  const handleAddInlineItem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!list || !newItemName.trim() || isSavingItem) return

    setIsSavingItem(true)
    const qty = parseFloat(newItemQty) || 1
    const price = parseFloat(newItemPrice.replace(',', '.')) || 0

    const newItem: MarketItem = {
      id: Date.now(),
      name: newItemName.trim(),
      isChecked: false,
      isBought: false,
      price,
      quantity: qty,
      unit: newItemUnit,
      category: newItemCategory || 'Geral',
      inMarket: false,       // Inicia sempre no planejamento!
      needsApproval: false,
    }

    const updatedItems = [...list.items, newItem]
    await persistItems(updatedItems)

    // Reset formulário mantendo unidade e categoria
    setNewItemName('')
    setNewItemQty('1')
    setNewItemPrice('')
    setIsSavingItem(false)
  }

  // 2. Ticar item no Planejamento: move para o Mercado com pendência de aprovação no app
  const handleSendToMarket = async (itemToMove: MarketItem) => {
    if (!list) return
    const updatedItems = list.items.map(item => {
      if (item.id === itemToMove.id) {
        return {
          ...item,
          inMarket: true,
          needsApproval: true, // Requer aprovação no app antes de entrar no carrinho
          isChecked: false,    // Fora do carrinho oficial até aprovar
        }
      }
      return item
    })
    await persistItems(updatedItems)
  }

  // 3. Desticar item do Mercado: traz de volta para o planejamento
  const handleReturnToPlanning = async (itemToReturn: MarketItem) => {
    if (!list) return
    const updatedItems = list.items.map(item => {
      if (item.id === itemToReturn.id) {
        return {
          ...item,
          inMarket: false,
          needsApproval: false,
          isChecked: false,
        }
      }
      return item
    })
    await persistItems(updatedItems)
  }

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // Subtotal calculado em tempo real no formulário
  const parsedQty = parseFloat(newItemQty) || 0
  const parsedPrice = parseFloat(newItemPrice.replace(',', '.')) || 0
  const estimatedSubtotal = parsedQty * parsedPrice

  if (loading) {
    return (
      <div className="container" style={{ paddingTop: 40 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <div className="skeleton" style={{ width: 140, height: 28 }} />
          <div className="skeleton" style={{ width: 100, height: 24, borderRadius: 999 }} />
        </div>
        <div className="skeleton" style={{ width: '100%', height: 160, borderRadius: 20, marginBottom: 24 }} />
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
            💡 <strong>Dica:</strong> Abra o aplicativo Tessera no celular, acesse a aba <em>Mercado</em> e toque no botão de compartilhar para gerar um link atualizado.
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

  // Divisão dos itens: Planejamento vs Mercado
  const planningItems = list.items.filter(i => !i.inMarket && !i.isBought)
  const marketItems = list.items.filter(i => i.inMarket && !i.isBought)

  const planningTotal = planningItems.reduce((acc, item) => acc + (item.price * (item.quantity || 1)), 0)
  const marketTotal = marketItems.reduce((acc, item) => acc + (item.price * (item.quantity || 1)), 0)

  return (
    <div className="container">
      {/* Top Bar: Live status, Theme Toggle, Install PWA, Share */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
        <div>
          <div className="live-badge" style={{ marginBottom: 8 }}>
            <div className="live-dot" />
            Ao Vivo
          </div>
          <h1 className="header-title">{list.title || 'Lista de Mercado'}</h1>
          <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 2 }}>
            Sincronizado em tempo real com o app Tessera
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {/* PWA Install Button (se disponível no navegador) */}
          {installPrompt && !isInstalled && (
            <button 
              className="btn btn-outline" 
              onClick={handleInstallApp}
              title="Instalar como aplicativo no celular ou desktop"
              style={{ padding: '8px 12px', fontSize: 12, gap: 6 }}
            >
              <Download size={14} color="var(--accent)" />
              <span>Instalar App</span>
            </button>
          )}

          {/* Theme Toggle Button */}
          <button 
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Mudar para modo claro' : 'Mudar para modo escuro'}
          >
            {theme === 'dark' ? (
              <Sun size={17} color="#F59E0B" className="theme-icon-enter" />
            ) : (
              <Moon size={17} color="#4A90E2" className="theme-icon-enter" />
            )}
          </button>

          {/* Share Link Button */}
          <button className="btn btn-outline" onClick={handleShare} style={{ padding: '8px 12px' }}>
            <Share2 size={14} />
            {copied ? 'Copiado!' : 'Compartilhar'}
          </button>
        </div>
      </div>

      {/* Overview Totals Card */}
      <div className="card" style={{ marginBottom: 20, padding: '16px 20px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
          <div>
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 0.8, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              No Planejamento ({planningItems.length})
            </span>
            <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginTop: 2 }}>
              {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(planningTotal)}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 0.8, color: 'var(--accent)', textTransform: 'uppercase' }}>
              No Mercado ({marketItems.length})
            </span>
            <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--accent)', marginTop: 2 }}>
              {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(marketTotal)}
            </div>
          </div>
        </div>
      </div>

      {/* FORMULÁRIO INLINE DIRETO NA TELA PRINCIPAL (ZERO MODAL) */}
      <div className="card" style={{ marginBottom: 28, background: 'var(--bg-card)', border: '1px solid var(--border)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 14 }}>
          <Sparkles size={15} color="var(--accent)" />
          <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: 1, textTransform: 'uppercase', color: 'var(--text-secondary)' }}>
            Adicionar ao Planejamento
          </span>
        </div>

        <form onSubmit={handleAddInlineItem} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {/* Nome do Produto */}
          <div>
            <input 
              type="text"
              className="input-field"
              placeholder="Nome do produto (ex: Leite, Café, Arroz...)"
              value={newItemName}
              onChange={e => setNewItemName(e.target.value)}
              required
            />
          </div>

          {/* Linha de Quantidade, Unidade e Preço Unitário */}
          <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 0.9fr 1fr', gap: 8 }}>
            <div>
              <label className="input-label">Quantidade</label>
              <input 
                type="number"
                step="any"
                min="0.01"
                className="input-field"
                value={newItemQty}
                onChange={e => setNewItemQty(e.target.value)}
                required
              />
            </div>

            <div>
              <label className="input-label">Unidade</label>
              <div className="segmented-control" style={{ height: 42, padding: 2 }}>
                <button 
                  type="button"
                  className={`segmented-btn ${newItemUnit === 'un' ? 'active' : ''}`}
                  onClick={() => setNewItemUnit('un')}
                  style={{ padding: '4px 6px', fontSize: 12 }}
                >
                  un
                </button>
                <button 
                  type="button"
                  className={`segmented-btn ${newItemUnit === 'kg' ? 'active' : ''}`}
                  onClick={() => setNewItemUnit('kg')}
                  style={{ padding: '4px 6px', fontSize: 12 }}
                >
                  kg
                </button>
              </div>
            </div>

            <div>
              <label className="input-label">Valor Unit. (R$)</label>
              <input 
                type="number"
                step="0.01"
                min="0"
                placeholder="0,00"
                className="input-field"
                value={newItemPrice}
                onChange={e => setNewItemPrice(e.target.value)}
              />
            </div>
          </div>

          {/* Categoria e Subtotal Calculado */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 4 }}>
            <div style={{ flex: 1, maxWidth: 220 }}>
              <select 
                className="input-field"
                value={newItemCategory}
                onChange={e => setNewItemCategory(e.target.value)}
                style={{ padding: '8px 12px', fontSize: 13, cursor: 'pointer' }}
              >
                <option value="Geral">Geral</option>
                <option value="Hortifrúti">Hortifrúti</option>
                <option value="Carnes & Frios">Carnes & Frios</option>
                <option value="Laticínios & Ovos">Laticínios & Ovos</option>
                <option value="Padaria">Padaria</option>
                <option value="Bebidas">Bebidas</option>
                <option value="Higiene & Limpeza">Higiene & Limpeza</option>
                <option value="Pet">Pet</option>
              </select>
            </div>

            {/* Subtotal estimado em tempo real */}
            <div style={{ textAlign: 'right', paddingLeft: 12 }}>
              <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Subtotal estimado:</span>
              <div style={{ fontSize: 14, fontWeight: 700, color: 'var(--text-primary)' }}>
                {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(estimatedSubtotal)}
              </div>
            </div>
          </div>

          {/* Botão de Adição Inline */}
          <button 
            type="submit" 
            className="btn btn-primary" 
            disabled={!newItemName.trim() || isSavingItem}
            style={{ width: '100%', marginTop: 6, height: 44 }}
          >
            <Plus size={16} />
            {isSavingItem ? 'Adicionando ao Planejamento...' : 'Adicionar ao Planejamento'}
          </button>
        </form>
      </div>

      {/* SEÇÃO 1: ITENS NO PLANEJAMENTO */}
      <div style={{ marginBottom: 32 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h2 style={{ fontSize: 14, fontWeight: 700, letterSpacing: 0.5, color: 'var(--text-primary)', textTransform: 'uppercase' }}>
              Planejamento
            </h2>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              ({planningItems.length} {planningItems.length === 1 ? 'item' : 'itens'})
            </span>
          </div>
          <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
            Toque para enviar ao mercado 👉
          </span>
        </div>

        {planningItems.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '32px 16px', color: 'var(--text-secondary)' }}>
            <ShoppingCart size={28} style={{ margin: '0 auto 8px', opacity: 0.35 }} />
            <p style={{ fontSize: 13 }}>Nenhum item pendente no planejamento.</p>
            <p style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>Adicione produtos no formulário acima para planejar as compras.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {planningItems.map((item) => {
              const itemTotal = (item.price || 0) * (item.quantity || 1)
              return (
                <div 
                  key={item.id}
                  onClick={() => handleSendToMarket(item)}
                  className="interactive-card"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-md)',
                  }}
                  title="Tocar para enviar este item para o mercado no app"
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <div 
                      className="custom-checkbox" 
                      style={{ pointerEvents: 'none' }}
                    />
                    <div>
                      <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>
                        {item.name}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        {item.quantity} {item.unit || 'un'} 
                        {item.price > 0 && ` × ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(item.price)}`}
                        {item.category && ` • ${item.category}`}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    {itemTotal > 0 && (
                      <div style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-primary)' }}>
                        {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(itemTotal)}
                      </div>
                    )}
                    <div 
                      style={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'center', 
                        width: 28, 
                        height: 28, 
                        borderRadius: '50%', 
                        background: 'var(--bg-surface)' 
                      }}
                    >
                      <ArrowRight size={14} color="var(--accent)" />
                    </div>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* SEÇÃO 2: ITENS ENVIADOS PARA O MERCADO */}
      <div style={{ marginBottom: 32 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <h2 style={{ fontSize: 14, fontWeight: 700, letterSpacing: 0.5, color: 'var(--accent)', textTransform: 'uppercase' }}>
              Enviados para o Mercado
            </h2>
            <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
              ({marketItems.length})
            </span>
          </div>
          <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>
            Entram no carrinho com sua aprovação no app
          </span>
        </div>

        {marketItems.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '24px 16px', color: 'var(--text-muted)', fontSize: 12 }}>
            Nenhum item marcado para o mercado no momento. Tique os itens do planejamento para enviá-los.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {marketItems.map((item) => {
              const itemTotal = (item.price || 0) * (item.quantity || 1)
              const isApproved = !item.needsApproval && item.isChecked

              return (
                <div 
                  key={item.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    background: isApproved ? 'var(--accent-subtle)' : 'var(--bg-surface)',
                    border: '1px solid',
                    borderColor: isApproved ? 'var(--border-active)' : 'var(--border)',
                    borderRadius: 'var(--radius-md)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    {isApproved ? (
                      <CheckCircle2 size={20} color="var(--accent)" />
                    ) : (
                      <Clock size={20} color="var(--text-muted)" />
                    )}
                    <div>
                      <div style={{ 
                        fontSize: 14, 
                        fontWeight: 600, 
                        color: 'var(--text-primary)',
                      }}>
                        {item.name}
                      </div>
                      <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                        {item.quantity} {item.unit || 'un'} 
                        {item.price > 0 && ` × ${new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(item.price)}`}
                        {' • '}
                        {isApproved ? (
                          <span style={{ color: 'var(--accent)', fontWeight: 600 }}>No carrinho do mercado</span>
                        ) : (
                          <span style={{ color: 'var(--text-secondary)' }}>Aguardando aprovação no app</span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    {itemTotal > 0 && (
                      <div style={{ fontSize: 13, fontWeight: 700, color: isApproved ? 'var(--accent)' : 'var(--text-primary)' }}>
                        {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(itemTotal)}
                      </div>
                    )}
                    <button 
                      onClick={() => handleReturnToPlanning(item)}
                      className="btn btn-outline"
                      style={{ padding: '6px 10px', fontSize: 11, height: 28, gap: 4 }}
                      title="Devolver item para o planejamento"
                    >
                      <RotateCcw size={12} />
                      Voltar
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </div>

      {/* Footer Branding */}
      <div style={{ textAlign: 'center', marginTop: 40, marginBottom: 20, fontSize: 11, color: 'var(--text-muted)' }}>
        Desenvolvido por <strong style={{ color: 'var(--text-primary)' }}>Tessera</strong> • Conexão Web PWA
      </div>
    </div>
  )
}
