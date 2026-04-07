import './style.css'
import { api } from './api'
import type {
  CaseDetail,
  CaseStatus,
  CaseSummary,
  EventDetail,
  EventSummary,
  OverviewResponse,
  ReviewStatus,
  RunSummary,
  SourceSummary,
} from './types'

type Filters = {
  status: string
  plate: string
  run_id: string
  review_status: string
}

type CaseFormState = {
  corrected_plate_number: string
  operator_note: string
  review_status: ReviewStatus
}

type AppState = {
  overview: OverviewResponse | null
  sources: SourceSummary[]
  runs: RunSummary[]
  events: EventSummary[]
  cases: CaseSummary[]
  selectedEventId: string | null
  selectedCaseId: string | null
  selectedEvent: EventDetail | null
  selectedCase: CaseDetail | null
  eventFilters: Filters
  caseFilters: Filters
  analysisSourceName: string
  caseForm: CaseFormState
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
  sources: [],
  runs: [],
  events: [],
  cases: [],
  selectedEventId: null,
  selectedCaseId: null,
  selectedEvent: null,
  selectedCase: null,
  eventFilters: { status: '', plate: '', run_id: '', review_status: '' },
  caseFilters: { status: '', plate: '', run_id: '', review_status: '' },
  analysisSourceName: '',
  caseForm: {
    corrected_plate_number: '',
    operator_note: '',
    review_status: '待复核',
  },
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

function selectedSource(): SourceSummary | null {
  return state.sources.find((item) => item.name === state.analysisSourceName) ?? state.sources[0] ?? null
}

function caseStatusBadge(status: CaseStatus): string {
  if (status === '已举报') return 'success'
  if (status === '待举报') return 'info'
  return 'warning'
}

function runStatusBadge(status: RunSummary['status']): string {
  if (status === 'done') return 'success'
  if (status === 'failed') return 'danger'
  return 'warning'
}

function reviewOptions(selected: string): string {
  return ['待复核', '复核通过', '复核退回']
    .map((value) => `<option value="${value}" ${selected === value ? 'selected' : ''}>${value}</option>`)
    .join('')
}

function statusOptions(selected: string, includeReported = true): string {
  const values = includeReported ? ['', '待复核', '待举报', '已举报'] : ['', '待复核', '待举报']
  const labels: Record<string, string> = {
    '': '全部状态',
    待复核: '待复核',
    待举报: '待举报',
    已举报: '已举报',
  }
  return values
    .map((value) => `<option value="${value}" ${selected === value ? 'selected' : ''}>${labels[value]}</option>`)
    .join('')
}

function runOptions(selected: string): string {
  const base = `<option value="">全部 run</option>`
  return [base]
    .concat(
      state.runs.map(
        (run) => `<option value="${run.id}" ${selected === run.id ? 'selected' : ''}>${run.id} · ${run.source_name}</option>`,
      ),
    )
    .join('')
}

function renderHeader() {
  const source = selectedSource()
  const latestRun = state.overview?.latest_run
  return `
    <header class="hero card">
      <div>
        <p class="eyebrow">高速公路应急车道违章辅助举报原型</p>
        <h1>run 历史、source 选择、筛选与人工复核闭环</h1>
        <p class="hero-copy">围绕 backend + web + android 主线补齐多次分析、案件复核、证据链与状态同步，不做 newnew 主链并轨。</p>
      </div>
      <div class="hero-actions">
        <label class="field">
          <span>分析视频源</span>
          <select id="source-select">
            ${state.sources
              .map(
                (item) => `<option value="${item.name}" ${state.analysisSourceName === item.name ? 'selected' : ''}>${escapeHtml(item.title)} · ${escapeHtml(item.name)}</option>`,
              )
              .join('')}
          </select>
        </label>
        <button class="action-button" id="analyze-demo" ${state.actionLoading ? 'disabled' : ''}>
          ${state.actionLoading ? '处理中...' : '发起分析'}
        </button>
        <div class="status-tip">
          <strong>${latestRun ? escapeHtml(latestRun.id) : '暂无 run'}</strong>
          <span>${latestRun ? escapeHtml(latestRun.message) : '等待分析任务启动'}</span>
        </div>
        ${source ? `<small class="muted">当前源：${escapeHtml(source.title)} / ${source.duration_seconds}s / ${source.frame_count} 帧</small>` : ''}
      </div>
    </header>
  `
}

function renderOverview() {
  const summary = state.overview?.summary
  const source = selectedSource()
  return `
    <section class="card overview-card">
      <div class="section-heading">
        <div>
          <p class="panel-kicker">总览</p>
          <h2>运行态势与当前 source</h2>
        </div>
        <span class="badge info">${summary?.active_source ?? '未选择'}</span>
      </div>
      <div class="stats-grid">
        <article class="stat-card"><small>事件总数</small><strong>${summary?.total_events ?? '--'}</strong></article>
        <article class="stat-card"><small>案件总数</small><strong>${summary?.total_cases ?? '--'}</strong></article>
        <article class="stat-card"><small>待复核案件</small><strong>${summary?.pending_review_cases ?? '--'}</strong></article>
        <article class="stat-card"><small>已举报案件</small><strong>${summary?.reported_cases ?? '--'}</strong></article>
      </div>
      ${source ? `
        <div class="source-preview">
          <div>
            <div class="section-heading compact"><h3>${escapeHtml(source.title)}</h3><span>${escapeHtml(source.location)}</span></div>
            <div class="meta-list">
              <span>source_name: ${escapeHtml(source.name)}</span>
              <span>FPS ${source.fps}</span>
              <span>时长 ${source.duration_seconds}s</span>
              <span>抽样 ${source.sample_interval_seconds}s</span>
              <span>片段 ${source.case_clip_seconds}s</span>
            </div>
          </div>
          ${source.preview_url ? `<img class="source-image" src="${source.preview_url}" alt="${escapeHtml(source.title)}" />` : '<div class="empty-box">暂无封面</div>'}
        </div>
      ` : '<div class="empty-box">暂无 source</div>'}
    </section>
  `
}

function renderRuns() {
  if (!state.runs.length) return `<div class="empty-box">暂无运行历史</div>`
  return `
    <section class="card">
      <div class="section-heading">
        <div>
          <p class="panel-kicker">Run 历史</p>
          <h2>分析任务状态</h2>
        </div>
        <span class="badge info">${state.runs.length} 个</span>
      </div>
      <div class="run-list">
        ${state.runs
          .map(
            (run) => `
              <article class="run-item">
                <div class="run-top">
                  <strong>${escapeHtml(run.id)}</strong>
                  <span class="badge ${runStatusBadge(run.status)}">${escapeHtml(run.status)}</span>
                </div>
                <div class="meta-list compact">
                  <span>${escapeHtml(run.source_name)}</span>
                  <span>${formatDateTime(run.started_at)}</span>
                  <span>${run.event_count} 事件 / ${run.case_count} 案件</span>
                  <span>${run.progress_percent}%</span>
                </div>
                <p class="muted">${escapeHtml(run.message || '暂无消息')}</p>
              </article>
            `,
          )
          .join('')}
      </div>
    </section>
  `
}

function renderFilterPanel(kind: 'event' | 'case') {
  const filters = kind === 'event' ? state.eventFilters : state.caseFilters
  return `
    <section class="card">
      <div class="section-heading">
        <div>
          <p class="panel-kicker">${kind === 'event' ? '事件筛选' : '案件筛选'}</p>
          <h2>${kind === 'event' ? '按 status / plate / run / review 过滤事件' : '按 status / plate / run / review 过滤案件'}</h2>
        </div>
      </div>
      <form class="filter-grid" data-filter-form="${kind}">
        <label class="field"><span>Status</span><select name="status">${statusOptions(filters.status)}</select></label>
        <label class="field"><span>Review</span><select name="review_status"><option value="">全部复核</option>${reviewOptions(filters.review_status)}</select></label>
        <label class="field"><span>Run</span><select name="run_id">${runOptions(filters.run_id)}</select></label>
        <label class="field"><span>Plate</span><input name="plate" value="${escapeHtml(filters.plate)}" placeholder="输入车牌关键字" /></label>
        <div class="inline-actions filter-actions">
          <button class="action-button" type="submit">应用筛选</button>
          <button class="action-button ghost" type="button" data-reset-filter="${kind}">重置</button>
        </div>
      </form>
    </section>
  `
}

function renderEventList() {
  if (state.loading) return `<div class="loading-box">正在加载事件列表...</div>`
  if (!state.events.length) return `<div class="empty-box">当前筛选条件下暂无事件。</div>`
  return `
    <div class="list-stack">
      ${state.events
        .map(
          (event) => `
            <button class="list-row ${state.selectedEventId === event.id ? 'is-active' : ''}" data-select-event="${event.id}">
              <div>
                <div class="row-title"><strong>${escapeHtml(event.plate_number)}</strong><span>${escapeHtml(event.id)}</span></div>
                <p>${escapeHtml(event.summary)}</p>
                <div class="meta-list compact">
                  <span>${escapeHtml(event.source_name)}</span>
                  <span>${escapeHtml(event.review_status)}</span>
                  <span>${escapeHtml(event.case_id ?? '未归档')}</span>
                </div>
              </div>
              <div class="row-side">
                <span class="badge ${caseStatusBadge(event.status)}">${escapeHtml(event.status)}</span>
                <small>${event.duration_seconds.toFixed(1)}s</small>
              </div>
            </button>
          `,
        )
        .join('')}
    </div>
  `
}

function renderCaseList() {
  if (state.loading) return `<div class="loading-box">正在加载案件列表...</div>`
  if (!state.cases.length) return `<div class="empty-box">当前筛选条件下暂无案件。</div>`
  return `
    <div class="list-stack">
      ${state.cases
        .map(
          (item) => `
            <button class="list-row ${state.selectedCaseId === item.id ? 'is-active' : ''}" data-select-case="${item.id}">
              <div>
                <div class="row-title"><strong>${escapeHtml(item.corrected_plate_number || item.plate_number)}</strong><span>${escapeHtml(item.id)}</span></div>
                <p>${escapeHtml(item.summary)}</p>
                <div class="meta-list compact">
                  <span>${escapeHtml(item.source_name)}</span>
                  <span>${escapeHtml(item.review_status)}</span>
                  <span>${item.event_count} 事件 / ${item.evidence_count} 证据</span>
                </div>
              </div>
              <div class="row-side">
                <span class="badge ${caseStatusBadge(item.status)}">${escapeHtml(item.status)}</span>
              </div>
            </button>
          `,
        )
        .join('')}
    </div>
  `
}

function renderEventDetail() {
  const detail = state.selectedEvent
  if (state.detailLoading && !detail) return `<div class="loading-box">正在加载事件详情...</div>`
  if (!detail) return `<div class="empty-box">选择事件后查看证据帧与时序信息。</div>`
  return `
    <div class="detail-stack">
      <div class="section-heading compact"><h3>${escapeHtml(detail.id)}</h3><span class="badge ${caseStatusBadge(detail.status)}">${escapeHtml(detail.status)}</span></div>
      <div class="meta-list">
        <span>run: ${escapeHtml(detail.run_id)}</span>
        <span>source: ${escapeHtml(detail.source_name)}</span>
        <span>review: ${escapeHtml(detail.review_status)}</span>
        <span>case: ${escapeHtml(detail.case_id ?? '未归档')}</span>
      </div>
      <p>${escapeHtml(detail.description)}</p>
      <div class="evidence-grid">
        ${detail.evidence
          .map(
            (item) => `
              <figure class="evidence-card">
                <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
                <figcaption>${escapeHtml(item.label)} · ${formatDateTime(item.captured_at)}</figcaption>
              </figure>
            `,
          )
          .join('')}
      </div>
      <div class="timeline-box">
        ${(detail.raw_analysis.timeline ?? [])
          .map((item) => `<div class="timeline-row"><span>${item.timestamp_seconds.toFixed(1)}s</span><p>${escapeHtml(item.description)}</p></div>`)
          .join('') || '<div class="empty-box">暂无时序记录</div>'}
      </div>
      <button
        class="action-button ${detail.status !== '待举报' ? 'ghost' : ''}"
        data-report-event="${detail.id}"
        ${state.actionLoading || detail.status !== '待举报' ? 'disabled' : ''}
      >
        ${detail.status === '已举报' ? '事件已举报' : detail.status === '待复核' ? '案件待复核，暂不可举报' : '提交事件模拟举报'}
      </button>
    </div>
  `
}

function renderCaseDetail() {
  const detail = state.selectedCase
  if (state.detailLoading && !detail) return `<div class="loading-box">正在加载案件详情...</div>`
  if (!detail) return `<div class="empty-box">选择案件后查看详情并编辑复核字段。</div>`
  return `
    <div class="detail-stack">
      <div class="section-heading compact">
        <div>
          <h3>${escapeHtml(detail.id)}</h3>
          <p class="muted">${escapeHtml(detail.source_name)} / run ${escapeHtml(detail.run_id)}</p>
        </div>
        <span class="badge ${caseStatusBadge(detail.status)}">${escapeHtml(detail.status)}</span>
      </div>
      <div class="meta-list">
        <span>原始车牌：${escapeHtml(detail.plate_number)}</span>
        <span>修正车牌：${escapeHtml(detail.corrected_plate_number ?? '未修正')}</span>
        <span>复核状态：${escapeHtml(detail.review_status)}</span>
        <span>更新时间：${formatDateTime(detail.updated_at)}</span>
      </div>
      ${detail.clip_url ? `<video class="clip-video" controls src="${detail.clip_url}"></video>` : '<div class="empty-box">暂无证据片段</div>'}
      <form class="editor-grid" id="case-editor-form">
        <label class="field">
          <span>corrected_plate_number</span>
          <input id="case-corrected-plate" value="${escapeHtml(state.caseForm.corrected_plate_number)}" placeholder="可人工修正车牌" />
        </label>
        <label class="field">
          <span>review_status</span>
          <select id="case-review-status">${reviewOptions(state.caseForm.review_status)}</select>
        </label>
        <label class="field full-span">
          <span>operator_note</span>
          <textarea id="case-operator-note" rows="4" placeholder="记录人工复核意见">${escapeHtml(state.caseForm.operator_note)}</textarea>
        </label>
        <div class="inline-actions full-span">
          <button class="action-button" type="submit" ${state.actionLoading ? 'disabled' : ''}>保存复核信息</button>
          <button
            class="action-button ${detail.status !== '待举报' ? 'ghost' : ''}"
            type="button"
            data-report-case="${detail.id}"
            ${state.actionLoading || detail.status !== '待举报' ? 'disabled' : ''}
          >
            ${detail.status === '已举报' ? '案件已举报' : detail.status === '待复核' ? '待复核，暂不可举报' : '提交案件模拟举报'}
          </button>
          ${detail.report_file_url ? `<a class="action-link" href="${detail.report_file_url}" target="_blank" rel="noreferrer">打开 TXT 文书</a>` : ''}
        </div>
      </form>
      <div class="section-heading compact"><h4>关联事件</h4><span>${detail.events.length} 条</span></div>
      <div class="timeline-box">
        ${detail.events
          .map(
            (event) => `<div class="timeline-row"><span>${escapeHtml(event.id)}</span><p>${escapeHtml(event.status)} / ${formatDateTime(event.first_seen)} / ${escapeHtml(event.plate_number)}</p></div>`,
          )
          .join('')}
      </div>
      <div class="section-heading compact"><h4>操作备注</h4><span>${escapeHtml(detail.review_status)}</span></div>
      <p class="muted">${escapeHtml(detail.operator_note || '暂无人工备注')}</p>
    </div>
  `
}

function render() {
  app!.innerHTML = `
    <div class="page-shell">
      ${renderHeader()}
      ${state.error ? `<div class="global-error">${escapeHtml(state.error)}</div>` : ''}
      ${state.toast ? `<div class="global-toast">${escapeHtml(state.toast)}</div>` : ''}
      <main class="layout-grid">
        ${renderOverview()}
        ${renderRuns()}
        ${renderFilterPanel('event')}
        ${renderFilterPanel('case')}
        <section class="card"><div class="section-heading"><h2>事件列表</h2><span class="badge warning">${state.events.length}</span></div>${renderEventList()}</section>
        <section class="card"><div class="section-heading"><h2>案件列表</h2><span class="badge info">${state.cases.length}</span></div>${renderCaseList()}</section>
        <section class="card"><div class="section-heading"><h2>事件详情</h2><span class="badge info">${escapeHtml(state.selectedEventId ?? '未选择')}</span></div>${renderEventDetail()}</section>
        <section class="card"><div class="section-heading"><h2>案件详情 / 人工复核</h2><span class="badge info">${escapeHtml(state.selectedCaseId ?? '未选择')}</span></div>${renderCaseDetail()}</section>
      </main>
    </div>
  `
  bindEvents()
}

function bindEvents() {
  document.querySelector<HTMLSelectElement>('#source-select')?.addEventListener('change', (event) => {
    state.analysisSourceName = (event.target as HTMLSelectElement).value
  })

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

  document.querySelectorAll<HTMLFormElement>('[data-filter-form]').forEach((form) => {
    form.addEventListener('submit', async (event) => {
      event.preventDefault()
      const kind = form.dataset.filterForm as 'event' | 'case'
      const formData = new FormData(form)
      const nextFilters = {
        status: String(formData.get('status') ?? ''),
        plate: String(formData.get('plate') ?? ''),
        run_id: String(formData.get('run_id') ?? ''),
        review_status: String(formData.get('review_status') ?? ''),
      }
      if (kind === 'event') state.eventFilters = nextFilters
      else state.caseFilters = nextFilters
      await refreshDashboard()
    })
  })

  document.querySelectorAll<HTMLElement>('[data-reset-filter]').forEach((button) => {
    button.addEventListener('click', async () => {
      const kind = button.dataset.resetFilter as 'event' | 'case'
      if (kind === 'event') state.eventFilters = { status: '', plate: '', run_id: '', review_status: '' }
      else state.caseFilters = { status: '', plate: '', run_id: '', review_status: '' }
      await refreshDashboard()
    })
  })

  document.querySelector<HTMLInputElement>('#case-corrected-plate')?.addEventListener('input', (event) => {
    state.caseForm.corrected_plate_number = (event.target as HTMLInputElement).value
  })
  document.querySelector<HTMLSelectElement>('#case-review-status')?.addEventListener('change', (event) => {
    state.caseForm.review_status = (event.target as HTMLSelectElement).value as ReviewStatus
  })
  document.querySelector<HTMLTextAreaElement>('#case-operator-note')?.addEventListener('input', (event) => {
    state.caseForm.operator_note = (event.target as HTMLTextAreaElement).value
  })
  document.querySelector<HTMLFormElement>('#case-editor-form')?.addEventListener('submit', async (event) => {
    event.preventDefault()
    await submitCaseEdit()
  })
}

async function refreshDashboard() {
  state.loading = true
  state.error = null
  render()
  try {
    const [overview, sources, runs, events, cases] = await Promise.all([
      api.getOverview(),
      api.getSources(),
      api.getRuns(),
      api.getEvents(state.eventFilters),
      api.getCases(state.caseFilters),
    ])
    state.overview = overview
    state.sources = sources
    state.runs = runs
    state.events = events
    state.cases = cases
    state.analysisSourceName = state.analysisSourceName || overview.source.name || sources[0]?.name || ''

    state.selectedEventId = state.selectedEventId && events.some((item) => item.id === state.selectedEventId) ? state.selectedEventId : events[0]?.id ?? null
    state.selectedCaseId = state.selectedCaseId && cases.some((item) => item.id === state.selectedCaseId) ? state.selectedCaseId : cases[0]?.id ?? null
    state.loading = false
    render()

    if (state.selectedEventId) await loadEvent(state.selectedEventId, false)
    else state.selectedEvent = null
    if (state.selectedCaseId) await loadCase(state.selectedCaseId, false)
    else state.selectedCase = null
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

function setCaseFormFromDetail(detail: CaseDetail) {
  state.caseForm = {
    corrected_plate_number: detail.corrected_plate_number ?? '',
    operator_note: detail.operator_note ?? '',
    review_status: detail.review_status,
  }
}

async function loadCase(caseId: string, withRender = true) {
  state.selectedCaseId = caseId
  state.detailLoading = true
  if (withRender) render()
  try {
    state.selectedCase = await api.getCase(caseId)
    setCaseFormFromDetail(state.selectedCase)
  } catch (error) {
    state.error = error instanceof Error ? error.message : '案件详情加载失败'
  } finally {
    state.detailLoading = false
    render()
  }
}

async function analyzeDemo() {
  state.actionLoading = true
  state.error = null
  state.toast = `正在分析 ${state.analysisSourceName || '默认视频源'}...`
  render()
  try {
    const result = await api.analyzeDemo(state.analysisSourceName)
    state.toast = `分析完成：${result.source_name} / ${result.events_created} 条事件 / ${result.cases_created} 个案件`
    await refreshDashboard()
  } catch (error) {
    state.error = error instanceof Error ? error.message : '分析失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

async function submitCaseEdit() {
  if (!state.selectedCaseId) return
  state.actionLoading = true
  state.error = null
  render()
  try {
    const updated = await api.updateCase(state.selectedCaseId, {
      corrected_plate_number: state.caseForm.corrected_plate_number || null,
      operator_note: state.caseForm.operator_note,
      review_status: state.caseForm.review_status,
    })
    state.selectedCase = updated
    setCaseFormFromDetail(updated)
    state.toast = '案件复核信息已保存'
    await refreshDashboard()
    await loadCase(updated.id, false)
  } catch (error) {
    state.error = error instanceof Error ? error.message : '案件更新失败'
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
    await loadCase(caseId, false)
  } catch (error) {
    state.error = error instanceof Error ? error.message : '案件举报失败'
  } finally {
    state.actionLoading = false
    render()
  }
}

refreshDashboard()
