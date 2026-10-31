// Client HTTP verso il backend Spring. Il token JWT è salvato nel localStorage.

const TOKEN_KEY = 'fm_token'

export const tokenStore = {
  get: (): string | null => {
    try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
  },
  set: (t: string) => { try { localStorage.setItem(TOKEN_KEY, t) } catch { /* ignore */ } },
  clear: () => { try { localStorage.removeItem(TOKEN_KEY) } catch { /* ignore */ } },
}

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message) }
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(fn: () => void) { onUnauthorized = fn }

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {}
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  let payload: BodyInit | undefined
  if (body instanceof FormData) payload = body
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    payload = JSON.stringify(body)
  }
  const res = await fetch(`/api${path}`, { method, headers, body: payload })
  if (res.status === 401 && token) onUnauthorized()
  if (!res.ok) {
    let msg = `Errore ${res.status}`
    try {
      const data = await res.json()
      msg = data.detail || data.message || msg
    } catch { /* risposta senza JSON */ }
    throw new ApiError(res.status, msg)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export const api = {
  get: <T>(p: string) => request<T>('GET', p),
  post: <T>(p: string, b?: unknown) => request<T>('POST', p, b),
  put: <T>(p: string, b?: unknown) => request<T>('PUT', p, b),
  patch: <T>(p: string, b?: unknown) => request<T>('PATCH', p, b),
  del: <T>(p: string) => request<T>('DELETE', p),
}

// ---- Tipi condivisi con il backend ----

export type Category =
  | 'DAIRY' | 'MEAT' | 'FISH' | 'VEGETABLES' | 'FRUIT' | 'EGGS' | 'BEVERAGES'
  | 'SAUCES' | 'LEFTOVERS' | 'BAKERY' | 'FROZEN' | 'OTHER'

export const CATEGORIES: { value: Category; label: string; icon: string }[] = [
  { value: 'DAIRY', label: 'Latticini', icon: '🧀' },
  { value: 'MEAT', label: 'Carne', icon: '🥩' },
  { value: 'FISH', label: 'Pesce', icon: '🐟' },
  { value: 'VEGETABLES', label: 'Verdura', icon: '🥦' },
  { value: 'FRUIT', label: 'Frutta', icon: '🍎' },
  { value: 'EGGS', label: 'Uova', icon: '🥚' },
  { value: 'BEVERAGES', label: 'Bevande', icon: '🥤' },
  { value: 'SAUCES', label: 'Salse', icon: '🥫' },
  { value: 'LEFTOVERS', label: 'Avanzi', icon: '🍲' },
  { value: 'BAKERY', label: 'Pane e dolci', icon: '🥐' },
  { value: 'FROZEN', label: 'Surgelati', icon: '🧊' },
  { value: 'OTHER', label: 'Altro', icon: '📦' },
]

export const categoryInfo = (c: Category) => CATEGORIES.find(x => x.value === c) ?? CATEGORIES[CATEGORIES.length - 1]

export type FoodSource = 'MANUAL' | 'PHOTO' | 'FRIDGE_SCAN' | 'ASSISTANT'

export interface User { id: number; email: string; displayName: string }
export interface AuthResponse { token: string; user: User }

export interface Food {
  id: number
  name: string
  category: Category
  quantity: number | null
  unit: string | null
  expiryDate: string | null
  daysToExpiry: number | null
  status: 'EXPIRED' | 'EXPIRING_SOON' | 'OK' | 'UNKNOWN'
  source: FoodSource
  notes: string | null
}

export interface FoodInput {
  name: string
  category: Category
  quantity: number | null
  unit: string | null
  expiryDate: string | null
  source?: FoodSource
  notes?: string | null
}

export interface RecognizedFood {
  name: string
  category: string
  quantity: number
  unit: string
  expiryDate: string
  expiryFromLabel: boolean
  confidence: number
}

export interface ShoppingItem { id: number; name: string; quantity: string | null; checked: boolean }

export interface Notification { id: number; message: string; foodItemId: number | null; read: boolean; createdAt: string }

export interface Recipe {
  title: string
  description: string
  minutes: number
  usedFridgeItems: string[]
  missingIngredients: string[]
  steps: string[]
}

export interface ChatTurn { role: 'user' | 'assistant'; text: string }
export interface ChatResult { reply: string; actions: string[] }
