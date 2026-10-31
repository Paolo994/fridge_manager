import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth'
import NotificationBell from './NotificationBell'

const links = [
  { to: '/', label: 'Frigo', icon: '🧊' },
  { to: '/shopping', label: 'Spesa', icon: '🛒' },
  { to: '/recipes', label: 'Ricette', icon: '🍳' },
  { to: '/assistant', label: 'Assistente', icon: '💬' },
]

export default function Layout() {
  const { user, logout } = useAuth()
  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">🥬 Fridge Manager</div>
        <nav className="nav">
          {links.map(l => (
            <NavLink key={l.to} to={l.to} end className={({ isActive }) => (isActive ? 'active' : '')}>
              <span aria-hidden>{l.icon}</span> <span className="nav-label">{l.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="topbar-right">
          <NotificationBell />
          <span className="muted user-name">{user?.displayName}</span>
          <button className="btn ghost small" onClick={logout}>Esci</button>
        </div>
      </header>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
