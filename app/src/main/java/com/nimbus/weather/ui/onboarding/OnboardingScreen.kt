package com.nimbus.weather.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSelectCity: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context.applicationContext) }
    val cityName by settings.cityName.collectAsState(initial = "")
    val favourites by settings.favouriteCities.collectAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (cityName.isNotBlank()) {
            Text(
                text = stringResource(R.string.onboarding_current_city, cityName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val favouriteNames = favourites.map { it.name }.filter { it != cityName }
        if (favouriteNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.onboarding_favourites,
                    favouriteNames.joinToString(", ")
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSelectCity,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_city))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.start))
        }
    }
}