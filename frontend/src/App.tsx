import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './components/Layout'
import AuthPage from './pages/AuthPage'
import FridgePage from './pages/FridgePage'
import ShoppingPage from './pages/ShoppingPage'
import RecipesPage from './pages/RecipesPage'
import AssistantPage from './pages/AssistantPage'

export default function App() {
  const { user, loading } = useAuth()

  if (loading) return <div className="center muted">Caricamento…</div>

  if (!user) {
    return (
      <Routes>
        <Route path="/login" element={<AuthPage mode="login" />} />
        <Route path="/register" element={<AuthPage mode="register" />} />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<FridgePage />} />
        <Route path="/shopping" element={<ShoppingPage />} />
        <Route path="/recipes" element={<RecipesPage />} />
        <Route path="/assistant" element={<AssistantPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
