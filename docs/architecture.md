# Архитектура Nimbus Weather

## Стек

| Компонент | Выбор |
|---|---|
| Язык | Kotlin (jvmTarget 17) |
| UI | Jetpack Compose + Material 3 (BOM 2024.12.01) |
| Виджет | Glance (glance / glance-appwidget / glance-material3 1.2.0-rc01) |
| Архитектура | MVVM: ViewModel + Repository, ручной DI |
| Сеть | Retrofit 2.11 + OkHttp 4.12 + kotlinx.serialization 1.7.3 |
| Асинхронность | Coroutines + Flow |
| Настройки | DataStore Preferences 1.1.1 |
| Фон | WorkManager 2.10 |
| Навигация | navigation-compose 2.8.5 (NavHost в MainActivity) |
| Splash | core-splashscreen 1.0.1 |
| Версии | minSdk 26, compileSdk/targetSdk 35, versionCode — автоинкремент (app/version.properties), versionName "1.1" (меняется вручную) |

## Сеть (Open-Meteo)

### Геокодинг (поиск города)

```
GET https://geocoding-api.open-meteo.com/v1/search?name=Москва&count=10&language=ru&format=json
```

Ответ: `results[]` с `name`, `latitude`, `longitude`, `country`, `admin1`, `timezone`, `local_names` (переводы названий).

Поиск идёт на языке приложения (`GeocodingApi.searchCities(language)`), без дефолта.

### Погода (один запрос)

```
GET https://api.open-meteo.com/v1/forecast
  ?latitude=50.45
  &longitude=30.52
  &current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index
  &daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,uv_index_max
  &hourly=temperature_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,relative_humidity_2m,apparent_temperature,uv_index
  &timezone=auto
  &forecast_days=7
```

`timezone=auto` — сервер сам выбирает таймзону по координатам.

Состав полей ответа:
- `current` — температура, влажность, ощущение, осадки, код погоды (WMO), давление на поверхности, ветер (скорость, направление, порывы), УФ-индекс
- `hourly` — температура, осадки, код погоды, скорость/направление ветра, влажность, ощущение, УФ-индекс
- `daily` — код погоды, макс/мин температура, макс/мин ощущение, восход/закат, сумма осадков, макс вероятность осадков, макс скорость ветра, макс порывы, доминирующее направление, макс УФ-индекс

### AQI (Air Quality Index)

```
GET https://air-quality-api.open-meteo.com/v1/air-quality
  ?latitude=50.45
  &longitude=30.52
  &current=european_aqi,us_aqi,pm2_5,pm10,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide,ozone
```

AQI живёт на отдельном хосте: в `ApiClient` для него создан отдельный Retrofit (`AIR_QUALITY_BASE_URL`). На `api.open-meteo.com` эндпоинт даёт 404 — из-за этого AQI молча пропадал (историю см. в docs/history.md).

Состав полей: европейский AQI (0–100+), US AQI, PM2.5, PM10, NO₂, SO₂, CO, O₃.

### WMO Weather Codes (по коду → ресурс-строка `weatherDescriptionRes`)

| Код | Значение | Код | Значение |
|---|---|---|---|
| 0 | ясно | 61–67 | дождь (лёгкий/умеренный/сильный/ледяной) |
| 1 | преимущественно ясно | 71–77 | снег (лёгкий/умеренный/сильный/зёрна) |
| 2 | переменная облачность | 80–82 | ливневый дождь |
| 3 | пасмурно | 85–86 | снегопад |
| 45 | туман | 95 | гроза |
| 48 | изморозь | 96, 99 | гроза с градом |
| 51–57 | морось (в т.ч. ледяная) | | |

Классификация УФ (`uv_low`..`uv_extreme`) и 8 румбов ветра (`windDirection`) — в ресурсах, локализованы.

## Структура кода (`app/src/main/java/com/nimbus/weather`)

```
data/
├── api/        WeatherApi.kt, GeocodingApi.kt, AirQualityApi.kt, ApiClient.kt
├── model/      WeatherResponse.kt, GeocodingResponse.kt, AirQualityResponse.kt,
│               Location.kt
├── repository/ WeatherRepository.kt, WeatherCache.kt
└── local/      SettingsDataStore.kt
ui/
├── theme/      Theme.kt, Color.kt, Type.kt
├── onboarding/ OnboardingScreen.kt
├── home/       HomeScreen.kt, HomeViewModel.kt
├── settings/   SettingsScreen.kt, SettingsViewModel.kt
├── location/   LocationSearchScreen.kt, LocationSearchViewModel.kt
├── widgetcustomize/ WidgetCustomizeScreen.kt, WidgetCustomizeViewModel.kt
└── components/ CurrentWeatherCard.kt, DailyForecastCard.kt, HourlyForecastBar.kt,
                AqiCard.kt, WeatherIcon.kt
widget/         ClockTempWidget.kt (и ClockTempWidgetReceiver внутри), WidgetPalette.kt,
                WidgetRender.kt (поток рендер-данных + fitBaseSp — адаптивный шрифт)
service/        WeatherUpdateWorker.kt (и object WeatherUpdateScheduler внутри),
                NotificationHelper.kt, WidgetUpdateManager.kt, KeepAliveService.kt
util/           CityNameResolver.kt, CityNameTranslator.kt, LanguageHelper.kt,
                DateTimeUtils.kt, WeatherCodeUtils.kt, TemperatureUtils.kt,
                WindDirection.kt, Constants.kt, ThemeMode.kt
```

## Кэш

- `WeatherCache` — ответы API (погода + AQI + таймстемп) в JSON-файле в `cacheDir`
- При ошибке сети `WeatherRepository` читает из кэша
- TTL = 2 × интервал обновления (`setTtlHours` в `HomeViewModel.loadWeather`), при успешном обновлении кэш перезаписывается
- Индикатор «показаны кэшированные данные» — AssistChip на главном экране (`showingCachedWeather`)

## Зависимости (app/build.gradle.kts)

```kotlin
// Compose BOM
platform("androidx.compose:compose-bom:2024.12.01")
androidx.compose.material3, ui, ui-tooling(-preview), material-icons-extended

// Activity + Lifecycle
androidx.activity:activity-compose:1.9.3
androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7
androidx.lifecycle:lifecycle-runtime-compose:2.8.7

// Navigation
androidx.navigation:navigation-compose:2.8.5

// Glance
androidx.glance:glance:1.2.0-rc01
androidx.glance:glance-appwidget:1.2.0-rc01
androidx.glance:glance-material3:1.2.0-rc01

// Retrofit + OkHttp + serialization
com.squareup.retrofit2:retrofit:2.11.0
com.squareup.okhttp3:okhttp:4.12.0
com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0
org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3

// DataStore, WorkManager, Core, Splash
androidx.datastore:datastore-preferences:1.1.1
androidx.work:work-runtime-ktx:2.10.0
androidx.core:core-ktx:1.15.0
androidx.core:core-splashscreen:1.0.1

// Tests
junit 4.13.2, kotlinx-coroutines-test 1.9.0, mockk 1.13.13, turbine 1.2.0
```

## Подводные камни

- **Glance 1.2.0-rc01**: нет `clip`, нет `Surface` в glance-material3, `background` без формы. Скругление углов — только `androidx.glance.appwidget.cornerRadius(dp)` (найдено в AAR; классы `CornerRadiusKt`/`CornerRadiusModifier`)
- **Таймзона города — из геокодинга**: Open-Meteo отдаёт IANA-идентификатор в поле `timezone` ответа Geocoding API; валидация `DateTimeUtils.isValidTimeZoneId` (кэш `TimeZone.getAvailableIDs()`), fallback `Europe/Kiev`. GPS-определения города нет (play-services-location удалён в v1.5)
- **versionCode — автоинкремент**: `preBuild` в app/build.gradle.kts читает `app/version.properties` (gitignored), +1 при каждой сборке; установка APK «поверх» работает. versionName меняется вручную. Сбить счётчик можно очисткой файла — тогда versionCode упадёт, и обновление «поверх» не встанет (вылечится следующим релизом)
- **CI-сборка (`.github/workflows/android.yml`)**: на каждый push/PR в `main` — `assembleDebug` + `test`, debug APK заливается артефактом; не публикует релиз
- **Релиз в CI (`.github/workflows/release.yml`)**: пуш тега `v*` → `-PversionName` из тега, `-PversionCode` из `git rev-list --count HEAD` (монотонно), ключ `DEBUG_KEYSTORE_B64` из secrets восстанавливается в `~/.android/debug.keystore`; подпись — явная `signingConfigs.create("release")` в app/build.gradle.kts (SHA-1 `b9cdf73d…`, постоянный с v1.6; `signingConfigs.getByName("debug")` в CI не работает — AGP генерирует свой ключ)
- **POST_NOTIFICATIONS**: без пермишна в манифесте и runtime-запроса `showWeatherNotification` молча выходит на Android 13+
- **KeepAliveService (foreground, `START_STICKY`)**: тумблер «Перезапуск при закрытии» в настройках. Сервис с foreground-уведомлением канала `keep_alive` (`PRIORITY_MIN`, `CATEGORY_SERVICE`) + `foregroundServiceType="specialUse"` (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` обязателен на Android 14+). При `onTaskRemoved` сам себя перезапускает через `startForegroundService`. Не спасает от force-stop — это системное ограничение, обойти нельзя. На агрессивных прошивках (MIUI/EMUI/HyperOS и т. п.) ОС всё равно может убить процесс — пользователю нужно вручную отключить оптимизацию батареи для Nimbus