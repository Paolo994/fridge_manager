import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, type ChatResult, type ChatTurn } from '../api'
import { useAiStatus } from '../useAiStatus'

interface Msg extends ChatTurn { actions?: string[] }

const EXAMPLES = [
  'Cosa scade questa settimana?',
  'Ho finito il latte e le uova',
  'Ho comprato 6 yogurt che scadono il 15',
  'Cosa mi serve per fare una carbonara?',
]

export default function AssistantPage() {
  const aiEnabled = useAiStatus()
  const [messages, setMessages] = useState<Msg[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => { endRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, busy])

  const send = async (text: string) => {
    if (!text.trim() || busy) return
    const history: ChatTurn[] = messages.slice(-20).map(({ role, text }) => ({ role, text }))
    setMessages(m => [...m, { role: 'user', text }])
    setInput('')
    setBusy(true)
    try {
      const res = await api.post<ChatResult>('/ai/assistant', { message: text, history })
      setMessages(m => [...m, { role: 'assistant', text: res.reply, actions: res.actions }])
    } catch (err) {
      setMessages(m => [...m, { role: 'assistant', text: '⚠️ ' + (err as Error).message }])
    } finally {
      setBusy(false)
    }
  }

  const submit = (e: FormEvent) => { e.preventDefault(); send(input) }

  if (aiEnabled === false) {
    return (
      <section>
        <div className="page-head"><h2>Assistente</h2></div>
        <div className="info">Serve l'IA: imposta <code>ANTHROPIC_API_KEY</code> sul backend.</div>
      </section>
    )
  }

  return (
    <section className="chat-page">
      <div className="page-head"><h2>Assistente di cucina</h2></div>
      <div className="chat">
        {messages.length === 0 && (
          <div className="empty">
            <p>Chiedimi del frigo, oppure dimmi cosa hai finito o comprato: aggiorno io frigo e lista della spesa.</p>
            <div className="examples">
              {EXAMPLES.map(e => <button key={e} className="btn ghost small" onClick={() => send(e)}>{e}</button>)}
            </div>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} className={`bubble ${m.role}`}>
            <div className="bubble-text">{m.text}</div>
            {m.actions && m.actions.length > 0 && (
              <ul className="bubble-actions">{m.actions.map((a, j) => <li key={j}>✓ {a}</li>)}</ul>
            )}
          </div>
        ))}
        {busy && <div className="bubble assistant muted">Sto pensando…</div>}
        <div ref={endRef} />
      </div>
      <form className="inline-form chat-input" onSubmit={submit}>
        <input value={input} onChange={e => setInput(e.target.value)} placeholder="Scrivi un messaggio…" maxLength={2000} />
        <button className="btn primary" disabled={busy || !input.trim()}>Invia</button>
      </form>
    </section>
  )
}
