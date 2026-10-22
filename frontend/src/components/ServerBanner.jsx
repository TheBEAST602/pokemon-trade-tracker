import { useSyncExternalStore } from 'react'
import { serverStatus } from '../api/client.js'

/** Free hosting sleeps when idle; tell the user why the first load is slow. */
export default function ServerBanner() {
  const slow = useSyncExternalStore(serverStatus.subscribe, serverStatus.getSnapshot)
  if (!slow) return null
  return (
    <div className="banner" role="status">
      Waking the server. Free hosting sleeps when idle, so the first load can take up to a minute.
    </div>
  )
}
