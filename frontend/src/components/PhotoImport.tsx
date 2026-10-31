import { useState } from 'react'
import { api, CATEGORIES, type Category, type FoodInput, type RecognizedFood } from '../api'

interface Props {
  mode: 'food' | 'fridge'
  onSaved: () => void
  onCancel: () => void
}

interface Row extends FoodInput { selected: boolean; fromLabel: boolean; confidence: number }

const toCategory = (c: string): Category =>
  (CATEGORIES.find(x => x.value === c.toUpperCase())?.value ?? 'OTHER')

/**
 * Carica una foto, la fa analizzare all'IA e mostra i risultati in una tabella modificabile.
 * Niente viene salvato finché l'utente non conferma.
 */
export default function PhotoImport({ mode, onSaved, onCancel }: Props) {
  const [preview, setPreview] = useState<string | null>(null)
  const [rows, setRows] = useState<Row[] | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const analyze = async (file: File) => {
    setPreview(URL.createObjectURL(file))
    setRows(null)
    setError(null)
    setBusy(true)
    try {
      const form = new FormData()
      form.append('photo', file)
      const found = await api.post<RecognizedFood[]>(mode === 'food' ? '/ai/recognize-food' : '/ai/scan-fridge', form)
      setRows(found.map(f => ({
        name: f.name,
        category: toCategory(f.category),
        quantity: f.quantity > 0 ? f.quantity : null,
        unit: f.unit || null,
        expiryDate: f.expiryDate || null,
        source: mode === 'food' ? 'PHOTO' : 'FRIDGE_SCAN',
        selected: f.confidence >= 0.5,
        fromLabel: f.expiryFromLabel,
        confidence: f.confidence,
      })))
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const update = (i: number, patch: Partial<Row>) =>
    setRows(r => r && r.map((row, j) => (j === i ? { ...row, ...patch } : row)))

  const save = async () => {
    if (!rows) return
    const chosen = rows.filter(r => r.selected && r.name.trim())
    if (chosen.length === 0) return
    setBusy(true)
    try {
      await api.post('/food/batch', chosen.map(({ selected: _s, fromLabel: _f, confidence: _c, ...food }) => food))
      onSaved()
    } catch (err) {
      setError((err as Error).message)
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <h3>{mode === 'food' ? '📷 Aggiungi da foto' : '🧊 Scansiona il frigo'}</h3>
      <p className="muted">
        {mode === 'food'
          ? "Fotografa l'alimento con l'etichetta e la data di scadenza ben visibili."
          : "Fotografa l'interno del frigo aperto: l'IA elenca gli alimenti che riconosce."}
      </p>
      <label className="btn secondary file-btn">
        {preview ? 'Cambia foto' : 'Scegli o scatta foto'}
        <input type="file" accept="image/*" capture="environment" hidden
               onChange={e => e.target.files?.[0] && analyze(e.target.files[0])} />
      </label>

      {preview && <img src={preview} alt="Anteprima" className="preview" />}
      {busy && !rows && <p className="muted">Analisi in corso… può richiedere qualche secondo.</p>}
      {error && <div className="error">{error}</div>}

      {rows && rows.length === 0 && <p className="muted">Nessun alimento riconosciuto. Prova con una foto più nitida.</p>}
      {rows && rows.length > 0 && (
        <>
          <p className="muted">Controlla e correggi prima di salvare. Le date stimate (non lette dall'etichetta) sono segnate con ≈.</p>
          <div className="table-wrap">
            <table className="review">
              <thead>
                <tr><th></th><th>Nome</th><th>Categoria</th><th>Qtà</th><th>Scadenza</th></tr>
              </thead>
              <tbody>
                {rows.map((r, i) => (
                  <tr key={i} className={r.selected ? '' : 'dim'}>
                    <td><input type="checkbox" checked={r.selected} onChange={e => update(i, { selected: e.target.checked })} /></td>
                    <td><input value={r.name} onChange={e => update(i, { name: e.target.value })} /></td>
                    <td>
                      <select value={r.category} onChange={e => update(i, { category: e.target.value as Category })}>
                        {CATEGORIES.map(c => <option key={c.value} value={c.value}>{c.icon} {c.label}</option>)}
                      </select>
                    </td>
                    <td className="qty">
                      <input type="number" min="0" step="any" value={r.quantity ?? ''}
                             onChange={e => update(i, { quantity: e.target.value === '' ? null : Number(e.target.value) })} />
                      <input value={r.unit ?? ''} placeholder="unità" onChange={e => update(i, { unit: e.target.value || null })} />
                    </td>
                    <td>
                      <input type="date" value={r.expiryDate ?? ''} onChange={e => update(i, { expiryDate: e.target.value || null, fromLabel: true })} />
                      {r.expiryDate && !r.fromLabel && <span title="Data stimata dall'IA" className="muted"> ≈</span>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <div className="actions">
        <button className="btn ghost" onClick={onCancel}>Annulla</button>
        {rows && rows.some(r => r.selected) && (
          <button className="btn primary" onClick={save} disabled={busy}>
            Aggiungi {rows.filter(r => r.selected).length} al frigo
          </button>
        )}
      </div>
    </div>
  )
}
