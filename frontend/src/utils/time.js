export function formatDistanceToNow(timestamp) {
  if (!timestamp) return ''
  const diff = Date.now() - new Date(timestamp).getTime()
  const mins  = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days  = Math.floor(diff / 86400000)
  if (mins < 1)   return 'now'
  if (mins < 60)  return `${mins}m`
  if (hours < 24) return `${hours}h`
  if (days < 7)   return `${days}d`
  return new Date(timestamp).toLocaleDateString([], { month: 'short', day: 'numeric' })
}
