import { api } from './api'
import type { AnalysisResult, SynthesisResponse } from './types'
import { drawDetections, drawTimeline } from './utils/draw'
import './style.css'

const app = document.querySelector<HTMLDivElement>('#app')!

let videoId: string | null = null
let videoUrl: string | null = null
let analysis: AnalysisResult | null = null

let duration = 30
let seed = 42
let numViolating = 2
let fps = 30

let render: () => void = () => {
  app.innerHTML = `
    <div class="control-bar">
      <label>时长(s) <input type="number" id="cfg-dur" value="${duration}" min="5" max="120"></label>
      <label>FPS <input type="number" id="cfg-fps" value="${fps}" min="10" max="60"></label>
      <label>违规车辆 <input type="number" id="cfg-vio" value="${numViolating}" min="0" max="5"></label>
      <label>Seed <input type="number" id="cfg-seed" value="${seed}"></label>
      <button class="btn btn-primary" id="btn-synth">合成视频</button>
      <button class="btn btn-secondary" id="btn-analyze" ${!videoId ? 'disabled' : ''}>运行检测</button>
      <span class="age">${videoId ? `video: ${videoId}` : '未合成'}</span>
      <span class="age">${analysis ? `检测完成 | ${analysis.violation_segments.length} 个违规段` : ''}</span>
    </div>

    ${videoUrl
      ? `<div class="video-stage">
           <video id="player" src="${videoUrl}" controls></video>
           <canvas id="overlay-canvas"></canvas>
         </div>`
      : `<div class="placeholder"><h2>高速公路应急车道违规检测</h2><p>点击「合成视频」生成演示视频，然后「运行检测」</p></div>`}

    ${analysis
      ? `<div class="timeline-section">
           <h3>违规时间轴（红=违规段）</h3>
           <canvas id="timeline-canvas" height="40"></canvas>
           <div class="frame-strip" id="strip"></div>
         </div>`
      : ''}
  `

  bindEvents()
  if (analysis) drawTimelineCanvas()
}

function bindEvents() {
  document.getElementById('btn-synth')?.addEventListener('click', async () => {
    duration = +(document.getElementById('cfg-dur') as HTMLInputElement).value
    fps = +(document.getElementById('cfg-fps') as HTMLInputElement).value
    numViolating = +(document.getElementById('cfg-vio') as HTMLInputElement).value
    seed = +(document.getElementById('cfg-seed') as HTMLInputElement).value

    try {
      const resp: SynthesisResponse = await api.synthesis({
        duration_seconds: duration, fps, num_normal: 4, num_violating: numViolating, seed,
      })
      videoId = resp.video_id
      videoUrl = resp.video_url
      analysis = null
      render()
    } catch (e) {
      alert(`合成失败: ${e}`)
    }
  })

  document.getElementById('btn-analyze')?.addEventListener('click', async () => {
    if (!videoId) return
    try {
      analysis = await api.analyze(videoId)
      render()
    } catch (e) {
      alert(`检测失败: ${e}`)
      analysis = null
    }
  })
}

function drawTimelineCanvas() {
  if (!analysis) return
  const canvas = document.getElementById('timeline-canvas') as HTMLCanvasElement | null
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  canvas.width = canvas.clientWidth
  canvas.height = 40
  drawTimeline(ctx, analysis.violation_segments, analysis.total_frames, canvas.width, canvas.height)

  const strip = document.getElementById('strip')!
  const violationFrames = analysis.frame_results.filter(f => f.has_violation).slice(0, 8)
  strip.innerHTML = violationFrames.map(f =>
    `<div style="text-align:center">
       <div style="width:140px;height:80px;background:#1a1a2e;border-radius:6px;display:flex;align-items:center;justify-content:center;font-size:11px;color:#a0a0c0">
         frame ${f.frame_idx}<br>${f.detections.length} vehicle(s)
       </div>
     </div>`
  ).join('')
}

let syncInterval: ReturnType<typeof setInterval> | null = null

function startSyncOverlay() {
  if (syncInterval) clearInterval(syncInterval)
  if (!videoUrl || !analysis) return
  syncInterval = setInterval(syncOverlay, 50)
}

function syncOverlay() {
  const video = document.getElementById('player') as HTMLVideoElement | null
  const canvas = document.getElementById('overlay-canvas') as HTMLCanvasElement | null
  if (!video || !canvas || !analysis) return

  canvas.width = video.clientWidth
  canvas.height = video.clientHeight
  const ctx = canvas.getContext('2d')!
  ctx.clearRect(0, 0, canvas.width, canvas.height)

  const currentFrame = Math.floor(video.currentTime * analysis.fps)
  const frameData = analysis.frame_results.find(f => f.frame_idx === currentFrame)
  if (frameData && frameData.has_violation) {
    const scaleX = canvas.width / 1280
    const scaleY = canvas.height / 720
    ctx.save()
    ctx.scale(scaleX, scaleY)
    drawDetections(ctx, frameData.detections, canvas.width, canvas.height)
    ctx.restore()
  }
}

// Override render to also start overlay sync
const origRender = render
render = () => {
  origRender()
  startSyncOverlay()
}

render()
