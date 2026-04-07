package com.yrd.emergencylanemobile

import android.net.Uri
import android.os.Bundle
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val demoApi: DemoApiService = retrofit.create(DemoApiService::class.java)

interface DemoApiService {
    @GET("overview")
    suspend fun getOverview(): MobileOverview

    @GET("runs")
    suspend fun getRuns(): List<MobileRunSummary>

    @GET("cases")
    suspend fun getCases(@Query("status") status: String? = null): List<MobileCaseSummary>

    @GET("cases/{caseId}")
    suspend fun getCase(@Path("caseId") caseId: String): MobileCaseDetail

    @PATCH("cases/{caseId}")
    suspend fun updateCase(@Path("caseId") caseId: String, @Body payload: MobileCaseUpdateRequest): MobileCaseDetail

    @POST("cases/{caseId}/report")
    suspend fun reportCase(@Path("caseId") caseId: String): MobileReportResponse
}

data class MobileOverview(
    val summary: MobileSummary,
    @SerializedName("latest_run") val latestRun: MobileRunSummary?,
    val runs: List<MobileRunSummary> = emptyList(),
    val source: MobileSource,
    val system: MobileSystem,
)

data class MobileSummary(
    @SerializedName("total_events") val totalEvents: Int,
    @SerializedName("total_cases") val totalCases: Int,
    @SerializedName("reported_cases") val reportedCases: Int,
    @SerializedName("pending_review_cases") val pendingReviewCases: Int,
    @SerializedName("avg_confidence") val avgConfidence: Double,
)

data class MobileSource(
    val name: String,
    val title: String,
    @SerializedName("preview_url") val previewUrl: String?,
    @SerializedName("duration_seconds") val durationSeconds: Double,
)

data class MobileSystem(
    @SerializedName("web_role") val webRole: String,
    @SerializedName("android_role") val androidRole: String,
    @SerializedName("reference_basis") val referenceBasis: String,
)

data class MobileRunSummary(
    val id: String,
    @SerializedName("source_name") val sourceName: String,
    val status: String,
    val message: String,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("finished_at") val finishedAt: String?,
    @SerializedName("progress_percent") val progressPercent: Int,
    @SerializedName("event_count") val eventCount: Int,
    @SerializedName("case_count") val caseCount: Int,
)

data class MobileEventSummary(
    val id: String,
    @SerializedName("plate_number") val plateNumber: String,
    val status: String,
    @SerializedName("first_seen") val firstSeen: String,
)

data class MobileEvidence(
    val label: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("captured_at") val capturedAt: String,
)

data class MobileTimelineItem(
    @SerializedName("timestamp_seconds") val timestampSeconds: Double,
    val description: String,
)

data class MobileRawAnalysis(
    val timeline: List<MobileTimelineItem> = emptyList(),
)

data class MobileCaseSummary(
    val id: String,
    @SerializedName("run_id") val runId: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String?,
    @SerializedName("review_status") val reviewStatus: String,
    @SerializedName("operator_note") val operatorNote: String,
    val status: String,
    val location: String,
    val confidence: Double,
    val summary: String,
    @SerializedName("event_count") val eventCount: Int,
    @SerializedName("evidence_count") val evidenceCount: Int,
    @SerializedName("clip_url") val clipUrl: String?,
)

data class MobileCaseDetail(
    val id: String,
    @SerializedName("run_id") val runId: String,
    @SerializedName("source_name") val sourceName: String,
    @SerializedName("plate_number") val plateNumber: String,
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String?,
    @SerializedName("review_status") val reviewStatus: String,
    @SerializedName("operator_note") val operatorNote: String,
    val status: String,
    val location: String,
    val confidence: Double,
    val summary: String,
    @SerializedName("clip_url") val clipUrl: String?,
    @SerializedName("report_content") val reportContent: String?,
    @SerializedName("report_file_url") val reportFileUrl: String?,
    @SerializedName("reported_at") val reportedAt: String?,
    val evidence: List<MobileEvidence>,
    val events: List<MobileEventSummary>,
    @SerializedName("raw_analysis") val rawAnalysis: MobileRawAnalysis,
)

data class MobileCaseUpdateRequest(
    @SerializedName("corrected_plate_number") val correctedPlateNumber: String? = null,
    @SerializedName("operator_note") val operatorNote: String? = null,
    @SerializedName("review_status") val reviewStatus: String? = null,
)

data class MobileReportResponse(
    val message: String,
)

data class MobileUiState(
    val overview: MobileOverview? = null,
    val runs: List<MobileRunSummary> = emptyList(),
    val cases: List<MobileCaseSummary> = emptyList(),
    val selectedCaseId: String? = null,
    val selectedCase: MobileCaseDetail? = null,
    val selectedStatusFilter: String? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val toast: String? = null,
    val error: String? = null,
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MobileUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val previous = _uiState.value
            _uiState.value = previous.copy(loading = true, error = null)
            try {
                val overview = demoApi.getOverview()
                val runs = demoApi.getRuns()
                val cases = demoApi.getCases(previous.selectedStatusFilter)
                val selectedCaseId = previous.selectedCaseId?.takeIf { id -> cases.any { it.id == id } } ?: cases.firstOrNull()?.id
                val selectedCase = selectedCaseId?.let { demoApi.getCase(it) }
                _uiState.value = MobileUiState(
                    overview = overview,
                    runs = runs,
                    cases = cases,
                    selectedCaseId = selectedCaseId,
                    selectedCase = selectedCase,
                    selectedStatusFilter = previous.selectedStatusFilter,
                    loading = false,
                    toast = previous.toast,
                )
            } catch (error: Exception) {
                _uiState.value = previous.copy(loading = false, error = error.message ?: "加载失败")
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
                val detail = demoApi.getCase(caseId)
                _uiState.value = _uiState.value.copy(selectedCase = detail, busy = false)
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = error.message ?: "案件详情加载失败")
            }
        }
    }

    fun updateCaseReview(caseId: String, correctedPlate: String, operatorNote: String, reviewStatus: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val detail = demoApi.updateCase(
                    caseId,
                    MobileCaseUpdateRequest(
                        correctedPlateNumber = correctedPlate.ifBlank { null },
                        operatorNote = operatorNote,
                        reviewStatus = reviewStatus,
                    ),
                )
                val cases = demoApi.getCases(_uiState.value.selectedStatusFilter)
                _uiState.value = _uiState.value.copy(
                    cases = cases,
                    selectedCase = detail,
                    busy = false,
                    toast = "案件复核信息已更新",
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = error.message ?: "案件更新失败")
            }
        }
    }

    fun reportCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val result = demoApi.reportCase(caseId)
                val detail = demoApi.getCase(caseId)
                val cases = demoApi.getCases(_uiState.value.selectedStatusFilter)
                _uiState.value = _uiState.value.copy(
                    cases = cases,
                    selectedCase = detail,
                    busy = false,
                    toast = result.message,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = error.message ?: "案件举报失败")
            }
        }
    }
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel() as T
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF08101F)) {
                    MobileApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp(viewModel: MainViewModel = viewModel(factory = MainViewModelFactory())) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("应急车道移动协同端") }) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08101F))
                .padding(innerPadding),
        ) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { HeaderSection(state = state, onRefresh = { viewModel.refresh() }) }
                    item { RunSummarySection(state.runs, state.overview?.latestRun) }
                    item { FilterSection(state.selectedStatusFilter, onSelect = { viewModel.selectStatusFilter(it) }) }
                    item { SystemRoleSection(state.overview?.system) }
                    item {
                        Text(
                            text = "案件列表",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(state.cases, key = { it.id }) { item ->
                        CaseRow(item = item, selected = item.id == state.selectedCaseId, onClick = { viewModel.selectCase(item.id) })
                    }
                    item {
                        CaseDetailSection(
                            detail = state.selectedCase,
                            busy = state.busy,
                            onSave = { caseId, correctedPlate, note, reviewStatus ->
                                viewModel.updateCaseReview(caseId, correctedPlate, note, reviewStatus)
                            },
                            onReport = { caseId -> viewModel.reportCase(caseId) },
                        )
                    }
                    item {
                        state.error?.let { InfoCard(title = "错误", body = it, accent = Color(0xFFFCA5A5)) }
                    }
                    item {
                        state.toast?.let { InfoCard(title = "状态", body = it) }
                    }
                }
            }

            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderSection(state: MobileUiState, onRefresh: () -> Unit) {
    val overview = state.overview ?: return
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF10203A))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "移动协同复核入口",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "以共享后端为数据源，展示 run 摘要、案件状态筛选、复核字段编辑与举报状态同步。",
                color = Color(0xFFD7E3FF),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("事件", overview.summary.totalEvents.toString())
                MetricChip("案件", overview.summary.totalCases.toString())
                MetricChip("待复核", overview.summary.pendingReviewCases.toString())
                MetricChip("已举报", overview.summary.reportedCases.toString())
            }
            Button(onClick = onRefresh, enabled = !state.busy) {
                Text(if (state.busy) "同步中..." else "刷新数据")
            }
            Text(text = "当前 source：${overview.source.title} / ${overview.source.name}", color = Color(0xFF93C5FD))
        }
    }
}

@Composable
fun RunSummarySection(runs: List<MobileRunSummary>, latestRun: MobileRunSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "最新 run",
            body = latestRun?.let {
                "${it.id}\n${it.sourceName} · ${it.status} · ${it.eventCount} 事件 / ${it.caseCount} 案件\n${it.message}"
            } ?: "暂无运行记录",
        )
        if (runs.isNotEmpty()) {
            runs.take(3).forEach { run ->
                InfoCard(
                    title = run.id,
                    body = "${run.sourceName} · ${run.status} · ${run.progressPercent}%\n${run.message}",
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(selectedStatus: String?, onSelect: (String?) -> Unit) {
    val options = listOf<String?>(null, "待复核", "待举报", "已举报")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "案件状态筛选",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { value ->
                val selected = selectedStatus == value
                TextButton(onClick = { onSelect(value) }) {
                    Text(
                        text = value ?: "全部",
                        color = if (selected) Color(0xFF38BDF8) else Color(0xFFD7E3FF),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
fun SystemRoleSection(system: MobileSystem?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(title = "Android 端定位", body = system?.androidRole ?: "移动协同查看 / 案件复核辅助 / 举报状态同步")
        InfoCard(title = "Web 端定位", body = system?.webRole ?: "总览 / run 历史 / 事件案件筛选 / 证据链展示")
        InfoCard(title = "边界说明", body = system?.referenceBasis ?: "仅参考 newnew，不并轨 CameraX/JNI 主链")
    }
}

@Composable
fun CaseRow(item: MobileCaseSummary, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFF153257) else Color(0xFF0F172A)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = item.id, color = Color(0xFF93C5FD), style = MaterialTheme.typography.labelMedium)
            Text(text = item.correctedPlateNumber ?: item.plateNumber, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = item.summary, color = Color(0xFFD7E3FF))
            Text(
                text = "${item.status} · ${item.reviewStatus} · ${item.sourceName}",
                color = Color(0xFFBFD0EA),
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.operatorNote.isNotBlank()) {
                Text(text = item.operatorNote, color = Color(0xFF93C5FD), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseDetailSection(
    detail: MobileCaseDetail?,
    busy: Boolean,
    onSave: (String, String, String, String) -> Unit,
    onReport: (String) -> Unit,
) {
    if (detail == null) {
        InfoCard(title = "案件详情", body = "先在上方选择一个案件。")
        return
    }

    var correctedPlate by remember(detail.id, detail.correctedPlateNumber) { mutableStateOf(detail.correctedPlateNumber.orEmpty()) }
    var operatorNote by remember(detail.id, detail.operatorNote) { mutableStateOf(detail.operatorNote) }
    var reviewStatus by remember(detail.id, detail.reviewStatus) { mutableStateOf(detail.reviewStatus) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "${detail.id} / ${detail.plateNumber}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = "run: ${detail.runId} · source: ${detail.sourceName}", color = Color(0xFF93C5FD))
                Text(text = detail.summary, color = Color(0xFFD7E3FF))
                Text(text = "状态 ${detail.status} · 复核 ${detail.reviewStatus}", color = Color(0xFFBFD0EA))
                detail.clipUrl?.let { clipUrl ->
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        factory = { context ->
                            VideoView(context).apply {
                                setVideoURI(Uri.parse(clipUrl))
                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    start()
                                }
                            }
                        },
                        update = { view ->
                            view.setVideoURI(Uri.parse(clipUrl))
                            view.setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = true
                                view.start()
                            }
                        },
                    )
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "人工复核", color = Color.White, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = correctedPlate,
                    onValueChange = { correctedPlate = it },
                    label = { Text("corrected plate") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = operatorNote,
                    onValueChange = { operatorNote = it },
                    label = { Text("operator note") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("待复核", "复核通过", "复核退回").forEach { option ->
                        TextButton(onClick = { reviewStatus = option }) {
                            Text(
                                text = option,
                                color = if (reviewStatus == option) Color(0xFF38BDF8) else Color(0xFFD7E3FF),
                                fontWeight = if (reviewStatus == option) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onSave(detail.id, correctedPlate, operatorNote, reviewStatus) }, enabled = !busy) {
                        Text(if (busy) "保存中..." else "保存复核信息")
                    }
                    Button(onClick = { onReport(detail.id) }, enabled = !busy && detail.status == "待举报") {
                        Text(if (detail.status == "待举报") "提交案件模拟举报" else "待复核后再举报")
                    }
                }
                Text(text = detail.reportContent ?: "暂无文书", color = Color(0xFFD7E3FF))
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "关键证据", color = Color.White, fontWeight = FontWeight.Bold)
                if (detail.evidence.isEmpty()) {
                    Text(text = "暂无证据图", color = Color(0xFFBFD0EA))
                } else {
                    detail.evidence.take(3).forEach { item ->
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                        Text(text = "${item.label} · ${item.capturedAt}", color = Color(0xFFBFD0EA), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (detail.events.isNotEmpty()) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "关联事件", color = Color.White, fontWeight = FontWeight.Bold)
                    detail.events.forEach { event ->
                        Text(text = "${event.id} · ${event.status} · ${event.firstSeen}", color = Color(0xFFD7E3FF))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Card(shape = RoundedCornerShape(999.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F))) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF38BDF8)),
            )
            Text(text = "$label $value", color = Color.White)
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, accent: Color = Color(0xFF93C5FD)) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, color = accent, fontWeight = FontWeight.Bold)
            Text(text = body, color = Color(0xFFD7E3FF))
        }
    }
}
