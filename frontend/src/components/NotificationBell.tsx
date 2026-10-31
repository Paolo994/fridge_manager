import { useCallback, useEffect, useRef, useState } from 'react'
import { api, type Notification } from '../api'

interface Res { unread: number; items: Notification[] }

const POLL_MS = 60_000

export default function NotificationBell() {
  const [data, setData] = useState<Res>({ unread: 0, items: [] })
  const [open, setOpen] = useState(false)
  const lastUnread = useRef(0)

  const load = useCallback(async () => {
    try {
      const res = await api.get<Res>('/notifications')
      // Notifica del browser se arrivano nuovi avvisi e l'utente l'ha permessa
      if (res.unread > lastUnread.current && 'Notification' in window && window.Notification.permission === 'granted') {
        const newest = res.items.find(n => !n.read)
        if (newest) new window.Notification('Fridge Manager', { body: newest.message })
      }
      lastUnread.current = res.unread
      setData(res)
    } catch { /* riprova al prossimo giro */ }
  }, [])

  useEffect(() => {
    load()
    const t = setInterval(load, POLL_MS)
    return () => clearInterval(t)
  }, [load])

  const toggle = () => {
    setOpen(o => !o)
    if ('Notification' in window && window.Notification.permission === 'default') {
      window.Notification.requestPermission()
    }
  }

  const markAll = async () => {
    await api.post('/notifications/read-all')
    load()
  }

  const markOne = async (n: Notification) => {
    if (n.read) return
    await api.post(`/notifications/${n.id}/read`)
    load()
  }

  return (
    <div className="bell">
      <button className="btn ghost small" onClick={toggle} aria-label="Notifiche">
        🔔{data.unread > 0 && <span className="badge-count">{data.unread}</span>}
      </button>
      {open && (
        <div className="dropdown">
          <div className="dropdown-head">
            <strong>Avvisi di scadenza</strong>
            {data.unread > 0 && <button className="link" onClick={markAll}>Segna tutti letti</button>}
          </div>
          {data.items.length === 0 && <p className="muted pad">Nessun avviso.</p>}
          <ul>
            {data.items.map(n => (
              <li key={n.id} className={n.read ? 'read' : ''} onClick={() => markOne(n)}>
                <span>{n.message}</span>
                <small className="muted">{new Date(n.createdAt).toLocaleDateString('it-IT')}</small>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
