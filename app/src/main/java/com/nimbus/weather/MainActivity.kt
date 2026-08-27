package com.nimbus.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.service.KeepAliveService
import com.nimbus.weather.service.NotificationHelper
import com.nimbus.weather.service.WeatherUpdateScheduler
import com.nimbus.weather.service.WidgetUpdateManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = try {
            runBlocking { SettingsDataStore(newBase).appLanguage.first() }
        } catch (_: Exception) { "auto" }
        val locale = LanguageHelper.resolveLocale(language)
        Locale.setDefault(locale)
        super.attachBaseContext(LanguageHelper.createContextWithLocale(newBase, locale))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.createChannel(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val settings = SettingsDataStore(this@MainActivity)
            val interval = settings.updateIntervalHours.first()
            WeatherUpdateScheduler.schedule(this@MainActivity, interval)
            if (settings.keepAliveEnabled.first()) {
                KeepAliveService.start(this@MainActivity)
            }
            WidgetUpdateManager.refreshAllWidgets(this@MainActivity)
        }

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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        val settings = SettingsDataStore(context.applicationContext)
        startDest = if (settings.onboardingDone.first()) "home" else "onboarding"

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted && settings.notificationsEnabled.first()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val dest = startDest ?: return

    NavHost(
        navController = navController,
        startDestination = dest,
        enterTransition = { slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300)) }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    scope.launch {
                        val settings = SettingsDataStore(context.applicationContext)
                        settings.setOnboardingDone()
                    }
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSelectCity = {
                    navController.navigate("location_search?closeOnSelect=true")
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

        composable(
            route = "location_search?closeOnSelect={closeOnSelect}",
            arguments = listOf(navArgument("closeOnSelect") { defaultValue = false })
        ) {
            val viewModel: LocationSearchViewModel = viewModel()
            val closeOnSelect = it.arguments?.getBoolean("closeOnSelect") ?: false
            LocationSearchScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onCitySelected = {
                    navController.popBackStack()
                },
                closeOnSelect = closeOnSelect
            )
        }
    }
}
