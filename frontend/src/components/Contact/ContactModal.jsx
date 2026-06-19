import { useState } from 'react'

// Public client-side key from https://web3forms.com (safe to ship in frontend).
// Submissions are emailed to the address that created the key.
const WEB3FORMS_ACCESS_KEY = 'f8d2a6ba-9e4f-437f-b079-080f934e23a7'

const S = {
  overlay: {
    position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.6)', backdropFilter: 'blur(3px)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '20px', zIndex: 1000,
  },
  card: {
    width: '100%', maxWidth: '440px', background: '#fff', borderRadius: '18px',
    padding: '28px 26px', boxShadow: '0 20px 60px rgba(0,0,0,0.35)',
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    boxSizing: 'border-box', maxHeight: '90vh', overflowY: 'auto',
  },
  head: { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: '4px' },
  title: { fontSize: '22px', fontWeight: 800, color: '#0f172a', letterSpacing: '-0.4px' },
  close: { background: 'none', border: 'none', fontSize: '22px', color: '#94a3b8', cursor: 'pointer', lineHeight: 1, padding: '2px' },
  sub: { fontSize: '14px', color: '#64748b', marginBottom: '22px', lineHeight: 1.6 },
  label: { display: 'block', fontSize: '13px', fontWeight: 600, color: '#374151', marginBottom: '6px' },
  field: { marginBottom: '16px' },
  input: (focused) => ({
    width: '100%', padding: '11px 14px', border: `1.5px solid ${focused ? '#3b82f6' : '#e2e8f0'}`,
    borderRadius: '10px', fontSize: '16px', color: '#0f172a', background: '#f8fafc',
    outline: 'none', boxSizing: 'border-box', transition: 'all .2s',
    boxShadow: focused ? '0 0 0 3px rgba(59,130,246,0.12)' : 'none',
  }),
  btn: (disabled) => ({
    width: '100%', padding: '13px', background: 'linear-gradient(135deg, #4f46e5, #1d4ed8)', color: '#fff',
    border: 'none', borderRadius: '12px', fontSize: '15px', fontWeight: 700,
    cursor: disabled ? 'default' : 'pointer', marginTop: '4px', opacity: disabled ? 0.7 : 1,
    boxShadow: '0 4px 14px rgba(29,78,216,0.35)',
  }),
  note: (ok) => ({
    borderRadius: '10px', padding: '11px 14px', fontSize: '13px', lineHeight: 1.5, marginBottom: '18px',
    background: ok ? '#ecfdf5' : '#fef2f2', border: `1px solid ${ok ? '#a7f3d0' : '#fecaca'}`,
    color: ok ? '#047857' : '#dc2626',
  }),
}

export default function ContactModal({ onClose }) {
  const [name, setName]       = useState('')
  const [email, setEmail]     = useState('')
  const [message, setMessage] = useState('')
  const [status, setStatus]   = useState('idle') // idle | sending | success | error
  const [focused, setFocused] = useState('')

  async function handleSubmit(e) {
    e.preventDefault()
    setStatus('sending')
    try {
      const res = await fetch('https://api.web3forms.com/submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({
          access_key: WEB3FORMS_ACCESS_KEY,
          subject: 'PolyLingual Chat — new contact message',
          from_name: 'PolyLingual Chat',
          name, email, message,
        }),
      })
      const data = await res.json()
      setStatus(data.success ? 'success' : 'error')
    } catch {
      setStatus('error')
    }
  }

  const F = (key) => ({
    onFocus: () => setFocused(key),
    onBlur: () => setFocused(''),
    style: S.input(focused === key),
  })

  return (
    <div style={S.overlay} onClick={onClose}>
      <div style={S.card} onClick={e => e.stopPropagation()}>
        <div style={S.head}>
          <div style={S.title}>Get in touch</div>
          <button onClick={onClose} aria-label="Close" style={S.close}>×</button>
        </div>
        <p style={S.sub}>
          Have a suggestion, found a bug, or just want to connect? Drop me a message — it goes straight to my inbox.
        </p>

        {status === 'success' ? (
          <div style={S.note(true)}>
            Thanks! Your message was sent — I'll get back to you soon. 🙌
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            {status === 'error' && (
              <div style={S.note(false)}>Something went wrong sending your message. Please try again.</div>
            )}
            <div style={S.field}>
              <label style={S.label}>Your Name</label>
              <input type="text" value={name} required placeholder="Your name"
                onChange={e => setName(e.target.value)} {...F('name')} />
            </div>
            <div style={S.field}>
              <label style={S.label}>Email</label>
              <input type="email" value={email} required placeholder="you@example.com"
                onChange={e => setEmail(e.target.value)} {...F('email')} />
            </div>
            <div style={S.field}>
              <label style={S.label}>Message</label>
              <textarea value={message} required placeholder="What's on your mind?" rows={4}
                onChange={e => setMessage(e.target.value)}
                {...F('message')} style={{ ...S.input(focused === 'message'), resize: 'vertical', minHeight: '96px', fontFamily: 'inherit' }} />
            </div>
            <button type="submit" disabled={status === 'sending'} style={S.btn(status === 'sending')}>
              {status === 'sending' ? 'Sending…' : 'Send Message →'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}
