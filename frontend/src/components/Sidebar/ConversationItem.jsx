import { useState } from 'react'
import { formatDistanceToNow } from '../../utils/time'

export default function ConversationItem({ conversation, isActive, onClick }) {
  const { name, pictureUrl, lastMessage, lastMessageTime, unreadCount } = conversation

  return (
    <button onClick={onClick} style={{
      width: '100%', display: 'flex', alignItems: 'center', gap: '12px',
      padding: '11px 14px', border: 'none',
      background: isActive ? 'rgba(59,130,246,0.15)' : 'transparent',
      borderLeft: `3px solid ${isActive ? '#3b82f6' : 'transparent'}`,
      cursor: 'pointer', textAlign: 'left', transition: 'all .15s',
    }}
    onMouseEnter={e => { if (!isActive) e.currentTarget.style.background = 'rgba(255,255,255,0.05)' }}
    onMouseLeave={e => { if (!isActive) e.currentTarget.style.background = 'transparent' }}
    >
      <Avatar name={name} pictureUrl={pictureUrl} />

      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '3px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{
            fontSize: '14px', fontWeight: 600, lineHeight: 1.2,
            color: isActive ? '#93c5fd' : '#e2e8f0',
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            flex: 1, marginRight: '8px',
          }}>
            {name}
          </span>
          <span style={{ fontSize: '11px', color: '#475569', flexShrink: 0 }}>
            {lastMessageTime ? formatDistanceToNow(lastMessageTime) : ''}
          </span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{
            fontSize: '12px', color: '#64748b', lineHeight: 1.2,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            flex: 1, marginRight: '6px',
          }}>
            {lastMessage || 'Start a conversation'}
          </span>
          {unreadCount > 0 && (
            <span style={{
              background: '#3b82f6', color: '#fff', fontSize: '10px', fontWeight: 700,
              borderRadius: '10px', padding: '2px 7px', flexShrink: 0,
            }}>
              {unreadCount}
            </span>
          )}
        </div>
      </div>
    </button>
  )
}

export function Avatar({ name, pictureUrl, size = 'md' }) {
  const dim = size === 'sm' ? 32 : size === 'lg' ? 48 : 40
  const fontSize = size === 'sm' ? 12 : size === 'lg' ? 16 : 14
  const initials = name?.split(' ').map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'

  const fallback = (
    <div style={{
      width: dim, height: dim, borderRadius: '50%', flexShrink: 0,
      background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      color: '#fff', fontSize, fontWeight: 700,
    }}>
      {initials}
    </div>
  )

  if (!pictureUrl) return fallback
  return <ImgWithFallback src={pictureUrl} alt={name} dim={dim} fallback={fallback} />
}

function ImgWithFallback({ src, alt, dim, fallback }) {
  const [failed, setFailed] = useState(false)
  if (failed) return fallback
  return (
    <img src={src} alt={alt} onError={() => setFailed(true)}
      style={{ width: dim, height: dim, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
    />
  )
}
