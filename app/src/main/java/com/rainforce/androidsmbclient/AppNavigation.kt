package com.rainforce.androidsmbclient

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rainforce.androidsmbclient.model.SambaExplorerViewModel
import com.rainforce.androidsmbclient.ui.SplashScreen

@Composable
fun AppNavigation(viewModel: SambaExplorerViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("explorer") { SambaExplorerView(viewModel) }
    }
}