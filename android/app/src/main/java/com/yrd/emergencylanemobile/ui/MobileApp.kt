package com.yrd.emergencylanemobile.ui

import android.content.Intent
import android.net.Uri
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.emergencylaneguard.LocalDetectionActivity
import com.yrd.emergencylanemobile.R
import com.yrd.emergencylanemobile.model.MobileUiState
import com.yrd.emergencylanemobile.ui.components.InfoCard
import com.yrd.emergencylanemobile.ui.components.SummaryCard
import com.yrd.emergencylanemobile.ui.screens.CaseDetailSection
import com.yrd.emergencylanemobile.ui.screens.CaseRow
import com.yrd.emergencylanemobile.ui.screens.HeaderSection
import com.yrd.emergencylanemobile.ui.screens.RunSummarySection
import com.yrd.emergencylanemobile.ui.screens.SystemRoleSection
import com.yrd.emergencylanemobile.viewmodel.MainViewModel
import com.yrd.emergencylanemobile.viewmodel.MainViewModelFactory

private val PageBackground = Color(0xFFF4F0FB)
private val SurfaceCard = Color(0xFFF8F4FF)
private val SurfaceStrong = Color(0xFFF1EAFE)
private val InkPrimary = Color(0xFF2F1F46)
private val InkSecondary = Color(0xFF7A6E92)
private val AccentPurple = Color(0xFF9F86FF)
private val AccentBlue = Color(0xFF7C9DFF)
private val AccentRose = Color(0xFFE68BAF)

private enum class MobileTab(val label: String, val marker: String) {
    OVERVIEW("概览", "总"),
    CASES("案件", "案"),
    LOCAL("本地检测", "端"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileApp() {
    val context = LocalContext.current.applicationContext
    val factory = remember(context) { MainViewModelFactory(context) }
    val viewModel: MainViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsState()
    val currentTab = rememberSaveable { mutableStateOf(MobileTab.OVERVIEW) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("应急车道移动端", color = InkPrimary, fontWeight = FontWeight.Bold)
                        Text("案件闭环 · 本地检测 · 真机演示", color = InkSecondary, style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBackground),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceCard) {
                MobileTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab.value == tab,
                        onClick = { currentTab.value = tab },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (currentTab.value == tab) AccentPurple.copy(alpha = 0.18f) else Color.Transparent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = tab.marker,
                                    color = if (currentTab.value == tab) AccentPurple else InkSecondary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        },
                        label = { Text(tab.label, color = if (currentTab.value == tab) InkPrimary else InkSecondary) },
                    )
                }
            }
        },
        containerColor = PageBackground,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBackground)
                .padding(innerPadding),
        ) {
            val showFullScreenLoading = state.loading && state.overview == null && state.cases.isEmpty()
            if (showFullScreenLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AccentPurple)
            } else {
                when (currentTab.value) {
                    MobileTab.OVERVIEW -> OverviewTab(
                        state = state,
                        onOpenLocalMode = {
                            context.startActivity(Intent(context, LocalDetectionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                    )
                    MobileTab.CASES -> CasesTab(
                        state = state,
                        onSelectCase = { viewModel.selectCase(it) },
                        onSave = { caseId, correctedPlate, note, reviewStatus ->
                            viewModel.updateCaseReview(caseId, correctedPlate, note, reviewStatus)
                        },
                        onSaveLocalDraft = { caseId, localPlate, sceneNote, reviewStatus ->
                            viewModel.saveLocalDraft(caseId, localPlate, sceneNote, reviewStatus)
                        },
                        onClearLocalDraft = { caseId -> viewModel.clearLocalDraft(caseId) },
                        onReport = { caseId -> viewModel.reportCase(caseId) },
                    )
                    MobileTab.LOCAL -> LocalModeTab(
                        state = state,
                        onOpenLocalMode = {
                            context.startActivity(Intent(context, LocalDetectionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                    )
                }
            }

            if (state.busy) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(20.dp), color = AccentPurple)
            }
        }
    }
}

@Composable
private fun OverviewTab(
    state: MobileUiState,
    onOpenLocalMode: () -> Unit,
) {
    val overview = state.overview
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroCard(
                title = "项目概览",
                body = "移动端为端侧独立检测演示：CameraX 实时采集 + YOLOv8n 目标检测 + HyperLPR3 车牌识别 + Room 本地归档，与 Web 端互不依赖。",
                buttonLabel = "打开本地检测页面",
                onClick = onOpenLocalMode,
            )
        }
        item {
            SummaryRow(
                items = listOf(
                    Triple("图片量", (state.cases.sumOf { it.evidenceCount }).toString(), "证据图汇总"),
                    Triple("违章车辆", (overview?.summary?.totalCases ?: state.cases.size).toString(), "案件沉淀"),
                    Triple("已举报", (overview?.summary?.reportedCases ?: 0).toString(), "模拟流程"),
                ),
            )
        }
        item { HeaderSection(state = state) }
        item { RunSummarySection(state.runs, state.overview?.latestRun) }
        item { SystemRoleSection(state.overview?.system) }
        state.toast?.let { message ->
            item { InfoCard(title = "状态", body = message) }
        }
    }
}

@Composable
private fun CasesTab(
    state: MobileUiState,
    onSelectCase: (String) -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onSaveLocalDraft: (String, String, String, String) -> Unit,
    onClearLocalDraft: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val totalEvidence = state.cases.sumOf { it.evidenceCount }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionLead(
                title = "证据链闭环",
                body = "先看本地内置原视频、clip 和证据图，再向下进入案件筛选、复核和模拟举报，避免展示依赖不稳定的远程媒体。",
            )
        }
        item {
            SummaryRow(
                items = listOf(
                    Triple("案件量", state.cases.size.toString(), "当前列表"),
                    Triple("证据图", totalEvidence.toString(), "离线讲解"),
                    Triple("待复核", state.cases.count { it.reviewStatus == "待复核" }.toString(), "人工处理"),
                ),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LoopingVideoCard(
                    title = "demo 原视频",
                    caption = "APK 内置原始演示视频",
                    resId = R.raw.case_demo_source,
                    modifier = Modifier.weight(1f),
                )
                LoopingVideoCard(
                    title = "案件 clip",
                    caption = "APK 内置案件片段",
                    resId = R.raw.case_demo_clip,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            EvidenceGalleryCard(
                title = "案件证据图",
                body = "本地 asset 直接展示 3 张证据图，用于真机彩排时稳定讲解“识别-归档-举报”链路。",
                imagePaths = listOf(
                    "file:///android_asset/case_demo_evidence/ev-1.jpg",
                    "file:///android_asset/case_demo_evidence/ev-2.jpg",
                    "file:///android_asset/case_demo_evidence/ev-3.jpg",
                ),
                badge = "离线素材",
            )
        }
        item {
            ReportAssistCard(
                state = state,
                onSelectCase = onSelectCase,
                onReport = onReport,
            )
        }
        item {
            Text(
                text = "案件列表",
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        if (state.cases.isEmpty()) {
            item { InfoCard(title = "案件列表", body = "当前没有可展示的案件数据。") }
        } else {
            items(state.cases, key = { it.id }) { item ->
                CaseRow(
                    item = item,
                    selected = item.id == state.selectedCaseId,
                    onClick = { onSelectCase(item.id) },
                )
            }
        }
        item {
            CaseDetailSection(
                detail = state.selectedCase,
                localDraft = state.localDraft,
                busy = state.busy,
                onSave = onSave,
                onSaveLocalDraft = onSaveLocalDraft,
                onClearLocalDraft = onClearLocalDraft,
                onReport = onReport,
            )
        }
        state.toast?.let { message ->
            item { InfoCard(title = "状态", body = message) }
        }
    }
}

@Composable
private fun LocalModeTab(
    state: MobileUiState,
    onOpenLocalMode: () -> Unit,
) {
    val localImageCount = 4
    val pendingCount = state.cases.count { it.status != "已举报" }
    val reportedCount = state.overview?.summary?.reportedCases ?: 0
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionLead(
                title = "本地检测展示",
                body = "这里收口成更像端侧产品页：先看摘要与示例图库，再进入 Pending / Home / Cases 进行真实端侧演示。",
            )
        }
        item {
            SummaryRow(
                items = listOf(
                    Triple("图片量", localImageCount.toString(), "本地示例"),
                    Triple("待处理", pendingCount.toString(), "端侧候选"),
                    Triple("已举报", reportedCount.toString(), "云端同步"),
                ),
            )
        }
        item {
            HeroCard(
                title = "打开本地检测页面",
                body = "进入 CameraX 实时检测、本地待处理素材与本地案例集页面，继续展示端侧闭环。",
                buttonLabel = "进入 Pending / Home / Cases",
                onClick = onOpenLocalMode,
            )
        }
        item {
            LoopingVideoCard(
                title = "本地检测演示视频",
                caption = "origin/newnew 演示片段已打包进 APK，本页直接离线播放。",
                resId = R.raw.newnew_demo_clip,
            )
        }
        item {
            EvidenceGalleryCard(
                title = "疑似占用应急车道",
                body = "高风险样例以图库卡片方式展示，适合讲解端侧如何筛出候选素材。",
                imagePaths = listOf(
                    "file:///android_asset/local_demo_samples/yes-1.png",
                    "file:///android_asset/local_demo_samples/yes-2.png",
                ),
                badge = "高优先级",
            )
        }
        item {
            EvidenceGalleryCard(
                title = "正常通行样例",
                body = "正常样例用于说明本地检测页不是只看异常，也能完成基础分类展示。",
                imagePaths = listOf(
                    "file:///android_asset/local_demo_samples/no-1.png",
                    "file:///android_asset/local_demo_samples/no-2.png",
                ),
                badge = "对照样例",
            )
        }
        item {
            InfoCard(
                title = "本地页签说明",
                body = "Pending 看待处理素材，Home 做相机实时检测，Cases 看端侧沉淀案例。功能保留，但入口表达更偏产品展示。",
            )
        }
        state.localDraft?.let { draft ->
            item {
                InfoCard(
                    title = "最近端侧草稿",
                    body = "保存时间 ${draft.savedAt ?: "未知"} · plate ${draft.localPlateCandidate.ifBlank { "--" }} · 状态 ${draft.reviewStatus}",
                )
            }
        }
        item {
            InfoCard(
                title = "协同关系",
                body = "端侧负责 CameraX 采集 + YOLOv8n 目标检测 + HyperLPR3 车牌识别 + Room 本地归档；Web 端负责 Mock 案件复核与证据链展示。两端独立运行，共用同一产品故事线。",
            )
        }
        state.error?.let { message ->
            item { InfoCard(title = "错误", body = message, accent = AccentRose) }
        }
        state.toast?.let { message ->
            item { InfoCard(title = "状态", body = message) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportAssistCard(
    state: MobileUiState,
    onSelectCase: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    val detail = state.selectedCase
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(26.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "举报文案与证据提交", color = InkPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = "这块直接在 APK 内执行演示提交：选中案件、生成文案、点击后立即完成模拟举报，不依赖后端。", color = InkSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.cases.forEach { item ->
                    Button(
                        onClick = { onSelectCase(item.id) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.id == state.selectedCaseId) AccentPurple.copy(alpha = 0.18f) else SurfaceStrong,
                            contentColor = if (item.id == state.selectedCaseId) AccentPurple else InkSecondary,
                        ),
                    ) {
                        Text(item.correctedPlateNumber ?: item.plateNumber)
                    }
                }
            }
            if (detail == null) {
                Text(text = "请选择一个案件后再生成举报文案。", color = InkSecondary)
            } else {
                InfoCard(
                    title = "当前提交内容",
                    body = buildString {
                        appendLine("案件：${detail.id} / ${detail.plateNumber}")
                        appendLine("状态：${detail.status} · 复核：${detail.reviewStatus}")
                        appendLine("证据：${detail.evidence.size} 张图片 + 顶部本地 clip")
                        append(detail.reportContent ?: "将根据当前检测内容生成一份模拟举报文案。")
                    },
                )
                Button(
                    onClick = { onReport(detail.id) },
                    enabled = !state.busy,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.White),
                ) {
                    Text(if (detail.status == "已举报") "再次模拟提交" else "APK 内直接模拟提交")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(items: List<Triple<String, String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, (title, value, hint) ->
            SummaryCard(
                title = title,
                value = value,
                hint = hint,
                accent = when (index) {
                    0 -> AccentPurple
                    1 -> AccentBlue
                    else -> AccentRose
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    body: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(28.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text = title, color = InkPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(text = body, color = InkSecondary)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple, contentColor = Color.White),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun SectionLead(title: String, body: String) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(26.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, color = InkPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = body, color = InkSecondary)
        }
    }
}

@Composable
private fun LoopingVideoCard(
    title: String,
    caption: String,
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoUri = remember(resId, context.packageName) {
        Uri.parse("android.resource://${context.packageName}/$resId")
    }
    var errorMessage by remember(resId) { mutableStateOf<String?>(null) }
    val videoViewState = remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(resId) {
        onDispose {
            videoViewState.value?.stopPlayback()
            videoViewState.value = null
        }
    }

    androidx.compose.material3.Card(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title, color = InkPrimary, fontWeight = FontWeight.Bold)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(22.dp)),
                factory = { viewContext ->
                    VideoView(viewContext).apply {
                        videoViewState.value = this
                        setVideoURI(videoUri)
                        setOnPreparedListener { mediaPlayer ->
                            errorMessage = null
                            mediaPlayer.isLooping = true
                            start()
                        }
                        setOnErrorListener { _, what, extra ->
                            errorMessage = "播放失败（$what/$extra），请重试真机安装后的本地素材。"
                            true
                        }
                    }
                },
            )
            Text(text = caption, color = InkSecondary, style = MaterialTheme.typography.bodySmall)
            errorMessage?.let {
                Text(text = it, color = AccentRose, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EvidenceGalleryCard(
    title: String,
    body: String,
    imagePaths: List<String>,
    badge: String,
) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(26.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = SurfaceCard),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, color = InkPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text = body, color = InkSecondary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                imagePaths.forEachIndexed { index, imagePath ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceStrong),
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AsyncImage(
                                model = imagePath,
                                contentDescription = title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = badge,
                                    color = AccentPurple,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(AccentPurple.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                                Text(text = "样例 ${index + 1}", color = InkSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
