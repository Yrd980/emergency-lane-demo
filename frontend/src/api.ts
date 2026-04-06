
import { API_BASE_URL } from './config'
import type {
  AnalyzeTaskResponse,
  CaseDetail,
  CaseSummary,
  EventDetail,
  EventSummary,
  OverviewResponse,
  ReportResponse,
} from './types'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options?.headers ?? {}),
    },
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}

export const api = {
  getOverview: () => request<OverviewResponse>('/overview'),
  getEvents: () => request<EventSummary[]>('/events'),
  getEvent: (eventId: string) => request<EventDetail>(`/events/${eventId}`),
  getCases: () => request<CaseSummary[]>('/cases'),
  getCase: (caseId: string) => request<CaseDetail>(`/cases/${caseId}`),
  reportEvent: (eventId: string) =>
    request<ReportResponse>(`/events/${eventId}/report`, { method: 'POST' }),
  reportCase: (caseId: string) =>
    request<ReportResponse>(`/cases/${caseId}/report`, { method: 'POST' }),
  analyzeDemo: () => request<AnalyzeTaskResponse>('/tasks/analyze-demo', { method: 'POST' }),
}
