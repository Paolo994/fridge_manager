import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'

export default function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { login, register } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const isLogin = mode === 'login'

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      if (isLogin) await login(email, password)
      else await register(email, password, name)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <form className="card auth-card" onSubmit={submit}>
        <h1>🥬 Fridge Manager</h1>
        <p className="muted">{isLogin ? 'Accedi al tuo frigo' : 'Crea un account'}</p>
        {!isLogin && (
          <label>Nome
            <input value={name} onChange={e => setName(e.target.value)} required maxLength={80} autoComplete="name" />
          </label>
        )}
        <label>Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>Password
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} required
                 minLength={isLogin ? undefined : 8} autoComplete={isLogin ? 'current-password' : 'new-password'} />
        </label>
        {!isLogin && <small className="muted">Almeno 8 caratteri.</small>}
        {error && <div className="error">{error}</div>}
        <button className="btn primary" disabled={busy}>{busy ? 'Attendi…' : isLogin ? 'Accedi' : 'Registrati'}</button>
        <p className="muted center-text">
          {isLogin ? <>Non hai un account? <Link to="/register">Registrati</Link></>
                   : <>Hai già un account? <Link to="/login">Accedi</Link></>}
        </p>
      </form>
    </div>
  )
}
