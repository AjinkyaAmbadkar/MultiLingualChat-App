const BASE = '/api/messages'

export async function getConversations(token) {
  const res = await fetch(`${BASE}/conversations`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Failed to fetch conversations')
  return res.json()
}

export async function getChatHistory(token, user1Id, user2Id) {
  const res = await fetch(`${BASE}/history?user1Id=${user1Id}&user2Id=${user2Id}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new Error('Failed to fetch chat history')
  return res.json()
}
