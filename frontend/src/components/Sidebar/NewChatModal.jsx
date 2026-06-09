import { useState, useMemo, useEffect } from 'react'
import { useAuth } from '../../context/AuthContext'
import { getAllUsers } from '../../api/users'
import { Avatar } from './ConversationItem'

export default function NewChatModal({ onSelect, onClose }) {
  const { auth }                        = useAuth()
  const [users, setUsers]               = useState([])
  const [search, setSearch]             = useState('')
  const [loading, setLoading]           = useState(true)

  useEffect(() => {
    getAllUsers(auth.token)
      .then(all => setUsers(all.filter(u => u.id !== auth.user.id)))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  const filtered = useMemo(() =>
    users.filter(u =>
      u.name.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase())
    ), [users, search])

  return (
    <div onClick={onClose} style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        background: '#fff', borderRadius: '16px', boxShadow: '0 20px 50px rgba(0,0,0,0.2)',
        width: '100%', maxWidth: '360px', overflow: 'hidden',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      }}>
        {/* Header */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '16px 20px', borderBottom: '1px solid #f1f5f9',
        }}>
          <h2 style={{ margin: 0, fontSize: '16px', fontWeight: 700, color: '#1e293b' }}>
            Start a New Chat
          </h2>
          <button onClick={onClose} style={{
            background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer',
            color: '#94a3b8', lineHeight: 1, padding: '2px',
          }}>✕</button>
        </div>

        {/* Search */}
        <div style={{ padding: '12px 16px', borderBottom: '1px solid #f1f5f9' }}>
          <input
            autoFocus type="text" placeholder="🔍  Search by name or email…"
            value={search} onChange={e => setSearch(e.target.value)}
            style={{
              width: '100%', padding: '10px 14px', border: '1.5px solid #e2e8f0',
              borderRadius: '10px', fontSize: '14px', outline: 'none',
              background: '#f8fafc', boxSizing: 'border-box', color: '#1e293b',
            }}
          />
        </div>

        {/* User list */}
        <div style={{ overflowY: 'auto', maxHeight: '320px' }}>
          {loading ? (
            <p style={{ textAlign: 'center', color: '#94a3b8', fontSize: '13px', padding: '32px' }}>
              Loading…
            </p>
          ) : filtered.length === 0 ? (
            <p style={{ textAlign: 'center', color: '#94a3b8', fontSize: '13px', padding: '32px' }}>
              No users found
            </p>
          ) : filtered.map(user => (
            <button key={user.id} onClick={() => onSelect(user)} style={{
              width: '100%', display: 'flex', alignItems: 'center', gap: '12px',
              padding: '12px 16px', border: 'none', background: 'transparent',
              cursor: 'pointer', textAlign: 'left', transition: 'background .15s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'}
            onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
            >
              <Avatar name={user.name} pictureUrl={user.pictureUrl} />
              <div>
                <p style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#1e293b' }}>
                  {user.name}
                </p>
                <p style={{ margin: 0, fontSize: '12px', color: '#94a3b8' }}>
                  {user.preferredLanguage}
                </p>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
