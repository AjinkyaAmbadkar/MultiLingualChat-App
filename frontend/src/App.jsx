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
  const { setConversations, addMessage } = useChat()

  // WebSocket — connects only when logged in
  const { sendMessage } = useWebSocket({
    token:     auth?.token,
    userId:    auth?.user?.id,
    onMessage: addMessage,
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
      <ChatWindow sendMessage={sendMessage} />
    </div>
  )
}
