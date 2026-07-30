package com.nimbus.weather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.ui.home.HomeScreen
import com.nimbus.weather.ui.home.HomeViewModel
import com.nimbus.weather.ui.location.LocationSearchScreen
import com.nimbus.weather.ui.location.LocationSearchViewModel
import com.nimbus.weather.ui.settings.SettingsScreen
import com.nimbus.weather.ui.settings.SettingsViewModel
import com.nimbus.weather.ui.theme.NimbusWeatherTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        WeatherUpdateScheduler.schedule(this)

        setContent {
            NimbusWeatherTheme {
                NimbusApp()
            }
        }
    }
}

@Composable
fun NimbusApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val viewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable("settings") {
            val viewModel: SettingsViewModel = viewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCitySearchClick = { navController.navigate("location_search") }
            )
        }

        composable("location_search") {
            val viewModel: LocationSearchViewModel = viewModel()
            LocationSearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCitySelected = {
                    navController.popBackStack("home", false)
                }
            )
        }
    }
}
