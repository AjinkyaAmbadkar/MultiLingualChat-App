import { useEffect } from 'react'
import { useAuth } from './context/AuthContext'
import { useChat } from './context/ChatContext'
import { useWebSocket } from './hooks/useWebSocket'
import { getConversations } from './api/messages'
import AuthPage from './components/Auth/AuthPage'
import Sidebar from './components/Sidebar/Sidebar'
import ChatWindow from './components/Chat/ChatWindow'

export default function App() {
  const { auth }                         = useAuth()
  const { setConversations, addMessage, setTyping, setPresence } = useChat()

  // WebSocket — connects only when logged in
  const { sendMessage, sendTyping } = useWebSocket({
    token:      auth?.token,
    userId:     auth?.user?.id,
    onMessage:  addMessage,
    onTyping:   (event) => setTyping(event.senderId, event.typing),
    onPresence: (event) => setPresence(event.userId, event.online),
  })

  // Load conversation list on login
  useEffect(() => {
    if (!auth) return
    getConversations(auth.token)
      .then(setConversations)
      .catch(console.error)
  }, [auth?.token])

  if (!auth) return <AuthPage />

  return (
    <div className="flex h-screen bg-slate-100 overflow-hidden">
      <Sidebar />
      <ChatWindow sendMessage={sendMessage} sendTyping={sendTyping} />
    </div>
  )
}
