package com.rainforce.androidsmbclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.rainforce.androidsmbclient.model.SambaExplorerViewModel
import com.rainforce.androidsmbclient.ui.theme.AndroidSMBClientTheme


class MainActivity : ComponentActivity() {

    private val viewModel: SambaExplorerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidSMBClientTheme {
                AppNavigation(viewModel)
            }
        }
    }
}




