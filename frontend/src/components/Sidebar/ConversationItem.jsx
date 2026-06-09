import { useState } from 'react'
import { formatDistanceToNow } from '../../utils/time'

export default function ConversationItem({ conversation, isActive, onClick }) {
  const { name, pictureUrl, lastMessage, lastMessageTime, unreadCount } = conversation

  return (
    <button onClick={onClick} style={{
      width: '100%', display: 'flex', alignItems: 'center', gap: '12px',
      padding: '12px 16px', border: 'none', background: isActive ? '#eff6ff' : 'transparent',
      borderLeft: isActive ? '3px solid #2563eb' : '3px solid transparent',
      cursor: 'pointer', textAlign: 'left', transition: 'background .15s',
    }}
    onMouseEnter={e => { if (!isActive) e.currentTarget.style.background = '#f8fafc' }}
    onMouseLeave={e => { if (!isActive) e.currentTarget.style.background = 'transparent' }}
    >
      <Avatar name={name} pictureUrl={pictureUrl} />
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '3px' }}>
        {/* Row 1: name + time */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{
            fontSize: '14px', fontWeight: 600, lineHeight: 1.2,
            color: isActive ? '#1d4ed8' : '#1e293b',
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
            flex: 1, marginRight: '8px',
          }}>
            {name}
          </span>
          <span style={{ fontSize: '11px', color: '#94a3b8', flexShrink: 0, lineHeight: 1.2 }}>
            {lastMessageTime ? formatDistanceToNow(lastMessageTime) : ''}
          </span>
        </div>
        {/* Row 2: last message + unread badge */}
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
              background: '#2563eb', color: '#fff', fontSize: '11px', fontWeight: 600,
              borderRadius: '10px', padding: '1px 7px', flexShrink: 0,
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
      color: '#fff', fontSize, fontWeight: 700, letterSpacing: '0.5px',
    }}>
      {initials}
    </div>
  )

  if (!pictureUrl) return fallback

  return (
    <ImgWithFallback
      src={pictureUrl} alt={name} dim={dim} fallback={fallback}
    />
  )
}

function ImgWithFallback({ src, alt, dim, fallback }) {
  const [failed, setFailed] = useState(false)
  if (failed) return fallback
  return (
    <img src={src} alt={alt}
      onError={() => setFailed(true)}
      style={{ width: dim, height: dim, borderRadius: '50%', objectFit: 'cover', flexShrink: 0 }}
    />
  )
}
