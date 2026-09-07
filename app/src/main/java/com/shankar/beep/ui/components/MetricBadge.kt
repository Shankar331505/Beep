package com.shankar.beep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shankar.beep.model.SoundCategory
import com.shankar.beep.ui.theme.Hairline
import com.shankar.beep.ui.theme.InkRaised
import com.shankar.beep.ui.theme.IvoryMuted
import com.shankar.beep.ui.theme.UiSans

@Composable
fun CategoryBadge(category: SoundCategory, modifier: Modifier = Modifier) {
    val accent = categoryAccent(category)
    val wash = categoryWash(category)

    Box(
        modifier = modifier
            .background(wash, shape = RoundedCornerShape(6.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category.displayName.uppercase(),
            color = accent,
            fontFamily = UiSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(InkRaised, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Hairline, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label  $value",
            color = IvoryMuted,
            fontFamily = UiSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
