import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, categoryInfo, CATEGORIES, type Category, type Food, type FoodInput } from '../api'
import FoodForm from '../components/FoodForm'
import PhotoImport from '../components/PhotoImport'
import { useAiStatus } from '../useAiStatus'

type Panel = null | 'manual' | 'photo' | 'fridge' | { edit: Food }

function expiryLabel(f: Food) {
  if (f.daysToExpiry === null) return 'Nessuna scadenza'
  const d = f.daysToExpiry
  if (d < -1) return `Scaduto da ${-d} giorni`
  if (d === -1) return 'Scaduto ieri'
  if (d === 0) return 'Scade oggi'
  if (d === 1) return 'Scade domani'
  return `Scade tra ${d} giorni`
}

export default function FridgePage() {
  const [items, setItems] = useState<Food[]>([])
  const [loading, setLoading] = useState(true)
  const [panel, setPanel] = useState<Panel>(null)
  const [filter, setFilter] = useState<Category | 'ALL'>('ALL')
  const [query, setQuery] = useState('')
  const aiEnabled = useAiStatus()

  const load = useCallback(async () => {
    setItems(await api.get<Food[]>('/food'))
    setLoading(false)
  }, [])

  useEffect(() => { load() }, [load])

  const visible = useMemo(() => items.filter(f =>
    (filter === 'ALL' || f.category === filter) &&
    f.name.toLowerCase().includes(query.trim().toLowerCase())), [items, filter, query])

  const counts = useMemo(() => ({
    expired: items.filter(f => f.status === 'EXPIRED').length,
    soon: items.filter(f => f.status === 'EXPIRING_SOON').length,
  }), [items])

  const save = async (input: FoodInput) => {
    if (panel && typeof panel === 'object') await api.put(`/food/${panel.edit.id}`, input)
    else await api.post('/food', input)
    setPanel(null)
    load()
  }

  const remove = async (f: Food, addToShopping: boolean) => {
    await api.del(`/food/${f.id}?addToShopping=${addToShopping}`)
    load()
  }

  return (
    <section>
      <div className="page-head">
        <h2>Il mio frigo</h2>
        <div className="actions wrap">
          <button className="btn primary" onClick={() => setPanel('manual')}>＋ Aggiungi</button>
          <button className="btn secondary" onClick={() => setPanel('photo')} disabled={!aiEnabled}
                  title={aiEnabled ? '' : 'IA non configurata sul server'}>📷 Da foto</button>
          <button className="btn secondary" onClick={() => setPanel('fridge')} disabled={!aiEnabled}
                  title={aiEnabled ? '' : 'IA non configurata sul server'}>🧊 Scansiona frigo</button>
        </div>
      </div>

      {aiEnabled === false && (
        <div className="info">Le funzioni IA (foto, ricette, assistente) sono disattivate: manca <code>ANTHROPIC_API_KEY</code> sul backend.</div>
      )}

      {(counts.expired > 0 || counts.soon > 0) && (
        <div className="summary">
          {counts.expired > 0 && <span className="pill expired">{counts.expired} scaduti</span>}
          {counts.soon > 0 && <span className="pill soon">{counts.soon} in scadenza</span>}
        </div>
      )}

      {panel === 'manual' && <FoodForm onSubmit={save} onCancel={() => setPanel(null)} />}
      {panel && typeof panel === 'object' && <FoodForm initial={panel.edit} onSubmit={save} onCancel={() => setPanel(null)} />}
      {(panel === 'photo' || panel === 'fridge') && (
        <PhotoImport mode={panel === 'photo' ? 'food' : 'fridge'}
                     onSaved={() => { setPanel(null); load() }} onCancel={() => setPanel(null)} />
      )}

      <div className="filters">
        <input placeholder="Cerca…" value={query} onChange={e => setQuery(e.target.value)} />
        <select value={filter} onChange={e => setFilter(e.target.value as Category | 'ALL')}>
          <option value="ALL">Tutte le categorie</option>
          {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.icon} {c.label}</option>)}
        </select>
      </div>

      {loading ? <p className="muted">Caricamento…</p> : visible.length === 0 ? (
        <div className="empty">
          <p>{items.length === 0 ? 'Il frigo è vuoto.' : 'Nessun alimento corrisponde ai filtri.'}</p>
          {items.length === 0 && <p className="muted">Aggiungi il primo alimento a mano o con una foto.</p>}
        </div>
      ) : (
        <ul className="food-list">
          {visible.map(f => {
            const cat = categoryInfo(f.category)
            return (
              <li key={f.id} className={`food status-${f.status}`}>
                <span className="food-icon" aria-hidden>{cat.icon}</span>
                <div className="food-main">
                  <strong>{f.name}</strong>
                  <span className="muted">
                    {cat.label}{f.quantity != null && ` · ${f.quantity}${f.unit ? ' ' + f.unit : ''}`}
                  </span>
                </div>
                <span className={`expiry status-${f.status}`}>{expiryLabel(f)}</span>
                <div className="food-actions">
                  <button className="btn ghost small" title="Modifica" onClick={() => setPanel({ edit: f })}>✏️</button>
                  <button className="btn ghost small" title="Finito: rimuovi e aggiungi alla spesa"
                          onClick={() => remove(f, true)}>🛒</button>
                  <button className="btn ghost small" title="Rimuovi" onClick={() => remove(f, false)}>🗑️</button>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
