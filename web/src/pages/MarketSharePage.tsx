import React, { useEffect, useState } from 'react'
import { supabase } from '../supabaseClient'
import { ShoppingCart, RefreshCw, AlertCircle, Share2, Home, Plus, X } from 'lucide-react'

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

  // Add Item Modal state
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)
  const [newItemName, setNewItemName] = useState('')
  const [newItemQty, setNewItemQty] = useState('1')
  const [newItemUnit, setNewItemUnit] = useState('un')
  const [newItemPrice, setNewItemPrice] = useState('')
  const [newItemCategory, setNewItemCategory] = useState('Geral')
  const [isSavingItem, setIsSavingItem] = useState(false)

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

  // Toggle item check
  const toggleItem = async (index: number) => {
    if (!list) return
    const updatedItems = [...list.items]
    updatedItems[index] = {
      ...updatedItems[index],
      isChecked: !updatedItems[index].isChecked,
    }

    const updatedDoc: MarketListDoc = { ...list, items: updatedItems, updated_at: new Date().toISOString() }
    setList(updatedDoc)
    localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_market_lists')
        .update({ items: updatedItems, updated_at: new Date().toISOString() })
        .eq('id', listId)
    } catch (err: unknown) {
      console.error('Failed to update item:', err)
    }
  }

  // Add new item from Web
  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!list || !newItemName.trim()) return

    setIsSavingItem(true)
    const qty = parseFloat(newItemQty) || 1
    const price = parseFloat(newItemPrice.replace(',', '.')) || 0

    const newItem: MarketItem = {
      id: Date.now(),
      name: newItemName.trim(),
      isChecked: true, // Default checked in cart when added at the market
      isBought: false,
      price,
      quantity: qty,
      unit: newItemUnit,
      category: newItemCategory || 'Geral',
    }

    const updatedItems = [...list.items, newItem]
    const updatedDoc: MarketListDoc = { ...list, items: updatedItems, updated_at: new Date().toISOString() }

    // Optimistic local update
    setList(updatedDoc)
    localStorage.setItem(`tessera_market_${listId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_market_lists')
        .update({ items: updatedItems, updated_at: new Date().toISOString() })
        .eq('id', listId)

      // Reset form
      setNewItemName('')
      setNewItemQty('1')
      setNewItemPrice('')
      setNewItemCategory('Geral')
      setIsAddModalOpen(false)
    } catch (err: unknown) {
      console.error('Failed to add item:', err)
    } finally {
      setIsSavingItem(false)
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

        <div style={{ display: 'flex', gap: 8 }}>
          <button 
            className="btn btn-primary" 
            onClick={() => setIsAddModalOpen(true)}
            style={{ padding: '8px 14px' }}
          >
            <Plus size={16} /> Adicionar Item
          </button>
          <button className="btn btn-outline" onClick={handleShare} style={{ padding: '8px 12px' }}>
            <Share2 size={14} />
            {copied ? 'Copiado!' : 'Compartilhar'}
          </button>
        </div>
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
            <p style={{ fontSize: 14, marginBottom: 16 }}>Nenhum item na lista no momento.</p>
            <button className="btn btn-primary" onClick={() => setIsAddModalOpen(true)}>
              <Plus size={16} /> Adicionar primeiro item
            </button>
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

      {/* Add Item Modal */}
      {isAddModalOpen && (
        <div className="modal-overlay" onClick={() => setIsAddModalOpen(false)}>
          <div className="modal-content" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)' }}>Adicionar ao Carrinho</h2>
              <button 
                className="btn btn-outline" 
                onClick={() => setIsAddModalOpen(false)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32 }}
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleAddItem} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <div>
                <label className="input-label">Nome do Produto</label>
                <input 
                  type="text"
                  className="input-field"
                  placeholder="Ex: Leite Desnatado, Café, Maçã..."
                  value={newItemName}
                  onChange={e => setNewItemName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
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
                  <div className="segmented-control">
                    <button 
                      type="button"
                      className={`segmented-btn ${newItemUnit === 'un' ? 'active' : ''}`}
                      onClick={() => setNewItemUnit('un')}
                    >
                      Unidade (un)
                    </button>
                    <button 
                      type="button"
                      className={`segmented-btn ${newItemUnit === 'kg' ? 'active' : ''}`}
                      onClick={() => setNewItemUnit('kg')}
                    >
                      Quilo (kg)
                    </button>
                  </div>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label className="input-label">Preço Unitário (R$)</label>
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

                <div>
                  <label className="input-label">Categoria</label>
                  <select 
                    className="input-field"
                    value={newItemCategory}
                    onChange={e => setNewItemCategory(e.target.value)}
                    style={{ cursor: 'pointer' }}
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
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 8 }}>
                <button 
                  type="button" 
                  className="btn btn-outline" 
                  onClick={() => setIsAddModalOpen(false)}
                  style={{ flex: 1 }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={!newItemName.trim() || isSavingItem}
                  style={{ flex: 1 }}
                >
                  {isSavingItem ? 'Adicionando...' : 'Adicionar Item'}
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

