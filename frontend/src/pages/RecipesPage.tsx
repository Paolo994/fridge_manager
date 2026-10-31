import { useState } from 'react'
import { api, type Recipe } from '../api'
import { useAiStatus } from '../useAiStatus'

export default function RecipesPage() {
  const aiEnabled = useAiStatus()
  const [preferences, setPreferences] = useState('')
  const [recipes, setRecipes] = useState<Recipe[] | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [added, setAdded] = useState<Record<number, boolean>>({})

  const suggest = async () => {
    setBusy(true)
    setError(null)
    setAdded({})
    try {
      const res = await api.post<{ recipes: Recipe[] }>('/ai/recipes', { preferences })
      setRecipes(res.recipes)
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const addMissing = async (i: number, r: Recipe) => {
    await api.post('/shopping', { items: r.missingIngredients.map(name => ({ name, quantity: null })) })
    setAdded(a => ({ ...a, [i]: true }))
  }

  return (
    <section>
      <div className="page-head"><h2>Ricette</h2></div>
      <p className="muted">Ricette pensate per usare prima gli alimenti che stanno per scadere.</p>

      {aiEnabled === false ? (
        <div className="info">Serve l'IA: imposta <code>ANTHROPIC_API_KEY</code> sul backend.</div>
      ) : (
        <div className="inline-form">
          <input placeholder="Preferenze (es. vegetariano, veloce, senza glutine)…" value={preferences}
                 onChange={e => setPreferences(e.target.value)} maxLength={300} />
          <button className="btn primary" onClick={suggest} disabled={busy || !aiEnabled}>
            {busy ? 'Sto pensando…' : '✨ Suggerisci ricette'}
          </button>
        </div>
      )}

      {error && <div className="error">{error}</div>}

      {recipes && recipes.length === 0 && <p className="muted">Nessuna ricetta trovata.</p>}
      <div className="recipes">
        {recipes?.map((r, i) => (
          <article key={i} className="card recipe">
            <h3>{r.title}</h3>
            <p className="muted">⏱ {r.minutes} min · {r.description}</p>
            <p><strong>Dal frigo:</strong> {r.usedFridgeItems.join(', ') || '—'}</p>
            {r.missingIngredients.length > 0 && (
              <p>
                <strong>Da comprare:</strong> {r.missingIngredients.join(', ')}{' '}
                <button className="btn ghost small" onClick={() => addMissing(i, r)} disabled={added[i]}>
                  {added[i] ? '✓ In lista' : '🛒 Aggiungi alla spesa'}
                </button>
              </p>
            )}
            <ol>{r.steps.map((s, j) => <li key={j}>{s}</li>)}</ol>
          </article>
        ))}
      </div>
    </section>
  )
}
