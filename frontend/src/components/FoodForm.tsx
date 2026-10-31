import { useState, type FormEvent } from 'react'
import { CATEGORIES, type Category, type Food, type FoodInput } from '../api'

interface Props {
  initial?: Food
  onSubmit: (input: FoodInput) => Promise<void>
  onCancel: () => void
}

export default function FoodForm({ initial, onSubmit, onCancel }: Props) {
  const [name, setName] = useState(initial?.name ?? '')
  const [category, setCategory] = useState<Category>(initial?.category ?? 'OTHER')
  const [quantity, setQuantity] = useState(initial?.quantity?.toString() ?? '')
  const [unit, setUnit] = useState(initial?.unit ?? '')
  const [expiryDate, setExpiryDate] = useState(initial?.expiryDate ?? '')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await onSubmit({
        name,
        category,
        quantity: quantity === '' ? null : Number(quantity),
        unit: unit || null,
        expiryDate: expiryDate || null,
        source: initial?.source ?? 'MANUAL',
      })
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form-grid" onSubmit={submit}>
      <h3>{initial ? 'Modifica alimento' : 'Nuovo alimento'}</h3>
      <label className="span2">Nome
        <input value={name} onChange={e => setName(e.target.value)} required maxLength={120} autoFocus />
      </label>
      <label>Categoria
        <select value={category} onChange={e => setCategory(e.target.value as Category)}>
          {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.icon} {c.label}</option>)}
        </select>
      </label>
      <label>Scadenza
        <input type="date" value={expiryDate} onChange={e => setExpiryDate(e.target.value)} />
      </label>
      <label>Quantità
        <input type="number" min="0" step="any" value={quantity} onChange={e => setQuantity(e.target.value)} />
      </label>
      <label>Unità
        <input value={unit} onChange={e => setUnit(e.target.value)} placeholder="pz, g, l…" maxLength={20} />
      </label>
      {error && <div className="error span2">{error}</div>}
      <div className="actions span2">
        <button type="button" className="btn ghost" onClick={onCancel}>Annulla</button>
        <button className="btn primary" disabled={busy}>{busy ? 'Salvataggio…' : 'Salva'}</button>
      </div>
    </form>
  )
}
