import React, { useEffect, useState, useMemo } from 'react'
import { supabase } from '../supabaseClient'
import { 
  Sparkles, 
  Plus, 
  ExternalLink, 
  CheckCircle2, 
  Circle, 
  Trash2, 
  Edit3, 
  Search, 
  Home, 
  Share2, 
  Sun, 
  Moon, 
  Download, 
  AlertCircle, 
  RefreshCw, 
  X, 
  Check, 
  ShoppingBag,
  Image as ImageIcon,
  Heart
} from 'lucide-react'
import { useTheme } from '../hooks/useTheme'
import { usePwaInstall } from '../hooks/usePwaInstall'
import { PwaInstructionsModal } from '../components/PwaInstructionsModal'
import { saveRecentItem } from '../utils/recentStorage'

export interface SharedWishItem {
  id: string | number
  title: string
  targetValue: number
  currentValue?: number
  imageUrl?: string
  buyUrl?: string
  category?: string
  priorityClassification?: 'Baixa' | 'Moderado' | 'Alta' | 'Urgente' | string
  isBought?: boolean
  created_at?: number
  created_by?: string
}

export interface SharedWishesHubDoc {
  id: string
  title: string
  items: SharedWishItem[]
  created_at: string
  updated_at: string
}

const CATEGORIES = [
  'Todas',
  'Eletrônicos',
  'Casa',
  'Roupas',
  'Viagem',
  'Beleza',
  'Games',
  'Livros',
  'Geral'
]

const PRIORITIES: Array<{ label: string; value: string; color: string }> = [
  { label: 'Baixa', value: 'Baixa', color: '#64748B' },
  { label: 'Moderado', value: 'Moderado', color: '#2DD4BF' },
  { label: 'Alta', value: 'Alta', color: '#F59E0B' },
  { label: 'Urgente', value: 'Urgente', color: '#EF4444' }
]

function formatCurrency(val: number): string {
  return new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
  }).format(val || 0)
}

export const WishSharePage: React.FC<{ hubId: string }> = ({ hubId }) => {
  const [hub, setHub] = useState<SharedWishesHubDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  // Filters
  const [activeTab, setActiveTab] = useState<'all' | 'active' | 'bought'>('all')
  const [selectedCategory, setSelectedCategory] = useState('Todas')
  const [searchQuery, setSearchQuery] = useState('')

  // Theme & PWA
  const { theme, toggleTheme } = useTheme()
  const { isInstalled, installApp, showHelpModal, setShowHelpModal, isIos } = usePwaInstall()

  // Modals & Feedback
  const [isAddModalOpen, setIsAddModalOpen] = useState(false)
  const [editingItem, setEditingItem] = useState<SharedWishItem | null>(null)
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Form State
  const [formTitle, setFormTitle] = useState('')
  const [formPrice, setFormPrice] = useState('')
  const [formBuyUrl, setFormBuyUrl] = useState('')
  const [formImageUrl, setFormImageUrl] = useState('')
  const [formCategory, setFormCategory] = useState('Eletrônicos')
  const [formPriority, setFormPriority] = useState('Moderado')

  // Reset form
  const resetForm = () => {
    setFormTitle('')
    setFormPrice('')
    setFormBuyUrl('')
    setFormImageUrl('')
    setFormCategory('Eletrônicos')
    setFormPriority('Moderado')
  }

  // Load initial Hub
  useEffect(() => {
    async function loadHub(isInitial = false) {
      if (isInitial) {
        setLoading(true)
        setError(null)
      }
      try {
        const { data, error: sbError } = await supabase
          .from('shared_wishes_hub')
          .select('*')
          .eq('id', hubId)
          .single()

        if (sbError && sbError.code !== 'PGRST116') {
          throw sbError
        }

        if (data) {
          const doc = data as SharedWishesHubDoc
          setHub(doc)
          localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(doc))
          saveRecentItem({ type: 'wishes', id: hubId, title: doc.title || 'Lista de Desejos' })
        } else {
          // Se ainda não existir no Supabase, inicializa vazio e cria
          const initialDoc: SharedWishesHubDoc = {
            id: hubId,
            title: 'Lista de Desejos',
            items: [],
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString()
          }
          setHub(initialDoc)
          await supabase.from('shared_wishes_hub').insert([initialDoc])
          saveRecentItem({ type: 'wishes', id: hubId, title: initialDoc.title })
        }
      } catch (err: unknown) {
        console.error('Erro ao carregar lista de desejos:', err)
        const cached = localStorage.getItem(`tessera_wishes_${hubId}`)
        if (cached && isInitial) {
          try {
            const cachedDoc = JSON.parse(cached) as SharedWishesHubDoc
            setHub(cachedDoc)
            saveRecentItem({ type: 'wishes', id: hubId, title: cachedDoc.title })
            setError(null)
            setLoading(false)
            return
          } catch {
            // ignore
          }
        }
        if (isInitial) {
          const msg = err instanceof Error ? err.message : 'Não foi possível carregar a lista de desejos.'
          setError(msg)
        }
      } finally {
        if (isInitial) {
          setLoading(false)
        }
      }
    }

    loadHub(true)

    // Realtime Supabase
    const channel = supabase
      .channel(`wishes-hub-${hubId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'shared_wishes_hub',
          filter: `id=eq.${hubId}`
        },
        (payload) => {
          if (payload.new && typeof payload.new === 'object') {
            const updated = payload.new as unknown as SharedWishesHubDoc
            setHub(updated)
            localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(updated))
          }
        }
      )
      .subscribe()

    // Polling a cada 5s quando visível
    const pollInterval = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadHub(false)
      }
    }, 5000)

    return () => {
      clearInterval(pollInterval)
      supabase.removeChannel(channel)
    }
  }, [hubId])

  const handleShare = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(window.location.href)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // Adicionar novo desejo - SEM NECESSIDADE DE APROVAÇÃO
  const handleAddWish = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!hub || !formTitle.trim()) return

    setIsSubmitting(true)
    const cleanPrice = parseFloat(formPrice.replace(',', '.')) || 0

    const newWish: SharedWishItem = {
      id: `wish_${Date.now()}`,
      title: formTitle.trim(),
      targetValue: cleanPrice,
      currentValue: 0,
      imageUrl: formImageUrl.trim() || undefined,
      buyUrl: formBuyUrl.trim() || undefined,
      category: formCategory,
      priorityClassification: formPriority,
      isBought: false,
      created_by: 'Web',
      created_at: Date.now()
    }

    const currentItems = hub.items || []
    const updatedItems = [newWish, ...currentItems]
    const updatedDoc: SharedWishesHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_wishes_hub').upsert(updatedDoc)
      setFeedbackMessage('Desejo adicionado com sucesso!')
      resetForm()
      setIsAddModalOpen(false)
      setTimeout(() => setFeedbackMessage(null), 4000)
    } catch (err) {
      console.error('Erro ao salvar desejo:', err)
      setFeedbackMessage('Erro ao salvar no servidor. Salvo localmente.')
    } finally {
      setIsSubmitting(false)
    }
  }

  // Abrir modal de edição
  const openEditModal = (item: SharedWishItem) => {
    setEditingItem(item)
    setFormTitle(item.title)
    setFormPrice(item.targetValue ? String(item.targetValue) : '')
    setFormBuyUrl(item.buyUrl || '')
    setFormImageUrl(item.imageUrl || '')
    setFormCategory(item.category || 'Geral')
    setFormPriority(item.priorityClassification || 'Moderado')
  }

  // Salvar edição
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!hub || !editingItem || !formTitle.trim()) return

    setIsSubmitting(true)
    const cleanPrice = parseFloat(formPrice.replace(',', '.')) || 0

    const updatedItems = (hub.items || []).map((item) => {
      if (item.id === editingItem.id) {
        return {
          ...item,
          title: formTitle.trim(),
          targetValue: cleanPrice,
          buyUrl: formBuyUrl.trim() || undefined,
          imageUrl: formImageUrl.trim() || undefined,
          category: formCategory,
          priorityClassification: formPriority
        }
      }
      return item
    })

    const updatedDoc: SharedWishesHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_wishes_hub').upsert(updatedDoc)
      setFeedbackMessage('Desejo atualizado!')
      setEditingItem(null)
      resetForm()
      setTimeout(() => setFeedbackMessage(null), 3500)
    } catch (err) {
      console.error('Erro ao editar desejo:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  // Alternar status de Comprado / Planejando
  const handleToggleBought = async (itemId: string | number) => {
    if (!hub) return

    const updatedItems = (hub.items || []).map((item) => {
      if (item.id === itemId) {
        const nextBought = !item.isBought
        return {
          ...item,
          isBought: nextBought,
          currentValue: nextBought ? item.targetValue : 0
        }
      }
      return item
    })

    const updatedDoc: SharedWishesHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_wishes_hub').upsert(updatedDoc)
    } catch (err) {
      console.error('Erro ao atualizar status de compra:', err)
    }
  }

  // Deletar desejo
  const handleDelete = async (itemId: string | number) => {
    if (!hub) return
    if (!window.confirm('Tem certeza que deseja excluir este desejo?')) return

    const updatedItems = (hub.items || []).filter((item) => item.id !== itemId)
    const updatedDoc: SharedWishesHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_wishes_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_wishes_hub').upsert(updatedDoc)
      setFeedbackMessage('Desejo excluído.')
      setTimeout(() => setFeedbackMessage(null), 3000)
    } catch (err) {
      console.error('Erro ao excluir desejo:', err)
    }
  }

  // Items calculados e filtrados
  const items = hub?.items || []

  const stats = useMemo(() => {
    const total = items.length
    const bought = items.filter((i) => i.isBought).length
    const active = total - bought
    const totalActiveValue = items
      .filter((i) => !i.isBought)
      .reduce((sum, i) => sum + (Number(i.targetValue) || 0), 0)
    const totalBoughtValue = items
      .filter((i) => i.isBought)
      .reduce((sum, i) => sum + (Number(i.targetValue) || 0), 0)

    return { total, bought, active, totalActiveValue, totalBoughtValue }
  }, [items])

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      // Filtro de aba
      if (activeTab === 'active' && item.isBought) return false
      if (activeTab === 'bought' && !item.isBought) return false

      // Filtro de categoria
      if (selectedCategory !== 'Todas' && item.category !== selectedCategory) {
        return false
      }

      // Filtro de busca
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase()
        const titleMatch = item.title.toLowerCase().includes(query)
        const catMatch = (item.category || '').toLowerCase().includes(query)
        return titleMatch || catMatch
      }

      return true
    })
  }, [items, activeTab, selectedCategory, searchQuery])

  if (loading) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: 80 }}>
        <RefreshCw size={36} className="spin" color="var(--accent)" style={{ margin: '0 auto 16px' }} />
        <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Carregando lista de desejos...</p>
      </div>
    )
  }

  if (error && !hub) {
    return (
      <div className="container" style={{ textAlign: 'center', paddingTop: 80 }}>
        <div 
          style={{ 
            width: 48, 
            height: 48, 
            borderRadius: '50%', 
            background: 'var(--danger-subtle)', 
            display: 'inline-flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            marginBottom: 16
          }}
        >
          <AlertCircle size={24} color="var(--danger)" />
        </div>
        <h2 style={{ fontSize: 18, marginBottom: 8 }}>Não foi possível carregar</h2>
        <p style={{ color: 'var(--text-secondary)', fontSize: 13, marginBottom: 24 }}>{error}</p>
        <button type="button" className="btn btn-primary" onClick={() => window.location.reload()}>
          Tentar novamente
        </button>
      </div>
    )
  }

  return (
    <div className="container" style={{ paddingTop: 20, paddingBottom: 80, maxWidth: 600 }}>
      {/* Header Bar */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <a 
          href="/" 
          className="btn btn-outline" 
          style={{ width: 36, height: 36, padding: 0, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-full)' }}
          title="Voltar ao Início"
        >
          <Home size={16} color="var(--text-secondary)" />
        </a>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {!isInstalled && (
            <button 
              type="button" 
              className="btn btn-outline" 
              onClick={installApp}
              style={{ padding: '6px 12px', fontSize: 11, height: 34, borderRadius: 'var(--radius-full)', gap: 6, display: 'inline-flex', alignItems: 'center' }}
            >
              <Download size={13} color="var(--accent)" />
              <span>Instalar</span>
            </button>
          )}

          <button 
            type="button" 
            className="btn btn-outline" 
            onClick={handleShare}
            style={{ width: 36, height: 36, padding: 0, display: 'inline-flex', alignItems: 'center', justifyContent: 'center', borderRadius: 'var(--radius-full)' }}
            title="Compartilhar Link"
          >
            {copied ? <Check size={16} color="var(--accent)" /> : <Share2 size={16} color="var(--text-secondary)" />}
          </button>

          <button 
            type="button" 
            className="theme-toggle-btn" 
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Ativar modo claro' : 'Ativar modo escuro'}
          >
            {theme === 'dark' ? <Sun size={16} color="#F59E0B" /> : <Moon size={16} color="#4A90E2" />}
          </button>
        </div>
      </header>

      {/* Título Principal & Subtítulo */}
      <div style={{ textAlign: 'center', marginBottom: 20 }}>
        <div 
          style={{ 
            width: 48, 
            height: 48, 
            borderRadius: '50%', 
            background: 'var(--accent-subtle)', 
            border: '1px solid var(--border-active)', 
            display: 'inline-flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            marginBottom: 12
          }}
        >
          <Heart size={24} color="var(--accent)" />
        </div>
        <h1 className="header-title" style={{ fontSize: 22, fontWeight: 600, marginBottom: 4 }}>
          Lista de Desejos
        </h1>
        <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          Planejamento de compras e metas compartilhadas em tempo real.
        </p>
      </div>

      {/* Banner de Feedback */}
      {feedbackMessage && (
        <div 
          style={{ 
            padding: '10px 14px', 
            borderRadius: 'var(--radius-md)', 
            background: 'var(--accent-subtle)', 
            border: '1px solid var(--border-active)', 
            color: 'var(--text-primary)',
            fontSize: 12,
            marginBottom: 16,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Sparkles size={16} color="var(--accent)" />
            <span>{feedbackMessage}</span>
          </div>
          <button 
            type="button" 
            onClick={() => setFeedbackMessage(null)}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={14} />
          </button>
        </div>
      )}

      {/* Estatísticas Rápidas (Cards de Métrica) */}
      <div 
        style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(3, 1fr)', 
          gap: 8, 
          marginBottom: 20 
        }}
      >
        <div className="card" style={{ padding: '12px 10px', textAlign: 'center' }}>
          <span style={{ fontSize: 10, color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, letterSpacing: 0.5 }}>
            Ativos
          </span>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--accent)', marginTop: 2 }}>
            {stats.active}
          </div>
          <div style={{ fontSize: 10, color: 'var(--text-secondary)', marginTop: 2 }}>
            {formatCurrency(stats.totalActiveValue)}
          </div>
        </div>

        <div className="card" style={{ padding: '12px 10px', textAlign: 'center' }}>
          <span style={{ fontSize: 10, color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, letterSpacing: 0.5 }}>
            Realizados
          </span>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--success)', marginTop: 2 }}>
            {stats.bought}
          </div>
          <div style={{ fontSize: 10, color: 'var(--text-secondary)', marginTop: 2 }}>
            {formatCurrency(stats.totalBoughtValue)}
          </div>
        </div>

        <div className="card" style={{ padding: '12px 10px', textAlign: 'center' }}>
          <span style={{ fontSize: 10, color: 'var(--text-muted)', textTransform: 'uppercase', fontWeight: 600, letterSpacing: 0.5 }}>
            Total
          </span>
          <div style={{ fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', marginTop: 2 }}>
            {stats.total}
          </div>
          <div style={{ fontSize: 10, color: 'var(--text-secondary)', marginTop: 2 }}>
            itens
          </div>
        </div>
      </div>

      {/* Botão de Adição Rápida */}
      <button 
        type="button"
        className="btn btn-primary"
        onClick={() => {
          resetForm()
          setIsAddModalOpen(true)
        }}
        style={{ 
          width: '100%', 
          height: 44, 
          marginBottom: 18, 
          borderRadius: 'var(--radius-md)', 
          fontSize: 14, 
          fontWeight: 600,
          gap: 8,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: 'var(--shadow-sm)'
        }}
      >
        <Plus size={18} />
        <span>Adicionar Novo Desejo</span>
      </button>

      {/* Barra de Pesquisa */}
      <div style={{ position: 'relative', marginBottom: 12 }}>
        <Search size={16} color="var(--text-muted)" style={{ position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)' }} />
        <input 
          type="text"
          placeholder="Pesquisar produto ou categoria..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="input"
          style={{ 
            paddingLeft: 38, 
            height: 40, 
            fontSize: 13, 
            borderRadius: 'var(--radius-full)' 
          }}
        />
        {searchQuery && (
          <button 
            type="button"
            onClick={() => setSearchQuery('')}
            style={{ 
              position: 'absolute', 
              right: 12, 
              top: '50%', 
              transform: 'translateY(-50%)', 
              background: 'none', 
              border: 'none', 
              color: 'var(--text-muted)',
              cursor: 'pointer'
            }}
          >
            <X size={14} />
          </button>
        )}
      </div>

      {/* Tabs de Filtro: Todos / Planejando / Comprados */}
      <div 
        style={{ 
          display: 'flex', 
          background: 'var(--bg-surface)', 
          borderRadius: 'var(--radius-full)', 
          padding: 4, 
          marginBottom: 14,
          border: '1px solid var(--border)'
        }}
      >
        <button 
          type="button"
          onClick={() => setActiveTab('all')}
          style={{ 
            flex: 1, 
            padding: '7px 0', 
            borderRadius: 'var(--radius-full)', 
            border: 'none', 
            fontSize: 12, 
            fontWeight: 600,
            cursor: 'pointer',
            background: activeTab === 'all' ? 'var(--bg-card)' : 'transparent',
            color: activeTab === 'all' ? 'var(--accent)' : 'var(--text-muted)',
            boxShadow: activeTab === 'all' ? 'var(--shadow-sm)' : 'none',
            transition: 'all 0.15s ease'
          }}
        >
          Todos ({stats.total})
        </button>
        <button 
          type="button"
          onClick={() => setActiveTab('active')}
          style={{ 
            flex: 1, 
            padding: '7px 0', 
            borderRadius: 'var(--radius-full)', 
            border: 'none', 
            fontSize: 12, 
            fontWeight: 600,
            cursor: 'pointer',
            background: activeTab === 'active' ? 'var(--bg-card)' : 'transparent',
            color: activeTab === 'active' ? 'var(--accent)' : 'var(--text-muted)',
            boxShadow: activeTab === 'active' ? 'var(--shadow-sm)' : 'none',
            transition: 'all 0.15s ease'
          }}
        >
          Planejando ({stats.active})
        </button>
        <button 
          type="button"
          onClick={() => setActiveTab('bought')}
          style={{ 
            flex: 1, 
            padding: '7px 0', 
            borderRadius: 'var(--radius-full)', 
            border: 'none', 
            fontSize: 12, 
            fontWeight: 600,
            cursor: 'pointer',
            background: activeTab === 'bought' ? 'var(--bg-card)' : 'transparent',
            color: activeTab === 'bought' ? 'var(--success)' : 'var(--text-muted)',
            boxShadow: activeTab === 'bought' ? 'var(--shadow-sm)' : 'none',
            transition: 'all 0.15s ease'
          }}
        >
          Conquistados ({stats.bought})
        </button>
      </div>

      {/* Filtro Horizontal de Categorias */}
      <div 
        style={{ 
          display: 'flex', 
          gap: 6, 
          overflowX: 'auto', 
          paddingBottom: 10, 
          marginBottom: 16,
          scrollbarWidth: 'none'
        }}
      >
        {CATEGORIES.map((cat) => {
          const isSelected = selectedCategory === cat
          return (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              style={{
                padding: '5px 12px',
                borderRadius: 'var(--radius-full)',
                border: isSelected ? '1px solid var(--border-active)' : '1px solid var(--border)',
                background: isSelected ? 'var(--accent-subtle)' : 'var(--bg-surface)',
                color: isSelected ? 'var(--accent)' : 'var(--text-secondary)',
                fontSize: 11,
                fontWeight: isSelected ? 600 : 400,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
                transition: 'all 0.15s ease'
              }}
            >
              {cat}
            </button>
          )
        })}
      </div>

      {/* Lista de Cards de Desejos */}
      {filteredItems.length === 0 ? (
        <div 
          className="card" 
          style={{ 
            padding: '40px 20px', 
            textAlign: 'center', 
            background: 'var(--bg-card)',
            marginTop: 10
          }}
        >
          <ShoppingBag size={40} color="var(--text-muted)" style={{ margin: '0 auto 12px', opacity: 0.6 }} />
          <h3 style={{ fontSize: 15, fontWeight: 600, marginBottom: 6 }}>Nenhum desejo encontrado</h3>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 20 }}>
            {searchQuery || selectedCategory !== 'Todas' || activeTab !== 'all'
              ? 'Tente ajustar os filtros ou termo de busca.'
              : 'Clique no botão acima para cadastrar o primeiro desejo!'}
          </p>
          <button 
            type="button"
            className="btn btn-outline"
            onClick={() => {
              resetForm()
              setIsAddModalOpen(true)
            }}
            style={{ fontSize: 13, padding: '8px 16px', borderRadius: 'var(--radius-full)' }}
          >
            Adicionar Desejo
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {filteredItems.map((item) => {
            const isBought = item.isBought === true
            const priorityInfo = PRIORITIES.find((p) => p.value === item.priorityClassification) || PRIORITIES[1]

            return (
              <div 
                key={item.id}
                className="card interactive-card"
                style={{
                  padding: 14,
                  display: 'flex',
                  gap: 14,
                  alignItems: 'flex-start',
                  background: isBought ? 'var(--bg-surface)' : 'var(--bg-card)',
                  opacity: isBought ? 0.75 : 1,
                  transition: 'all 0.2s ease',
                  border: isBought ? '1px solid var(--border)' : '1px solid var(--border)'
                }}
              >
                {/* Imagem do Produto ou Fallback */}
                <div 
                  style={{ 
                    width: 72, 
                    height: 72, 
                    borderRadius: 'var(--radius-sm)', 
                    overflow: 'hidden', 
                    background: 'var(--bg-surface)', 
                    border: '1px solid var(--border)',
                    flexShrink: 0,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    position: 'relative'
                  }}
                >
                  {item.imageUrl ? (
                    <img 
                      src={item.imageUrl} 
                      alt={item.title}
                      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      onError={(e) => {
                        // Fallback em caso de link quebrado
                        e.currentTarget.style.display = 'none'
                      }}
                    />
                  ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                      <ImageIcon size={22} color="var(--text-muted)" />
                    </div>
                  )}
                </div>

                {/* Conteúdo Central */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 4, flexWrap: 'wrap' }}>
                    {item.category && (
                      <span 
                        style={{ 
                          fontSize: 10, 
                          fontWeight: 600, 
                          padding: '1px 6px', 
                          borderRadius: 'var(--radius-full)', 
                          background: 'var(--bg-surface)', 
                          color: 'var(--text-secondary)',
                          border: '1px solid var(--border)'
                        }}
                      >
                        {item.category}
                      </span>
                    )}
                    <span 
                      style={{ 
                        fontSize: 10, 
                        fontWeight: 600, 
                        padding: '1px 6px', 
                        borderRadius: 'var(--radius-full)', 
                        background: `${priorityInfo.color}15`, 
                        color: priorityInfo.color,
                        border: `1px solid ${priorityInfo.color}30`
                      }}
                    >
                      {item.priorityClassification || 'Moderado'}
                    </span>
                    {isBought && (
                      <span 
                        style={{ 
                          fontSize: 10, 
                          fontWeight: 600, 
                          padding: '1px 6px', 
                          borderRadius: 'var(--radius-full)', 
                          background: 'var(--success-subtle)', 
                          color: 'var(--success)' 
                        }}
                      >
                        Conquistado 🎉
                      </span>
                    )}
                  </div>

                  <h3 
                    style={{ 
                      fontSize: 14, 
                      fontWeight: 600, 
                      color: isBought ? 'var(--text-muted)' : 'var(--text-primary)', 
                      textDecoration: isBought ? 'line-through' : 'none',
                      marginBottom: 6,
                      lineHeight: 1.3
                    }}
                  >
                    {item.title}
                  </h3>

                  {/* Preço e Botão de Loja */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                    <div style={{ fontSize: 13, fontWeight: 700, color: isBought ? 'var(--text-muted)' : 'var(--accent)' }}>
                      {formatCurrency(item.targetValue)}
                    </div>

                    {item.buyUrl && (
                      <a 
                        href={item.buyUrl.startsWith('http') ? item.buyUrl : `https://${item.buyUrl}`}
                        target="_blank" 
                        rel="noopener noreferrer"
                        style={{ 
                          fontSize: 11, 
                          color: 'var(--accent)', 
                          display: 'inline-flex', 
                          alignItems: 'center', 
                          gap: 4,
                          textDecoration: 'none',
                          fontWeight: 500
                        }}
                      >
                        <span>Ver na loja</span>
                        <ExternalLink size={12} />
                      </a>
                    )}
                  </div>
                </div>

                {/* Ações Laterais */}
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                  <button 
                    type="button"
                    onClick={() => handleToggleBought(item.id)}
                    title={isBought ? 'Marcar como não comprado' : 'Marcar como comprado'}
                    style={{ 
                      background: 'none', 
                      border: 'none', 
                      cursor: 'pointer', 
                      padding: 4,
                      color: isBought ? 'var(--success)' : 'var(--text-muted)'
                    }}
                  >
                    {isBought ? <CheckCircle2 size={22} /> : <Circle size={22} />}
                  </button>

                  <button 
                    type="button"
                    onClick={() => openEditModal(item)}
                    title="Editar desejo"
                    style={{ 
                      background: 'none', 
                      border: 'none', 
                      cursor: 'pointer', 
                      padding: 4,
                      color: 'var(--text-muted)'
                    }}
                  >
                    <Edit3 size={15} />
                  </button>

                  <button 
                    type="button"
                    onClick={() => handleDelete(item.id)}
                    title="Excluir desejo"
                    style={{ 
                      background: 'none', 
                      border: 'none', 
                      cursor: 'pointer', 
                      padding: 4,
                      color: 'var(--text-muted)'
                    }}
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* MODAL 1: Adicionar Novo Desejo (SEM NECESSIDADE DE APROVAÇÃO) */}
      {isAddModalOpen && (
        <div 
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.7)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 16
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget) setIsAddModalOpen(false)
          }}
        >
          <div 
            className="card"
            style={{
              width: '100%',
              maxWidth: 480,
              padding: 24,
              borderRadius: 'var(--radius-lg)',
              background: 'var(--bg-card)',
              boxShadow: 'var(--shadow-lg)',
              maxHeight: '90vh',
              overflowY: 'auto'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Sparkles size={20} color="var(--accent)" />
                <h2 style={{ fontSize: 17, fontWeight: 600 }}>Novo Desejo</h2>
              </div>
              <button 
                type="button"
                onClick={() => setIsAddModalOpen(false)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleAddWish} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Nome do Produto / Desejo *
                </label>
                <input 
                  type="text"
                  required
                  placeholder="Ex: Tênis Nike, Fone Bluetooth, Cafeteira..."
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                  autoFocus
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Valor Estimado (R$)
                  </label>
                  <input 
                    type="number"
                    step="0.01"
                    placeholder="0,00"
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    className="input"
                    style={{ width: '100%', height: 42, fontSize: 13 }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Prioridade
                  </label>
                  <select 
                    value={formPriority}
                    onChange={(e) => setFormPriority(e.target.value)}
                    className="input"
                    style={{ width: '100%', height: 42, fontSize: 13 }}
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p.value} value={p.value}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Categoria
                </label>
                <select 
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                >
                  {CATEGORIES.filter((c) => c !== 'Todas').map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Link do Produto (Opcional)
                </label>
                <input 
                  type="url"
                  placeholder="https://loja.com/produto"
                  value={formBuyUrl}
                  onChange={(e) => setFormBuyUrl(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Link da Foto / Imagem (Opcional)
                </label>
                <input 
                  type="url"
                  placeholder="https://exemplo.com/foto.jpg"
                  value={formImageUrl}
                  onChange={(e) => setFormImageUrl(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                />
              </div>

              {/* Preview da foto se digitada */}
              {formImageUrl.trim() && (
                <div style={{ textAlign: 'center', marginTop: 4 }}>
                  <img 
                    src={formImageUrl.trim()} 
                    alt="Preview" 
                    style={{ maxHeight: 110, maxWidth: '100%', borderRadius: 'var(--radius-sm)', objectFit: 'contain', border: '1px solid var(--border)' }}
                    onError={(e) => {
                      e.currentTarget.style.display = 'none'
                    }}
                  />
                </div>
              )}

              <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
                <button 
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setIsAddModalOpen(false)}
                  style={{ flex: 1, height: 42, borderRadius: 'var(--radius-md)' }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit"
                  disabled={isSubmitting || !formTitle.trim()}
                  className="btn btn-primary"
                  style={{ flex: 1, height: 42, borderRadius: 'var(--radius-md)', fontWeight: 600 }}
                >
                  {isSubmitting ? 'Salvando...' : 'Adicionar Desejo'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 2: Editar Desejo Existente */}
      {editingItem && (
        <div 
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0, 0, 0, 0.7)',
            backdropFilter: 'blur(4px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 16
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget) setEditingItem(null)
          }}
        >
          <div 
            className="card"
            style={{
              width: '100%',
              maxWidth: 480,
              padding: 24,
              borderRadius: 'var(--radius-lg)',
              background: 'var(--bg-card)',
              boxShadow: 'var(--shadow-lg)',
              maxHeight: '90vh',
              overflowY: 'auto'
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 18 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <Edit3 size={18} color="var(--accent)" />
                <h2 style={{ fontSize: 17, fontWeight: 600 }}>Editar Desejo</h2>
              </div>
              <button 
                type="button"
                onClick={() => setEditingItem(null)}
                style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveEdit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Nome do Produto / Desejo *
                </label>
                <input 
                  type="text"
                  required
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Valor Estimado (R$)
                  </label>
                  <input 
                    type="number"
                    step="0.01"
                    value={formPrice}
                    onChange={(e) => setFormPrice(e.target.value)}
                    className="input"
                    style={{ width: '100%', height: 42, fontSize: 13 }}
                  />
                </div>

                <div>
                  <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                    Prioridade
                  </label>
                  <select 
                    value={formPriority}
                    onChange={(e) => setFormPriority(e.target.value)}
                    className="input"
                    style={{ width: '100%', height: 42, fontSize: 13 }}
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p.value} value={p.value}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Categoria
                </label>
                <select 
                  value={formCategory}
                  onChange={(e) => setFormCategory(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                >
                  {CATEGORIES.filter((c) => c !== 'Todas').map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Link do Produto (Opcional)
                </label>
                <input 
                  type="url"
                  value={formBuyUrl}
                  onChange={(e) => setFormBuyUrl(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                />
              </div>

              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--text-secondary)', display: 'block', marginBottom: 6 }}>
                  Link da Foto / Imagem (Opcional)
                </label>
                <input 
                  type="url"
                  value={formImageUrl}
                  onChange={(e) => setFormImageUrl(e.target.value)}
                  className="input"
                  style={{ width: '100%', height: 42, fontSize: 13 }}
                />
              </div>

              {formImageUrl.trim() && (
                <div style={{ textAlign: 'center', marginTop: 4 }}>
                  <img 
                    src={formImageUrl.trim()} 
                    alt="Preview" 
                    style={{ maxHeight: 110, maxWidth: '100%', borderRadius: 'var(--radius-sm)', objectFit: 'contain', border: '1px solid var(--border)' }}
                    onError={(e) => {
                      e.currentTarget.style.display = 'none'
                    }}
                  />
                </div>
              )}

              <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
                <button 
                  type="button"
                  className="btn btn-outline"
                  onClick={() => setEditingItem(null)}
                  style={{ flex: 1, height: 42, borderRadius: 'var(--radius-md)' }}
                >
                  Cancelar
                </button>
                <button 
                  type="submit"
                  disabled={isSubmitting || !formTitle.trim()}
                  className="btn btn-primary"
                  style={{ flex: 1, height: 42, borderRadius: 'var(--radius-md)', fontWeight: 600 }}
                >
                  {isSubmitting ? 'Salvando...' : 'Salvar Alterações'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* PWA Modal de Instruções */}
      {showHelpModal && (
        <PwaInstructionsModal isOpen={showHelpModal} isIos={isIos} onClose={() => setShowHelpModal(false)} />
      )}
    </div>
  )
}
