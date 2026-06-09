export default function MessageBubble({ message, isSent }) {
  const time = message.timestamp
    ? new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : ''

  const text = isSent
    ? message.originalText
    : (message.translatedText || message.originalText)

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
          {text}
        </p>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
          gap: '4px', marginTop: '4px',
        }}>
          <span style={{ fontSize: '11px', color: isSent ? 'rgba(255,255,255,0.6)' : '#94a3b8' }}>
            {time}
          </span>
          {isSent && (
            <span style={{ fontSize: '11px', color: 'rgba(255,255,255,0.7)' }}>✓✓</span>
          )}
        </div>
      </div>
    </div>
  )
}
