import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useChat } from '../../context/ChatContext'
import ConversationItem, { Avatar } from './ConversationItem'
import NewChatModal from './NewChatModal'

export default function Sidebar() {
  const { auth, signOut }                 = useAuth()
  const { conversations, setConversations,
          activeConversation,
          setActiveConversation,
          setMessages }                   = useChat()
  const [showModal, setShowModal]         = useState(false)

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
      width: '300px', flexShrink: 0, background: '#fff',
      borderRight: '1px solid #e2e8f0',
      display: 'flex', flexDirection: 'column', height: '100vh',
    }}>
      {/* Profile bar */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px', borderBottom: '1px solid #f1f5f9',
        background: '#fff',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Avatar name={auth.user.name} pictureUrl={auth.user.pictureUrl} size="sm" />
          <div>
            <p style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#1e293b' }}>
              {auth.user.name}
            </p>
            <p style={{ margin: 0, fontSize: '11px', color: '#94a3b8' }}>
              {auth.user.preferredLanguage}
            </p>
          </div>
        </div>
        <button onClick={signOut} title="Sign out" style={{
          background: 'none', border: 'none', cursor: 'pointer',
          fontSize: '18px', color: '#94a3b8', padding: '4px',
          borderRadius: '6px', lineHeight: 1,
        }}
        onMouseEnter={e => e.currentTarget.style.color = '#ef4444'}
        onMouseLeave={e => e.currentTarget.style.color = '#94a3b8'}
        >
          ⏻
        </button>
      </div>

      {/* New Chat button */}
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #f1f5f9' }}>
        <button onClick={() => setShowModal(true)} style={{
          width: '100%', padding: '10px', background: '#2563eb',
          color: '#fff', border: 'none', borderRadius: '10px',
          fontSize: '14px', fontWeight: 600, cursor: 'pointer',
          transition: 'background .2s',
        }}
        onMouseEnter={e => e.currentTarget.style.background = '#1d4ed8'}
        onMouseLeave={e => e.currentTarget.style.background = '#2563eb'}
        >
          + New Chat
        </button>
      </div>

      {/* Conversations */}
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {conversations.length === 0 ? (
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center',
            justifyContent: 'center', height: '100%', color: '#94a3b8',
            padding: '32px 24px', textAlign: 'center', gap: '10px',
          }}>
            <span style={{ fontSize: '40px' }}>💬</span>
            <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.5 }}>
              No conversations yet.<br />Click <strong>+ New Chat</strong> to start one.
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

      {showModal && <NewChatModal onSelect={handleSelectUser} onClose={() => setShowModal(false)} />}
    </aside>
  )
}
