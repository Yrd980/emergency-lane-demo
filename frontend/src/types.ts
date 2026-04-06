
export interface EventSummary {
  id: string
  case_id?: string | null
  plate_number: string
  status: '待举报' | '已举报'
  location: string
  lane_name: string
  first_seen: string
  last_seen: string
  duration_seconds: number
  confidence: number
  summary: string
  report_number?: string | null
}

export interface EvidenceItem {
  label: string
  image_url: string
  captured_at: string
}

export interface EventDetail extends EventSummary {
  source_name: string
  description: string
  vehicle_count: number
  reported_at?: string | null
  report_content?: string | null
  evidence: EvidenceItem[]
  raw_analysis: {
    source_mode?: string[]
    timeline?: Array<{
      timestamp_seconds: number
      plate_number: string
      confidence: number
      description: string
      frame_path: string
    }>
  }
}

export interface CaseSummary {
  id: string
  plate_number: string
  status: '待举报' | '已举报'
  location: string
  confidence: number
  summary: string
  duration_seconds: number
  first_seen: string
  last_seen: string
  event_count: number
  evidence_count: number
  clip_url?: string | null
  report_number?: string | null
}

export interface CaseDetail extends CaseSummary {
  created_at: string
  updated_at: string
  report_content?: string | null
  report_file_url?: string | null
  reported_at?: string | null
  events: EventSummary[]
  evidence: EvidenceItem[]
  raw_analysis: {
    source_mode?: string[]
    timeline?: Array<{
      timestamp_seconds: number
      plate_number: string
      confidence: number
      description: string
      frame_path: string
    }>
  }
}

export interface OverviewResponse {
  summary: {
    total_events: number
    pending_events: number
    reported_events: number
    recognized_plates: number
    avg_confidence: number
    last_run_at?: string | null
    active_source: string
    total_cases: number
    reported_cases: number
  }
  trend: Array<{
    label: string
    count: number
  }>
  recent_events: EventSummary[]
  recent_cases: CaseSummary[]
  source: {
    name: string
    video_url: string
    preview_url: string
    fps: number
    frame_count: number
    duration_seconds: number
    sample_interval_seconds: number
    case_clip_seconds: number
    reference_mode: string
  }
  system: {
    web_role: string
    android_role: string
    reference_basis: string
  }
}

export interface AnalyzeTaskResponse {
  task_id: string
  mode: string
  analyzed_frames: number
  events_created: number
  cases_created: number
  started_at: string
  finished_at: string
  message: string
  source: OverviewResponse['source']
}

export interface ReportResponse {
  event_id?: string
  case_id?: string
  status: '已举报'
  report_number: string
  reported_at: string
  message: string
}
