export type CaseStatus = '待复核' | '待举报' | '已举报'
export type ReviewStatus = '待复核' | '复核通过' | '复核退回'

export interface SourceSummary {
  name: string
  title: string
  video_url: string
  preview_url?: string | null
  fps: number
  frame_count: number
  duration_seconds: number
  sample_interval_seconds: number
  case_clip_seconds: number
  reference_mode: string
  location: string
  lane_label: string
}

export interface RunSummary {
  id: string
  source_name: string
  source_video: string
  status: 'running' | 'done' | 'failed'
  mode: string
  started_at?: string | null
  finished_at?: string | null
  progress_percent: number
  message: string
  error_message?: string | null
  events_created: number
  cases_created: number
  event_count: number
  case_count: number
}

export interface EventSummary {
  id: string
  run_id: string
  case_id?: string | null
  plate_number: string
  corrected_plate_number?: string | null
  review_status: ReviewStatus
  source_name: string
  status: CaseStatus
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
  run_id: string
  source_name: string
  plate_number: string
  corrected_plate_number?: string | null
  review_status: ReviewStatus
  operator_note: string
  status: CaseStatus
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
    pending_review_cases: number
  }
  latest_run?: RunSummary | null
  runs: RunSummary[]
  trend: Array<{
    label: string
    count: number
  }>
  recent_events: EventSummary[]
  recent_cases: CaseSummary[]
  source: SourceSummary
  sources: SourceSummary[]
  pipeline: Array<{
    key: string
    title: string
    owner: string
    summary: string
    evidence: string[]
  }>
  system: {
    web_role: string
    android_role: string
    reference_basis: string
  }
}

export interface AnalyzeTaskResponse {
  task_id: string
  run_id: string
  mode: string
  source_name: string
  analyzed_frames: number
  events_created: number
  cases_created: number
  started_at: string
  finished_at: string
  message: string
  source: SourceSummary
  run: RunSummary
}

export interface ReportResponse {
  event_id?: string
  case_id?: string
  status: '已举报'
  report_number: string
  reported_at: string
  message: string
}

export interface CaseUpdateRequest {
  corrected_plate_number?: string | null
  operator_note?: string | null
  review_status?: ReviewStatus
  status?: Exclude<CaseStatus, '已举报'>
}

export interface RunDetail extends RunSummary {
  events: EventSummary[]
  cases: Array<{
    id: string
    plate_number: string
    corrected_plate_number?: string | null
    review_status: ReviewStatus
    status: CaseStatus
  }>
}
