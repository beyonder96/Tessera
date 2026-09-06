export interface RecentItem {
  type: 'market' | 'finance'
  id: string
  title: string
  updatedAt: number
}

const STORAGE_KEY_RECENT = 'tessera_recent_shares'
const STORAGE_KEY_LAST = 'tessera_last_active_route'

function isRecentItem(item: unknown): item is RecentItem {
  if (!item || typeof item !== 'object') return false
  const candidate = item as Record<string, unknown>
  return (
    (candidate.type === 'market' || candidate.type === 'finance') &&
    typeof candidate.id === 'string' &&
    candidate.id.trim().length > 0 &&
    typeof candidate.title === 'string' &&
    typeof candidate.updatedAt === 'number'
  )
}

export function saveRecentItem(item: { type: 'market' | 'finance'; id: string; title: string }): void {
  if (typeof window === 'undefined') return
  try {
    const trimmedId = item.id.trim()
    if (!trimmedId) return

    const newItem: RecentItem = {
      type: item.type,
      id: trimmedId,
      title: item.title.trim() || (item.type === 'market' ? 'Lista de Mercado' : 'Resumo Financeiro'),
      updatedAt: Date.now(),
    }

    // 1. Grava a última rota ativa
    localStorage.setItem(STORAGE_KEY_LAST, JSON.stringify(newItem))

    // 2. Atualiza a lista de acessos recentes (máx. 6 itens, deduplicados)
    const existing = getRecentItems()
    const filtered = existing.filter(i => !(i.type === newItem.type && i.id === newItem.id))
    const updated = [newItem, ...filtered].slice(0, 6)
    localStorage.setItem(STORAGE_KEY_RECENT, JSON.stringify(updated))
  } catch (error: unknown) {
    console.error('Erro ao salvar item recente:', error)
  }
}

export function getRecentItems(): RecentItem[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = localStorage.getItem(STORAGE_KEY_RECENT)
    if (!raw) return []
    const parsed: unknown = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      return parsed.filter(isRecentItem)
    }
  } catch (error: unknown) {
    console.error('Erro ao ler acessos recentes:', error)
  }
  return []
}

export function getLastActiveRoute(): RecentItem | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = localStorage.getItem(STORAGE_KEY_LAST)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    if (isRecentItem(parsed)) {
      return parsed
    }
  } catch (error: unknown) {
    console.error('Erro ao ler última rota ativa:', error)
  }
  return null
}

export function removeRecentItem(type: 'market' | 'finance', id: string): void {
  if (typeof window === 'undefined') return
  try {
    const existing = getRecentItems()
    const updated = existing.filter(i => !(i.type === type && i.id === id))
    localStorage.setItem(STORAGE_KEY_RECENT, JSON.stringify(updated))

    const last = getLastActiveRoute()
    if (last && last.type === type && last.id === id) {
      if (updated.length > 0) {
        localStorage.setItem(STORAGE_KEY_LAST, JSON.stringify(updated[0]))
      } else {
        localStorage.removeItem(STORAGE_KEY_LAST)
      }
    }
  } catch (error: unknown) {
    console.error('Erro ao remover item recente:', error)
  }
}

export function clearRecentItems(): void {
  if (typeof window === 'undefined') return
  try {
    localStorage.removeItem(STORAGE_KEY_RECENT)
    localStorage.removeItem(STORAGE_KEY_LAST)
  } catch (error: unknown) {
    console.error('Erro ao limpar acessos recentes:', error)
  }
}
