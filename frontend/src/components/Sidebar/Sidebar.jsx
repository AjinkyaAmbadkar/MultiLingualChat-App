import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useChat } from '../../context/ChatContext'
import ConversationItem, { Avatar } from './ConversationItem'
import NewChatModal from './NewChatModal'
import SettingsModal from './SettingsModal'

export default function Sidebar() {
  const { auth, signOut }                 = useAuth()
  const { conversations, setConversations,
          activeConversation,
          setActiveConversation,
          setMessages }                   = useChat()
  const [showModal, setShowModal]         = useState(false)
  const [showSettings, setShowSettings]   = useState(false)

  function handleSelectUser(user) {
    setShowModal(false)
    const existing = conversations.find(c => c.userId === user.id)
    if (existing) {
      setActiveConversation({ userId: user.id, name: user.name, pictureUrl: user.pictureUrl })
      return
    }
    setConversations(prev => [{
      userId: user.id, name: user.name, pictureUrl: user.pictureUrl,
      lastMessage: '', lastMessageTime: null, unreadCount: 0,
    }, ...prev])
    setActiveConversation({ userId: user.id, name: user.name, pictureUrl: user.pictureUrl })
    setMessages([])
  }

  return (
    <aside style={{
      width: '300px', flexShrink: 0,
      background: '#0f172a',
      display: 'flex', flexDirection: 'column', height: '100vh',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    }}>

      {/* App branding bar */}
      <div style={{
        padding: '20px 16px 16px',
        borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <div style={{
            width: '32px', height: '32px', borderRadius: '9px',
            background: 'linear-gradient(135deg, #3b82f6, #1d4ed8)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px',
          }}>💬</div>
          <span style={{ fontSize: '15px', fontWeight: 700, color: '#f1f5f9', letterSpacing: '-0.2px' }}>
            MultiLingual Chat
          </span>
        </div>

        {/* Profile row */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Avatar name={auth.user.name} pictureUrl={auth.user.pictureUrl} size="sm" />
          <div>
            <p style={{ margin: 0, fontSize: '13px', fontWeight: 600, color: '#e2e8f0' }}>
              {auth.user.name}
            </p>
            <p style={{ margin: 0, fontSize: '11px', color: '#64748b' }}>
              {auth.user.preferredLanguage}
            </p>
          </div>
        </div>
      </div>

      {/* New Chat button */}
      <div style={{ padding: '12px 14px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
        <button onClick={() => setShowModal(true)} style={{
          width: '100%', padding: '10px', background: '#1d4ed8',
          color: '#fff', border: 'none', borderRadius: '10px',
          fontSize: '13px', fontWeight: 600, cursor: 'pointer', letterSpacing: '0.1px',
          transition: 'background .2s', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
        }}
        onMouseEnter={e => e.currentTarget.style.background = '#2563eb'}
        onMouseLeave={e => e.currentTarget.style.background = '#1d4ed8'}
        >
          <span style={{ fontSize: '16px', lineHeight: 1 }}>+</span> New Chat
        </button>
      </div>

      {/* Section label */}
      <div style={{ padding: '14px 16px 6px' }}>
        <span style={{ fontSize: '10px', fontWeight: 700, color: '#475569', letterSpacing: '1px', textTransform: 'uppercase' }}>
          Messages
        </span>
      </div>

      {/* Conversation list */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {conversations.length === 0 ? (
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '60%', color: '#475569',
            padding: '32px 20px', textAlign: 'center', gap: '10px',
          }}>
            <span style={{ fontSize: '36px' }}>💬</span>
            <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.6, color: '#64748b' }}>
              No conversations yet.<br />Click <strong style={{ color: '#93c5fd' }}>+ New Chat</strong> to start one.
            </p>
          </div>
        ) : conversations.map(c => (
          <ConversationItem
            key={c.userId}
            conversation={c}
            isActive={activeConversation?.userId === c.userId}
            onClick={() => {
              setActiveConversation({ userId: c.userId, name: c.name, pictureUrl: c.pictureUrl })
              setMessages([])
            }}
          />
        ))}
      </div>

      {/* Bottom footer — settings + sign out */}
      <div style={{
        borderTop: '1px solid rgba(255,255,255,0.06)',
        padding: '12px 16px',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      }}>
        <span style={{ fontSize: '11px', color: '#334155' }}>v1.0</span>
        <div style={{ display: 'flex', gap: '6px' }}>
          <button onClick={() => setShowSettings(true)} title="Settings" style={{
            background: 'none', border: '1px solid rgba(255,255,255,0.08)', cursor: 'pointer',
            fontSize: '15px', color: '#475569', padding: '6px 10px', borderRadius: '8px', lineHeight: 1,
            transition: 'all .15s',
          }}
          onMouseEnter={e => { e.currentTarget.style.color = '#93c5fd'; e.currentTarget.style.borderColor = '#3b82f6' }}
          onMouseLeave={e => { e.currentTarget.style.color = '#475569'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)' }}
          >⚙️</button>
          <button onClick={signOut} title="Sign out" style={{
            background: 'none', border: '1px solid rgba(255,255,255,0.08)', cursor: 'pointer',
            fontSize: '15px', color: '#475569', padding: '6px 10px', borderRadius: '8px', lineHeight: 1,
            transition: 'all .15s',
          }}
          onMouseEnter={e => { e.currentTarget.style.color = '#ef4444'; e.currentTarget.style.borderColor = '#ef4444' }}
          onMouseLeave={e => { e.currentTarget.style.color = '#475569'; e.currentTarget.style.borderColor = 'rgba(255,255,255,0.08)' }}
          >⏻</button>
        </div>
      </div>

      {showModal && <NewChatModal onSelect={handleSelectUser} onClose={() => setShowModal(false)} />}
      {showSettings && <SettingsModal onClose={() => setShowSettings(false)} />}
    </aside>
  )
}
