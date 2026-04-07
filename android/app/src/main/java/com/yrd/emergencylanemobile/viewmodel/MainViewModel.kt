package com.yrd.emergencylanemobile.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yrd.emergencylanemobile.ApiBaseUrlStore
import com.yrd.emergencylanemobile.BuildConfig
import com.yrd.emergencylanemobile.model.MobileConnectionState
import com.yrd.emergencylanemobile.model.MobileCaseUpdateRequest
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(MobileUiState(connection = initialConnectionState()))
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
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

    private fun refreshedConnectionState(
        lastOverviewSummary: String? = _uiState.value.connection.lastOverviewSummary,
        lastSuccessAt: String? = _uiState.value.connection.lastSuccessAt,
        lastError: String? = _uiState.value.connection.lastError,
    ): MobileConnectionState {
        val resolved = resolveApiBaseUrl(apiBaseUrlStore)
        return MobileConnectionState(
            activeBaseUrl = resolved.activeBaseUrl,
            activeSource = resolved.activeSource,
            buildDefaultBaseUrl = resolved.buildDefaultBaseUrl,
            buildDefaultSource = resolved.buildDefaultSource,
            emulatorFallbackBaseUrl = resolved.emulatorFallbackBaseUrl,
            lanHintBaseUrl = resolved.lanHintBaseUrl,
            hasRuntimeOverride = resolved.hasRuntimeOverride,
            lastOverviewSummary = lastOverviewSummary,
            lastSuccessAt = lastSuccessAt,
            lastError = lastError,
        )
    }

    private fun demoApi(baseUrl: String = _uiState.value.connection.activeBaseUrl): DemoApiService =
        createDemoApiService(baseUrl)

    fun refresh() {
        viewModelScope.launch {
            val previous = _uiState.value
            val connection = refreshedConnectionState(lastError = null)
            _uiState.value = previous.copy(
                loading = previous.overview == null,
                refreshing = true,
                error = null,
                connection = connection,
            )
            try {
                val api = demoApi(connection.activeBaseUrl)
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
                    toast = previous.toast,
                    connection = refreshedConnectionState(
                        lastOverviewSummary = overviewSummary,
                        lastSuccessAt = nowTimeLabel(),
                        lastError = null,
                    ),
                )
            } catch (error: Exception) {
                val message = errorMessage(error, "加载失败")
                _uiState.value = previous.copy(
                    loading = false,
                    refreshing = false,
                    error = message,
                    connection = refreshedConnectionState(lastError = message),
                )
            }
        }
    }

    fun selectStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        refresh()
    }

    fun selectCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, selectedCaseId = caseId, error = null)
            try {
                val detail = demoApi().getCase(caseId)
                _uiState.value = _uiState.value.copy(selectedCase = detail, busy = false)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    error = errorMessage(error, "案件详情加载失败"),
                )
            }
        }
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
                _uiState.value = _uiState.value.copy(
                    cases = cases,
                    selectedCase = detail,
                    busy = false,
                    toast = "案件复核信息已更新",
                )
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
            connection = refreshedConnectionState(lastError = null),
        )
        refresh()
    }

    fun clearDebugApiBaseUrlOverride() {
        apiBaseUrlStore.clearRuntimeOverride()
        _uiState.value = _uiState.value.copy(
            toast = "已恢复构建默认地址：${BuildConfig.API_BASE_URL}",
            error = null,
            connection = refreshedConnectionState(lastError = null),
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
