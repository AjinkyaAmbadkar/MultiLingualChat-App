export default function MessageBubble({ message, isSent }) {
  const time = message.timestamp
    ? new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : ''

  const text = isSent
    ? message.originalText
    : (message.translatedText || message.originalText)

  return (
    <div style={{ display: 'flex', justifyContent: isSent ? 'flex-end' : 'flex-start', marginBottom: '4px' }}>
      <div style={{
        maxWidth: '68%',
        padding: '10px 14px',
        borderRadius: isSent ? '18px 18px 4px 18px' : '18px 18px 18px 4px',
        background: isSent ? '#2563eb' : '#ffffff',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        color: isSent ? '#fff' : '#1e293b',
      }}>
        <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.5, wordBreak: 'break-word' }}>
          {text}
        </p>
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
          gap: '4px', marginTop: '4px',
        }}>
          <span style={{ fontSize: '11px', color: isSent ? 'rgba(255,255,255,0.7)' : '#94a3b8' }}>
            {time}
          </span>
          {isSent && (
            <span style={{ fontSize: '12px', color: 'rgba(255,255,255,0.8)' }}>✓✓</span>
          )}
        </div>
      </div>
    </div>
  )
}
