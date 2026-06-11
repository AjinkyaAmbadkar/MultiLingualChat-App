import { createContext, useContext, useState, useCallback } from 'react'

const ChatContext = createContext(null)

export function ChatProvider({ children }) {
  const [conversations, setConversations] = useState([])
  const [activeConversation, setActiveConversation] = useState(null) // { userId, name, pictureUrl }
  const [messages, setMessages] = useState([])                        // messages for active conversation
  const [typingUsers, setTypingUsers] = useState({})                  // { [userId]: true/false }
  const [onlineUsers, setOnlineUsers] = useState({})                  // { [userId]: true/false }

  const setTyping = useCallback((senderId, isTyping) => {
    setTypingUsers(prev => ({ ...prev, [senderId]: isTyping }))
  }, [])

  const setPresence = useCallback((userId, online) => {
    setOnlineUsers(prev => ({ ...prev, [userId]: online }))
  }, [])

  // Mark all messages from senderId as read in local state
  const markMessagesRead = useCallback((senderId) => {
    setMessages(prev => prev.map(m =>
      String(m.senderId) === String(senderId) ? { ...m, isRead: true } : m
    ))
  }, [])

  // Called by useWebSocket when a new message arrives over STOMP
  const addMessage = useCallback((msg, myId) => {
    setMessages(prev => {
      if (prev.some(m => m.id === msg.id)) return prev
      return [...prev, msg]
    })

    // Show preview in the current user's language
    const previewText = String(msg.senderId) === String(myId)
      ? msg.originalText
      : (msg.translatedText || msg.originalText)

    // Update the conversation list preview with the latest message
    setConversations(prev => prev.map(c => {
      const isThisConversation =
        String(c.userId) === String(msg.senderId) ||
        String(c.userId) === String(msg.receiverId)
      if (!isThisConversation) return c
      return { ...c, lastMessage: previewText, lastMessageTime: msg.timestamp }
    }))
  }, [])

  return (
    <ChatContext.Provider value={{
      conversations, setConversations,
      activeConversation, setActiveConversation,
      messages, setMessages,
      addMessage,
      typingUsers, setTyping,
      onlineUsers, setPresence,
      markMessagesRead,
    }}>
      {children}
    </ChatContext.Provider>
  )
}

export function useChat() {
  return useContext(ChatContext)
}
