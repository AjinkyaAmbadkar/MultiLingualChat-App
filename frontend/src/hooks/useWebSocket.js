import { useEffect, useRef, useCallback } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

export function useWebSocket({ token, userId, onMessage, onTyping, onPresence, onReadReceipt }) {
  const clientRef = useRef(null)

  // Keep refs to the latest callbacks so the subscription closure is never stale.
  // Without this, callbacks captured at connect time would miss updates (e.g. when
  // onMessage gains a privateKey dependency after login).
  const onMessageRef     = useRef(onMessage)
  const onTypingRef      = useRef(onTyping)
  const onPresenceRef    = useRef(onPresence)
  const onReadReceiptRef = useRef(onReadReceipt)

  useEffect(() => { onMessageRef.current     = onMessage     }, [onMessage])
  useEffect(() => { onTypingRef.current      = onTyping      }, [onTyping])
  useEffect(() => { onPresenceRef.current    = onPresence    }, [onPresence])
  useEffect(() => { onReadReceiptRef.current = onReadReceipt }, [onReadReceipt])

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
            onPresenceRef.current?.(data)
          } else if (data.type === 'READ_RECEIPT') {
            onReadReceiptRef.current?.(data)
          } else if ('typing' in data) {
            onTypingRef.current?.(data)
          } else {
            onMessageRef.current?.(data)
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

  const sendReadReceipt = useCallback((senderId) => {
    if (!clientRef.current?.connected) return
    clientRef.current.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ senderId }),
    })
  }, [])

  return { sendMessage, sendTyping, sendReadReceipt }
}
