package com.example.focusguard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Forest = Color(0xFF176B50)
val Mist = Color(0xFFF3F7F4)
val Ink = Color(0xFF172D25)

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Forest, secondary = Color(0xFF576E5A),
        background = Mist, surface = Color.White, onBackground = Ink, onSurface = Ink,
        primaryContainer = Color(0xFFD7EDDF), onPrimaryContainer = Ink), content = content)
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
fun Heading(title: String, detail: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF617469))
}

fun timeLabel(ms: Long): String {
    val minutes = ms.coerceAtLeast(0) / 60_000
    return if (minutes >= 60) "${minutes / 60} giờ ${minutes % 60} phút" else "$minutes phút"
}
