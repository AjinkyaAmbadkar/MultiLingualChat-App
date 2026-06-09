import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * Manages the STOMP WebSocket connection.
 *
 * - Connects on mount with the JWT in STOMP CONNECT headers
 * - Subscribes to /topic/user.{myUserId} for incoming messages
 * - Exposes sendMessage() for the chat input
 * - Disconnects cleanly on unmount
 */
export function useWebSocket({ token, userId, onMessage }) {
  const clientRef = useRef(null)

  useEffect(() => {
    if (!token || !userId) return

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => {
        client.subscribe(`/topic/user.${userId}`, (frame) => {
          const msg = JSON.parse(frame.body)
          onMessage(msg)
        })
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message)
      },
    })

    client.activate()
    clientRef.current = client

    return () => { client.deactivate() }
  }, [token, userId]) // intentionally omit onMessage — it's stable via useCallback

  const sendMessage = useCallback((receiverId, originalText) => {
    if (!clientRef.current?.connected) return
    clientRef.current.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ receiverId, originalText }),
    })
  }, [])

  return { sendMessage }
}
