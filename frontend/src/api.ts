import { API_BASE_URL } from './config'
import type {
  AnalysisResult,
  SynthesisRequest,
  SynthesisResponse,
} from './types'

async function request<T>(path: string, options?: RequestInit & { json?: unknown }): Promise<T> {
  const headers = new Headers(options?.headers)
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const resp = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options?.json === undefined ? options?.body : JSON.stringify(options.json),
  })
  if (!resp.ok) {
    const msg = await resp.text()
    throw new Error(msg || `Request failed: ${resp.status}`)
  }
  return resp.json() as Promise<T>
}

export const api = {
  synthesis: (payload: SynthesisRequest) =>
    request<SynthesisResponse>('/synthesis', { method: 'POST', json: payload }),

  analyze: (videoId: string) =>
    request<AnalysisResult>(`/analyze/${videoId}`, { method: 'POST' }),

  getResults: (videoId: string) =>
    request<AnalysisResult>(`/results/${videoId}`),
}
