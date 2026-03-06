import './style.css'
import { api } from './api'
import type { EventDetail, EventSummary, OverviewResponse } from './types'

type AppState = {
  overview: OverviewResponse | null
  events: EventSummary[]
  selectedEventId: string | null
  selectedEvent: EventDetail | null
  loading: boolean
  detailLoading: boolean
  actionLoading: boolean
  error: string | null
  toast: string | null
}

const app = document.querySelector<HTMLDivElement>('#app')

if (!app) {
  throw new Error('App container not found')
}

const state: AppState = {
  overview: null,
  events: [],
  selectedEventId: null,
  selectedEvent: null,
  loading: true,
  detailLoading: false,
  actionLoading: false,
  error: null,
  toast: null,
}

function formatDateTime(value?: string | null): string {
  if (!value) return '暂无'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function renderTrend() {
  const trend = state.overview?.trend ?? []
  if (!trend.length) {
    return `<div class="empty-box">等待分析任务生成趋势数据</div>`
  }

  const maxCount = Math.max(...trend.map((item) => item.count), 1)
  return `
    <div class="trend-chart">
      ${trend
        .map(
          (item) => `
            <div class="trend-item">
              <div class="trend-bar-wrap">
                <div class="trend-bar" style="height:${Math.max((item.count / maxCount) * 100, 12)}%"></div>
              </div>
              <span class="trend-count">${item.count}</span>
              <span class="trend-label">${escapeHtml(item.label)}</span>
            </div>
          `,
        )
        .join('')}
    </div>
  `
}

function renderRecentEvents() {
  const recentEvents = state.overview?.recent_events ?? []
  if (!recentEvents.length) {
    return `<div class="empty-box">暂无最近事件</div>`
  }

  return `
    <div class="event-chip-list">
      ${recentEvents
        .map(
          (event) => `
            <button class="event-chip ${state.selectedEventId === event.id ? 'is-active' : ''}" data-select-event="${event.id}">
              <span>${escapeHtml(event.id)}</span>
              <strong>${escapeHtml(event.plate_number)}</strong>
              <small>${event.status}</small>
            </button>
          `,
        )
        .join('')}
    </div>
  `
}

function renderEvents() {
  if (state.loading) {
    return `<div class="panel-body loading-box">正在拉取事件列表...</div>`
  }

  if (!state.events.length) {
    return `<div class="panel-body empty-box">当前没有事件，点击“重新分析演示视频”生成样例。</div>`
  }

  return `
    <div class="event-list">
      ${state.events
        .map(
          (event) => `
            <button class="event-row ${state.selectedEventId === event.id ? 'is-active' : ''}" data-select-event="${event.id}">
              <div>
                <div class="event-title">
                  <span>${escapeHtml(event.id)}</span>
                  <strong>${escapeHtml(event.plate_number)}</strong>
                </div>
                <p>${escapeHtml(event.summary)}</p>
                <div class="event-meta">
                  <span>${formatDateTime(event.first_seen)}</span>
                  <span>${escapeHtml(event.location)}</span>
                </div>
              </div>
              <div class="event-side">
                <span class="badge ${event.status === '已举报' ? 'success' : 'warning'}">${event.status}</span>
                <span>${event.duration_seconds.toFixed(1)}s</span>
                <span>置信度 ${(event.confidence * 100).toFixed(0)}%</span>
              </div>
            </button>
          `,
        )
        .join('')}
    </div>
  `
}

function renderEvidence() {
  if (state.detailLoading) {
    return `<div class="loading-box mini">正在加载事件详情...</div>`
  }

  const detail = state.selectedEvent
  if (!detail) {
    return `<div class="empty-box">选择左侧事件后，这里会显示证据链和举报信息。</div>`
  }

  const evidence = detail.evidence
    .map(
      (item) => `
        <figure class="evidence-card">
          <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
          <figcaption>
            <strong>${escapeHtml(item.label)}</strong>
            <span>${formatDateTime(item.captured_at)}</span>
          </figcaption>
        </figure>
      `,
    )
    .join('')

  const timeline = detail.raw_analysis.timeline?.length
    ? detail.raw_analysis.timeline
        .map(
          (item) => `
            <li>
              <span>${item.timestamp_seconds.toFixed(1)}s</span>
              <p>${escapeHtml(item.description)}</p>
            </li>
          `,
        )
        .join('')
    : `<li><span>--</span><p>暂无时序记录</p></li>`

  return `
    <div class="mobile-phone">
      <div class="phone-topbar">
        <span>移动举报辅助端</span>
        <small>${detail.status}</small>
      </div>
      <div class="phone-body">
        <div class="phone-card highlight">
          <h3>${escapeHtml(detail.id)}</h3>
          <p>${escapeHtml(detail.description)}</p>
          <div class="phone-grid">
            <span>车牌：${escapeHtml(detail.plate_number)}</span>
            <span>来源：${escapeHtml(detail.source_name)}</span>
            <span>位置：${escapeHtml(detail.location)}</span>
            <span>车道：${escapeHtml(detail.lane_name)}</span>
          </div>
        </div>

        <div class="phone-card">
          <div class="section-heading">
            <h4>证据图片</h4>
            <span>${detail.evidence.length} 张</span>
          </div>
          <div class="evidence-grid">${evidence}</div>
        </div>

        <div class="phone-card">
          <div class="section-heading">
            <h4>时序融合结果</h4>
            <span>${detail.vehicle_count} 车目标</span>
          </div>
          <ul class="timeline-list">${timeline}</ul>
        </div>

        <div class="phone-card report-card">
          <div class="section-heading">
            <h4>举报信息</h4>
            <span>${detail.report_number ?? '待生成'}</span>
          </div>
          <div class="report-grid">
            <span>首次发现：${formatDateTime(detail.first_seen)}</span>
            <span>最后记录：${formatDateTime(detail.last_seen)}</span>
            <span>持续时间：${detail.duration_seconds.toFixed(1)} 秒</span>
            <span>模型置信度：${(detail.confidence * 100).toFixed(0)}%</span>
            <span>提交状态：${detail.status}</span>
            <span>提交时间：${formatDateTime(detail.reported_at)}</span>
          </div>
          <button
            class="action-button wide ${detail.status === '已举报' ? 'ghost' : ''}"
            data-report-event="${detail.id}"
            ${state.actionLoading || detail.status === '已举报' ? 'disabled' : ''}
          >
            ${detail.status === '已举报' ? '已完成模拟举报' : state.actionLoading ? '正在提交...' : '一键模拟举报'}
          </button>
        </div>
      </div>
    </div>
  `
}

function render() {
  const root = app!
  const overview = state.overview
  const summary = overview?.summary
  const source = overview?.source

  root.innerHTML = `
    <div class="page-shell">
      <header class="hero">
        <div>
          <p class="eyebrow">高速公路应急车道违章辅助举报原型</p>
          <h1>智谱 API 驱动的视频分析与移动协同举报演示</h1>
          <p class="hero-copy">
            连续播放演示视频，按关键帧调用多模态识别，再结合时序融合生成证据链、车牌和模拟举报结果。
          </p>
        </div>
        <div class="hero-actions">
          <button class="action-button" id="analyze-demo" ${state.actionLoading ? 'disabled' : ''}>
            ${state.actionLoading ? '分析中...' : '重新分析演示视频'}
          </button>
          <span class="status-tip">${state.toast ?? '系统待命中'}</span>
        </div>
      </header>

      ${state.error ? `<div class="global-error">${escapeHtml(state.error)}</div>` : ''}

      <main class="layout-grid">
        <section class="panel panel-large">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">政府大屏 · 总览</p>
              <h2>检测态势与视频源</h2>
            </div>
            <span class="badge info">${summary?.active_source ?? 'demo-highway-camera-01'}</span>
          </div>

          <div class="stats-grid">
            <article class="stat-card">
              <small>疑似事件总数</small>
              <strong>${summary?.total_events ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>待举报</small>
              <strong>${summary?.pending_events ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>已举报</small>
              <strong>${summary?.reported_events ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>车牌识别覆盖</small>
              <strong>${summary?.recognized_plates ?? '--'}</strong>
            </article>
          </div>

          <div class="dashboard-grid">
            <article class="video-panel">
              <div class="section-heading">
                <h3>演示视频源</h3>
                <span>抽样间隔 ${source?.sample_interval_seconds ?? '--'}s</span>
              </div>
              ${
                source
                  ? `<video class="dashboard-video" src="${source.video_url}" poster="${source.preview_url}" controls muted loop></video>`
                  : `<div class="empty-box video-fallback">视频源尚未就绪</div>`
              }
              <div class="video-meta">
                <span>时长 ${source?.duration_seconds ?? '--'} 秒</span>
                <span>帧数 ${source?.frame_count ?? '--'}</span>
                <span>FPS ${source?.fps ?? '--'}</span>
                <span>上次运行 ${formatDateTime(summary?.last_run_at)}</span>
              </div>
            </article>

            <article class="chart-panel">
              <div class="section-heading">
                <h3>事件时段分布</h3>
                <span>平均置信度 ${summary ? `${(summary.avg_confidence * 100).toFixed(0)}%` : '--'}</span>
              </div>
              ${renderTrend()}
              <div class="section-heading sub">
                <h3>最近事件</h3>
              </div>
              ${renderRecentEvents()}
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">事件中心</p>
              <h2>疑似违章列表</h2>
            </div>
            <span class="badge warning">${state.events.length} 条</span>
          </div>
          ${renderEvents()}
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">移动端预览</p>
              <h2>详情与举报</h2>
            </div>
            <span class="badge success">${state.selectedEvent?.plate_number ?? '未选择'}</span>
          </div>
          <div class="panel-body">
            ${renderEvidence()}
          </div>
        </section>
      </main>
    </div>
  `

  document.getElementById('analyze-demo')?.addEventListener('click', handleAnalyzeDemo)
  document.querySelectorAll<HTMLElement>('[data-select-event]').forEach((element) => {
    element.addEventListener('click', () => {
      const eventId = element.dataset.selectEvent
      if (eventId) {
        void selectEvent(eventId)
      }
    })
  })
  document.querySelector<HTMLElement>('[data-report-event]')?.addEventListener('click', () => {
    if (state.selectedEventId) {
      void handleReport(state.selectedEventId)
    }
  })
}

async function bootstrap() {
  render()
  try {
    await refreshOverviewAndEvents()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '加载失败'
  } finally {
    state.loading = false
    render()
  }
}

async function refreshOverviewAndEvents() {
  state.loading = true
  state.error = null
  render()

  const [overview, events] = await Promise.all([api.getOverview(), api.getEvents()])
  state.overview = overview
  state.events = events
  const fallbackId = state.selectedEventId && events.some((event) => event.id === state.selectedEventId)
    ? state.selectedEventId
    : events[0]?.id ?? null
  state.selectedEventId = fallbackId
  if (fallbackId) {
    await selectEvent(fallbackId, false)
  } else {
    state.selectedEvent = null
  }
  state.loading = false
}

async function selectEvent(eventId: string, rerender = true) {
  state.selectedEventId = eventId
  state.detailLoading = true
  if (rerender) render()
  try {
    state.selectedEvent = await api.getEvent(eventId)
  } catch (error) {
    state.error = error instanceof Error ? error.message : '事件详情加载失败'
  } finally {
    state.detailLoading = false
    render()
  }
}

async function handleAnalyzeDemo() {
  state.actionLoading = true
  state.toast = '正在调用后端分析演示视频...'
  state.error = null
  render()
  try {
    const result = await api.analyzeDemo()
    state.toast = `${result.message} 共分析 ${result.analyzed_frames} 帧，生成 ${result.events_created} 个事件。`
    await refreshOverviewAndEvents()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '演示任务执行失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

async function handleReport(eventId: string) {
  state.actionLoading = true
  state.error = null
  state.toast = '正在提交模拟举报...'
  render()
  try {
    const result = await api.reportEvent(eventId)
    state.toast = `${result.message} 举报编号：${result.report_number}`
    await refreshOverviewAndEvents()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '模拟举报失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

void bootstrap()
