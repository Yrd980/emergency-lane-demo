package com.yrd.emergencylanemobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yrd.emergencylanemobile.model.MobileCaseDetail
import com.yrd.emergencylanemobile.model.MobileCaseSummary
import com.yrd.emergencylanemobile.model.MobileLocalDraft
import com.yrd.emergencylanemobile.ui.components.InfoCard

private val InkPrimary = Color(0xFF2F1F46)
private val InkSecondary = Color(0xFF7A6E92)
private val AccentPurple = Color(0xFF9F86FF)
private val AccentRose = Color(0xFFE68BAF)
private val SoftSurface = Color(0xFFF8F4FF)
private val SoftSurfaceStrong = Color(0xFFF1EAFE)

@Composable
fun CaseRow(item: MobileCaseSummary, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xFFF0E8FF) else SoftSurface),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = item.id, color = AccentPurple, style = MaterialTheme.typography.labelMedium)
            Text(text = item.correctedPlateNumber ?: item.plateNumber, color = InkPrimary, fontWeight = FontWeight.Bold)
            Text(text = item.summary, color = InkSecondary)
            Text(
                text = "${item.status} · ${item.reviewStatus} · ${item.sourceName}",
                color = InkSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "置信度 ${String.format(java.util.Locale.US, "%.2f", item.confidence)} · ${item.evidenceCount} 张证据图 · ${item.eventCount} 个事件",
                color = InkSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            if (item.operatorNote.isNotBlank()) {
                Text(text = item.operatorNote, color = AccentPurple, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseDetailSection(
    detail: MobileCaseDetail?,
    localDraft: MobileLocalDraft?,
    busy: Boolean,
    onSave: (String, String, String, String) -> Unit,
    onSaveLocalDraft: (String, String, String, String) -> Unit,
    onClearLocalDraft: (String) -> Unit,
    onReport: (String) -> Unit,
) {
    if (detail == null) {
        InfoCard(title = "案件详情", body = "先在上方选择一个案件。")
        return
    }

    var correctedPlate by remember(detail.id, detail.correctedPlateNumber) { mutableStateOf(detail.correctedPlateNumber.orEmpty()) }
    var operatorNote by remember(detail.id, detail.operatorNote) { mutableStateOf(detail.operatorNote) }
    var reviewStatus by remember(detail.id, detail.reviewStatus) { mutableStateOf(detail.reviewStatus) }
    var localPlateCandidate by remember(detail.id, localDraft?.localPlateCandidate) { mutableStateOf(localDraft?.localPlateCandidate.orEmpty()) }
    var localSceneNote by remember(detail.id, localDraft?.sceneNote) { mutableStateOf(localDraft?.sceneNote.orEmpty()) }
    var localReviewStatus by remember(detail.id, localDraft?.reviewStatus) { mutableStateOf(localDraft?.reviewStatus ?: reviewStatus) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "${detail.id} / ${detail.plateNumber}", color = InkPrimary, fontWeight = FontWeight.Bold)
                Text(text = "run: ${detail.runId} · source: ${detail.sourceName}", color = AccentPurple)
                Text(text = detail.summary, color = InkSecondary)
                Text(text = "状态 ${detail.status} · 复核 ${detail.reviewStatus}", color = InkSecondary)
                Text(
                    text = if (detail.clipUrl.isNullOrBlank()) {
                        "本案当前无独立 clip 地址，展示片段已统一放到上方本地证据区，避免播放不稳定的远程视频。"
                    } else {
                        "案件片段展示已切到上方 APK 内置 clip，用于稳定讲解闭环；原始 clip 链接仅保留为数据字段，不在这里直接播放。"
                    },
                    color = InkSecondary,
                )
            }
        }

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "人工复核", color = InkPrimary, fontWeight = FontWeight.Bold)
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
                                color = if (reviewStatus == option) AccentPurple else InkSecondary,
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
                Text(text = detail.reportContent ?: "暂无文书", color = InkSecondary)
            }
        }

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "端侧补充草稿", color = InkPrimary, fontWeight = FontWeight.Bold)
                Text(
                    text = "本机缓存草稿用于端侧现场补录与离线演示，数据仅保存在本机。",
                    color = InkSecondary,
                )
                OutlinedTextField(
                    value = localPlateCandidate,
                    onValueChange = { localPlateCandidate = it },
                    label = { Text("端侧候选车牌") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = localSceneNote,
                    onValueChange = { localSceneNote = it },
                    label = { Text("现场补充说明") },
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("待复核", "复核通过", "复核退回").forEach { option ->
                        TextButton(onClick = { localReviewStatus = option }) {
                            Text(
                                text = option,
                                color = if (localReviewStatus == option) AccentPurple else InkSecondary,
                                fontWeight = if (localReviewStatus == option) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                localDraft?.savedAt?.let { savedAt ->
                    Text(text = "最近缓存：$savedAt · ${localDraft.inputMode}", color = AccentPurple)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onSaveLocalDraft(detail.id, localPlateCandidate, localSceneNote, localReviewStatus) }, enabled = !busy) {
                        Text("保存端侧草稿")
                    }
                    TextButton(onClick = {
                        localPlateCandidate = ""
                        localSceneNote = ""
                        localReviewStatus = reviewStatus
                        onClearLocalDraft(detail.id)
                    }) {
                        Text("清空草稿")
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SoftSurface)) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "关键证据", color = InkPrimary, fontWeight = FontWeight.Bold)
                if (detail.evidence.isEmpty()) {
                    Text(text = "暂无证据图", color = InkSecondary)
                } else {
                    detail.evidence.take(3).forEach { item ->
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                        Text(text = "${item.label} · ${item.capturedAt}", color = InkSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (detail.events.isNotEmpty()) {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SoftSurfaceStrong)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "关联事件", color = InkPrimary, fontWeight = FontWeight.Bold)
                    detail.events.forEach { event ->
                        Text(text = "${event.id} · ${event.status} · ${event.firstSeen}", color = InkSecondary)
                    }
                }
            }
        }
    }
}
