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
  &current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index
  &daily=weather_code,temperature_2m_max,temperature_2m_min,apparent_temperature_max,apparent_temperature_min,sunrise,sunset,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,uv_index_max
  &hourly=temperature_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,relative_humidity_2m,apparent_temperature,uv_index
  &timezone=auto
  &forecast_days=7
```

Текущие данные (`current`): температура, влажность, ощущение, осадки, код погоды (WMO), давление на поверхности, ветер (скорость, направление, порывы), УФ-индекс.

Почасовые данные (`hourly`): температура, осадки, код погоды, скорость/направление ветра, влажность, ощущение, УФ-индекс.

Дневные данные (`daily`): код погоды, макс/мин температура, макс/мин ощущение, восход/закат, сумма осадков, макс вероятность осадков, макс скорость ветра, макс порывы, доминирующее направление, макс УФ-индекс.

### AQI (Air Quality Index)

```
GET /v1/air-quality
  ?latitude=50.45
  &longitude=30.52
  &current=european_aqi,us_aqi,pm2_5,pm10,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide,ozone
```

Текущие данные: европейский AQI (0–100+), US AQI, PM2.5, PM10, NO₂, SO₂, CO, O₃.

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
│   │   ├── GeocodingApi.kt        # Retrofit: /v1/search
│   │   └── AirQualityApi.kt       # Retrofit: /v1/air-quality
│   ├── model/
│   │   ├── WeatherResponse.kt     # DTO
│   │   ├── GeocodingResponse.kt   # DTO
│   │   └── AirQualityResponse.kt  # DTO
│   ├── repository/
│   │   ├── WeatherRepository.kt   # Погода + AQI
│   │   └── WeatherCache.kt        # Кэш ответов API
│   └── local/
│       └── SettingsDataStore.kt   # DataStore Preferences
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   ├── onboarding/
│   │   └── OnboardingScreen.kt
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── SettingsViewModel.kt
│   │   └── WidgetPreviewScreen.kt  # Превью + настройка виджета
│   ├── location/
│   │   ├── LocationSearchScreen.kt
│   │   └── LocationSearchViewModel.kt
│   └── components/
│       ├── CurrentWeatherCard.kt
│       ├── DailyForecastCard.kt
│       ├── HourlyForecastBar.kt    # Почасовой прогноз
│       ├── AqiCard.kt              # Индекс качества воздуха
│       └── WeatherIcon.kt
├── widget/
│   ├── ClockTempWidget.kt
│   ├── TempForecastWidget.kt
│   └── WidgetUpdateManager.kt
├── service/
│   ├── WeatherUpdateWorker.kt     # WorkManager
│   └── NotificationHelper.kt      # Push-уведомления
├── navigation/
│   └── NavGraph.kt                # Навигация (если выделить)
└── util/
    ├── Constants.kt
    ├── WeatherCodeUtils.kt
    ├── TemperatureUtils.kt
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
- УФ-индекс
- Восход / Закат

Средняя часть — почасовой прогноз на сегодня (HourlyForecast):
- Горизонтальный список с интервалом (каждые 3 или 6 часов)
- Каждая карточка: время, иконка погоды, температура, осадки

Нижняя половина — список DailyForecastCard на 7 дней:
- День недели + дата
- Иконка погоды
- Маx/Min температура
- Ощущается макс/мин
- Осадки (mm)
- Вероятность осадков (%)
- Ветер (макс скорость + направление)
- УФ-индекс
- Восход / Закат
- Давление
- Влажность

### 2. Настройки (SettingsScreen)

- Переключатель для виджетов: фактическая температура / по ощущению
- Выбор города (ведёт на LocationSearchScreen)
- Единицы: °C/°F
- Тема: Системная / Светлая / Тёмная
- Интервал фонового обновления: 2 ч / 12 ч / 24 ч
- Включение/отключение уведомлений о погоде
- Превью виджета с настройкой цвета фона и прозрачности

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
- Размер: настраиваемый (min 1×1)
- Системное время (ЧЧ:ММ)
- Крупно температура + иконка погоды
- Название города
- Температура: actual/feels like — из настроек
- Тёмная тема: автоматически (системная) + принудительно из настроек

### TempForecastWidget (температура + неделя)
- Размер: настраиваемый (min 2×1, max 2×2)
- Текущая температура крупно + иконка
- Строка 3–7 дней: день недели, иконка, макс/мин
- Температура: actual/feels like — из настроек
- Тёмная тема: автоматически (системная) + принудительно из настроек

### Настройка внешнего вида
- Превью виджета в приложении
- Цвет фона (выбор из палитры или кастомный hex)
- Прозрачность фона (слайдер)
- Цвет текста (авто — контрастный к фону, либо вручную)

## Фоновое обновление

- WorkManager: PeriodicWorkRequest с настраиваемым интервалом
- Доступные интервалы: 2 часа / 12 часов / 24 часа
- Интервал хранится в DataStore, при изменении — перепланировка WorkManager (KEEP → не сбрасывать, UPDATE → пересоздать)
- При открытии приложения — принудительное обновление (если данные устарели более чем на интервал)
- После обновления: DataStore + broadcast для виджетов

## Кэширование

- Ответы API кэшируются в локальном файле (kotlinx.serialization → JSON)
- При отсутствии сети — загрузка из кэша
- TTL кэша: 2 × интервал обновления
- При успешном обновлении из сети — кэш перезаписывается

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

## TODO

### Расширенные метрики погоды
- [ ] УФ-индекс на главном экране (CurrentWeatherCard + DailyForecastCard)
- [ ] Ветер: добавить направление текстом (С/СВ/В/ЮВ/Ю/ЮЗ/З/СЗ)
- [ ] Влажность, давление — уже есть в CurrentWeatherCard

### AQI (Air Quality Index)
- [ ] `AirQualityApi.kt` — Retrofit-интерфейс для `/v1/air-quality`
- [ ] `AirQualityResponse.kt` — DTO
- [ ] Запрос AQI вместе с погодой или отдельно
- [ ] Отображение AQI на главном экране (значение + цвет: зелёный/жёлтый/оранжевый/красный/фиолетовый)
- [ ] Настройка: показывать AQI (вкл/выкл)

### Почасовой прогноз
- [ ] `HourlyForecastCard` — компонент для одной записи (время, иконка, температура, осадки)
- [ ] Горизонтальный список `LazyRow` на главном экране под CurrentWeatherCard
- [ ] Интервал отображения: каждые 3 или 6 часов (настройка)
- [ ] Адаптация под экран: скролл, если не влезает

### Избранные города
- [ ] DataStore: список избранных городов (List<String> с lat/lon)
- [ ] HomeScreen: свайп влево/право для переключения между избранными городами
- [ ] LocationSearchScreen: кнопка "Добавить в избранное" рядом с результатом
- [ ] SettingsScreen: управление списком избранных (удалить, порядок)

### Виджеты — тёмная тема + кастомизация
- [ ] ClockTempWidget: чтение `themeMode` из DataStore, применение тёмной палитры
- [ ] TempForecastWidget: то же
- [ ] Превью виджета: экран с preview + настройка цвета фона, прозрачности, цвета текста
- [ ] Сохранение настроек кастомизации в DataStore

### Кэширование
- [ ] `WeatherCache.kt` — кэш в JSON-файле в `cacheDir`
- [ ] `WeatherRepository`: при загрузке — писать в кэш, при ошибке сети — читать из кэша
- [ ] TTL кэша: `2 * updateInterval`
- [ ] Индикатор "показаны кэшированные данные" на главном экране

### Настройка интервала обновления
- [ ] DataStore: `updateIntervalHours` (2 / 12 / 24)
- [ ] SettingsScreen: выбор интервала
- [ ] При изменении — перепланировка `WeatherUpdateScheduler` (cancel + enqueue)
- [ ] UI: текущий интервал отображается в настройках

### Планшеты (адаптивный лейаут)
- [ ] `WindowSizeClass` (compact / medium / expanded)
- [ ] Главный экран: на expanded (планшет) — двухколоночный лейаут: CurrentWeatherCard слева, список прогнозов справа
- [ ] Почасовой прогноз: больше колонок на планшете
- [ ] Настройки: `NavigationRail` вместо `BottomNavigation`

### Локализация: чешский
- [ ] `values-cs/strings.xml` — перевод всех строк
- [ ] Проверка: `LanguageHelper` корректно определяет чешский

### Анимации (уже частично)
- [x] AnimatedVisibility на главном экране при появлении данных
- [ ] Плавная смена температуры (animateIntAsState)
- [ ] Анимация перехода между экранами (NavHost enterTransition/exitTransition)
- [ ] Анимированные иконки погоды (дождь, снег, солнце)

<!-- План только для локального использования. Не публиковать на GitHub. -->
```
