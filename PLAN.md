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

> Статус на 2026-08-08. Завершённое помечено `[x]`.

### Расширенные метрики погоды
- [x] УФ-индекс на главном экране (CurrentWeatherCard + DailyForecastCard)
- [x] Ветер: направление текстом (С/СВ/В/ЮВ/Ю/ЮЗ/З/СЗ — 8 румбов, локализовано: RU/UK/EN/CS)
- [x] Влажность, давление — уже есть в CurrentWeatherCard

### AQI (Air Quality Index)
- [x] `AirQualityApi.kt` — Retrofit-интерфейс для `/v1/air-quality`
- [x] `AirQualityResponse.kt` — DTO
- [x] Запрос AQI вместе с погодой или отдельно
- [x] Отображение AQI на главном экране (`AqiCard`)
- [x] Настройка: показывать AQI (вкл/выкл) — DataStore `show_aqi`, SettingsScreen toggle, HomeViewModel не запрашивает AQI при выключенной настройке

### Почасовой прогноз
- [x] `HourlyForecastBar` — горизонтальный список LazyRow (время, иконка, температура, осадки)
- [x] Интервал отображения: каждые 3 или 6 часов — настройка в Settings (DataStore `hourly_interval_hours`)

### Избранные города
- [x] DataStore: `favourite_cities` (JSON-список `FavouriteCity`), add/remove/set — в коде есть
- [x] HomeScreen: свайп влево/вправо между городами (HorizontalPager + чипы сверху)
- [x] LocationSearchScreen: звёздочка «в избранное» у каждого результата (toggle)
- [x] SettingsScreen: управление списком избранных (удалить, порядок ↑/↓)

### Виджеты — кастомизация (общий стиль)
- [x] Тёмная тема из `themeMode` (системная/принудительная) — было
- [x] `WidgetPalette.kt`: резолвер фона + прозрачности + контрастного текста, применён в обоих виджетах
- [x] `WidgetCustomizeScreen`: превью, палитра 10 цветов, слайдер прозрачности, цвет текста (авто/чёрный/белый), сброс
- [x] Мгновенное сохранение в DataStore + перерисовка виджетов через `WidgetUpdateManager.refreshAllWidgets`
- [x] Вход: Настройки → Виджеты → Внешний вид

### Кэширование
- [x] `WeatherCache.kt` — кэш в JSON-файле в `cacheDir` (погода + AQI + таймстемп)
- [x] `WeatherRepository`: при ошибке сети — чтение из кэша
- [x] TTL: привязан к интервалу обновления (`2 × интервал`), `setTtlHours` вызывается в HomeViewModel.loadWeather
- [x] Индикатор "показаны кэшированные данные" на главном экране (AssistChip под шапкой, `showingCachedWeather` в репозитории)

### Настройка интервала обновления
- [x] DataStore: `updateIntervalHours` (2 / 12 / 24)
- [x] SettingsScreen: выбор интервала
- [x] Перепланировка `WeatherUpdateScheduler` (cancel + enqueue) при изменении
- [x] UI: текущий интервал отображается в настройках

### Планшеты (адаптивный лейаут)
- [x] Главный экран: на широких экранах (>= 600dp) — двухколоночный лейаут (`TabletLayout` в HomeScreen)
- [x] Доп. экраны: поиск города, настройки — одноколоночные (ок для планшетов)

### Локализация
- [x] `values-cs/strings.xml` + `values-ru`/`values-uk`/EN
- [x] Переключатель языка в настройках (авто/EN/UK/RU/CS), применяется перезапуском
- [x] Чешский в переключателе языка + `LanguageHelper.resolveLocale("cs")`
- [x] Единицы измерения (m/s, hPa, °C/°F) — из ресурсов, локализованы

### Анимации
- [x] AnimatedVisibility на главном экране при появлении данных
- [x] Плавная смена температуры (animateIntAsState, 600 мс) в CurrentWeatherCard
- [x] Анимация переходов NavHost (slide + fade, 300 мс) в MainActivity
- [x] Анимированные иконки погоды: crossfade при смене кода + пульсация масштаба для осадков/грозы (WeatherIcon)

---

## Текущая сессия (2026-08-08, после финальной сборки прошлой сессии)

> Пользовательские баги: (1) названия городов и «Киев» не локализуются при смене языка; (2) УФ-индекс частично на английском; (3) добавление в избранное неинтуитивное — нужна кнопка у поля поиска; (4) AQI не показывается вообще; (5) краш при входе в «Виджеты → Внешний вид».

### Причины (уже найдены)
- **AQI не показывается**: `AirQualityApi` создавался на `api.open-meteo.com`, а сервис живёт на `air-quality-api.open-meteo.com` → 404 → `aqi = null`. **ИСПРАВЛЕНО**: отдельный `airQualityRetrofit` на `AIR_QUALITY_BASE_URL` в ApiClient.
- **Краш «Виджеты → Внешний вид»**: конструктор `WidgetCustomizeViewModel(application, settings = ...)` — два параметра, `viewModel()` его не создаёт → RuntimeException. **ИСПРАВЛЕНО**: конструктор только `(Application)`, `settings` создаётся внутри.
- **УФ-индекс по-английски**: `formatUvIndex()` хардкодил «Low/Moderate/…». **ИСПРАВЛЕНО**: `uvCategory()` возвращает id ресурса; строки `uv_low`..`uv_extreme` в 4 локалях; формат `"7 (Высокий)"`.

### Выполнено в этой сессии
- [x] ApiClient: отдельный Retrofit для AQI (закрывает баг 4)
- [x] WidgetCustomizeViewModel: конструктор (Application) только (баг 5)
- [x] UV: строки категорий в 4 локализации + `uvCategory()` (баг 2)
- [x] **Локализация названий городов** (баг 1): `GeocodingResult.localNames` (`local_names`); `FavouriteCity`/`LocationSnapshot` + `localNames`; `setLocation(..., localNames)` + ключ `city_local_names`; `CityNameResolver.displayName()` с KNOWN_TRANSLATIONS для «Киев×Київ×Kyiv»; HomeViewModel/SettingsViewModel/LocationSearchViewModel резолвят имена (`favouriteDisplayNames`, `cityLocalNames`); HomeScreen чипы и CurrentWeatherCard, SettingsScreen список – на языке приложения
- [x] **UX избранных** (баг 3, по уточнению пользователя): звёздочка с главного экрана УБРАНА (и методы HomeViewModel.addCurrentCityToFavourites/removeFavouriteCity удалены); DataStore `KEY_RECENT_CITIES` (JSON, макс 5, новые сверху) + flow + add/remove; LocationSearchScreen: под полем поиска блок «Выбранные города» с кнопками звёздочка (избранное) и крест (удалить из истории); клик по городу БОЛЬШЕ не закрывает экран (кроме онбординга — маршрут `location_search?closeOnSelect=true`)
- [x] Строки `recent_cities_title`, `add_to_favourites`, `remove_from_history` (x4 локали)
- [x] Сборка `gradlew assembleDebug test` — OK (только deprecation-предупреждения)

### Что ещё можно сделать (вне скоупа текущей сессии)
- [x] GPS: таймзона при «Моё местоположение» — из Geocoder (`extras["timezone"]`), валидация по tzdata, fallback `Europe/Kiev`
- [ ] Планшетный `NavigationRail` для настроек (screenWidth>=600), `WindowSizeClass`

---

## Сессия 2026-08-08 (вторая волна фиксов)

### 1. Почасовка «раскадровка» на 24 часа — СДЕЛАНО
- `DEFAULT_HOURLY_INTERVAL_HOURS = 1`, `SettingsUiState.hourlyIntervalHours = 1`
- `mapHourly`: `endIndex = startIndex + 24` (1 ч → 24 карточки, 3 ч → 8, 6 ч → 4; раньше всегда 8)
- SettingsScreen: пункт `hourly_interval_1h` («Каждый час») первым, выше 3/6 ч
- Строки x4 локали

### 2. Уведомления-информер температуры — СДЕЛАНО
- `NotificationHelper.showWeatherNotification`: title `Сейчас 22°` (строка `weather_update_notification_title` с %1$d, x4), `setAutoCancel(false)` + `setOnlyAlertOnce(true)` — информер висит в шторке, обновляется без звука
- Показывается: при КАЖДОЙ загрузке главного экрана (HomeViewModel.loadWeather, если `notificationsEnabled`), фоново (Worker — было), и сразу при включении тумблера в настройках (SettingsViewModel)

### 3. Локализация городов по языку приложения (не системы) — СДЕЛАНО
- HomeViewModel.loadWeather читает язык из `settings.appLanguage.first()` (не из неинициализированного state) — убирает первый кадр-с-системным-языком
- При смене языка приложение сразу перезагружает город (collect appLanguage → loadWeather)
- Геокодинг идёт на языке приложения: `GeocodingApi.searchCities(language)` без дефолта, `WeatherRepository.searchCities(query, language)`, LocationSearchVM резолвит язык
- Fallback `CityNameResolver.KNOWN_TRANSLATIONS` (Киев/Київ/Kyiv) для городов, выбранных ещё до хранения `local_names`
- Замечание: города, сохранённые в избранном/истории ДО этого фикса, переводятся только если есть `local_names` в записи (или в KNOWN_TRANSLATIONS) — повторный выбор города в поиске записывает переводы

---

## Сессия 2026-08-08 (GPS-таймзона)

### Таймзона при «Моё местоположение» — СДЕЛАНО
- Раньше: `onMyLocationClick` писал жёстко `"Europe/Kiev"` в `setLocation`
- Теперь: `resolvePlace(lat, lon)` — один вызов Geocoder, возвращает пару (город, таймзона); таймзона читается из `Address.extras["timezone"]` (Google backend кладёт IANA-ид), валидируется по tzdata (`isValidTimeZoneId` в `DateTimeUtils.kt`, кэш `TimeZone.getAvailableIDs()`), при отсутствии — fallback `Europe/Kiev`
- Константы `Address.EXTRA_TIMEZONE_ID` в SDK нет (проверено на compileSdk 35) — строковый ключ "timezone"
- Сборка `gradlew assembleDebug test` — OK

---

## Сессия 2026-08-08 (пятая волна — багфиксы по тесту пользователя)

### Уведомление с температурой не появлялось — ПРИЧИНА + ФИКС
- В манифесте не было `POST_NOTIFICATIONS` → на Android 13+ `checkSelfPermission` возвращал denied → `showWeatherNotification` тихо выходил
- ФИКС: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` в манифест + runtime-запрос в `NimbusApp` (rememberLauncherForActivityResult, API 33+, если `notificationsEnabled`)
- На Android ≤12 канал уже создавался в MainActivity — там уведомление работало и раньше

### Второй виджет (TempForecastWidget) — УДАЛЁН
- Удалены `TempForecastWidget.kt`, `temp_forecast_widget_info.xml`, receiver из манифеста, обновление в `WidgetUpdateManager`
- Из `WidgetCustomizeScreen`: убрано превью недельного виджета, мёртвые строки `widget_*_name/desc` из 4 локалей
- Установленный ранее виджет на устройстве пропадёт после переустановки (провайдера больше нет)

### ClockTempWidget — время слева, температура справа в ЛЮБОМ размере
- Был компактный режим (ширина < 200dp): только температура. Порог снижен до 160dp; даже в маленьком размере теперь `Row` (время слева, темп справа), просто мельче шрифт (16/22sp) и без описания погоды

### Палитра виджетов — серый убран, фиолетовый — нормальный круг
- Убран `#E0E0E0` из `BG_PALETTE`
- `ColorDot`: 40dp сплошной круг, у выбранного — внутреннее белое кольцо (28dp), чтобы выбранный цвет читался на тёмных фонах

### Недельный прогноз — фактические температуры
- В `DailyForecastCard` строка «Ощущается» заменена на «Температура» с фактическими макс/мин (label `temperature` в 4 локалях); вычисление tmax/tmin вынесено на уровень карточки

### Онбординг — только выбор города
- Убраны кнопки °C/°F; `onFinish` без параметра (единицы больше не выбираются при старте)
- Текст приветствия обновлён в 4 локалях: «Выберите город, чтобы начать»

### Сборка `gradlew assembleDebug test` — OK

---

## Сессия 2026-08-08 (третья волна)

### Сброс настроек — СДЕЛАНО
- `SettingsDataStore.resetAll()` — `prefs.clear()` (тот самый файлик DataStore)
- Кнопка «Сбросить настройки» (красная OutlinedButton) внизу SettingsScreen + AlertDialog подтверждения
- After reset: перепланировка WorkManager на дефолт + перезапуск приложения (как при смене языка)
- Строки `reset_settings*`, `cancel` x4

### ClockTempWidget — время слева, погода справа — СДЕЛАНО
- Некомпактный (>200dp): `Row` время слева (weight), справа Column(temp крупно + описание мелко); компактный 1×1 — как было (только температура)

### Цвета виджетов не работали — ПРИЧИНА НАЙДЕНА И ИСПРАВЛЕНА
- `parseHex("..."/"#FFFFFF")` дважды добавлял `#` → `##FFFFFF` → `parseColor` бросал → прозрачный/дефолт. 
- Исправлено нормализацией префикса в `WidgetPalette.parseHexColor` и `WidgetCustomizeScreen.parseHex` — кружочки палитры, превью и сами виджеты теперь красятся

<!-- План только для локального использования. Не публиковать на GitHub. -->
```
