package com.rainforce.androidsmbclient.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DynamicShortenText(fullText: String) {
    val maxTextLength = 45
    val displayText = if (fullText.length > maxTextLength) {
        fullText.take(maxTextLength) + "..."
    } else {
        fullText
    }
    Text(
        modifier = Modifier.padding(3.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        text = displayText,
        style = MaterialTheme.typography.bodySmall
    )
}