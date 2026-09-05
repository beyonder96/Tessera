import React from 'react'
import { Download, X, Share, MoreVertical, PlusSquare } from 'lucide-react'

interface PwaInstructionsModalProps {
  isOpen: boolean
  onClose: () => void
  isIos: boolean
}

export const PwaInstructionsModal: React.FC<PwaInstructionsModalProps> = ({
  isOpen,
  onClose,
  isIos,
}) => {
  if (!isOpen) return null

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.72)',
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: 16,
      }}
      onClick={onClose}
    >
      <div
        className="card"
        style={{
          width: '100%',
          maxWidth: 420,
          background: 'var(--bg-card)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-lg)',
          padding: 24,
          boxShadow: '0 16px 32px rgba(0, 0, 0, 0.4)',
        }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                background: 'var(--accent-subtle)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Download size={18} color="var(--accent)" />
            </div>
            <div>
              <h3 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Instalar Tessera
              </h3>
              <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>
                Acesso rápido e offline via PWA
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: 4,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <X size={18} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginBottom: 20 }}>
          {isIos ? (
            <>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 12,
                  background: 'var(--bg-surface)',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                }}
              >
                <Share size={18} color="var(--accent)" style={{ marginTop: 2, flexShrink: 0 }} />
                <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  1. Toque no botão <strong>Compartilhar</strong> na barra do Safari.
                </div>
              </div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 12,
                  background: 'var(--bg-surface)',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                }}
              >
                <PlusSquare size={18} color="var(--accent)" style={{ marginTop: 2, flexShrink: 0 }} />
                <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  2. Role para baixo e selecione <strong>Adicionar à Tela de Início</strong>.
                </div>
              </div>
            </>
          ) : (
            <>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 12,
                  background: 'var(--bg-surface)',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                }}
              >
                <MoreVertical size={18} color="var(--accent)" style={{ marginTop: 2, flexShrink: 0 }} />
                <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  1. Toque nos <strong>três pontos (⋮)</strong> no canto superior do navegador.
                </div>
              </div>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 12,
                  background: 'var(--bg-surface)',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border)',
                }}
              >
                <Download size={18} color="var(--accent)" style={{ marginTop: 2, flexShrink: 0 }} />
                <div style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                  2. Selecione <strong>Instalar aplicativo</strong> ou <strong>Adicionar à tela inicial</strong>.
                </div>
              </div>
            </>
          )}
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={onClose}
          style={{ width: '100%', height: 40, fontSize: 13, fontWeight: 600 }}
        >
          Entendi
        </button>
      </div>
    </div>
  )
}
