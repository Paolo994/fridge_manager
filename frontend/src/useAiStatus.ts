import { useEffect, useState } from 'react'
import { api } from './api'

/** true/false quando noto, null mentre carica. */
export function useAiStatus() {
  const [enabled, setEnabled] = useState<boolean | null>(null)
  useEffect(() => {
    api.get<{ enabled: boolean }>('/ai/status').then(r => setEnabled(r.enabled)).catch(() => setEnabled(false))
  }, [])
  return enabled
}
