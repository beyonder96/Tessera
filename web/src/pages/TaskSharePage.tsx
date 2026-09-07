import React, { useEffect, useState, useMemo } from 'react'
import { supabase } from '../supabaseClient'
import { 
  CheckCircle2, 
  Circle, 
  Clock, 
  AlertCircle, 
  Share2, 
  Home, 
  Plus, 
  Sun, 
  Moon, 
  Download, 
  RefreshCw, 
  Trash2, 
  Check, 
  Bell, 
  Calendar, 
  User, 
  X,
  MessageSquare
} from 'lucide-react'
import { useTheme } from '../hooks/useTheme'
import { usePwaInstall } from '../hooks/usePwaInstall'
import { PwaInstructionsModal } from '../components/PwaInstructionsModal'
import { saveRecentItem } from '../utils/recentStorage'

export interface SharedTaskItem {
  id: string
  title: string
  description?: string
  type: 'notice' | 'task'
  target_user: 'kenned' | 'me'
  due_date?: number
  due_time?: string
  status: 'pending' | 'approved' | 'completed' | 'dismissed'
  created_by: string
  created_at: number
  completed_at?: number
}

export interface SharedTasksHubDoc {
  id: string
  title: string
  items: SharedTaskItem[]
  created_at: string
  updated_at: string
}

export const TaskSharePage: React.FC<{ hubId: string }> = ({ hubId }) => {
  const [hub, setHub] = useState<SharedTasksHubDoc | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  // Filter state
  const [activeFilter, setActiveFilter] = useState<'all' | 'kenned' | 'me' | 'notices' | 'completed'>('all')

  // Theme & PWA
  const { theme, toggleTheme } = useTheme()
  const { isInstalled, installApp, showHelpModal, setShowHelpModal, isIos } = usePwaInstall()

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [itemType, setItemType] = useState<'notice' | 'task'>('notice')
  const [targetUser, setTargetUser] = useState<'kenned' | 'me'>('kenned')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [dueDate, setDueDate] = useState(() => {
    const today = new Date()
    const yyyy = today.getFullYear()
    const mm = String(today.getMonth() + 1).padStart(2, '0')
    const dd = String(today.getDate()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd}`
  })
  const [dueTime, setDueTime] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null)

  // Auto-detect @kenned in title
  const handleTitleChange = (val: string) => {
    setTitle(val)
    if (val.toLowerCase().includes('@kenned') && targetUser !== 'kenned') {
      setTargetUser('kenned')
    }
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
          .from('shared_tasks_hub')
          .select('*')
          .eq('id', hubId)
          .single()

        if (sbError && sbError.code !== 'PGRST116') {
          throw sbError
        }

        if (data) {
          const doc = data as SharedTasksHubDoc
          setHub(doc)
          localStorage.setItem(`tessera_tasks_${hubId}`, JSON.stringify(doc))
          saveRecentItem({ type: 'tasks', id: hubId, title: doc.title || 'Tarefas & Lembretes' })
        } else {
          // Se não existir, inicializa doc vazio
          const initialDoc: SharedTasksHubDoc = {
            id: hubId,
            title: 'Tarefas e Lembretes',
            items: [],
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString()
          }
          setHub(initialDoc)
          await supabase.from('shared_tasks_hub').insert([initialDoc])
          saveRecentItem({ type: 'tasks', id: hubId, title: initialDoc.title })
        }
      } catch (err: unknown) {
        console.error('Erro ao carregar hub de tarefas:', err)
        const cached = localStorage.getItem(`tessera_tasks_${hubId}`)
        if (cached && isInitial) {
          try {
            const cachedDoc = JSON.parse(cached) as SharedTasksHubDoc
            setHub(cachedDoc)
            saveRecentItem({ type: 'tasks', id: hubId, title: cachedDoc.title })
            setError(null)
            setLoading(false)
            return
          } catch {
            // ignore
          }
        }
        if (isInitial) {
          const msg = err instanceof Error ? err.message : 'Não foi possível carregar as tarefas.'
          setError(msg)
        }
      } finally {
        if (isInitial) {
          setLoading(false)
        }
      }
    }

    loadHub(true)

    // Sincronização em tempo real via Realtime Supabase
    const channel = supabase
      .channel(`tasks-hub-${hubId}`)
      .on(
        'postgres_changes',
        {
          event: '*',
          schema: 'public',
          table: 'shared_tasks_hub',
          filter: `id=eq.${hubId}`
        },
        (payload) => {
          if (payload.new && typeof payload.new === 'object') {
            const updated = payload.new as unknown as SharedTasksHubDoc
            setHub(updated)
            localStorage.setItem(`tessera_tasks_${hubId}`, JSON.stringify(updated))
          }
        }
      )
      .subscribe()

    // Polling a cada 5s quando a aba está visível
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

  // Create new task or notice
  const handleCreateItem = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!hub || !title.trim()) return

    setIsSubmitting(true)
    let parsedDateMillis: number | undefined
    if (itemType === 'task' && dueDate) {
      const [y, m, d] = dueDate.split('-').map(Number)
      if (y && m && d) {
        parsedDateMillis = new Date(y, m - 1, d, 12, 0, 0).getTime()
      }
    }

    const isKenned = targetUser === 'kenned'
    const newItem: SharedTaskItem = {
      id: `task_${Date.now()}`,
      title: title.trim(),
      description: description.trim() || undefined,
      type: itemType,
      target_user: targetUser,
      due_date: parsedDateMillis,
      due_time: dueTime.trim() || undefined,
      status: isKenned ? 'pending' : 'approved',
      created_by: 'Web',
      created_at: Date.now()
    }

    const currentItems = hub.items || []
    const updatedItems = [newItem, ...currentItems]
    const updatedDoc: SharedTasksHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_tasks_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase
        .from('shared_tasks_hub')
        .upsert(updatedDoc)

      setFeedbackMessage(
        isKenned 
          ? 'Aviso enviado para o Kenned com notificação prioritária!' 
          : 'Tarefa adicionada ao mural e enviada ao celular do Kenned!'
      )
      setTitle('')
      setDescription('')
      setDueTime('')
      setIsModalOpen(false)
      setTimeout(() => setFeedbackMessage(null), 4500)
    } catch (err: unknown) {
      console.error('Erro ao salvar item:', err)
    } finally {
      setIsSubmitting(false)
    }
  }

  // Toggle item completion
  const handleToggleComplete = async (itemId: string) => {
    if (!hub) return
    const updatedItems = hub.items.map(item => {
      if (item.id === itemId) {
        const isDone = item.status === 'completed'
        return {
          ...item,
          status: isDone ? (item.target_user === 'kenned' ? 'approved' : 'approved') : 'completed',
          completed_at: isDone ? undefined : Date.now()
        } as SharedTaskItem
      }
      return item
    })

    const updatedDoc: SharedTasksHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_tasks_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_tasks_hub').upsert(updatedDoc)
    } catch (err) {
      console.error('Erro ao atualizar item:', err)
    }
  }

  // Delete item
  const handleDeleteItem = async (itemId: string) => {
    if (!hub) return
    const updatedItems = hub.items.filter(item => item.id !== itemId)
    const updatedDoc: SharedTasksHubDoc = {
      ...hub,
      items: updatedItems,
      updated_at: new Date().toISOString()
    }

    setHub(updatedDoc)
    localStorage.setItem(`tessera_tasks_${hubId}`, JSON.stringify(updatedDoc))

    try {
      await supabase.from('shared_tasks_hub').upsert(updatedDoc)
    } catch (err) {
      console.error('Erro ao excluir item:', err)
    }
  }

  // Filtered items
  const filteredItems = useMemo(() => {
    if (!hub) return []
    const items = hub.items || []
    switch (activeFilter) {
      case 'kenned':
        return items.filter(i => i.target_user === 'kenned' && i.status !== 'completed')
      case 'me':
        return items.filter(i => i.target_user === 'me' && i.status !== 'completed')
      case 'notices':
        return items.filter(i => i.type === 'notice' && i.status !== 'completed')
      case 'completed':
        return items.filter(i => i.status === 'completed')
      default:
        return items.filter(i => i.status !== 'completed')
    }
  }, [hub, activeFilter])

  const pendingNoticesCount = useMemo(() => {
    if (!hub) return 0
    return (hub.items || []).filter(i => i.target_user === 'kenned' && i.status === 'pending').length
  }, [hub])

  // Format date helper
  const formatFriendlyDate = (dateMs?: number, timeStr?: string) => {
    if (!dateMs) return timeStr ? `Às ${timeStr}` : ''
    const d = new Date(dateMs)
    const dateFormatted = d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })
    if (timeStr) {
      return `${dateFormatted} às ${timeStr}`
    }
    return dateFormatted
  }

  return (
    <div className="container" style={{ paddingTop: 20, paddingBottom: 64, maxWidth: 640 }}>
      {/* Top Bar com Ações Globais */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div className="live-badge">
          <div className="live-dot" />
          Ao Vivo
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {!isInstalled && (
            <button 
              type="button"
              className="btn btn-outline" 
              onClick={installApp}
              title="Instalar aplicativo"
              style={{ padding: '6px 12px', fontSize: 11, height: 36, borderRadius: 'var(--radius-full)', gap: 6, display: 'inline-flex', alignItems: 'center' }}
            >
              <Download size={14} color="var(--accent)" />
              <span>Instalar</span>
            </button>
          )}

          <button 
            type="button"
            className="theme-toggle-btn"
            onClick={toggleTheme}
            title={theme === 'dark' ? 'Mudar para tema claro' : 'Mudar para tema escuro'}
            aria-label="Alternar tema claro/escuro"
          >
            {theme === 'dark' ? (
              <Sun key="sun" size={17} color="#F59E0B" className="theme-icon-enter" />
            ) : (
              <Moon key="moon" size={17} color="#4A90E2" className="theme-icon-enter" />
            )}
          </button>

          <button 
            type="button"
            className="btn btn-outline"
            onClick={handleShare}
            title="Copiar link desta central"
            style={{ width: 36, height: 36, padding: 0, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            {copied ? <Check size={16} color="var(--accent)" /> : <Share2 size={16} />}
          </button>

          <button 
            type="button"
            className="btn btn-outline"
            onClick={() => {
              sessionStorage.setItem('tessera_skip_autoredirect', 'true')
              window.location.href = '/?home=true'
            }}
            title="Voltar ao início"
            style={{ width: 36, height: 36, padding: 0, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
          >
            <Home size={16} />
          </button>
        </div>
      </div>

      {/* Header Principal */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
          <div>
            <h1 style={{ fontSize: 22, fontWeight: 600, color: 'var(--text-primary)', letterSpacing: -0.3 }}>
              Tarefas & Avisos
            </h1>
            <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginTop: 4 }}>
              Mural sincronizado entre você e o Kenned.
            </p>
          </div>

          <button 
            type="button"
            className="btn btn-primary"
            onClick={() => setIsModalOpen(true)}
            style={{ padding: '8px 14px', fontSize: 12, borderRadius: 'var(--radius-md)', display: 'inline-flex', alignItems: 'center', gap: 6 }}
          >
            <Plus size={16} />
            <span>Novo</span>
          </button>
        </div>

        {/* Feedback Alert */}
        {feedbackMessage && (
          <div style={{ 
            marginTop: 12, 
            padding: '10px 14px', 
            borderRadius: 'var(--radius-sm)', 
            background: 'rgba(113, 215, 205, 0.1)', 
            border: '1px solid rgba(113, 215, 205, 0.3)',
            color: 'var(--accent)',
            fontSize: 12,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            animation: 'fadeIn 180ms ease-out'
          }}>
            <Check size={16} />
            <span>{feedbackMessage}</span>
          </div>
        )}
      </div>

      {/* Segmented Filter Control */}
      <div style={{ 
        display: 'flex', 
        gap: 6, 
        overflowX: 'auto', 
        paddingBottom: 4, 
        marginBottom: 20, 
        scrollbarWidth: 'none' 
      }}>
        {[
          { key: 'all', label: 'Todas' },
          { key: 'kenned', label: 'Para Kenned', badge: pendingNoticesCount > 0 ? pendingNoticesCount : undefined },
          { key: 'me', label: 'Minhas' },
          { key: 'notices', label: 'Avisos' },
          { key: 'completed', label: 'Concluídas' }
        ].map(filter => {
          const isActive = activeFilter === filter.key
          return (
            <button
              key={filter.key}
              type="button"
              onClick={() => setActiveFilter(filter.key as typeof activeFilter)}
              style={{
                padding: '6px 12px',
                borderRadius: 'var(--radius-full)',
                fontSize: 12,
                fontWeight: isActive ? 600 : 400,
                background: isActive ? 'var(--accent)' : 'var(--bg-surface)',
                color: isActive ? '#0B0D13' : 'var(--text-secondary)',
                border: `1px solid ${isActive ? 'transparent' : 'var(--border)'}`,
                cursor: 'pointer',
                whiteSpace: 'nowrap',
                transition: 'all 150ms ease-out',
                display: 'inline-flex',
                alignItems: 'center',
                gap: 6
              }}
            >
              <span>{filter.label}</span>
              {filter.badge !== undefined && (
                <span style={{
                  fontSize: 10,
                  fontWeight: 600,
                  padding: '1px 5px',
                  borderRadius: 99,
                  background: isActive ? '#0B0D13' : 'var(--accent)',
                  color: isActive ? '#FFFFFF' : '#0B0D13'
                }}>
                  {filter.badge}
                </span>
              )}
            </button>
          )
        })}
      </div>

      {/* Content Area */}
      {loading ? (
        // Loading Skeleton
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[1, 2, 3].map(n => (
            <div 
              key={n} 
              className="card" 
              style={{ height: 72, padding: 16, display: 'flex', alignItems: 'center', gap: 12, opacity: 0.5 }}
            >
              <div style={{ width: 22, height: 22, borderRadius: '50%', background: 'var(--border)' }} />
              <div style={{ flex: 1 }}>
                <div style={{ width: '50%', height: 14, background: 'var(--border)', borderRadius: 4, marginBottom: 8 }} />
                <div style={{ width: '30%', height: 10, background: 'var(--border)', borderRadius: 4 }} />
              </div>
            </div>
          ))}
        </div>
      ) : error ? (
        // Error State
        <div className="card" style={{ textAlign: 'center', padding: 32 }}>
          <AlertCircle size={36} color="var(--danger)" style={{ margin: '0 auto 12px' }} />
          <div style={{ fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>Falha ao sincronizar</div>
          <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 4 }}>{error}</p>
          <button 
            type="button" 
            className="btn btn-outline" 
            onClick={() => window.location.reload()}
            style={{ marginTop: 16, fontSize: 12 }}
          >
            <RefreshCw size={14} style={{ marginRight: 6 }} />
            Tentar novamente
          </button>
        </div>
      ) : filteredItems.length === 0 ? (
        // Empty State
        <div 
          className="card" 
          style={{ 
            textAlign: 'center', 
            padding: '48px 24px', 
            background: 'var(--bg-surface)', 
            border: '1px dashed var(--border)' 
          }}
        >
          <div style={{ 
            width: 48, 
            height: 48, 
            borderRadius: '50%', 
            background: 'var(--accent-subtle)', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center', 
            margin: '0 auto 16px' 
          }}>
            <MessageSquare size={22} color="var(--accent)" />
          </div>
          <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' }}>
            Nenhum item nesta lista
          </div>
          <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 4, maxWidth: 320, margin: '4px auto 16px' }}>
            {activeFilter === 'completed' 
              ? 'Nenhuma tarefa ou aviso foi concluído ainda.' 
              : 'Clique no botão acima para enviar um novo aviso ao Kenned ou salvar uma tarefa.'}
          </p>
          {activeFilter !== 'completed' && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setIsModalOpen(true)}
              style={{ fontSize: 12, padding: '8px 16px', borderRadius: 'var(--radius-md)' }}
            >
              Criar Primeiro Item
            </button>
          )}
        </div>
      ) : (
        // Items List
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {filteredItems.map(item => {
            const isCompleted = item.status === 'completed'
            const isKenned = item.target_user === 'kenned'
            const isNotice = item.type === 'notice'
            const isPending = item.status === 'pending'

            return (
              <div 
                key={item.id}
                className="card interactive-card"
                style={{
                  padding: '14px 16px',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 12,
                  opacity: isCompleted ? 0.6 : 1,
                  background: isPending ? 'rgba(45, 212, 191, 0.03)' : undefined,
                  border: isPending ? '1px solid rgba(45, 212, 191, 0.25)' : undefined
                }}
              >
                {/* Complete Checkbox */}
                <button
                  type="button"
                  onClick={() => handleToggleComplete(item.id)}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    padding: 2,
                    cursor: 'pointer',
                    color: isCompleted ? 'var(--accent)' : 'var(--text-muted)',
                    display: 'flex',
                    alignItems: 'center',
                    marginTop: 2
                  }}
                  aria-label={isCompleted ? 'Marcar como não concluído' : 'Marcar como concluído'}
                >
                  {isCompleted ? (
                    <CheckCircle2 size={20} color="var(--accent)" />
                  ) : (
                    <Circle size={20} />
                  )}
                </button>

                {/* Main Content */}
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <span style={{
                      fontSize: 14,
                      fontWeight: 500,
                      color: isCompleted ? 'var(--text-muted)' : 'var(--text-primary)',
                      textDecoration: isCompleted ? 'line-through' : 'none'
                    }}>
                      {item.title}
                    </span>
                  </div>

                  {item.description && (
                    <p style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 4, lineHeight: 1.4 }}>
                      {item.description}
                    </p>
                  )}

                  {/* Metadata Badges */}
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 8, flexWrap: 'wrap' }}>
                    {/* Badge Tipo: Aviso ou Tarefa */}
                    <span style={{
                      fontSize: 10,
                      fontWeight: 600,
                      padding: '2px 6px',
                      borderRadius: 4,
                      background: isNotice ? 'rgba(245, 158, 11, 0.12)' : 'rgba(74, 144, 226, 0.12)',
                      color: isNotice ? '#F59E0B' : '#4A90E2',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4
                    }}>
                      {isNotice ? <Bell size={10} /> : <Clock size={10} />}
                      {isNotice ? 'Aviso' : 'Tarefa'}
                    </span>

                    {/* Badge Destinatário */}
                    <span style={{
                      fontSize: 10,
                      fontWeight: 600,
                      padding: '2px 6px',
                      borderRadius: 4,
                      background: isKenned ? 'var(--accent-subtle)' : 'var(--bg-surface)',
                      color: isKenned ? 'var(--accent)' : 'var(--text-muted)',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4
                    }}>
                      <User size={10} />
                      {isKenned ? '@kenned' : 'Pessoal'}
                    </span>

                    {/* Status de Confirmação (se para Kenned) */}
                    {isKenned && !isCompleted && (
                      <span style={{
                        fontSize: 10,
                        padding: '2px 6px',
                        borderRadius: 4,
                        background: isPending ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)',
                        color: isPending ? 'var(--danger)' : 'var(--success)'
                      }}>
                        {isPending ? 'Aguardando no app' : '✓ Confirmado no app'}
                      </span>
                    )}

                    {/* Data / Horário */}
                    {(item.due_date || item.due_time) && (
                      <span style={{
                        fontSize: 10,
                        color: 'var(--text-muted)',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 3
                      }}>
                        <Calendar size={10} />
                        {formatFriendlyDate(item.due_date, item.due_time)}
                      </span>
                    )}
                  </div>
                </div>

                {/* Delete Button */}
                <button
                  type="button"
                  onClick={() => handleDeleteItem(item.id)}
                  className="btn btn-outline"
                  style={{
                    width: 28,
                    height: 28,
                    padding: 0,
                    border: 'none',
                    background: 'transparent',
                    color: 'var(--text-muted)',
                    cursor: 'pointer',
                    borderRadius: 4
                  }}
                  title="Excluir item"
                  aria-label="Excluir item"
                >
                  <Trash2 size={14} />
                </button>
              </div>
            )
          })}
        </div>
      )}

      {/* Modal de Criação */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content animate-fade-in-up" onClick={e => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <div>
                <h2 style={{ fontSize: 18, fontWeight: 600, color: 'var(--text-primary)' }}>
                  Novo Registro
                </h2>
                <p style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 2 }}>
                  Envie um aviso ao Kenned ou crie uma tarefa
                </p>
              </div>
              <button 
                type="button"
                className="btn btn-outline" 
                onClick={() => setIsModalOpen(false)}
                style={{ padding: 6, borderRadius: '50%', width: 32, height: 32, display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                aria-label="Fechar"
              >
                <X size={16} />
              </button>
            </div>

            <form onSubmit={handleCreateItem} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {/* Type Switcher: Aviso vs Tarefa */}
              <div>
                <label className="input-label">Tipo de Registro</label>
                <div className="segmented-control">
                  <button 
                    type="button"
                    className={`segmented-btn ${itemType === 'notice' ? 'active' : ''}`}
                    onClick={() => setItemType('notice')}
                  >
                    📌 Aviso / Recado
                  </button>
                  <button 
                    type="button"
                    className={`segmented-btn ${itemType === 'task' ? 'active' : ''}`}
                    onClick={() => setItemType('task')}
                  >
                    ⏱️ Tarefa com Prazo
                  </button>
                </div>
              </div>

              {/* Destinatário */}
              <div>
                <label className="input-label">Para quem é este item?</label>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                  <button
                    type="button"
                    onClick={() => setTargetUser('kenned')}
                    style={{
                      padding: '10px 12px',
                      borderRadius: 'var(--radius-sm)',
                      border: `1px solid ${targetUser === 'kenned' ? 'var(--accent)' : 'var(--border)'}`,
                      background: targetUser === 'kenned' ? 'var(--accent-subtle)' : 'var(--bg-surface)',
                      color: targetUser === 'kenned' ? 'var(--accent)' : 'var(--text-secondary)',
                      cursor: 'pointer',
                      fontSize: 12,
                      fontWeight: 600,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      transition: 'all 150ms ease-out'
                    }}
                  >
                    <Bell size={14} />
                    <span>Para o Kenned (Notifica no Celular)</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setTargetUser('me')}
                    style={{
                      padding: '10px 12px',
                      borderRadius: 'var(--radius-sm)',
                      border: `1px solid ${targetUser === 'me' ? 'var(--accent)' : 'var(--border)'}`,
                      background: targetUser === 'me' ? 'var(--accent-subtle)' : 'var(--bg-surface)',
                      color: targetUser === 'me' ? 'var(--accent)' : 'var(--text-secondary)',
                      cursor: 'pointer',
                      fontSize: 12,
                      fontWeight: 600,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      gap: 6,
                      transition: 'all 150ms ease-out'
                    }}
                  >
                    <User size={14} />
                    <span>Minha Tarefa (Compartilhada)</span>
                  </button>
                </div>
              </div>

              {/* Título */}
              <div>
                <label className="input-label">
                  Título {targetUser === 'kenned' && <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(use @kenned para marcar)</span>}
                </label>
                <input 
                  type="text"
                  className="input-field"
                  placeholder={itemType === 'notice' ? 'Ex: Amanhã vem o técnico da internet...' : 'Ex: Passar na farmácia pegar o remédio...'}
                  value={title}
                  onChange={e => handleTitleChange(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              {/* Descrição opcional */}
              <div>
                <label className="input-label">Detalhes ou Observações (opcional)</label>
                <textarea 
                  className="input-field"
                  rows={2}
                  placeholder="Informações complementares, endereço ou orientações..."
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                  style={{ resize: 'none', height: 68 }}
                />
              </div>

              {/* Campos de Data e Horário (apenas se for Tarefa) */}
              {itemType === 'task' && (
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div>
                    <label className="input-label">Data do Prazo</label>
                    <input 
                      type="date"
                      className="input-field"
                      value={dueDate}
                      onChange={e => setDueDate(e.target.value)}
                      required
                    />
                  </div>

                  <div>
                    <label className="input-label">Horário (opcional)</label>
                    <input 
                      type="time"
                      className="input-field"
                      value={dueTime}
                      onChange={e => setDueTime(e.target.value)}
                    />
                  </div>
                </div>
              )}

              {/* Box de Feedback Dinâmico */}
              {targetUser === 'kenned' ? (
                <div style={{ 
                  background: 'rgba(113, 215, 205, 0.08)', 
                  border: '1px solid rgba(113, 215, 205, 0.3)', 
                  borderRadius: 'var(--radius-sm)', 
                  padding: '10px 14px', 
                  fontSize: 12, 
                  color: 'var(--accent)',
                  lineHeight: 1.4,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8
                }}>
                  <Bell size={16} style={{ flexShrink: 0 }} />
                  <span>
                    <strong>Notificação Prioritária:</strong> Este item tocará no celular do Kenned com som prioritário e aguardará confirmação no aplicativo.
                  </span>
                </div>
              ) : (
                <div style={{ 
                  background: 'var(--bg-surface)', 
                  border: '1px solid var(--border)', 
                  borderRadius: 'var(--radius-sm)', 
                  padding: '10px 14px', 
                  fontSize: 12, 
                  color: 'var(--text-muted)',
                  lineHeight: 1.4
                }}>
                  👤 <strong>Item Pessoal:</strong> Ficará na sua lista de tarefas sem disparar avisos ou notificações para o Kenned.
                </div>
              )}

              {/* Botões do Modal */}
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
                  disabled={!title.trim() || isSubmitting}
                  style={{ flex: 1 }}
                >
                  {isSubmitting 
                    ? 'Salvando...' 
                    : targetUser === 'kenned' 
                      ? 'Enviar para o Kenned' 
                      : 'Adicionar à Lista'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Footer Branding */}
      <div style={{ textAlign: 'center', marginTop: 40, fontSize: 11, color: 'var(--text-muted)' }}>
        Desenvolvido por <strong style={{ color: 'var(--text-primary)' }}>Tessera</strong> • Conexão em Tempo Real
      </div>

      {/* Modal PWA */}
      <PwaInstructionsModal 
        isOpen={showHelpModal} 
        onClose={() => setShowHelpModal(false)} 
        isIos={isIos} 
      />
    </div>
  )
}
