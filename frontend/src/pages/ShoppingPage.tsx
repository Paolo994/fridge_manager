import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { api, type ShoppingItem } from '../api'

export default function ShoppingPage() {
  const [items, setItems] = useState<ShoppingItem[]>([])
  const [name, setName] = useState('')
  const [quantity, setQuantity] = useState('')

  const load = useCallback(async () => setItems(await api.get<ShoppingItem[]>('/shopping')), [])
  useEffect(() => { load() }, [load])

  const add = async (e: FormEvent) => {
    e.preventDefault()
    if (!name.trim()) return
    await api.post('/shopping', { items: [{ name, quantity: quantity || null }] })
    setName('')
    setQuantity('')
    load()
  }

  const toggle = async (i: ShoppingItem) => {
    await api.patch(`/shopping/${i.id}`, { checked: !i.checked })
    load()
  }

  const remove = async (i: ShoppingItem) => {
    await api.del(`/shopping/${i.id}`)
    load()
  }

  const clearChecked = async () => {
    await api.del('/shopping/checked')
    load()
  }

  const toBuy = items.filter(i => !i.checked).length

  return (
    <section>
      <div className="page-head">
        <h2>Lista della spesa</h2>
        {items.some(i => i.checked) && <button className="btn ghost" onClick={clearChecked}>Elimina comprati</button>}
      </div>

      <form className="inline-form" onSubmit={add}>
        <input placeholder="Cosa devi comprare?" value={name} onChange={e => setName(e.target.value)} maxLength={120} />
        <input placeholder="Quantità" value={quantity} onChange={e => setQuantity(e.target.value)} maxLength={40} className="short" />
        <button className="btn primary">Aggiungi</button>
      </form>

      {items.length === 0 ? (
        <div className="empty"><p>La lista è vuota.</p>
          <p className="muted">Quando finisci un alimento usa 🛒 nel frigo per aggiungerlo qui.</p></div>
      ) : (
        <>
          <p className="muted">{toBuy} da comprare</p>
          <ul className="shopping-list">
            {items.map(i => (
              <li key={i.id} className={i.checked ? 'checked' : ''}>
                <label>
                  <input type="checkbox" checked={i.checked} onChange={() => toggle(i)} />
                  <span>{i.name}</span>
                  {i.quantity && <span className="muted">· {i.quantity}</span>}
                </label>
                <button className="btn ghost small" onClick={() => remove(i)} title="Rimuovi">✕</button>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
