package com.rainforce.androidsmbclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun HighlightedButton(onClick: () -> Unit) {
    var isHighlighted by remember { mutableStateOf(false) }
    var isLoopingHighlighted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (isLoopingHighlighted) {
            isHighlighted = true
            delay(500)
            isHighlighted = false
            delay(500)// Change the duration as needed
        }
    }

    IconButton(
        onClick = {
            isLoopingHighlighted = false
            onClick()
        },
        modifier = Modifier
            .padding(bottom = 4.dp)
            .background(
                if (isHighlighted) MaterialTheme.colorScheme.inversePrimary else Color.Transparent,
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Filled.FolderOpen,
            contentDescription = "Open to choose local folder",
        )
    }
}