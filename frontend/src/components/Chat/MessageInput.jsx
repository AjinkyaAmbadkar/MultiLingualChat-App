import { useState, useRef } from 'react'

export default function MessageInput({ onSend, onTyping, disabled }) {
  const [text, setText] = useState('')
  const typingTimerRef  = useRef(null)

  function handleChange(e) {
    setText(e.target.value)
    if (onTyping) {
      onTyping(true)
      clearTimeout(typingTimerRef.current)
      typingTimerRef.current = setTimeout(() => onTyping(false), 2000)
    }
  }

  function submit() {
    const trimmed = text.trim()
    if (!trimmed || disabled) return
    // Stop typing indicator immediately on send
    if (onTyping) {
      clearTimeout(typingTimerRef.current)
      onTyping(false)
    }
    onSend(trimmed)
    setText('')
  }

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: '10px',
      padding: '12px 16px', background: '#fff',
      borderTop: '1px solid #e2e8f0',
    }}>
      <input
        type="text" value={text} disabled={disabled}
        onChange={handleChange}
        onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() } }}
        placeholder="Type a message…"
        style={{
          flex: 1, padding: '11px 18px', border: '1.5px solid #e2e8f0',
          // 16px minimum — anything smaller makes iOS Safari auto-zoom when the input gets focus
          borderRadius: '24px', fontSize: '16px', outline: 'none',
          background: '#f8fafc', color: '#1e293b', transition: 'border-color .2s',
          boxSizing: 'border-box',
        }}
        onFocus={e => e.target.style.borderColor = '#2563eb'}
        onBlur={e => e.target.style.borderColor = '#e2e8f0'}
      />
      <button
        onClick={submit}
        disabled={!text.trim() || disabled}
        style={{
          width: '44px', height: '44px', borderRadius: '50%', flexShrink: 0,
          background: !text.trim() || disabled ? '#cbd5e1' : '#2563eb',
          border: 'none', cursor: !text.trim() || disabled ? 'not-allowed' : 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          transition: 'background .2s',
        }}
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24"
          fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
          <line x1="22" y1="2" x2="11" y2="13" />
          <polygon points="22 2 15 22 11 13 2 9 22 2" />
        </svg>
      </button>
    </div>
  )
}
