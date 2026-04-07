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
                        toast = "网络失败，已切换本地缓存快照",
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
                    _uiState.value = previous.copy(
                        loading = false,
                        refreshing = false,
                        error = message,
                        connection = connectionWithSnapshot(lastError = message, usingCachedData = false),
                    )
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
                        toast = "案件详情加载失败，已显示本地缓存副本",
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
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    error = errorMessage(error, "案件更新失败"),
                )
            }
        }
    }

    fun reportCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val api = demoApi()
                val result = api.reportCase(caseId)
                val detail = api.getCase(caseId)
                val cases = api.getCases(_uiState.value.selectedStatusFilter)
                _uiState.value = _uiState.value.copy(
                    cases = cases,
                    selectedCase = detail,
                    busy = false,
                    toast = result.message,
                )
                persistSnapshot(detail)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    error = errorMessage(error, "案件举报失败"),
                )
            }
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
