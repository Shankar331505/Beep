package com.shankar.beep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.theme.*

@Composable
fun CategoryBadge(category: SoundCategory, modifier: Modifier = Modifier) {
    val (bgColor, textColor, borderColor) = when (category) {
        SoundCategory.EMERGENCY -> Triple(EmergencyRedBg, EmergencyCoral, EmergencyCoral.copy(alpha = 0.5f))
        SoundCategory.DOMESTIC -> Triple(DarkCard, PrimaryBlue, PrimaryBlue.copy(alpha = 0.4f))
        SoundCategory.SOCIAL -> Triple(AmberBg, AmberWarning, AmberWarning.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category.displayName.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(DarkSurface, shape = RoundedCornerShape(8.dp))
            .border(1.dp, DarkCardBorder, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: $value",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
