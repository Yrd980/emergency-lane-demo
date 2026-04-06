
import './style.css'
import { api } from './api'
import type { CaseDetail, CaseSummary, EventDetail, EventSummary, OverviewResponse } from './types'

type AppState = {
  overview: OverviewResponse | null
  events: EventSummary[]
  cases: CaseSummary[]
  selectedEventId: string | null
  selectedCaseId: string | null
  selectedEvent: EventDetail | null
  selectedCase: CaseDetail | null
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
  cases: [],
  selectedEventId: null,
  selectedCaseId: null,
  selectedEvent: null,
  selectedCase: null,
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

function renderEventList() {
  if (state.loading) return `<div class="loading-box">正在加载事件列表...</div>`
  if (!state.events.length) return `<div class="empty-box">暂无事件，点击“重新分析演示视频”生成样例。</div>`

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
                  <span>${escapeHtml(event.case_id ?? '未入案件')}</span>
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

function renderCaseList() {
  if (state.loading) return `<div class="loading-box">正在加载案件库...</div>`
  if (!state.cases.length) return `<div class="empty-box">暂无案件归档，等待分析完成。</div>`

  return `
    <div class="case-list">
      ${state.cases
        .map(
          (item) => `
            <button class="case-row ${state.selectedCaseId === item.id ? 'is-active' : ''}" data-select-case="${item.id}">
              <div class="case-row-main">
                <span class="case-id">${escapeHtml(item.id)}</span>
                <strong>${escapeHtml(item.plate_number)}</strong>
                <p>${escapeHtml(item.summary)}</p>
              </div>
              <div class="case-row-side">
                <span class="badge ${item.status === '已举报' ? 'success' : 'info'}">${item.status}</span>
                <small>${item.event_count} 个事件 / ${item.evidence_count} 张证据</small>
              </div>
            </button>
          `,
        )
        .join('')}
    </div>
  `
}

function renderEventDetail() {
  if (state.detailLoading && !state.selectedEvent) return `<div class="loading-box mini">正在加载事件详情...</div>`
  const detail = state.selectedEvent
  if (!detail) return `<div class="empty-box">选择事件后，这里显示时序证据与移动端举报视图。</div>`

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
        <span>Android / 移动举报视图映射</span>
        <small>${detail.status}</small>
      </div>
      <div class="phone-body">
        <div class="phone-card highlight">
          <h3>${escapeHtml(detail.id)}</h3>
          <p>${escapeHtml(detail.description)}</p>
          <div class="phone-grid">
            <span>车牌：${escapeHtml(detail.plate_number)}</span>
            <span>案件：${escapeHtml(detail.case_id ?? '待归档')}</span>
            <span>位置：${escapeHtml(detail.location)}</span>
            <span>来源：${escapeHtml(detail.source_name)}</span>
          </div>
        </div>

        <div class="phone-card">
          <div class="section-heading">
            <h4>关键证据帧</h4>
            <span>${detail.evidence.length} 张</span>
          </div>
          <div class="evidence-grid">${evidence}</div>
        </div>

        <div class="phone-card">
          <div class="section-heading">
            <h4>时序融合</h4>
            <span>${detail.vehicle_count} 车目标</span>
          </div>
          <ul class="timeline-list">${timeline}</ul>
        </div>

        <div class="phone-card report-card">
          <div class="section-heading">
            <h4>事件举报</h4>
            <span>${detail.report_number ?? '待生成'}</span>
          </div>
          <div class="report-grid">
            <span>首次发现：${formatDateTime(detail.first_seen)}</span>
            <span>最后记录：${formatDateTime(detail.last_seen)}</span>
            <span>持续时长：${detail.duration_seconds.toFixed(1)} 秒</span>
            <span>状态：${detail.status}</span>
          </div>
          <p class="report-text compact">${escapeHtml(detail.report_content ?? '当前尚未提交模拟举报，案件级文书在右侧案件中心展示。')}</p>
          <button
            class="action-button wide ${detail.status === '已举报' ? 'ghost' : ''}"
            data-report-event="${detail.id}"
            ${state.actionLoading || detail.status === '已举报' ? 'disabled' : ''}
          >
            ${detail.status === '已举报' ? '已完成模拟举报' : state.actionLoading ? '正在提交...' : '提交事件模拟举报'}
          </button>
        </div>
      </div>
    </div>
  `
}

function renderCaseDetail() {
  if (state.detailLoading && !state.selectedCase) return `<div class="loading-box mini">正在加载案件详情...</div>`
  const detail = state.selectedCase
  if (!detail) return `<div class="empty-box">选择案件后，这里显示 15 秒片段、文书和双端协同说明。</div>`

  const linkedEvents = detail.events.length
    ? detail.events
        .map(
          (event) => `
            <li>
              <strong>${escapeHtml(event.id)}</strong>
              <span>${formatDateTime(event.first_seen)} - ${formatDateTime(event.last_seen)}</span>
            </li>
          `,
        )
        .join('')
    : '<li><strong>暂无</strong></li>'

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
    <div class="panel-stack">
      <div class="detail-card">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">案件中心</p>
            <h3>${escapeHtml(detail.id)} / ${escapeHtml(detail.plate_number)}</h3>
          </div>
          <span class="badge ${detail.status === '已举报' ? 'success' : 'warning'}">${detail.status}</span>
        </div>
        <div class="case-meta-grid">
          <span>地点：${escapeHtml(detail.location)}</span>
          <span>事件数：${detail.events.length}</span>
          <span>证据数：${detail.evidence.length}</span>
          <span>置信度：${(detail.confidence * 100).toFixed(0)}%</span>
        </div>
        ${
          detail.clip_url
            ? `<video class="clip-video" controls src="${detail.clip_url}"></video>`
            : '<div class="empty-box video-fallback">暂无 15 秒证据片段</div>'}
      </div>

      <div class="detail-card">
        <div class="section-heading">
          <h4>正式举报文书</h4>
          <span>${detail.report_number ?? '未提交'}</span>
        </div>
        <pre class="report-text">${escapeHtml(detail.report_content ?? '暂无文书')}</pre>
        <div class="inline-actions">
          ${
            detail.report_file_url
              ? `<a class="action-link" href="${detail.report_file_url}" target="_blank" rel="noreferrer">打开 TXT 文书</a>`
              : ''}
          <button
            class="action-button ${detail.status === '已举报' ? 'ghost' : ''}"
            data-report-case="${detail.id}"
            ${state.actionLoading || detail.status === '已举报' ? 'disabled' : ''}
          >
            ${detail.status === '已举报' ? '案件已举报' : state.actionLoading ? '正在提交...' : '提交案件模拟举报'}
          </button>
        </div>
      </div>

      <div class="detail-card dual-grid">
        <div>
          <div class="section-heading">
            <h4>案件关联事件</h4>
            <span>${detail.events.length} 条</span>
          </div>
          <ul class="mini-list">${linkedEvents}</ul>
        </div>
        <div>
          <div class="section-heading">
            <h4>案件时序链</h4>
            <span>${detail.evidence.length} 节点</span>
          </div>
          <ul class="timeline-list compact">${timeline}</ul>
        </div>
      </div>
    </div>
  `
}

function render() {
  const overview = state.overview
  const summary = overview?.summary
  const source = overview?.source
  const system = overview?.system

  app!.innerHTML = `
    <div class="page-shell">
      <header class="hero">
        <div>
          <p class="eyebrow">高速公路应急车道违章辅助举报原型</p>
          <h1>网页大屏 + Android 移动端的双端联动项目</h1>
          <p class="hero-copy">
            当前主仓保留 Web 大屏与后端分析闭环，并吸收参考 Android 原型中的案件化思路：关键帧抽样、车牌归档、15 秒证据片段、正式举报文书与移动协同查看。
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
              <h2>检测态势 / 双端分工 / 演示源</h2>
            </div>
            <span class="badge info">${summary?.active_source ?? 'demo-highway-camera-01'}</span>
          </div>

          <div class="stats-grid">
            <article class="stat-card">
              <small>疑似事件总数</small>
              <strong>${summary?.total_events ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>案件归档数</small>
              <strong>${summary?.total_cases ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>已提交举报</small>
              <strong>${summary?.reported_cases ?? '--'}</strong>
            </article>
            <article class="stat-card">
              <small>平均置信度</small>
              <strong>${summary ? `${Math.round(summary.avg_confidence * 100)}%` : '--'}</strong>
            </article>
          </div>

          <div class="dashboard-grid">
            <div class="video-panel">
              <div class="section-heading">
                <h3>演示视频源</h3>
                <span>${source?.reference_mode ?? '双端演示'}</span>
              </div>
              ${
                source
                  ? `<video class="dashboard-video" controls poster="${source.preview_url}" src="${source.video_url}"></video>`
                  : '<div class="empty-box video-fallback">尚未获取演示视频</div>'}
              <div class="video-meta">
                <span>时长 ${source?.duration_seconds ?? '--'} 秒</span>
                <span>FPS ${source?.fps ?? '--'}</span>
                <span>抽样间隔 ${source?.sample_interval_seconds ?? '--'} 秒</span>
                <span>证据片段 ${source?.case_clip_seconds ?? '--'} 秒</span>
              </div>
            </div>

            <div class="chart-panel">
              <div class="section-heading">
                <h3>时段趋势</h3>
                <span>最近任务</span>
              </div>
              ${renderTrend()}
            </div>
          </div>

          <div class="architecture-grid">
            <article class="role-card">
              <small>网页端职责</small>
              <strong>${escapeHtml(system?.web_role ?? '政府大屏总览')}</strong>
              <p>负责态势总览、案件库、证据链和统一操作入口。</p>
            </article>
            <article class="role-card">
              <small>Android 端职责</small>
              <strong>${escapeHtml(system?.android_role ?? '移动协同查看')}</strong>
              <p>负责移动端案件查看、事件详情、举报文书预览与提交。</p>
            </article>
            <article class="role-card">
              <small>参考来源</small>
              <strong>${escapeHtml(system?.reference_basis ?? '参考 Android 原型')}</strong>
              <p>将参考工程的模块设计和技术选型沉淀为当前双端项目方案。</p>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">事件列表</p>
              <h2>关键帧命中与时序事件</h2>
            </div>
            <span class="badge warning">${state.events.length} 条</span>
          </div>
          ${renderEventList()}
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">案件库</p>
              <h2>按车牌归档的案例中心</h2>
            </div>
            <span class="badge info">${state.cases.length} 个</span>
          </div>
          ${renderCaseList()}
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">移动端事件视图</p>
              <h2>事件详情 / 举报入口</h2>
            </div>
            <span class="badge info">${state.selectedEventId ?? '未选择'}</span>
          </div>
          ${renderEventDetail()}
        </section>

        <section class="panel">
          <div class="panel-heading">
            <div>
              <p class="panel-kicker">案件详情</p>
              <h2>证据片段 / 文书 / 双端协同</h2>
            </div>
            <span class="badge info">${state.selectedCaseId ?? '未选择'}</span>
          </div>
          ${renderCaseDetail()}
        </section>
      </main>
    </div>
  `

  bindEvents()
}

function bindEvents() {
  document.querySelector('#analyze-demo')?.addEventListener('click', async () => {
    await analyzeDemo()
  })

  document.querySelectorAll<HTMLElement>('[data-select-event]').forEach((node) => {
    node.addEventListener('click', async () => {
      const eventId = node.dataset.selectEvent
      if (!eventId) return
      await loadEvent(eventId)
    })
  })

  document.querySelectorAll<HTMLElement>('[data-select-case]').forEach((node) => {
    node.addEventListener('click', async () => {
      const caseId = node.dataset.selectCase
      if (!caseId) return
      await loadCase(caseId)
    })
  })

  document.querySelectorAll<HTMLElement>('[data-report-event]').forEach((node) => {
    node.addEventListener('click', async () => {
      const eventId = node.dataset.reportEvent
      if (!eventId) return
      await reportEvent(eventId)
    })
  })

  document.querySelectorAll<HTMLElement>('[data-report-case]').forEach((node) => {
    node.addEventListener('click', async () => {
      const caseId = node.dataset.reportCase
      if (!caseId) return
      await reportCase(caseId)
    })
  })
}

async function refreshDashboard() {
  state.loading = true
  state.error = null
  render()

  try {
    const [overview, events, cases] = await Promise.all([api.getOverview(), api.getEvents(), api.getCases()])
    state.overview = overview
    state.events = events
    state.cases = cases

    const nextEventId =
      state.selectedEventId && events.some((item) => item.id === state.selectedEventId)
        ? state.selectedEventId
        : events[0]?.id ?? null
    const nextCaseId =
      state.selectedCaseId && cases.some((item) => item.id === state.selectedCaseId)
        ? state.selectedCaseId
        : cases[0]?.id ?? null

    state.selectedEventId = nextEventId
    state.selectedCaseId = nextCaseId
    state.loading = false
    render()

    if (nextEventId) await loadEvent(nextEventId, false)
    if (nextCaseId) await loadCase(nextCaseId, false)
  } catch (error) {
    state.loading = false
    state.error = error instanceof Error ? error.message : '加载失败'
    render()
  }
}

async function loadEvent(eventId: string, withRender = true) {
  state.selectedEventId = eventId
  state.detailLoading = true
  if (withRender) render()
  try {
    state.selectedEvent = await api.getEvent(eventId)
    if (state.selectedEvent.case_id) state.selectedCaseId = state.selectedEvent.case_id
  } catch (error) {
    state.error = error instanceof Error ? error.message : '事件详情加载失败'
  } finally {
    state.detailLoading = false
    render()
  }
}

async function loadCase(caseId: string, withRender = true) {
  state.selectedCaseId = caseId
  state.detailLoading = true
  if (withRender) render()
  try {
    state.selectedCase = await api.getCase(caseId)
  } catch (error) {
    state.error = error instanceof Error ? error.message : '案件详情加载失败'
  } finally {
    state.detailLoading = false
    render()
  }
}

async function analyzeDemo() {
  state.actionLoading = true
  state.toast = '正在重新生成事件、案件与证据片段...'
  state.error = null
  render()

  try {
    const result = await api.analyzeDemo()
    state.toast = `分析完成：${result.events_created} 条事件，${result.cases_created} 个案件`
    await refreshDashboard()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '分析失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

async function reportEvent(eventId: string) {
  state.actionLoading = true
  state.error = null
  render()
  try {
    const result = await api.reportEvent(eventId)
    state.toast = result.message
    await refreshDashboard()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '事件举报失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

async function reportCase(caseId: string) {
  state.actionLoading = true
  state.error = null
  render()
  try {
    const result = await api.reportCase(caseId)
    state.toast = result.message
    await refreshDashboard()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '案件举报失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

refreshDashboard()
