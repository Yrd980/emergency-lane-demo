import { API_BASE_URL } from './config'
import type {
  AnalyzeTaskResponse,
  CaseDetail,
  CaseSummary,
  CaseUpdateRequest,
  EventDetail,
  EventSummary,
  OverviewResponse,
  ReportResponse,
  RunDetail,
  RunSummary,
  SourceSummary,
} from './types'

type QueryValue = string | null | undefined

function buildQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value) search.set(key, value)
  })
  const query = search.toString()
  return query ? `?${query}` : ''
}

async function request<T>(path: string, options?: RequestInit & { json?: unknown }): Promise<T> {
  const headers = new Headers(options?.headers)
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options?.json === undefined ? options?.body : JSON.stringify(options.json),
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}

export const api = {
  getOverview: () => request<OverviewResponse>('/overview'),
  getSources: () => request<SourceSummary[]>('/sources'),
  getRuns: () => request<RunSummary[]>('/runs'),
  getRun: (runId: string) => request<RunDetail>(`/runs/${runId}`),
  getEvents: (filters?: Record<string, QueryValue>) => request<EventSummary[]>(`/events${buildQuery(filters ?? {})}`),
  getEvent: (eventId: string) => request<EventDetail>(`/events/${eventId}`),
  getCases: (filters?: Record<string, QueryValue>) => request<CaseSummary[]>(`/cases${buildQuery(filters ?? {})}`),
  getCase: (caseId: string) => request<CaseDetail>(`/cases/${caseId}`),
  updateCase: (caseId: string, payload: CaseUpdateRequest) =>
    request<CaseDetail>(`/cases/${caseId}`, { method: 'PATCH', json: payload }),
  reportEvent: (eventId: string) => request<ReportResponse>(`/events/${eventId}/report`, { method: 'POST' }),
  reportCase: (caseId: string) => request<ReportResponse>(`/cases/${caseId}/report`, { method: 'POST' }),
  analyzeDemo: (source_name: string) =>
    request<AnalyzeTaskResponse>('/tasks/analyze-demo', { method: 'POST', json: { source_name } }),
}
