package com.yrd.emergencylanemobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yrd.emergencylanemobile.model.MobileRunSummary
import com.yrd.emergencylanemobile.model.MobileSystem
import com.yrd.emergencylanemobile.model.MobileUiState
import com.yrd.emergencylanemobile.ui.components.InfoCard
import com.yrd.emergencylanemobile.ui.components.MetricChip
import java.util.Locale

private val InkPrimary = Color(0xFF2F1F46)
private val InkSecondary = Color(0xFF7A6E92)
private val AccentPurple = Color(0xFF9F86FF)
private val SoftSurface = Color(0xFFF8F4FF)
private val SoftSurfaceStrong = Color(0xFFF1EAFE)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderSection(state: MobileUiState) {
    val overview = state.overview ?: return
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "移动协同概览",
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "当前 Android 端为独立演示模式：CameraX 实时预览 + YOLOv8n 目标检测 + HyperLPR3 车牌识别 + Room 本地归档，与 Web 端互不依赖。",
                color = InkSecondary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("事件", overview.summary.totalEvents.toString())
                MetricChip("案件", overview.summary.totalCases.toString())
                MetricChip("待复核", overview.summary.pendingReviewCases.toString())
                MetricChip("已举报", overview.summary.reportedCases.toString())
                MetricChip("均值置信度", String.format(Locale.US, "%.2f", overview.summary.avgConfidence))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = overview.source.title,
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(SoftSurfaceStrong)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Text(text = "时长 ${overview.source.durationSeconds}s", color = InkSecondary)
            }
        }
    }
}

@Composable
fun RunSummarySection(runs: List<MobileRunSummary>, latestRun: MobileRunSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "最新分析",
            body = latestRun?.let {
                buildString {
                    appendLine(it.id)
                    appendLine("${it.sourceName} · ${it.status} · ${it.eventCount} 事件 / ${it.caseCount} 案件")
                    append(it.message)
                }
            } ?: "暂无运行记录",
        )
        if (runs.isNotEmpty()) {
            runs.take(3).forEach { run ->
                InfoCard(
                    title = run.id,
                    body = buildString {
                        appendLine("${run.sourceName} · ${run.status} · ${run.progressPercent}%")
                        append(run.message)
                    },
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
            color = InkPrimary,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { value ->
                val selected = selectedStatus == value
                Card(
                    shape = RoundedCornerShape(999.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) AccentPurple.copy(alpha = 0.18f) else SoftSurfaceStrong),
                ) {
                    TextButton(onClick = { onSelect(value) }) {
                        Text(
                            text = value ?: "全部",
                            color = if (selected) AccentPurple else InkSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SystemRoleSection(system: MobileSystem?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(title = "Android 端定位", body = system?.androidRole ?: "端侧独立检测演示（CameraX / YOLOv8n+ncnn / HyperLPR3 / Room），不依赖 Web 后端")
        InfoCard(title = "Web 端定位", body = system?.webRole ?: "总览 / run 历史 / 事件案件筛选 / 证据链展示（纯 Mock 原型）")
        InfoCard(title = "边界说明", body = system?.referenceBasis ?: "Web 端与 Android 端各自独立运行，互不依赖，共用同一产品故事线")
    }
}
