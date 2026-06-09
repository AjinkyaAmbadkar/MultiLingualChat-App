import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket({ token, userId, onMessage, onTyping, onPresence }) {
  const clientRef = useRef(null)

  useEffect(() => {
    if (!token || !userId) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/user.${userId}`, (frame) => {
          const data = JSON.parse(frame.body)
          if (data.type === 'PRESENCE') {
            onPresence?.(data)
          } else if ('typing' in data) {
            onTyping?.(data)
          } else {
            onMessage(data)
          }
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message)
      },
    })

    client.activate()
    clientRef.current = client

    return () => { client.deactivate() }
  }, [token, userId])

  const sendMessage = useCallback((receiverId, originalText) => {
    if (!clientRef.current?.connected) return
    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ receiverId, originalText }),
    })
  }, [])

  const sendTyping = useCallback((receiverId, isTyping) => {
    if (!clientRef.current?.connected) return
    clientRef.current.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ receiverId, typing: isTyping }),
    })
  }, [])

  return { sendMessage, sendTyping }
}
