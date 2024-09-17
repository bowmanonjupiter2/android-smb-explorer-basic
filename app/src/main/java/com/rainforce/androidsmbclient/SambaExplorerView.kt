package com.rainforce.androidsmbclient

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.rainforce.androidsmbclient.model.SambaExplorerViewModel
import com.rainforce.androidsmbclient.ui.MainView

@Composable
fun SambaExplorerView(viewModel: SambaExplorerViewModel) {

    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val colorScheme = if (isDarkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(colorScheme = colorScheme) {
        MainView(viewModel)
    }
}