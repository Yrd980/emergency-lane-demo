export interface SynthesisRequest {
  duration_seconds: number
  num_normal: number
  num_violating: number
  seed: number
  fps: number
}

export interface SynthesisResponse {
  video_id: string
  video_url: string
  duration_seconds: number
  fps: number
  seed: number
}

export interface DetectionItem {
  label: string
  score: number
  x: number
  y: number
  w: number
  h: number
}

export interface FrameResultItem {
  frame_idx: number
  has_violation: boolean
  vehicle_count: number
  detections: DetectionItem[]
}

export interface ViolationSegment {
  start_frame: number
  end_frame: number
  duration_frames: number
  duration_seconds: number
  occupancy_ratio: number
  confidence: number
}

export interface AnalysisResult {
  status: string
  video_id: string
  total_frames: number
  fps: number
  violation_segments: ViolationSegment[]
  frame_results: FrameResultItem[]
  keyframe_count: number
  timeline_url: string
}
