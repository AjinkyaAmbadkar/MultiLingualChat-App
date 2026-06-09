import { useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useChat } from '../../context/ChatContext'
import { useCallback } from 'react'
import { getChatHistory } from '../../api/messages'
import { Avatar } from '../Sidebar/ConversationItem'
import MessageBubble from './MessageBubble'
import MessageInput from './MessageInput'

export default function ChatWindow({ sendMessage, sendTyping }) {
  const { auth }                                                      = useAuth()
  const { activeConversation, messages, setMessages, typingUsers, onlineUsers } = useChat()

  const isPartnerTyping = activeConversation && typingUsers[activeConversation.userId]
  const isPartnerOnline = activeConversation && onlineUsers[activeConversation.userId]

  const handleTyping = useCallback((isTyping) => {
    if (activeConversation) sendTyping(activeConversation.userId, isTyping)
  }, [activeConversation, sendTyping])
  const bottomRef                                     = useRef(null)

  useEffect(() => {
    if (!activeConversation) return
    setMessages([])
    getChatHistory(auth.token, auth.user.id, activeConversation.userId)
      .then(setMessages).catch(console.error)
  }, [activeConversation?.userId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (!activeConversation) {
    return (
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        background: '#f8fafc',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      }}>
        <div style={{
          width: '80px', height: '80px', borderRadius: '24px',
          background: 'linear-gradient(135deg, #dbeafe, #bfdbfe)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: '40px', marginBottom: '20px',
        }}>💬</div>
        <p style={{ margin: '0 0 8px', fontSize: '20px', fontWeight: 700, color: '#1e293b' }}>
          Your messages
        </p>
        <p style={{ margin: 0, fontSize: '14px', color: '#94a3b8', textAlign: 'center', maxWidth: '280px', lineHeight: 1.6 }}>
          Select a conversation from the sidebar or start a new one
        </p>
      </div>
    )
  }

  const grouped = groupByDate(messages)

  return (
    <div style={{
      flex: 1, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      background: '#f8fafc',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '12px',
        padding: '14px 24px', background: '#fff',
        borderBottom: '1px solid #e2e8f0',
        boxShadow: '0 1px 6px rgba(0,0,0,0.06)',
      }}>
        <Avatar name={activeConversation.name} pictureUrl={activeConversation.pictureUrl} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span style={{ fontSize: '15px', fontWeight: 700, color: '#0f172a', lineHeight: 1.2 }}>
            {activeConversation.name}
          </span>
          {isPartnerTyping ? (
            <span style={{ fontSize: '12px', color: '#3b82f6', lineHeight: 1.2, display: 'flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#3b82f6', display: 'inline-block' }} />
              typing...
            </span>
          ) : isPartnerOnline ? (
            <span style={{ fontSize: '12px', color: '#22c55e', lineHeight: 1.2, display: 'flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#22c55e', display: 'inline-block' }} />
              Online
            </span>
          ) : (
            <span style={{ fontSize: '12px', color: '#94a3b8', lineHeight: 1.2, display: 'flex', alignItems: 'center', gap: '4px' }}>
              <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#94a3b8', display: 'inline-block' }} />
              Offline
            </span>
          )}
        </div>
      </div>

      {/* Messages */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: '16px 24px',
        background: '#f1f5f9',
      }}>
        {grouped.map(({ date, msgs }) => (
          <div key={date}>
            <DateSeparator date={date} />
            {msgs.map(msg => (
              <MessageBubble
                key={msg.id}
                message={msg}
                isSent={String(msg.senderId) === String(auth.user.id)}
              />
            ))}
          </div>
        ))}
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', marginTop: '80px', color: '#94a3b8', fontSize: '14px' }}>
            No messages yet — say hello! 👋
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {isPartnerTyping && (
        <div style={{ padding: '0 24px 8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: '4px',
            background: '#fff', borderRadius: '20px 20px 20px 4px',
            padding: '10px 16px', boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}>
            {[0, 1, 2].map(i => (
              <span key={i} style={{
                width: '7px', height: '7px', borderRadius: '50%', background: '#94a3b8',
                display: 'inline-block',
                animation: 'typingBounce 1.2s infinite',
                animationDelay: `${i * 0.2}s`,
              }} />
            ))}
          </div>
          <span style={{ fontSize: '12px', color: '#94a3b8' }}>
            {activeConversation.name.split(' ')[0]} is typing…
          </span>
        </div>
      )}

      <MessageInput
        onSend={text => sendMessage(activeConversation.userId, text)}
        onTyping={handleTyping}
      />
    </div>
  )
}

function DateSeparator({ date }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', margin: '20px 0 14px' }}>
      <div style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
      <span style={{
        fontSize: '11px', color: '#94a3b8', background: '#fff',
        padding: '4px 12px', borderRadius: '20px', fontWeight: 500,
        border: '1px solid #e2e8f0',
      }}>
        {date}
      </span>
      <div style={{ flex: 1, height: '1px', background: '#e2e8f0' }} />
    </div>
  )
}

function groupByDate(messages) {
  const map = new Map()
  for (const msg of messages) {
    const date = msg.timestamp
      ? new Date(msg.timestamp).toLocaleDateString([], { month: 'long', day: 'numeric' })
      : 'Unknown'
    if (!map.has(date)) map.set(date, [])
    map.get(date).push(msg)
  }
  return Array.from(map.entries()).map(([date, msgs]) => ({ date, msgs }))
}
