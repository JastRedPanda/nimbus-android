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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nimbus.weather.R
import com.nimbus.weather.util.TemperatureUnit

@Composable
fun OnboardingScreen(
    onFinish: (TemperatureUnit) -> Unit,
    onSelectCity: () -> Unit
) {
    var tempUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }

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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.units_temperature),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { tempUnit = TemperatureUnit.CELSIUS },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.celsius))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { tempUnit = TemperatureUnit.FAHRENHEIT },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.fahrenheit))
        }

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedButton(
            onClick = onSelectCity,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.select_city))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onFinish(tempUnit) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.start))
        }
    }
}
