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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

private val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val demoApi: DemoApiService = retrofit.create(DemoApiService::class.java)

interface DemoApiService {
    @GET("overview")
    suspend fun getOverview(): MobileOverview

    @GET("cases")
    suspend fun getCases(): List<MobileCaseSummary>

    @GET("cases/{caseId}")
    suspend fun getCase(@Path("caseId") caseId: String): MobileCaseDetail

    @POST("cases/{caseId}/report")
    suspend fun reportCase(@Path("caseId") caseId: String): MobileReportResponse

    @POST("tasks/analyze-demo")
    suspend fun analyzeDemo(): MobileAnalyzeResponse
}

data class MobileOverview(
    val summary: MobileSummary,
    val source: MobileSource,
    val system: MobileSystem,
)

data class MobileSummary(
    @SerializedName("total_events") val totalEvents: Int,
    @SerializedName("total_cases") val totalCases: Int,
    @SerializedName("reported_cases") val reportedCases: Int,
    @SerializedName("avg_confidence") val avgConfidence: Double,
)

data class MobileSource(
    @SerializedName("video_url") val videoUrl: String,
    @SerializedName("preview_url") val previewUrl: String,
    @SerializedName("case_clip_seconds") val caseClipSeconds: Double,
)

data class MobileSystem(
    @SerializedName("web_role") val webRole: String,
    @SerializedName("android_role") val androidRole: String,
    @SerializedName("reference_basis") val referenceBasis: String,
)

data class MobileEventSummary(
    val id: String,
    @SerializedName("case_id") val caseId: String?,
    @SerializedName("plate_number") val plateNumber: String,
    val status: String,
    val location: String,
    @SerializedName("first_seen") val firstSeen: String,
    @SerializedName("last_seen") val lastSeen: String,
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
    @SerializedName("plate_number") val plateNumber: String,
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
    @SerializedName("plate_number") val plateNumber: String,
    val status: String,
    val location: String,
    val confidence: Double,
    val summary: String,
    @SerializedName("clip_url") val clipUrl: String?,
    @SerializedName("report_content") val reportContent: String?,
    @SerializedName("report_file_url") val reportFileUrl: String?,
    val evidence: List<MobileEvidence>,
    val events: List<MobileEventSummary>,
    @SerializedName("raw_analysis") val rawAnalysis: MobileRawAnalysis,
)

data class MobileReportResponse(
    val message: String,
)

data class MobileAnalyzeResponse(
    val message: String,
)

data class MobileUiState(
    val overview: MobileOverview? = null,
    val cases: List<MobileCaseSummary> = emptyList(),
    val selectedCaseId: String? = null,
    val selectedCase: MobileCaseDetail? = null,
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
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            try {
                val overview = demoApi.getOverview()
                val cases = demoApi.getCases()
                val selectedCaseId = _uiState.value.selectedCaseId?.takeIf { id -> cases.any { it.id == id } } ?: cases.firstOrNull()?.id
                val selectedCase = selectedCaseId?.let { demoApi.getCase(it) }
                _uiState.value = MobileUiState(
                    overview = overview,
                    cases = cases,
                    selectedCaseId = selectedCaseId,
                    selectedCase = selectedCase,
                    loading = false,
                    toast = _uiState.value.toast,
                )
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(loading = false, error = error.message ?: "加载失败")
            }
        }
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

    fun analyzeDemo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val result = demoApi.analyzeDemo()
                _uiState.value = _uiState.value.copy(toast = result.message, busy = false)
                refresh()
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = error.message ?: "分析失败")
            }
        }
    }

    fun reportCase(caseId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            try {
                val result = demoApi.reportCase(caseId)
                _uiState.value = _uiState.value.copy(toast = result.message, busy = false)
                refresh()
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(busy = false, error = error.message ?: "案件举报失败")
            }
        }
    }
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("应急车道移动协同端") })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF08101F))
                .padding(innerPadding),
        ) {
            when {
                state.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            HeaderSection(
                                state = state,
                                onAnalyze = { viewModel.analyzeDemo() },
                            )
                        }
                        item {
                            SystemRoleSection(state.overview?.system)
                        }
                        item {
                            Text(
                                text = "案件列表",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(state.cases, key = { it.id }) { item ->
                            CaseRow(
                                item = item,
                                selected = item.id == state.selectedCaseId,
                                onClick = { viewModel.selectCase(item.id) },
                            )
                        }
                        item {
                            CaseDetailSection(
                                detail = state.selectedCase,
                                busy = state.busy,
                                onReport = { caseId -> viewModel.reportCase(caseId) },
                            )
                        }
                        item {
                            state.error?.let {
                                InfoCard(title = "错误", body = it, accent = Color(0xFFFCA5A5))
                            }
                        }
                    }
                }
            }

            state.busy.takeIf { it }?.let {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp))
            }
        }
    }

    LaunchedEffect(state.toast) {
        // keep latest toast value visible in the status card only
    }
}

@Composable
fun HeaderSection(state: MobileUiState, onAnalyze: () -> Unit) {
    val overview = state.overview ?: return
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10203A)),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "双端项目移动协同入口",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "与 Web 大屏共享同一后端案件库，展示移动端的案件查看、片段播放与举报辅助。",
                color = Color(0xFFD7E3FF),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("事件", overview.summary.totalEvents.toString())
                MetricChip("案件", overview.summary.totalCases.toString())
                MetricChip("已举报", overview.summary.reportedCases.toString())
            }
            Button(onClick = onAnalyze, enabled = !state.busy) {
                Text(if (state.busy) "处理中..." else "重新分析演示视频")
            }
            state.toast?.let { Text(text = it, color = Color(0xFF93C5FD)) }
        }
    }
}

@Composable
fun SystemRoleSection(system: MobileSystem?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(title = "Android 端定位", body = system?.androidRole ?: "移动协同查看 / 案件详情 / 举报辅助")
        InfoCard(title = "网页端定位", body = system?.webRole ?: "政府大屏总览 / 案件库 / 证据链展示")
        InfoCard(title = "参考依据", body = system?.referenceBasis ?: "参考 Android 原型文档")
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
            Text(text = item.plateNumber, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = item.summary, color = Color(0xFFD7E3FF))
            Text(
                text = "${item.eventCount} 个事件 / ${item.evidenceCount} 张证据 / ${item.status}",
                color = Color(0xFFBFD0EA),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun CaseDetailSection(detail: MobileCaseDetail?, busy: Boolean, onReport: (String) -> Unit) {
    if (detail == null) {
        InfoCard(title = "案件详情", body = "先在上方选择一个案件。")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "${detail.id} / ${detail.plateNumber}", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = detail.summary, color = Color(0xFFD7E3FF))
                Text(text = "${detail.location} · ${(detail.confidence * 100).toInt()}%", color = Color(0xFFBFD0EA))
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

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "正式举报文书", color = Color.White, fontWeight = FontWeight.Bold)
                Text(text = detail.reportContent ?: "暂无文书", color = Color(0xFFD7E3FF))
                Button(
                    onClick = { onReport(detail.id) },
                    enabled = !busy && detail.status != "已举报",
                ) {
                    Text(if (detail.status == "已举报") "案件已举报" else "提交案件模拟举报")
                }
            }
        }

        if (detail.events.isNotEmpty()) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF111B2F))) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "关联事件", color = Color.White, fontWeight = FontWeight.Bold)
                    detail.events.forEach { event ->
                        Text(
                            text = "${event.id} · ${event.plateNumber} · ${event.firstSeen}",
                            color = Color(0xFFD7E3FF),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A5F)),
    ) {
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
