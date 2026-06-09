import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { updateLanguage } from '../../api/users'

const LANGUAGES = [
  'English', 'Spanish',
  'Hindi', 'Marathi', 'Tamil', 'Telugu', 'Malayalam', 'Punjabi',
  'French', 'German', 'Arabic', 'Japanese', 'Chinese',
]

export default function SettingsModal({ onClose }) {
  const { auth, updateUser } = useAuth()
  const [selected, setSelected]   = useState(auth.user.preferredLanguage || 'English')
  const [saving, setSaving]       = useState(false)
  const [error, setError]         = useState(null)
  const [saved, setSaved]         = useState(false)

  async function handleSave() {
    setSaving(true)
    setError(null)
    setSaved(false)
    try {
      await updateLanguage(auth.token, selected)
      updateUser({ preferredLanguage: selected })
      setSaved(true)
      setTimeout(onClose, 800)
    } catch {
      setError('Failed to save. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.55)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000,
    }} onClick={onClose}>
      <div style={{
        background: '#fff', borderRadius: '16px', padding: '28px',
        width: '360px', boxShadow: '0 20px 60px rgba(0,0,0,0.25)',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      }} onClick={e => e.stopPropagation()}>

        {/* Header */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
          <h2 style={{ margin: 0, fontSize: '17px', fontWeight: 700, color: '#0f172a' }}>Profile & Settings</h2>
          <button onClick={onClose} style={{
            background: 'none', border: 'none', cursor: 'pointer',
            fontSize: '20px', color: '#94a3b8', lineHeight: 1, padding: '2px 6px', borderRadius: '6px',
          }}>✕</button>
        </div>

        {/* Profile info (read-only) */}
        <div style={{
          background: '#f8fafc', borderRadius: '12px', padding: '16px',
          marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '14px',
        }}>
          {auth.user.pictureUrl
            ? <img src={auth.user.pictureUrl} alt="" style={{ width: '48px', height: '48px', borderRadius: '50%', objectFit: 'cover' }} />
            : <div style={{
                width: '48px', height: '48px', borderRadius: '50%',
                background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '20px', fontWeight: 700, color: '#fff',
              }}>{auth.user.name?.[0]?.toUpperCase()}</div>
          }
          <div>
            <p style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#0f172a' }}>{auth.user.name}</p>
            <p style={{ margin: '2px 0 0', fontSize: '12px', color: '#64748b' }}>{auth.user.email}</p>
          </div>
        </div>

        {/* Language selector */}
        <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#475569', marginBottom: '8px', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
          Preferred Language
        </label>
        <select value={selected} onChange={e => { setSelected(e.target.value); setSaved(false) }} style={{
          width: '100%', padding: '10px 12px', borderRadius: '10px',
          border: '1.5px solid #e2e8f0', fontSize: '14px', color: '#0f172a',
          background: '#fff', cursor: 'pointer', outline: 'none', marginBottom: '20px',
          appearance: 'none',
        }}>
          {LANGUAGES.map(lang => (
            <option key={lang} value={lang}>{lang}</option>
          ))}
        </select>

        {/* Feedback */}
        {error && <p style={{ margin: '0 0 12px', fontSize: '13px', color: '#ef4444' }}>{error}</p>}
        {saved && <p style={{ margin: '0 0 12px', fontSize: '13px', color: '#22c55e' }}>Saved!</p>}

        {/* Actions */}
        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={onClose} style={{
            flex: 1, padding: '10px', borderRadius: '10px',
            border: '1.5px solid #e2e8f0', background: '#fff',
            fontSize: '13px', fontWeight: 600, color: '#475569', cursor: 'pointer',
          }}>Cancel</button>
          <button onClick={handleSave} disabled={saving} style={{
            flex: 1, padding: '10px', borderRadius: '10px',
            border: 'none', background: saving ? '#93c5fd' : '#1d4ed8',
            fontSize: '13px', fontWeight: 600, color: '#fff', cursor: saving ? 'not-allowed' : 'pointer',
          }}>{saving ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </div>
  )
}
