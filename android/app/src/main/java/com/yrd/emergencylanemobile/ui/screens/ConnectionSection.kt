package com.yrd.emergencylanemobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yrd.emergencylanemobile.BuildConfig
import com.yrd.emergencylanemobile.model.MobileUiState
import com.yrd.emergencylanemobile.ui.components.InfoCard
import com.yrd.emergencylanemobile.ui.components.MetricChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionSection(
    state: MobileUiState,
    onRefresh: () -> Unit,
    onApplyBaseUrl: (String) -> Unit,
    onClearOverride: () -> Unit,
) {
    val connection = state.connection
    var draftBaseUrl by remember(connection.activeBaseUrl) { mutableStateOf(connection.activeBaseUrl) }

    Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF10203A))) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "连接诊断",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Android 端侧独立演示模式 — 当前展示内置离线数据。如需联调 Web 后端，可在此配置 API 地址。",
                color = Color(0xFFD7E3FF),
            )
            InfoCard(title = "当前 API", body = connection.activeBaseUrl)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("来源", connection.activeSource)
                MetricChip("默认", connection.buildDefaultSource)
                MetricChip("最近同步", connection.lastSuccessAt ?: "暂无")
                MetricChip("内容源", if (connection.usingCachedData) "本地缓存" else "实时后端")
                if (connection.hasRuntimeOverride) {
                    MetricChip("调试覆盖", "已启用")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("缓存案件", connection.cacheCaseCount.toString())
                MetricChip("缓存 run", connection.cacheRunCount.toString())
                MetricChip("缓存状态", if (connection.usingCachedData) "兜底中" else "在线")
                MetricChip("草稿", if (state.localDraft == null) "暂无" else "已缓存")
            }
            connection.lastOverviewSummary?.let { InfoCard(title = "总览回执", body = it) }
            connection.lastError?.let { InfoCard(title = "最近错误", body = it, accent = Color(0xFFFCA5A5)) }
            if (connection.cacheAvailable) {
                InfoCard(
                    title = "本地缓存层",
                    body = buildString {
                        append("最近缓存 ${connection.cacheStatus ?: "未知"}")
                        append(" · ${connection.cacheRunCount} 个 run / ${connection.cacheCaseCount} 个案件")
                        if (connection.usingCachedData) append(" · 当前已自动回退到本地缓存快照")
                    },
                )
            }
            state.localDraft?.let { draft ->
                InfoCard(
                    title = "端侧草稿",
                    body = "最近保存 ${draft.savedAt ?: "未知"} · 输入方式 ${draft.inputMode} · plate ${draft.localPlateCandidate.ifBlank { "--" }}",
                )
            }
            InfoCard(
                title = "联调提示",
                body = "本页主要服务于彩排调试场景。日常演示使用 APK 内置离线数据即可，无需连接后端。若需联调，后端使用 0.0.0.0 启动且真机在同一网段。",
            )
            if (BuildConfig.DEBUG) {
                OutlinedTextField(
                    value = draftBaseUrl,
                    onValueChange = { draftBaseUrl = it },
                    label = { Text("调试 API 地址") },
                    supportingText = {
                        Text(text = "示例 ${connection.lanHintBaseUrl} · 模拟器回退 ${connection.emulatorFallbackBaseUrl}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onRefresh, enabled = !state.refreshing && !state.busy) {
                        Text(if (state.refreshing) "检测中..." else "重新检测")
                    }
                    Button(onClick = { onApplyBaseUrl(draftBaseUrl) }, enabled = !state.refreshing && !state.busy) {
                        Text("保存并切换")
                    }
                    TextButton(onClick = { draftBaseUrl = connection.lanHintBaseUrl }) {
                        Text("填入局域网模板")
                    }
                    TextButton(onClick = {
                        draftBaseUrl = connection.buildDefaultBaseUrl
                        onClearOverride()
                    }) {
                        Text("恢复构建默认")
                    }
                }
                Text(
                    text = "支持在 android/local.properties、项目或用户 gradle.properties，或命令行 -PemergencyLaneApiBaseUrl=... 里注入默认地址。",
                    color = Color(0xFF93C5FD),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
