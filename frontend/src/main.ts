import './style.css'
import { api } from './api'
import { getRouteFromHash, setRouteHash, type AppRoute } from './router'
import { applyTheme, getInitialTheme, isThemeKey, type ThemeKey } from './theme'
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
  route: AppRoute
  themeKey: ThemeKey
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
  route: getRouteFromHash(),
  themeKey: getInitialTheme(),
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

applyTheme(state.themeKey)
if (!window.location.hash) {
  setRouteHash(state.route)
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

function navigate(route: AppRoute) {
  const hashWillChange = window.location.hash !== `#/${route}`
  state.route = route
  setRouteHash(route)
  if (!hashWillChange) render()
}

function renderHeader() {
  const navItems: Array<{ route: AppRoute; label: string; desc: string }> = [
    { route: 'overview', label: '项目总览', desc: '价值、能力与链路' },
    { route: 'events', label: '事件中心', desc: '识别结果与证据查看' },
    { route: 'cases', label: '案件中心', desc: '案件归档与材料输出' },
  ]

  return `
    <header class="topbar panel-entrance">
      <div class="brand-block">
        <p class="eyebrow">Emergency Lane Project</p>
        <strong>高速公路应急车道违章辅助举报</strong>
      </div>
      <nav class="top-nav" aria-label="Primary">
        ${navItems
          .map(
            (item) => `
              <button class="nav-chip ${state.route === item.route ? 'is-active' : ''}" data-nav-route="${item.route}">
                <span>${item.label}</span>
              </button>
            `,
          )
          .join('')}
      </nav>
    </header>
  `
}

function renderCommandDeck() {
  const source = selectedSource()
  const latestRun = state.overview?.latest_run
  const summary = state.overview?.summary

  return `
    <section class="command-deck panel-entrance">
      <div class="command-copy">
        <div class="hero-intro">
          <p class="eyebrow">项目展示</p>
          <h1>高速公路应急车道违章辅助举报</h1>
          <p class="deck-summary">围绕 FastAPI 数据源，Web 端展示项目全链路，Android 端承担移动协同复核，让从视频输入到案件输出的全过程清晰可见。</p>
        </div>
        <div class="deck-tags">
          <span>FastAPI 主数据源</span>
          <span>Android 协同复核</span>
          <span>证据链与文书联动</span>
        </div>
        <div class="hero-actions">
          <label class="field">
            <span>选择演示视频源</span>
            <select id="source-select">
              ${state.sources
                .map(
                  (item) => `<option value="${item.name}" ${state.analysisSourceName === item.name ? 'selected' : ''}>${escapeHtml(item.title)}</option>`,
                )
                .join('')}
            </select>
          </label>
          <button class="action-button" id="analyze-demo" ${state.actionLoading ? 'disabled' : ''}>
            ${state.actionLoading ? '处理中...' : '运行分析'}
          </button>
        </div>
        <div class="deck-meta">
          <div>
            <span>当前案件数</span>
            <strong>${summary?.total_cases ?? state.cases.length} 个</strong>
          </div>
          <div>
            <span>待复核</span>
            <strong>${summary?.pending_review_cases ?? state.cases.filter((item) => item.review_status === '待复核').length} 个</strong>
          </div>
          <div>
            <span>已完成举报</span>
            <strong>${summary?.reported_cases ?? state.cases.filter((item) => item.status === '已举报').length} 个</strong>
          </div>
          <div>
            <span>当前视频源</span>
            <strong>${escapeHtml(source?.title ?? '暂无视频源')}</strong>
          </div>
        </div>
      </div>
      <div class="hero-stage">
        <div class="hero-visual-panel">
          ${source?.preview_url ? `<img src="${source.preview_url}" alt="${escapeHtml(source.title)}" class="stage-preview" />` : '<div class="empty-box stage-empty">暂无封面</div>'}
          <div class="hero-visual-copy">
            <div>
              <strong>${escapeHtml(source?.title ?? '暂无视频源')}</strong>
              <p>${escapeHtml(source ? `${source.location} · ${source.lane_label}` : '请选择视频源以展示项目识别与取证结果')}</p>
            </div>
            ${latestRun ? `<div><strong>最新运行</strong><p>${escapeHtml(latestRun.source_name)} · ${latestRun.event_count} 事件 / ${latestRun.case_count} 案件</p></div>` : '<div><strong>最新运行</strong><p>暂无运行记录</p></div>'}
          </div>
        </div>
      </div>
    </section>
  `
}








function renderRunHistory() {
  if (!state.runs.length) return `<div class="empty-box">暂无运行历史</div>`
  return `
    <div class="run-stack">
      ${state.runs
        .slice(0, 6)
        .map(
          (run) => `
            <article class="run-card">
              <div class="run-card-top">
                <strong>${escapeHtml(run.id)}</strong>
                <span class="badge ${runStatusBadge(run.status)}">${escapeHtml(run.status)}</span>
              </div>
              <p>${escapeHtml(run.source_name)} · ${escapeHtml(run.message || '暂无消息')}</p>
              <div class="meta-line">
                <span>${formatDateTime(run.started_at)}</span>
                <span>${run.event_count} 事件 / ${run.case_count} 案件</span>
              </div>
              <div class="signal-progress subtle"><span style="width:${run.progress_percent}%"></span></div>
            </article>
          `,
        )
        .join('')}
    </div>
  `
}

function renderFilterPanel(kind: 'event' | 'case') {
  const filters = kind === 'event' ? state.eventFilters : state.caseFilters
  return `
    <form class="filter-grid compact-filter" data-filter-form="${kind}">
      <label class="field compact-field"><span>Status</span><select name="status">${statusOptions(filters.status)}</select></label>
      <label class="field compact-field"><span>Review</span><select name="review_status"><option value="">全部复核</option>${reviewOptions(filters.review_status)}</select></label>
      <label class="field compact-field"><span>Run</span><select name="run_id">${runOptions(filters.run_id)}</select></label>
      <label class="field compact-field"><span>Plate</span><input name="plate" value="${escapeHtml(filters.plate)}" placeholder="输入车牌关键字" /></label>
      <div class="inline-actions compact-actions">
        <button class="action-button small" type="submit">应用</button>
        <button class="action-button ghost small" type="button" data-reset-filter="${kind}">重置</button>
      </div>
    </form>
  `
}

function renderEventList() {
  if (state.loading) return `<div class="loading-box">正在加载事件列表...</div>`
  if (!state.events.length) return `<div class="empty-box">当前筛选条件下暂无事件。</div>`
  return `
    <div class="data-feed">
      ${state.events
        .map(
          (event) => `
            <button class="feed-row ${state.selectedEventId === event.id ? 'is-active' : ''}" data-select-event="${event.id}">
              <div>
                <div class="row-title"><strong>${escapeHtml(event.plate_number)}</strong><span>${escapeHtml(event.id)}</span></div>
                <p>${escapeHtml(event.summary)}</p>
                <div class="meta-line">
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
    <div class="data-feed">
      ${state.cases
        .map(
          (item) => `
            <button class="feed-row ${state.selectedCaseId === item.id ? 'is-active' : ''}" data-select-case="${item.id}">
              <div>
                <div class="row-title"><strong>${escapeHtml(item.corrected_plate_number || item.plate_number)}</strong><span>${escapeHtml(item.id)}</span></div>
                <p>${escapeHtml(item.summary)}</p>
                <div class="meta-line">
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

function renderEventInspector() {
  const detail = state.selectedEvent
  if (state.detailLoading && !detail) return `<div class="loading-box">正在加载事件详情...</div>`
  if (!detail) return `<div class="empty-box">选择事件后查看证据帧、时间轴与事件状态。</div>`
  return `
    <div class="workbench-stack">
      <div class="inspector-topline">
        <div>
          <h3>${escapeHtml(detail.id)}</h3>
          <p>${escapeHtml(detail.description)}</p>
        </div>
        <span class="badge ${caseStatusBadge(detail.status)}">${escapeHtml(detail.status)}</span>
      </div>
      <div class="meta-line wrap">
        <span>run ${escapeHtml(detail.run_id)}</span>
        <span>source ${escapeHtml(detail.source_name)}</span>
        <span>review ${escapeHtml(detail.review_status)}</span>
        <span>case ${escapeHtml(detail.case_id ?? '未归档')}</span>
      </div>
      <div class="evidence-grid">
        ${detail.evidence.length
          ? detail.evidence
              .map(
                (item) => `
                  <figure class="evidence-card media-card">
                    <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
                    <figcaption>${escapeHtml(item.label)} · ${formatDateTime(item.captured_at)}</figcaption>
                  </figure>
                `,
              )
              .join('')
          : '<div class="empty-box">暂无证据图</div>'}
      </div>
      <div class="timeline-box detail-timeline">
        ${(detail.raw_analysis.timeline ?? [])
          .map(
            (item) => `
              <div class="timeline-row">
                <span>${item.timestamp_seconds.toFixed(1)}s</span>
                <p>${escapeHtml(item.description)}</p>
              </div>
            `,
          )
          .join('') || '<div class="empty-box">暂无时序记录</div>'}
      </div>
      <div class="workbench-actions">
        <button
          class="action-button ${detail.status !== '待举报' ? 'ghost' : ''}"
          data-report-event="${detail.id}"
          ${state.actionLoading || detail.status !== '待举报' ? 'disabled' : ''}
        >
          ${detail.status === '已举报' ? '事件已举报' : detail.status === '待复核' ? '案件待复核，暂不可举报' : '提交事件模拟举报'}
        </button>
      </div>
    </div>
  `
}

function renderCaseWorkbench() {
  const detail = state.selectedCase
  if (state.detailLoading && !detail) return `<div class="loading-box">正在加载案件详情...</div>`
  if (!detail) return `<div class="empty-box">选择案件后查看证据链、片段、文书与复核信息。</div>`
  return `
    <div class="workbench-stack">
      <div class="inspector-topline">
        <div>
          <h3>${escapeHtml(detail.corrected_plate_number || detail.plate_number)}</h3>
          <p>${escapeHtml(detail.id)} · ${escapeHtml(detail.source_name)} / run ${escapeHtml(detail.run_id)}</p>
        </div>
        <span class="badge ${caseStatusBadge(detail.status)}">${escapeHtml(detail.status)}</span>
      </div>
      <div class="meta-line wrap">
        <span>原始车牌 ${escapeHtml(detail.plate_number)}</span>
        <span>复核 ${escapeHtml(detail.review_status)}</span>
        <span>更新时间 ${formatDateTime(detail.updated_at)}</span>
      </div>
      <div class="case-proof-grid showcase-proof-grid">
        <div class="clip-shell dashboard-panel nested-panel media-panel">
          <div class="section-heading compact"><h4>15 秒证据片段</h4><span>${detail.events.length} 条事件</span></div>
          ${detail.clip_url ? `<video class="clip-video" controls src="${detail.clip_url}"></video>` : '<div class="empty-box">暂无证据片段</div>'}
        </div>
        <div class="report-shell dashboard-panel nested-panel document-panel">
          <div class="section-heading compact"><h4>正式举报文书</h4><span>${escapeHtml(detail.review_status)}</span></div>
          <div class="report-box">${escapeHtml(detail.report_content || '暂无文书，先完成复核再生成。').replaceAll('\n', '<br />')}</div>
          ${detail.report_file_url ? `<a class="action-link" href="${detail.report_file_url}" target="_blank" rel="noreferrer">打开 TXT 文书</a>` : ''}
        </div>
      </div>
      <div class="section-heading compact"><h4>关键证据图</h4><span>${detail.evidence.length} 张</span></div>
      <div class="evidence-grid">
        ${detail.evidence.length
          ? detail.evidence
              .map(
                (item) => `
                  <figure class="evidence-card media-card">
                    <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
                    <figcaption>${escapeHtml(item.label)} · ${formatDateTime(item.captured_at)}</figcaption>
                  </figure>
                `,
              )
              .join('')
          : '<div class="empty-box">暂无证据图</div>'}
      </div>
      <form class="editor-grid review-form" id="case-editor-form">
        <label class="field compact-field">
          <span>corrected_plate_number</span>
          <input id="case-corrected-plate" value="${escapeHtml(state.caseForm.corrected_plate_number)}" placeholder="可人工修正车牌" />
        </label>
        <label class="field compact-field">
          <span>review_status</span>
          <select id="case-review-status">${reviewOptions(state.caseForm.review_status)}</select>
        </label>
        <label class="field full-span">
          <span>operator_note</span>
          <textarea id="case-operator-note" rows="4" placeholder="记录人工复核意见">${escapeHtml(state.caseForm.operator_note)}</textarea>
        </label>
        <div class="inline-actions full-span workbench-actions">
          <button class="action-button" type="submit" ${state.actionLoading ? 'disabled' : ''}>保存复核信息</button>
          <button
            class="action-button ${detail.status !== '待举报' ? 'ghost' : ''}"
            type="button"
            data-report-case="${detail.id}"
            ${state.actionLoading || detail.status !== '待举报' ? 'disabled' : ''}
          >
            ${detail.status === '已举报' ? '案件已举报' : detail.status === '待复核' ? '待复核，暂不可举报' : '提交案件模拟举报'}
          </button>
        </div>
      </form>
      <div class="section-heading compact"><h4>关联事件时间轴</h4><span>${detail.events.length} 条</span></div>
      <div class="timeline-box detail-timeline">
        ${detail.events
          .map(
            (event) => `
              <div class="timeline-row">
                <span>${escapeHtml(event.id)}</span>
                <p>${escapeHtml(event.status)} / ${formatDateTime(event.first_seen)} / ${escapeHtml(event.plate_number)}</p>
              </div>
            `,
          )
          .join('') || '<div class="empty-box">暂无关联事件</div>'}
      </div>
    </div>
  `
}

function renderOverviewPage() {
  return `
    <section class="page-section overview-page panel-entrance">
      ${renderCommandDeck()}
      <section class="dashboard-grid panel-entrance">
        <article class="dashboard-panel">
          <div class="section-heading">
            <p class="panel-kicker">运行历史</p>
            <h2>最近生成的分析任务</h2>
          </div>
          ${renderRunHistory()}
        </article>
        <article class="dashboard-panel">
          <div class="section-heading">
            <p class="panel-kicker">核心模块</p>
            <h2>进入事件与案件模块</h2>
          </div>
          <div class="run-stack">
            <button class="feed-row" data-nav-route="events">
              <div>
                <strong>事件中心</strong>
                <p>查看识别结果、证据帧、时间线与事件级状态变化。</p>
              </div>
              <span class="badge info">${state.events.length}</span>
            </button>
            <button class="feed-row" data-nav-route="cases">
              <div>
                <strong>案件中心</strong>
                <p>查看案件材料、证据片段、文书内容与复核信息。</p>
              </div>
              <span class="badge info">${state.cases.length}</span>
            </button>
          </div>
        </article>
      </section>
    </section>
  `
}

function renderEventsPage() {
  return `
    <section class="page-section panel-entrance">
      <div class="section-heading">
        <p class="panel-kicker">事件中心</p>
        <h2>查看识别事件结果、证据图像与时间线细节</h2>
      </div>
      <section class="two-column-shell">
        <article class="list-panel">
          ${renderFilterPanel('event')}
          ${renderEventList()}
        </article>
        <article class="detail-panel">
          ${renderEventInspector()}
        </article>
      </section>
    </section>
  `
}

function renderCasesPage() {
  return `
    <section class="page-section panel-entrance">
      <div class="section-heading">
        <p class="panel-kicker">案件中心</p>
        <h2>查看案件归档结果、证据材料与复核信息</h2>
      </div>
      <section class="two-column-shell">
        <article class="list-panel">
          ${renderFilterPanel('case')}
          ${renderCaseList()}
        </article>
        <article class="detail-panel">
          ${renderCaseWorkbench()}
        </article>
      </section>
    </section>
  `
}

function renderCurrentPage() {
  switch (state.route) {
    case 'events':
      return renderEventsPage()
    case 'cases':
      return renderCasesPage()
    case 'overview':
    default:
      return renderOverviewPage()
  }
}

function render() {
  app!.innerHTML = `
    <div class="page-shell">
      <div class="ambient-grid"></div>
      ${renderHeader()}
      ${state.error ? `<div class="global-error">${escapeHtml(state.error)}</div>` : ''}
      ${state.toast ? `<div class="global-toast">${escapeHtml(state.toast)}</div>` : ''}
      ${renderCurrentPage()}
    </div>
  `
  bindEvents()
}

function bindEvents() {
  document.querySelectorAll<HTMLElement>('[data-nav-route]').forEach((node) => {
    node.addEventListener('click', () => {
      const route = node.dataset.navRoute as AppRoute | undefined
      if (!route) return
      navigate(route)
    })
  })

  document.querySelector<HTMLSelectElement>('#theme-select')?.addEventListener('change', (event) => {
    const value = (event.target as HTMLSelectElement).value
    if (!isThemeKey(value)) return
    state.themeKey = value
    applyTheme(value)
    render()
  })

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
      const targetRoute = node.dataset.openRoute as AppRoute | undefined
      if (targetRoute) navigate(targetRoute)
    })
  })

  document.querySelectorAll<HTMLElement>('[data-select-case]').forEach((node) => {
    node.addEventListener('click', async () => {
      const caseId = node.dataset.selectCase
      if (!caseId) return
      await loadCase(caseId)
      const targetRoute = node.dataset.openRoute as AppRoute | undefined
      if (targetRoute) navigate(targetRoute)
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

window.addEventListener('hashchange', () => {
  state.route = getRouteFromHash()
  render()
})

refreshDashboard()
