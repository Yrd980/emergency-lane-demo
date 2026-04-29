package com.yrd.emergencylanemobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val SoftSurface = Color(0xFFF8F4FF)
private val SoftSurfaceStrong = Color(0xFFF1EAFE)
private val InkPrimary = Color(0xFF2F1F46)
private val InkSecondary = Color(0xFF7A6E92)
private val AccentPurple = Color(0xFF9F86FF)

@Composable
fun MetricChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSurfaceStrong),
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
                    .background(AccentPurple),
            )
            Text(text = "$label $value", color = InkPrimary)
        }
    }
}

@Composable
fun InfoCard(title: String, body: String, accent: Color = AccentPurple) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSurface),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, color = accent, fontWeight = FontWeight.Bold)
            Text(text = body, color = InkSecondary)
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    hint: String,
    accent: Color = AccentPurple,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SoftSurface),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accent),
                )
            }
            Text(text = value, color = InkPrimary, fontWeight = FontWeight.Bold)
            Text(text = title, color = InkPrimary, fontWeight = FontWeight.SemiBold)
            Text(text = hint, color = InkSecondary)
        }
    }
}
