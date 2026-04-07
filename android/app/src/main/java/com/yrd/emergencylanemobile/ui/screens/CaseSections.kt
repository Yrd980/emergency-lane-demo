package com.yrd.emergencylanemobile.ui.screens

import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.yrd.emergencylanemobile.model.MobileCaseDetail
import com.yrd.emergencylanemobile.model.MobileCaseSummary
import com.yrd.emergencylanemobile.model.MobileLocalDraft
import com.yrd.emergencylanemobile.ui.components.InfoCard

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
                Text(text = "端侧增强链路（本机缓存）", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    text = "为后续 CameraX / JNI / ncnn/YOLO / HyperLPR 输入预留的本机草稿位；当前仅保存到设备缓存，用于真机彩排和弱网补录。",
                    color = Color(0xFFD7E3FF),
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
                                color = if (localReviewStatus == option) Color(0xFF38BDF8) else Color(0xFFD7E3FF),
                                fontWeight = if (localReviewStatus == option) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                localDraft?.savedAt?.let { savedAt ->
                    Text(text = "最近缓存：$savedAt · ${localDraft.inputMode}", color = Color(0xFF93C5FD))
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
