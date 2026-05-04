import type { DetectionItem, ViolationSegment } from '../types'

export function drawDetections(
  ctx: CanvasRenderingContext2D,
  detections: DetectionItem[],
  _canvasW: number,
  _canvasH: number
) {
  ctx.strokeStyle = '#ff3333'
  ctx.lineWidth = 2
  ctx.fillStyle = '#ff3333'
  ctx.font = '12px monospace'

  for (const d of detections) {
    ctx.strokeRect(d.x, d.y, d.w, d.h)
    ctx.fillText(`${d.label} ${(d.score * 100).toFixed(0)}%`, d.x, d.y - 6)
  }
}

export function drawTimeline(
  ctx: CanvasRenderingContext2D,
  segments: ViolationSegment[],
  totalFrames: number,
  canvasW: number,
  canvasH: number
) {
  ctx.fillStyle = '#e8e8e8'
  ctx.fillRect(0, 0, canvasW, canvasH)

  ctx.fillStyle = '#e04040'
  for (const seg of segments) {
    const x1 = (seg.start_frame / totalFrames) * canvasW
    const x2 = (seg.end_frame / totalFrames) * canvasW
    ctx.fillRect(x1, 2, Math.max(x2 - x1, 2), canvasH - 4)
  }

  ctx.strokeStyle = '#ccc'
  ctx.lineWidth = 1
  ctx.strokeRect(0, 0, canvasW, canvasH)
}
