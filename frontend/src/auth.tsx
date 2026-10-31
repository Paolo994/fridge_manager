import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler, tokenStore, type AuthResponse, type User } from './api'

interface AuthState {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (!tokenStore.get()) { setLoading(false); return }
    api.get<User>('/auth/me')
      .then(setUser)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [logout])

  const handleAuth = (res: AuthResponse) => {
    tokenStore.set(res.token)
    setUser(res.user)
  }

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    logout,
    login: async (email, password) => handleAuth(await api.post<AuthResponse>('/auth/login', { email, password })),
    register: async (email, password, displayName) =>
      handleAuth(await api.post<AuthResponse>('/auth/register', { email, password, displayName })),
  }), [user, loading, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth fuori da AuthProvider')
  return ctx
}
