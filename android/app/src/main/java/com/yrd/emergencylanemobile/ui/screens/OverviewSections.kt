package com.yrd.emergencylanemobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yrd.emergencylanemobile.model.MobileRunSummary
import com.yrd.emergencylanemobile.model.MobileSystem
import com.yrd.emergencylanemobile.model.MobileUiState
import com.yrd.emergencylanemobile.ui.components.InfoCard
import com.yrd.emergencylanemobile.ui.components.MetricChip
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderSection(state: MobileUiState) {
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
                text = "当前 Android 已形成双轨：云侧继续消费 FastAPI 闭环做案件复核与举报同步；端侧新增本地检测模式，用于展示 CameraX / JNI / Room 能力。",
                color = Color(0xFFD7E3FF),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("事件", overview.summary.totalEvents.toString())
                MetricChip("案件", overview.summary.totalCases.toString())
                MetricChip("待复核", overview.summary.pendingReviewCases.toString())
                MetricChip("已举报", overview.summary.reportedCases.toString())
                MetricChip("均值置信度", String.format(Locale.US, "%.2f", overview.summary.avgConfidence))
            }
            Text(text = "当前 source：${overview.source.title} / ${overview.source.name}", color = Color(0xFF93C5FD))
            Text(text = "演示时长：${overview.source.durationSeconds}s", color = Color(0xFFBFD0EA))
        }
    }
}

@Composable
fun RunSummarySection(runs: List<MobileRunSummary>, latestRun: MobileRunSummary?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard(
            title = "最新 run",
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
        InfoCard(title = "Android 端定位", body = system?.androidRole ?: "云侧协同查看 / 端侧本地检测演示 / 案件复核辅助 / 举报状态同步")
        InfoCard(title = "Web 端定位", body = system?.webRole ?: "总览 / run 历史 / 事件案件筛选 / 证据链展示")
        InfoCard(title = "边界说明", body = system?.referenceBasis ?: "Android 同时支持云侧协同与端侧 CameraX/JNI/Room 本地检测演示")
    }
}
