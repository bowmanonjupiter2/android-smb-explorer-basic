package com.rainforce.androidsmbclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.rainforce.androidsmbclient.model.SMBFileListViewModel
import com.rainforce.androidsmbclient.ui.MainView
import com.rainforce.androidsmbclient.ui.theme.AndroidSMBClientTheme


class MainActivity : ComponentActivity() {

    private val viewModel: SMBFileListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidSMBClientTheme {
                SMBExplorerApp(viewModel)
            }
        }
    }
}

@Composable
fun SMBExplorerApp(viewModel: SMBFileListViewModel) {

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


