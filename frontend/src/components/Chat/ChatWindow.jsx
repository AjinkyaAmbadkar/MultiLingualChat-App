import { useEffect, useRef } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useChat } from '../../context/ChatContext'
import { getChatHistory } from '../../api/messages'
import { Avatar } from '../Sidebar/ConversationItem'
import MessageBubble from './MessageBubble'
import MessageInput from './MessageInput'

export default function ChatWindow({ sendMessage }) {
  const { auth }                                    = useAuth()
  const { activeConversation, messages, setMessages } = useChat()
  const bottomRef                                   = useRef(null)

  useEffect(() => {
    if (!activeConversation) return
    setMessages([])
    getChatHistory(auth.token, auth.user.id, activeConversation.userId)
      .then(setMessages)
      .catch(console.error)
  }, [activeConversation?.userId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  if (!activeConversation) {
    return (
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center',
        background: '#f8fafc', color: '#94a3b8', gap: '12px',
        fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
      }}>
        <span style={{ fontSize: '64px' }}>💬</span>
        <p style={{ margin: 0, fontSize: '18px', fontWeight: 600, color: '#64748b' }}>
          Select a conversation
        </p>
        <p style={{ margin: 0, fontSize: '13px' }}>
          Or start a new one from the sidebar
        </p>
      </div>
    )
  }

  const grouped = groupByDate(messages)

  return (
    <div style={{
      flex: 1, display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    }}>
      {/* Header */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '12px',
        padding: '14px 20px', background: '#fff',
        borderBottom: '1px solid #e2e8f0',
        boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
      }}>
        <Avatar name={activeConversation.name} pictureUrl={activeConversation.pictureUrl} />
        <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span style={{ fontSize: '15px', fontWeight: 700, color: '#1e293b', lineHeight: 1.2 }}>
            {activeConversation.name}
          </span>
          <span style={{ fontSize: '12px', color: '#22c55e', lineHeight: 1.2 }}>
            Online
          </span>
        </div>
      </div>

      {/* Messages area */}
      <div style={{
        flex: 1, overflowY: 'auto', padding: '20px 24px',
        background: '#f1f5f9',
        backgroundImage: 'radial-gradient(circle at 1px 1px, #e2e8f0 1px, transparent 0)',
        backgroundSize: '24px 24px',
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
          <div style={{ textAlign: 'center', marginTop: '60px', color: '#94a3b8', fontSize: '14px' }}>
            No messages yet. Say hello! 👋
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <MessageInput onSend={text => sendMessage(activeConversation.userId, text)} />
    </div>
  )
}

function DateSeparator({ date }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', margin: '16px 0 12px' }}>
      <div style={{ flex: 1, height: '1px', background: '#cbd5e1' }} />
      <span style={{
        fontSize: '11px', color: '#64748b', background: '#e2e8f0',
        padding: '3px 10px', borderRadius: '10px', fontWeight: 500,
      }}>
        {date}
      </span>
      <div style={{ flex: 1, height: '1px', background: '#cbd5e1' }} />
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
