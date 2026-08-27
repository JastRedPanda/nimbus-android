package com.nimbus.weather.ui.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nimbus.weather.R
import com.nimbus.weather.data.local.SettingsDataStore
import com.nimbus.weather.util.CityNameResolver
import com.nimbus.weather.util.LanguageHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    viewModel: LocationSearchViewModel,
    onBackClick: () -> Unit,
    onCitySelected: () -> Unit,
    closeOnSelect: Boolean = false
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.select_city)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.search_city)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            val langCode = if (state.appLanguage == "auto") {
                LanguageHelper.resolveLocale().language
            } else {
                state.appLanguage
            }

            LazyColumn {
                if (state.recentCities.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.recent_cities_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.recentCities) { city ->
                        RecentCityRow(
                            displayName = CityNameResolver.displayName(
                                city.name, city.localNames, langCode
                            ),
                            isFavourite = state.favouriteNames.contains(city.name),
                            onSelect = {
                                viewModel.selectRecentCity(city)
                                if (closeOnSelect) onCitySelected()
                            },
                            onToggleFavourite = { viewModel.toggleRecentFavourite(city) },
                            onRemove = { viewModel.removeRecentCity(city.name) }
                        )
                    }
                }

                when {
                    state.loading -> {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                    state.noResults -> {
                        item {
                            Text(
                                text = stringResource(R.string.no_results),
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        items(state.results) { result ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectCity(result)
                                        if (closeOnSelect) onCitySelected()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    val subtitle = listOfNotNull(
                                        result.admin1,
                                        result.country
                                    ).joinToString(", ")
                                    if (subtitle.isNotBlank()) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                val isFavourite = state.favouriteNames.contains(result.name)
                                IconToggleButton(
                                    checked = isFavourite,
                                    onCheckedChange = { viewModel.toggleFavourite(result) }
                                ) {
                                    Icon(
                                        imageVector = if (isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = stringResource(
                                            if (isFavourite) R.string.remove_from_favourites
                                            else R.string.add_to_favourites
                                        ),
                                        tint = if (isFavourite) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCityRow(
    displayName: String,
    isFavourite: Boolean,
    onSelect: () -> Unit,
    onToggleFavourite: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconToggleButton(
            checked = isFavourite,
            onCheckedChange = { onToggleFavourite() }
        ) {
            Icon(
                imageVector = if (isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = stringResource(
                    if (isFavourite) R.string.remove_from_favourites
                    else R.string.add_to_favourites
                ),
                tint = if (isFavourite) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_from_history)
            )
        }
    }
}
