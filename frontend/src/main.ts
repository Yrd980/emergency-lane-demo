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

function formatNumber(value: number | undefined, digits = 0): string {
  if (value === undefined || Number.isNaN(value)) return '--'
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(value)
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

function renderCommandDeck() {
  const source = selectedSource()
  const latestRun = state.overview?.latest_run
  const system = state.overview?.system
  return `
    <section class="command-deck panel-entrance">
      <div class="command-copy">
        <p class="eyebrow">双端协同答辩驾驶舱</p>
        <h1>生命通道占用识别、证据归档与辅助举报闭环</h1>
        <p class="deck-summary">围绕同一套 backend + Web + Android 数据链路，完整展示视频分析、事件融合、案件归档、证据链与人工复核同步。</p>
        <div class="deck-tags">
          <span>生命通道</span>
          <span>公众参与</span>
          <span>证据链闭环</span>
          <span>双端协同</span>
        </div>
        <div class="deck-meta">
          <div>
            <span>Web 角色</span>
            <strong>${escapeHtml(system?.web_role ?? '总览 / 分析控制 / 驾驶舱讲解')}</strong>
          </div>
          <div>
            <span>Android 角色</span>
            <strong>${escapeHtml(system?.android_role ?? '真机联调 / 协同复核 / 举报状态同步')}</strong>
          </div>
        </div>
      </div>
      <div class="command-actions">
        <div class="action-cluster">
          <label class="field compact-field">
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
        </div>
        <div class="signal-card">
          <span class="signal-label">Latest run</span>
          <strong>${latestRun ? escapeHtml(latestRun.id) : '暂无 run'}</strong>
          <p>${latestRun ? escapeHtml(latestRun.message) : '等待新的分析任务'}</p>
          <div class="signal-progress"><span style="width:${latestRun?.progress_percent ?? 0}%"></span></div>
          <small>${latestRun ? `${escapeHtml(latestRun.source_name)} · ${latestRun.event_count} 事件 / ${latestRun.case_count} 案件` : '可直接触发 demo 分析'}</small>
        </div>
        ${source ? `
          <div class="source-stage">
            <div class="source-stage-copy">
              <span class="signal-label">当前 source</span>
              <strong>${escapeHtml(source.title)}</strong>
              <p>${escapeHtml(source.location)} · ${escapeHtml(source.lane_label)}</p>
              <small>${source.duration_seconds}s · ${source.frame_count} 帧 · ${source.sample_interval_seconds}s 抽样</small>
            </div>
            ${source.preview_url ? `<img src="${source.preview_url}" alt="${escapeHtml(source.title)}" class="stage-preview" />` : '<div class="empty-box stage-empty">暂无封面</div>'}
          </div>
        ` : '<div class="empty-box stage-empty">暂无 source</div>'}
      </div>
    </section>
  `
}

function renderSituationRibbon() {
  const summary = state.overview?.summary
  const latestRun = state.overview?.latest_run
  const source = selectedSource()
  const cards = [
    {
      title: '分析主链',
      body: latestRun
        ? `${escapeHtml(latestRun.source_name)} · ${escapeHtml(latestRun.status)} · ${formatDateTime(latestRun.finished_at || latestRun.started_at)}`
        : '等待新的 analysis run',
      meta: latestRun ? `${latestRun.event_count} 事件 / ${latestRun.case_count} 案件` : '可直接从当前 source 发起分析',
    },
    {
      title: '双端协同',
      body: 'Web 大屏主控 + Android 真机协同复核',
      meta: '端侧已并入本地缓存层，弱网时继续支撑讲解与复核。',
    },
    {
      title: '证据闭环',
      body: `${formatNumber(summary?.pending_review_cases)} 待复核 / ${formatNumber(summary?.reported_cases)} 已举报`,
      meta: '事件 → 案件 → 证据图 → 15 秒片段 → 正式文书',
    },
    {
      title: '当前剧本',
      body: source ? `${escapeHtml(source.title)} / ${escapeHtml(source.location)}` : '暂无 source',
      meta: '答辩路径：选源 → 分析 → 事件/案件联动 → 复核 → 模拟举报',
    },
  ]

  return `
    <section class="situation-ribbon panel-entrance">
      ${cards
        .map(
          (item) => `
            <article class="ribbon-card">
              <span>${item.title}</span>
              <strong>${item.body}</strong>
              <p>${item.meta}</p>
            </article>
          `,
        )
        .join('')}
    </section>
  `
}

function renderMetricRail() {
  const summary = state.overview?.summary
  const latestRun = state.overview?.latest_run
  const metrics = [
    { label: '累计案件', value: formatNumber(summary?.total_cases), hint: '车辆级聚合后的案件池' },
    { label: '待复核', value: formatNumber(summary?.pending_review_cases), hint: '需要人工确认的案件' },
    { label: '已举报', value: formatNumber(summary?.reported_cases), hint: '已形成正式举报闭环' },
    { label: '平均置信度', value: formatNumber(summary?.avg_confidence, 2), hint: '多帧识别融合后的信号强度' },
    { label: '运行事件数', value: formatNumber(latestRun?.event_count), hint: '最新 run 识别出的事件' },
  ]

  return `
    <section class="metric-rail panel-entrance">
      ${metrics
        .map(
          (item) => `
            <article class="metric-tile">
              <span>${item.label}</span>
              <strong>${item.value}</strong>
              <small>${item.hint}</small>
            </article>
          `,
        )
        .join('')}
    </section>
  `
}

function renderMissionStrip() {
  const source = selectedSource()
  const latestRun = state.overview?.latest_run
  const focusCase = state.selectedCase ?? null
  const rehearsalSteps = [
    {
      label: '01 选源分析',
      title: source ? `${source.title}` : '选择演示视频源',
      detail: source ? `${source.duration_seconds}s / ${source.frame_count} 帧 / ${source.sample_interval_seconds}s 抽样` : '从 data/ 预置素材中切换演示源',
    },
    {
      label: '02 run 观察',
      title: latestRun ? `${latestRun.id} · ${latestRun.status}` : '等待新的 run',
      detail: latestRun
        ? `${latestRun.source_name} / ${latestRun.event_count} 事件 / ${latestRun.case_count} 案件 / ${latestRun.progress_percent}%`
        : '触发分析后观察任务状态与历史 run',
    },
    {
      label: '03 人工复核',
      title: focusCase ? `${focusCase.corrected_plate_number || focusCase.plate_number}` : '选择案件进入工作台',
      detail: focusCase
        ? `${focusCase.review_status} / ${focusCase.status} / ${focusCase.evidence.length} 张证据`
        : '修改车牌、备注与复核状态，确认 15 秒片段与文书',
    },
    {
      label: '04 双端同步',
      title: 'Web 驾驶舱 + Android 协同',
      detail: '以 FastAPI 为最终事实源，保持 run / case / report 状态一致',
    },
  ]

  return `
    <section class="mission-strip panel-entrance">
      <article class="dashboard-panel mission-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">答辩彩排路径</p>
            <h2>一屏讲清“输入源 → 识别融合 → 案件复核 → 双端协同”</h2>
          </div>
          <span class="badge info">闭环保留</span>
        </div>
        <div class="mission-chip-grid">
          <div class="mission-chip">
            <span>当前 source</span>
            <strong>${escapeHtml(source?.title ?? '暂无 source')}</strong>
            <small>${escapeHtml(source?.location ?? '待选择')} · ${escapeHtml(source?.lane_label ?? '应急车道场景')}</small>
          </div>
          <div class="mission-chip">
            <span>最新 run</span>
            <strong>${escapeHtml(latestRun?.id ?? '等待触发')}</strong>
            <small>${latestRun ? `${escapeHtml(latestRun.status)} · ${latestRun.progress_percent}% · ${latestRun.event_count} 事件` : '通过“发起分析”进入演示'}</small>
          </div>
          <div class="mission-chip">
            <span>案件复核焦点</span>
            <strong>${escapeHtml(focusCase ? focusCase.corrected_plate_number || focusCase.plate_number : '等待选中案件')}</strong>
            <small>${focusCase ? `${escapeHtml(focusCase.review_status)} / ${escapeHtml(focusCase.status)}` : '保留复核、文书、举报动作'}</small>
          </div>
          <div class="mission-chip">
            <span>联动原则</span>
            <strong>FastAPI 单一事实源</strong>
            <small>Web 负责驾驶舱展示，Android 负责现场协同复核</small>
          </div>
        </div>
      </article>
      <article class="dashboard-panel rehearsal-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">评委演示脚本</p>
            <h2>现场讲解顺序与当前重点</h2>
          </div>
        </div>
        <div class="rehearsal-list">
          ${rehearsalSteps
            .map(
              (step, index) => `
                <div class="rehearsal-step ${index === 2 && focusCase ? 'is-highlight' : ''}">
                  <span>${step.label}</span>
                  <strong>${escapeHtml(step.title)}</strong>
                  <p>${escapeHtml(step.detail)}</p>
                </div>
              `,
            )
            .join('')}
        </div>
      </article>
    </section>
  `
}

function renderTrendBars() {
  const trend = state.overview?.trend ?? []
  if (!trend.length) return `<div class="empty-box">暂无趋势数据</div>`
  const max = Math.max(...trend.map((item) => item.count), 1)
  return `
    <div class="trend-list">
      ${trend
        .map(
          (item) => `
            <div class="trend-row">
              <div>
                <strong>${escapeHtml(item.label)}</strong>
                <small>${item.count} 条</small>
              </div>
              <div class="trend-bar"><span style="width:${Math.max((item.count / max) * 100, 10)}%"></span></div>
            </div>
          `,
        )
        .join('')}
    </div>
  `
}

function renderPipelineRail() {
  const pipeline = state.overview?.pipeline ?? []
  if (!pipeline.length) return `<div class="empty-box">暂无 pipeline 信息</div>`
  return `
    <div class="pipeline-rail">
      ${pipeline
        .map(
          (step, index) => `
            <article class="pipeline-step">
              <span class="pipeline-index">0${index + 1}</span>
              <div>
                <strong>${escapeHtml(step.title)}</strong>
                <p>${escapeHtml(step.summary)}</p>
                <small>${escapeHtml(step.owner)} · ${step.evidence.map(escapeHtml).join(' / ')}</small>
              </div>
            </article>
          `,
        )
        .join('')}
    </div>
  `
}

function renderActivityFeed() {
  const recentEvents = state.overview?.recent_events ?? state.events.slice(0, 4)
  const recentCases = state.overview?.recent_cases ?? state.cases.slice(0, 4)
  return `
    <div class="activity-grid">
      <div>
        <div class="section-heading compact"><h3>最近事件</h3><span>${recentEvents.length}</span></div>
        <div class="compact-feed">
          ${recentEvents.length
            ? recentEvents
                .map(
                  (item) => `
                    <button class="feed-row compact" data-select-event="${item.id}">
                      <div>
                        <strong>${escapeHtml(item.plate_number)}</strong>
                        <p>${escapeHtml(item.summary)}</p>
                      </div>
                      <span class="badge ${caseStatusBadge(item.status)}">${escapeHtml(item.status)}</span>
                    </button>
                  `,
                )
                .join('')
            : '<div class="empty-box">暂无事件</div>'}
        </div>
      </div>
      <div>
        <div class="section-heading compact"><h3>最近案件</h3><span>${recentCases.length}</span></div>
        <div class="compact-feed">
          ${recentCases.length
            ? recentCases
                .map(
                  (item) => `
                    <button class="feed-row compact" data-select-case="${item.id}">
                      <div>
                        <strong>${escapeHtml(item.corrected_plate_number || item.plate_number)}</strong>
                        <p>${escapeHtml(item.summary)}</p>
                      </div>
                      <span class="badge ${caseStatusBadge(item.status)}">${escapeHtml(item.status)}</span>
                    </button>
                  `,
                )
                .join('')
            : '<div class="empty-box">暂无案件</div>'}
        </div>
      </div>
    </div>
  `
}

function renderStoryGrid() {
  return `
    <section class="story-grid panel-entrance">
      <article class="dashboard-panel narrative-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">态势趋势</p>
            <h2>分析节奏与答辩讲解线索</h2>
          </div>
        </div>
        ${renderTrendBars()}
      </article>
      <article class="dashboard-panel narrative-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">证据管线</p>
            <h2>从抽帧到正式举报</h2>
          </div>
        </div>
        ${renderPipelineRail()}
      </article>
      <article class="dashboard-panel narrative-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">协同视角</p>
            <h2>事件与案件最近动态</h2>
          </div>
        </div>
        ${renderActivityFeed()}
      </article>
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

function renderOperationsGrid() {
  return `
    <section class="operations-grid panel-entrance">
      <article class="dashboard-panel operations-panel run-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">运行看板</p>
            <h2>Run 历史与任务状态</h2>
          </div>
          <span class="badge info">${state.runs.length}</span>
        </div>
        ${renderRunHistory()}
      </article>
      <article class="dashboard-panel operations-panel list-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">事件流</p>
            <h2>按状态 / 车牌 / run 联动筛选</h2>
          </div>
          <span class="badge warning">${state.events.length}</span>
        </div>
        ${renderFilterPanel('event')}
        ${renderEventList()}
      </article>
      <article class="dashboard-panel operations-panel list-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">案件库</p>
            <h2>案件聚合与人工复核入口</h2>
          </div>
          <span class="badge info">${state.cases.length}</span>
        </div>
        ${renderFilterPanel('case')}
        ${renderCaseList()}
      </article>
    </section>
  `
}

function renderEventInspector() {
  const detail = state.selectedEvent
  if (state.detailLoading && !detail) return `<div class="loading-box">正在加载事件详情...</div>`
  if (!detail) return `<div class="empty-box">选择事件后查看证据帧、时间轴与举报入口。</div>`
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
                  <figure class="evidence-card">
                    <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
                    <figcaption>${escapeHtml(item.label)} · ${formatDateTime(item.captured_at)}</figcaption>
                  </figure>
                `,
              )
              .join('')
          : '<div class="empty-box">暂无证据图</div>'}
      </div>
      <div class="timeline-box">
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
  if (!detail) return `<div class="empty-box">选择案件后查看证据链、片段、文书与复核字段。</div>`
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
      <div class="case-proof-grid">
        <div class="clip-shell">
          <div class="section-heading compact"><h4>15 秒证据片段</h4><span>${detail.events.length} 条事件</span></div>
          ${detail.clip_url ? `<video class="clip-video" controls src="${detail.clip_url}"></video>` : '<div class="empty-box">暂无证据片段</div>'}
        </div>
        <div class="report-shell">
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
                  <figure class="evidence-card">
                    <img src="${item.image_url}" alt="${escapeHtml(item.label)}" />
                    <figcaption>${escapeHtml(item.label)} · ${formatDateTime(item.captured_at)}</figcaption>
                  </figure>
                `,
              )
              .join('')
          : '<div class="empty-box">暂无证据图</div>'}
      </div>
      <form class="editor-grid" id="case-editor-form">
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
      <div class="timeline-box">
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

function renderWorkbench() {
  return `
    <section class="workbench-grid panel-entrance">
      <article class="dashboard-panel workbench-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">事件工作台</p>
            <h2>证据帧、时间轴与事件举报</h2>
          </div>
          <span class="badge info">${escapeHtml(state.selectedEventId ?? '未选择')}</span>
        </div>
        ${renderEventInspector()}
      </article>
      <article class="dashboard-panel workbench-panel case-workbench-panel">
        <div class="section-heading">
          <div>
            <p class="panel-kicker">案件工作台</p>
            <h2>证据链、片段、文书与人工复核</h2>
          </div>
          <span class="badge info">${escapeHtml(state.selectedCaseId ?? '未选择')}</span>
        </div>
        ${renderCaseWorkbench()}
      </article>
    </section>
  `
}

function render() {
  app!.innerHTML = `
    <div class="page-shell">
      <div class="ambient-grid"></div>
      ${renderCommandDeck()}
      ${renderSituationRibbon()}
      ${state.error ? `<div class="global-error">${escapeHtml(state.error)}</div>` : ''}
      ${state.toast ? `<div class="global-toast">${escapeHtml(state.toast)}</div>` : ''}
      ${renderMetricRail()}
      ${renderMissionStrip()}
      ${renderStoryGrid()}
      ${renderOperationsGrid()}
      ${renderWorkbench()}
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
