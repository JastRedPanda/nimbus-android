package com.nimbus.weather

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.NotificationHelper
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.ui.home.HomeScreen
import com.nimbus.weather.ui.home.HomeViewModel
import com.nimbus.weather.ui.location.LocationSearchScreen
import com.nimbus.weather.ui.location.LocationSearchViewModel
import com.nimbus.weather.ui.onboarding.OnboardingScreen
import com.nimbus.weather.ui.settings.SettingsScreen
import com.nimbus.weather.ui.settings.SettingsViewModel
import com.nimbus.weather.ui.theme.NimbusWeatherTheme
import com.nimbus.weather.ui.widgetcustomize.WidgetCustomizeScreen
import com.nimbus.weather.util.LanguageHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = try {
            runBlocking { SettingsDataStore(newBase).appLanguage.first() }
        } catch (_: Exception) { "auto" }
        val locale = if (language == "auto") {
            LanguageHelper.resolveLocale()
        } else {
            LanguageHelper.resolveLocale(language)
        }
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        Locale.setDefault(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createChannel(this)
        WeatherUpdateScheduler.schedule(this)

        setContent {
            val settings = remember { SettingsDataStore(applicationContext) }
            val themeMode by settings.themeMode.collectAsState(initial = null)

            NimbusWeatherTheme(
                themeMode = themeMode ?: com.nimbus.weather.util.ThemeMode.SYSTEM
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    NimbusApp()
                }
            }
        }
    }
}

@Composable
fun NimbusApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var startDest by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val settings = SettingsDataStore(context.applicationContext)
        startDest = if (settings.onboardingDone.first()) "home" else "onboarding"
    }

    val dest = startDest ?: return

    NavHost(navController = navController, startDestination = dest) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = { tempUnit ->
                    scope.launch {
                        val settings = SettingsDataStore(context.applicationContext)
                        settings.setTempUnit(tempUnit)
                        settings.setOnboardingDone()
                    }
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSelectCity = {
                    navController.navigate("location_search")
                }
            )
        }

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
                onCitySearchClick = { navController.navigate("location_search") },
                onWidgetCustomizeClick = { navController.navigate("widget_customize") }
            )
        }

        composable("widget_customize") {
            WidgetCustomizeScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("location_search") {
            val viewModel: LocationSearchViewModel = viewModel()
            LocationSearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCitySelected = {
                    navController.popBackStack()
                }
            )
        }
    }
}
