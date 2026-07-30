# Nimbus Android — план приложения

## Стек

| Компонент | Выбор |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Виджеты | Glance (Jetpack Compose) |
| Архитектура | MVVM (ViewModel + Repository) |
| Сеть | Retrofit + OkHttp + Kotlinx Serialization |
| Асинхронность | Coroutines + Flow |
| DI | Ручной |
| Настройки | DataStore Preferences |
| Фон | WorkManager |
| Локация | Google Play Services Location |
| Min SDK | API 26 (Android 8.0) |
| Тема | Material 3 + Dynamic Colors + Dark theme |

## API Open-Meteo

### Геокодинг (поиск города)

```
GET https://geocoding-api.open-meteo.com/v1/search?name=Москва&count=10&language=ru&format=json
```

Ответ: список `results` с `name`, `latitude`, `longitude`, `country`, `admin1`, `timezone`.

### Погода (один запрос)

```
GET /v1/forecast
  ?latitude=50.45
  &longitude=30.52
  &current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m
  &daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant
  &timezone=auto
  &forecast_days=7
```

Текущие данные (`current`): температура, влажность, ощущение, осадки, код погоды (WMO), давление на поверхности, ветер (скорость, направление, порывы).

Дневные данные (`daily`): код погоды, макс/мин температура, макс/мин ощущение, восход/закат, сумма осадков, макс вероятность осадков, макс скорость ветра, макс порывы, доминирующее направление.

### WMO Weather Codes

- 0 — ясно
- 1 — преимущественно ясно
- 2 — переменная облачность
- 3 — пасмурно
- 45 — туман
- 48 — изморозь
- 51 — слабая морось
- 53 — умеренная морось
- 55 — сильная морось
- 56 — ледяная морось
- 57 — сильная ледяная морось
- 61 — небольшой дождь
- 63 — умеренный дождь
- 65 — сильный дождь
- 66 — ледяной дождь
- 67 — сильный ледяной дождь
- 71 — небольшой снег
- 73 — умеренный снег
- 75 — сильный снег
- 77 — снежные зёрна
- 80 — ливневый дождь
- 81 — умеренный ливень
- 82 — сильный ливень
- 85 — снегопад
- 86 — сильный снегопад
- 95 — гроза
- 96 — гроза с градом
- 99 — сильная гроза с градом

## Структура проекта

```
com.nimbus.weather/
├── data/
│   ├── api/
│   │   ├── WeatherApi.kt          # Retrofit: /v1/forecast
│   │   └── GeocodingApi.kt        # Retrofit: /v1/search
│   ├── model/
│   │   ├── WeatherResponse.kt     # DTO
│   │   └── GeocodingResponse.kt   # DTO
│   └── repository/
│       └── WeatherRepository.kt   # Единый источник данных
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   └── SettingsViewModel.kt
│   ├── location/
│   │   ├── LocationSearchScreen.kt
│   │   └── LocationSearchViewModel.kt
│   └── components/
│       ├── CurrentWeatherCard.kt
│       ├── DailyForecastCard.kt
│       └── WeatherIcon.kt
├── widget/
│   ├── ClockTempWidget.kt         # Виджет 1
│   ├── TempForecastWidget.kt      # Виджет 2
│   └── WidgetUpdateManager.kt
├── service/
│   └── WeatherUpdateWorker.kt     # WorkManager: 120 мин
├── data/
│   └── local/
│       └── SettingsDataStore.kt   # DataStore Preferences
└── util/
    ├── Constants.kt
    ├── WeatherCodeUtils.kt
    └── DateTimeUtils.kt
```

## Экраны

### 1. Главный экран (HomeScreen)

Верхняя половина — CurrentWeatherCard:
- Иконка погоды (WMO → вектор)
- Температура (°C)
- Ощущается как
- Осадки (mm)
- Ветер (скорость + направление + порывы)
- Давление (гПа)
- Влажность (%)
- Восход / Закат

Нижняя половина — список DailyForecastCard на 7 дней:
- День недели + дата
- Иконка погоды
- Маx/Min температура
- Ощущается макс/мин
- Осадки (mm)
- Вероятность осадков (%)
- Ветер (макс скорость + направление)
- Восход / Закат
- Давление
- Влажность

### 2. Настройки (SettingsScreen)

- Переключатель для виджетов: фактическая температура / по ощущению
- Выбор города (ведёт на LocationSearchScreen)
- Единицы: °C/°F, ветер м/с/км/ч, давление гПа/мм рт.ст.
- Тема: Системная / Светлая / Тёмная

### 3. Поиск города (LocationSearchScreen)

- Поле поиска → Geocoding API с debounce
- Список результатов: название, регион, страна
- Выбор → сохранение в DataStore → возврат на главную

## Локация

- При первом запуске: запрос разрешения GPS
- GPS включён → координаты → reverse geocode → город
- GPS выключен/отказ → дефолтный город: **Киев** (50.4501, 30.5234)
- Сохраняется: lat, lon, city_name, timezone
- Пользователь может сменить город через настройки в любой момент
- При последующих запусках: загружается сохранённый город

## Виджеты (Glance)

### ClockTempWidget (часы + температура)
- Размер: настраиваемый (min 2×1)
- Системное время (ЧЧ:ММ)
- Крупно температура + иконка погоды
- Название города
- Температура: actual/feels like — из настроек

### TempForecastWidget (температура + неделя)
- Размер: настраиваемый (min 4×1)
- Текущая температура крупно + иконка
- Строка 7 дней: день недели, иконка, макс/мин
- Температура: actual/feels like — из настроек

## Фоновое обновление

- WorkManager: PeriodicWorkRequest каждые 120 минут
- При открытии приложения — принудительное обновление
- После обновления: DataStore + broadcast для виджетов

## Зависимости (Gradle)

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.12.01"))
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material:material-icons-extended")

// Activity Compose
implementation("androidx.activity:activity-compose:1.9.3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Glance (виджеты)
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.glance:glance-material3:1.1.1")

// Retrofit
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.10.0")

// Location
implementation("com.google.android.gms:play-services-location:21.3.0")

// Splash Screen
implementation("androidx.core:core-splashscreen:1.0.1")
```

<!-- План только для локального использования. Не публиковать на GitHub. -->
```
