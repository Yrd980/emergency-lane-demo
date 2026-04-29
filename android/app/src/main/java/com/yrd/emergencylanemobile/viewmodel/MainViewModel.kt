package com.yrd.emergencylanemobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yrd.emergencylanemobile.ApiBaseUrlStore
import com.yrd.emergencylanemobile.BuildConfig
import com.yrd.emergencylanemobile.DeviceAssistStore
import com.yrd.emergencylanemobile.model.MobileCaseDetail
import com.yrd.emergencylanemobile.model.MobileCaseUpdateRequest
import com.yrd.emergencylanemobile.model.MobileEventSummary
import com.yrd.emergencylanemobile.model.MobileEvidence
import com.yrd.emergencylanemobile.model.MobileOverview
import com.yrd.emergencylanemobile.model.MobileRawAnalysis
import com.yrd.emergencylanemobile.model.MobileRunSummary
import com.yrd.emergencylanemobile.model.MobileSource
import com.yrd.emergencylanemobile.model.MobileSummary
import com.yrd.emergencylanemobile.model.MobileSystem
import com.yrd.emergencylanemobile.model.MobileTimelineItem
import com.yrd.emergencylanemobile.model.MobileConnectionState
import com.yrd.emergencylanemobile.model.MobileLocalDraft
import com.yrd.emergencylanemobile.model.MobileSyncSnapshot
import com.yrd.emergencylanemobile.model.MobileUiState
import com.yrd.emergencylanemobile.network.DemoApiService
import com.yrd.emergencylanemobile.network.createDemoApiService
import com.yrd.emergencylanemobile.normalizeApiBaseUrl
import com.yrd.emergencylanemobile.resolveApiBaseUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun nowTimeLabel(): String = SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(Date())

private fun errorMessage(error: Throwable, fallback: String): String {
    val message = error.message?.lineSequence()?.firstOrNull()?.trim().orEmpty()
    return message.ifBlank { fallback }
}

private fun bundledDemoCases() = listOf(
    com.yrd.emergencylanemobile.model.MobileCaseSummary(
        id = "demo-case-001",
        runId = "run-demo-offline",
        sourceName = "本地内置 demo",
        plateNumber = "浙A12345",
        correctedPlateNumber = "浙A12345",
        reviewStatus = "待复核",
        operatorNote = "使用 APK 内置素材做离线讲解",
        status = "待举报",
        location = "G60 沪昆高速",
        confidence = 0.96,
        summary = "车辆持续占用应急车道，已生成证据图与片段。",
        eventCount = 2,
        evidenceCount = 3,
        clipUrl = null,
    ),
    com.yrd.emergencylanemobile.model.MobileCaseSummary(
        id = "demo-case-002",
        runId = "run-demo-offline",
        sourceName = "本地内置 demo",
        plateNumber = "沪B67890",
        correctedPlateNumber = null,
        reviewStatus = "已复核",
        operatorNote = "演示用已举报案例",
        status = "已举报",
        location = "G2 京沪高速",
        confidence = 0.91,
        summary = "历史案例已完成复核与模拟举报。",
        eventCount = 1,
        evidenceCount = 2,
        clipUrl = null,
    ),
)

private fun bundledDemoCaseDetail(caseId: String) = when (caseId) {
    "demo-case-002" -> MobileCaseDetail(
        id = "demo-case-002",
        runId = "run-demo-offline",
        sourceName = "本地内置 demo",
        plateNumber = "沪B67890",
        correctedPlateNumber = null,
        reviewStatus = "已复核",
        operatorNote = "演示用已举报案例",
        status = "已举报",
        location = "G2 京沪高速",
        confidence = 0.91,
        summary = "历史案例已完成复核与模拟举报。",
        clipUrl = null,
        reportContent = "【模拟举报文书】沪B67890 于 G2 京沪高速占用应急车道，证据链完整，已完成演示性上报。",
        reportFileUrl = null,
        reportedAt = "2026-04-10 20:00",
        evidence = listOf(
            MobileEvidence("证据图 A", "file:///android_asset/case_demo_evidence/ev-2.jpg", "2026-04-10 19:55"),
            MobileEvidence("证据图 B", "file:///android_asset/case_demo_evidence/ev-3.jpg", "2026-04-10 19:56"),
        ),
        events = listOf(
            MobileEventSummary("event-demo-002", "沪B67890", "已归档", "2026-04-10 19:54"),
        ),
        rawAnalysis = MobileRawAnalysis(
            timeline = listOf(
                MobileTimelineItem(3.0, "车辆进入应急车道"),
                MobileTimelineItem(8.0, "案件归档并生成文书"),
            ),
        ),
    )
    else -> MobileCaseDetail(
        id = "demo-case-001",
        runId = "run-demo-offline",
        sourceName = "本地内置 demo",
        plateNumber = "浙A12345",
        correctedPlateNumber = "浙A12345",
        reviewStatus = "待复核",
        operatorNote = "使用 APK 内置素材做离线讲解",
        status = "待举报",
        location = "G60 沪昆高速",
        confidence = 0.96,
        summary = "车辆持续占用应急车道，已生成证据图与片段。",
        clipUrl = null,
        reportContent = "【模拟举报文书】浙A12345 在 G60 沪昆高速持续占用应急车道，系统已生成证据图、clip 与案件摘要。",
        reportFileUrl = null,
        reportedAt = null,
        evidence = listOf(
            MobileEvidence("证据图 A", "file:///android_asset/case_demo_evidence/ev-1.jpg", "2026-04-10 19:40"),
            MobileEvidence("证据图 B", "file:///android_asset/case_demo_evidence/ev-2.jpg", "2026-04-10 19:41"),
            MobileEvidence("证据图 C", "file:///android_asset/case_demo_evidence/ev-3.jpg", "2026-04-10 19:42"),
        ),
        events = listOf(
            MobileEventSummary("event-demo-001", "浙A12345", "已归档", "2026-04-10 19:39"),
            MobileEventSummary("event-demo-001-b", "浙A12345", "待举报", "2026-04-10 19:43"),
        ),
        rawAnalysis = MobileRawAnalysis(
            timeline = listOf(
                MobileTimelineItem(1.5, "车辆压线进入应急车道"),
                MobileTimelineItem(6.0, "车牌识别稳定为浙A12345"),
                MobileTimelineItem(12.0, "生成案件 clip 与证据图"),
            ),
        ),
    )
}

private fun bundledDemoOverview() = MobileOverview(
    summary = MobileSummary(
        totalEvents = 3,
        totalCases = 2,
        reportedCases = 1,
        pendingReviewCases = 1,
        avgConfidence = 0.94,
    ),
    latestRun = MobileRunSummary(
        id = "run-demo-offline",
        sourceName = "本地内置 demo",
        status = "完成",
        message = "后端不可达时自动切换到 APK 内置演示数据，确保案件页和本地检测页仍可讲解。",
        startedAt = "2026-04-10 19:38",
        finishedAt = "2026-04-10 19:45",
        progressPercent = 100,
        eventCount = 3,
        caseCount = 2,
    ),
    runs = listOf(
        MobileRunSummary(
            id = "run-demo-offline",
            sourceName = "本地内置 demo",
            status = "完成",
            message = "本地素材已生成事件、案件与证据链。",
            startedAt = "2026-04-10 19:38",
            finishedAt = "2026-04-10 19:45",
            progressPercent = 100,
            eventCount = 3,
            caseCount = 2,
        ),
    ),
    source = MobileSource(
        name = "bundled-demo",
        title = "APK 本地离线演示源",
        previewUrl = null,
        durationSeconds = 15.0,
    ),
    system = MobileSystem(
        webRole = "总览 / 事件案件筛选 / 证据链展示（纯 Mock 原型）",
        androidRole = "端侧独立检测演示：CameraX + YOLOv8n + HyperLPR3 + Room，真机本地闭环",
        referenceBasis = "Android 端独立于 Web 端运行，使用本地内置素材 + 端侧检测链进行演示，不依赖后端。",
    ),
)

private fun bundledDemoState(previous: MobileUiState, message: String): MobileUiState {
    val overview = bundledDemoOverview()
    val allCases = bundledDemoCases()
    val filteredCases = previous.selectedStatusFilter?.let { selected ->
        allCases.filter { it.status == selected || it.reviewStatus == selected }
    } ?: allCases
    val cases = filteredCases.ifEmpty { allCases }
    val selectedCaseId = previous.selectedCaseId?.takeIf { id -> cases.any { it.id == id } } ?: cases.first().id
    val detail = bundledDemoCaseDetail(selectedCaseId)
    return previous.copy(
        overview = overview,
        runs = overview.runs,
        cases = cases,
        selectedCaseId = selectedCaseId,
        selectedCase = detail,
        selectedStatusFilter = previous.selectedStatusFilter,
        loading = false,
        refreshing = false,
        error = null,
        toast = message,
    )
}


class MainViewModel(
    context: Context,
    private val apiBaseUrlStore: ApiBaseUrlStore = ApiBaseUrlStore(context.applicationContext),
    private val deviceAssistStore: DeviceAssistStore = DeviceAssistStore(context.applicationContext),
) : ViewModel() {
    private val _uiState = MutableStateFlow(MobileUiState(connection = initialConnectionState()))
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        hydrateFromSnapshot()
        refresh()
    }

    private fun initialConnectionState(): MobileConnectionState {
        val resolved = resolveApiBaseUrl(apiBaseUrlStore)
        return MobileConnectionState(
            activeBaseUrl = resolved.activeBaseUrl,
            activeSource = resolved.activeSource,
            buildDefaultBaseUrl = resolved.buildDefaultBaseUrl,
            buildDefaultSource = resolved.buildDefaultSource,
            emulatorFallbackBaseUrl = resolved.emulatorFallbackBaseUrl,
            lanHintBaseUrl = resolved.lanHintBaseUrl,
            hasRuntimeOverride = resolved.hasRuntimeOverride,
        )
    }

    private fun connectionWithSnapshot(
        snapshot: MobileSyncSnapshot? = deviceAssistStore.loadSnapshot(),
        lastOverviewSummary: String? = _uiState.value.connection.lastOverviewSummary,
        lastSuccessAt: String? = _uiState.value.connection.lastSuccessAt,
        lastError: String? = _uiState.value.connection.lastError,
        usingCachedData: Boolean = false,
    ): MobileConnectionState {
        val resolved = resolveApiBaseUrl(apiBaseUrlStore)
        val cacheStatus = snapshot?.let {
            "${it.runs.size} 个 run / ${it.cases.size} 个案件 / 最近缓存 ${it.cachedAt}"
        }
        return MobileConnectionState(
            activeBaseUrl = resolved.activeBaseUrl,
            activeSource = resolved.activeSource,
            buildDefaultBaseUrl = resolved.buildDefaultBaseUrl,
            buildDefaultSource = resolved.buildDefaultSource,
            emulatorFallbackBaseUrl = resolved.emulatorFallbackBaseUrl,
            lanHintBaseUrl = resolved.lanHintBaseUrl,
            hasRuntimeOverride = resolved.hasRuntimeOverride,
            cacheAvailable = snapshot != null,
            cacheStatus = cacheStatus,
            cacheRunCount = snapshot?.runs?.size ?: 0,
            cacheCaseCount = snapshot?.cases?.size ?: 0,
            usingCachedData = usingCachedData,
            lastOverviewSummary = lastOverviewSummary,
            lastSuccessAt = lastSuccessAt,
            lastError = lastError,
        )
    }

    private fun demoApi(baseUrl: String = _uiState.value.connection.activeBaseUrl): DemoApiService =
        createDemoApiService(baseUrl)

    private fun hydrateFromSnapshot() {
        val snapshot = deviceAssistStore.loadSnapshot() ?: return
        val selectedCaseId = snapshot.selectedCaseId ?: snapshot.cases.firstOrNull()?.id
        _uiState.value = _uiState.value.copy(
            overview = snapshot.overview,
            runs = snapshot.runs,
            cases = snapshot.cases,
            selectedCaseId = selectedCaseId,
            selectedCase = snapshot.selectedCase,
            selectedStatusFilter = snapshot.selectedStatusFilter,
            loading = false,
            connection = connectionWithSnapshot(snapshot = snapshot),
            localDraft = selectedCaseId?.let(deviceAssistStore::loadDraft),
        )
    }

    private fun persistSnapshot(selectedCase: MobileCaseDetail? = _uiState.value.selectedCase) {
        val current = _uiState.value
        val snapshot = MobileSyncSnapshot(
            cachedAt = nowTimeLabel(),
            overview = current.overview,
            runs = current.runs,
            cases = current.cases,
            selectedCaseId = current.selectedCaseId,
            selectedCase = selectedCase,
            selectedStatusFilter = current.selectedStatusFilter,
        )
        deviceAssistStore.saveSnapshot(snapshot)
        _uiState.value = _uiState.value.copy(
            connection = connectionWithSnapshot(
                snapshot = snapshot,
                lastOverviewSummary = current.connection.lastOverviewSummary,
                lastSuccessAt = current.connection.lastSuccessAt,
                lastError = current.connection.lastError,
                usingCachedData = false,
            ),
        )
    }

    fun refresh() {
        viewModelScope.launch {
            val previous = _uiState.value
            val snapshot = deviceAssistStore.loadSnapshot()
            _uiState.value = previous.copy(
                loading = previous.overview == null,
                refreshing = true,
                error = null,
                connection = connectionWithSnapshot(snapshot = snapshot, lastError = null, usingCachedData = false),
            )
            try {
                val api = demoApi()
                val overview = api.getOverview()
                val runs = api.getRuns()
                val cases = api.getCases(previous.selectedStatusFilter)
                val selectedCaseId = previous.selectedCaseId?.takeIf { id -> cases.any { it.id == id } } ?: cases.firstOrNull()?.id
                val selectedCase = selectedCaseId?.let { api.getCase(it) }
                val overviewSummary = "总览拉取成功：${overview.summary.totalCases} 案件 / ${overview.summary.pendingReviewCases} 待复核 / ${overview.summary.reportedCases} 已举报"
                _uiState.value = MobileUiState(
                    overview = overview,
                    runs = runs,
                    cases = cases,
                    selectedCaseId = selectedCaseId,
                    selectedCase = selectedCase,
                    selectedStatusFilter = previous.selectedStatusFilter,
                    loading = false,
                    refreshing = false,
                    busy = false,
                    toast = previous.toast,
                    error = null,
                    connection = connectionWithSnapshot(
                        snapshot = snapshot,
                        lastOverviewSummary = overviewSummary,
                        lastSuccessAt = nowTimeLabel(),
                        lastError = null,
                        usingCachedData = false,
                    ),
                    localDraft = selectedCaseId?.let(deviceAssistStore::loadDraft),
                )
                persistSnapshot(selectedCase)
            } catch (error: Exception) {
                val message = errorMessage(error, "加载失败")
                val cached = deviceAssistStore.loadSnapshot()
                if (cached != null) {
                    val fallbackCaseId = previous.selectedCaseId ?: cached.selectedCaseId ?: cached.cases.firstOrNull()?.id
                    _uiState.value = previous.copy(
                        overview = previous.overview ?: cached.overview,
                        runs = if (previous.runs.isNotEmpty()) previous.runs else cached.runs,
                        cases = if (previous.cases.isNotEmpty()) previous.cases else cached.cases,
                        selectedCaseId = fallbackCaseId,
                        selectedCase = previous.selectedCase ?: cached.selectedCase,
                        selectedStatusFilter = previous.selectedStatusFilter ?: cached.selectedStatusFilter,
                        loading = false,
                        refreshing = false,
                        error = null,
                        toast = previous.toast,
                        connection = connectionWithSnapshot(
                            snapshot = cached,
                            lastOverviewSummary = previous.connection.lastOverviewSummary,
                            lastSuccessAt = previous.connection.lastSuccessAt,
                            lastError = message,
                            usingCachedData = true,
                        ),
                        localDraft = fallbackCaseId?.let(deviceAssistStore::loadDraft),
                    )
                } else {
                    val offlineState = bundledDemoState(previous, "后端暂不可达，已切换到本地演示数据")
                    _uiState.value = offlineState.copy(
                        connection = connectionWithSnapshot(
                            snapshot = null,
                            lastOverviewSummary = "当前使用 APK 内置离线演示数据",
                            lastSuccessAt = nowTimeLabel(),
                            lastError = message,
                            usingCachedData = true,
                        ),
                        localDraft = offlineState.selectedCaseId?.let(deviceAssistStore::loadDraft),
                    )
                    persistSnapshot(_uiState.value.selectedCase)
                }
            }
        }
    }

    fun selectStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        refresh()
    }

    fun selectCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                busy = true,
                selectedCaseId = caseId,
                localDraft = deviceAssistStore.loadDraft(caseId),
                error = null,
            )
            try {
                val detail = demoApi().getCase(caseId)
                _uiState.value = _uiState.value.copy(selectedCase = detail, busy = false)
                persistSnapshot(detail)
            } catch (error: Exception) {
                val cached = deviceAssistStore.loadSnapshot()
                val cachedDetail = cached?.selectedCase?.takeIf { it.id == caseId }
                if (cachedDetail != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedCase = cachedDetail,
                        busy = false,
                        error = null,
                        toast = _uiState.value.toast,
                        connection = connectionWithSnapshot(
                            snapshot = cached,
                            lastOverviewSummary = _uiState.value.connection.lastOverviewSummary,
                            lastSuccessAt = _uiState.value.connection.lastSuccessAt,
                            lastError = errorMessage(error, "案件详情加载失败"),
                            usingCachedData = true,
                        ),
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = errorMessage(error, "案件详情加载失败"),
                    )
                }
            }
        }
    }

    fun saveLocalDraft(caseId: String, localPlate: String, sceneNote: String, reviewStatus: String) {
        val draft = MobileLocalDraft(
            caseId = caseId,
            localPlateCandidate = localPlate.trim(),
            sceneNote = sceneNote.trim(),
            reviewStatus = reviewStatus,
            savedAt = nowTimeLabel(),
        )
        deviceAssistStore.saveDraft(draft)
        _uiState.value = _uiState.value.copy(localDraft = draft, toast = "端侧草稿已保存到本机缓存")
    }

    fun clearLocalDraft(caseId: String) {
        deviceAssistStore.clearDraft(caseId)
        _uiState.value = _uiState.value.copy(localDraft = null, toast = "端侧草稿已清空")
    }

    private fun applyOfflineReviewUpdate(caseId: String, correctedPlate: String, operatorNote: String, reviewStatus: String) {
        val current = _uiState.value
        val updatedCases = current.cases.map { item ->
            if (item.id == caseId) {
                item.copy(
                    correctedPlateNumber = correctedPlate.ifBlank { item.correctedPlateNumber },
                    operatorNote = operatorNote.ifBlank { item.operatorNote },
                    reviewStatus = reviewStatus,
                    status = if (reviewStatus == "复核通过") "待举报" else item.status,
                )
            } else item
        }
        val baseDetail = current.selectedCase ?: bundledDemoCaseDetail(caseId)
        val updatedDetail = baseDetail.copy(
            correctedPlateNumber = correctedPlate.ifBlank { baseDetail.correctedPlateNumber },
            operatorNote = operatorNote.ifBlank { baseDetail.operatorNote },
            reviewStatus = reviewStatus,
            status = if (reviewStatus == "复核通过") "待举报" else baseDetail.status,
        )
        _uiState.value = current.copy(
            cases = updatedCases,
            selectedCase = updatedDetail,
            busy = false,
            toast = "已按离线演示模式更新复核信息",
            error = null,
        )
        persistSnapshot(updatedDetail)
    }

    private fun applyOfflineReport(caseId: String) {
        val current = _uiState.value
        val reportTime = nowTimeLabel()
        val updatedCases = current.cases.map { item ->
            if (item.id == caseId) item.copy(status = "已举报", reviewStatus = "复核通过") else item
        }
        val baseDetail = current.selectedCase ?: bundledDemoCaseDetail(caseId)
        val updatedDetail = baseDetail.copy(
            status = "已举报",
            reviewStatus = "复核通过",
            reportedAt = reportTime,
            reportContent = baseDetail.reportContent ?: "【模拟举报文书】已根据当前检测结果生成举报文案并完成演示提交。",
        )
        _uiState.value = current.copy(
            cases = updatedCases,
            selectedCase = updatedDetail,
            busy = false,
            toast = "离线演示提交成功：举报文案和证据已归档",
            error = null,
        )
        persistSnapshot(updatedDetail)
    }

    fun updateCaseReview(caseId: String, correctedPlate: String, operatorNote: String, reviewStatus: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val api = demoApi()
                val detail = api.updateCase(
                    caseId,
                    MobileCaseUpdateRequest(
                        correctedPlateNumber = correctedPlate.ifBlank { null },
                        operatorNote = operatorNote,
                        reviewStatus = reviewStatus,
                    ),
                )
                val cases = api.getCases(_uiState.value.selectedStatusFilter)
                deviceAssistStore.clearDraft(caseId)
                _uiState.value = _uiState.value.copy(
                    cases = cases,
                    selectedCase = detail,
                    localDraft = null,
                    busy = false,
                    toast = "案件复核信息已更新",
                )
                persistSnapshot(detail)
            } catch (error: Exception) {
                if (_uiState.value.connection.usingCachedData) {
                    applyOfflineReviewUpdate(caseId, correctedPlate, operatorNote, reviewStatus)
                } else {
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = errorMessage(error, "案件更新失败"),
                    )
                }
            }
        }
    }

    fun reportCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            applyOfflineReport(caseId)
        }
    }

    fun applyDebugApiBaseUrl(raw: String) {
        val normalized = normalizeApiBaseUrl(raw)
        if (normalized == null) {
            _uiState.value = _uiState.value.copy(error = "请输入可访问的 API 地址，例如 http://192.168.1.8:8000/api/")
            return
        }

        apiBaseUrlStore.saveRuntimeOverride(normalized)
        _uiState.value = _uiState.value.copy(
            toast = "已切换调试地址：$normalized",
            error = null,
            connection = connectionWithSnapshot(lastError = null),
        )
        refresh()
    }

    fun clearDebugApiBaseUrlOverride() {
        apiBaseUrlStore.clearRuntimeOverride()
        _uiState.value = _uiState.value.copy(
            toast = "已恢复构建默认地址：${BuildConfig.API_BASE_URL}",
            error = null,
            connection = connectionWithSnapshot(lastError = null),
        )
        refresh()
    }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(context) as T
    }
}
