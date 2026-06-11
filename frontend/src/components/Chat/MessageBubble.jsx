import { useState } from 'react'

export default function MessageBubble({ message, isSent }) {
  const [showOriginal, setShowOriginal] = useState(false)

  const time = message.timestamp
    ? new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : ''

  // Sender always sees what they typed. Receiver sees the translated version.
  // Toggle reveals the other side's text.
  const preferredText = isSent
    ? message.originalText
    : (message.translatedText || message.originalText)

  const hasOriginal = !isSent && message.originalText && message.originalText !== preferredText
  const displayText = showOriginal ? message.originalText : preferredText

  return (
    <div style={{ display: 'flex', justifyContent: isSent ? 'flex-end' : 'flex-start', marginBottom: '6px' }}>
      <div style={{
        maxWidth: '65%',
        padding: '10px 14px 8px',
        borderRadius: isSent ? '20px 20px 4px 20px' : '20px 20px 20px 4px',
        background: isSent ? '#1d4ed8' : '#ffffff',
        boxShadow: isSent ? '0 2px 8px rgba(29,78,216,0.25)' : '0 1px 4px rgba(0,0,0,0.08)',
        color: isSent ? '#fff' : '#0f172a',
      }}>
        <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.55, wordBreak: 'break-word' }}>
          {displayText}
        </p>

        {hasOriginal && (
          <button
            onClick={() => setShowOriginal(prev => !prev)}
            style={{
              background: 'none', border: 'none', cursor: 'pointer', padding: '2px 0 0',
              fontSize: '11px',
              color: isSent ? 'rgba(255,255,255,0.55)' : '#94a3b8',
              textDecoration: 'underline', display: 'block',
            }}
          >
            {showOriginal ? 'See translation' : 'See original'}
          </button>
        )}

        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
          gap: '4px', marginTop: '4px',
        }}>
          <span style={{ fontSize: '11px', color: isSent ? 'rgba(255,255,255,0.6)' : '#94a3b8' }}>
            {time}
          </span>
          {isSent && (
            <span style={{
              fontSize: '11px',
              color: message.isRead ? '#4ade80' : 'rgba(255,255,255,0.5)',
              fontWeight: message.isRead ? 700 : 400,
            }}>✓✓</span>
          )}
        </div>
      </div>
    </div>
  )
}
