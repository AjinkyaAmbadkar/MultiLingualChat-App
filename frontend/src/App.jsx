import { useEffect, useCallback } from 'react'
import { useAuth } from './context/AuthContext'
import { useChat } from './context/ChatContext'
import { useWebSocket } from './hooks/useWebSocket'
import { useIsMobile } from './hooks/useIsMobile'
import { getConversations } from './api/messages'
import { decryptMessage } from './utils/crypto'
import AuthPage from './components/Auth/AuthPage'
import Sidebar from './components/Sidebar/Sidebar'
import ChatWindow from './components/Chat/ChatWindow'

export default function App() {
  const { auth, privateKey }             = useAuth()
  const { setConversations, addMessage, setTyping, setPresence, markMessagesRead, activeConversation } = useChat()
  const isMobile = useIsMobile()

  // Decrypt then add — keeps MessageBubble unaware of encryption
  const onMessage = useCallback(async (msg) => {
    const decrypted = await decryptMessage(msg, privateKey, auth?.user?.id)
    addMessage(decrypted, auth?.user?.id)
  }, [privateKey, auth?.user?.id, addMessage])

  // WebSocket — connects only when logged in
  const { sendMessage, sendTyping, sendReadReceipt } = useWebSocket({
    token:           auth?.token,
    userId:          auth?.user?.id,
    onMessage,
    onTyping:        (event) => setTyping(event.senderId, event.typing),
    onPresence:      (event) => setPresence(event.userId, event.online),
    onReadReceipt:   (event) => markMessagesRead(event.senderId),
  })

  // Load conversation list on login
  useEffect(() => {
    if (!auth) return
    getConversations(auth.token)
      .then(setConversations)
      .catch(console.error)
  }, [auth?.token])

  if (!auth) return <AuthPage />

  // Mobile: WhatsApp-style single pane — conversation list OR open chat, never both.
  // 100dvh (not 100vh) so the layout tracks the real visible area when the
  // mobile browser's address bar collapses/expands.
  return (
    <div style={{ display: 'flex', height: '100dvh', overflow: 'hidden', background: '#f1f5f9' }}>
      {(!isMobile || !activeConversation) && <Sidebar />}
      {(!isMobile || activeConversation) && (
        <ChatWindow sendMessage={sendMessage} sendTyping={sendTyping} sendReadReceipt={sendReadReceipt} />
      )}
    </div>
  )
}
