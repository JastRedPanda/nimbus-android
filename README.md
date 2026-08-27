# Nimbus Weather

Free and open-source Android weather app. No ads, no trackers, no API keys.

## Features

- Current weather, hourly strip and 7-day forecast: temperature, feels like, humidity, pressure, wind, UV index, precipitation, sunrise / sunset
- Air quality index (AQI) with six components
- City search with debounce, favourites and recent cities
- Tablet-friendly layout on screens ≥ 600 dp
- Home screen widget 1×4 (Glance): city time + date + temperature, 9 background colors + auto, transparency and text color settings
- Weather notifications (toggle)
- Theme: system / light / dark
- Background updates via WorkManager (2 / 12 / 24 h)
- Pull-to-refresh and offline cache
- Languages: English, Ukrainian, Russian, Czech — switchable in-app, no restart; saved city names translate automatically to the chosen language
- Temperature units: °C / °F
- Material 3 design

## Install

Download the APK from [Releases](https://github.com/JastRedPanda/nimbus-android/releases). Requires Android 8.0+ (minSdk 26).

## Build from source

Requirements: JDK 17, Android SDK (compileSdk 35).

```
gradlew.bat assembleDebug
gradlew.bat test
```

Open the repository in Android Studio and run the `app` module.

## Data source

All data comes from [Open-Meteo](https://open-meteo.com): weather forecasts, geocoding and air quality. Free of charge, no registration or API key required. The app resolves the IANA time zone for each selected city.

## Tech stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM (ViewModel + Repository)
- Retrofit + OkHttp + kotlinx.serialization
- Glance (Android widgets)
- WorkManager (background updates)
- DataStore Preferences
- Coroutines + Flow

## Widget

Single 1×4 home screen widget: large city time on the left (with date, text or numeric format), temperature on the right. Customizable background (9 colors + auto, transparency slider) and text color (auto / black / white). Tap opens the app.

---

<details>
<summary>Українською</summary>

# Nimbus Weather

Безкоштовний застосунок погоди для Android. Без реклами, без трекерів, без ключів API.

## Можливості

- Поточна погода, погодинний прогноз і прогноз на 7 днів: температура, відчуття, вологість, тиск, вітер, УФ-індекс, опади, схід / захід сонця
- Індекс якості повітря (AQI) з шістьма компонентами
- Пошук міст, обране та нещодавні міста
- Планшетне компонування на екранах ≥ 600 dp
- Віджет на головному екрані 1×4 (Glance): час міста + дата + температура, 9 кольорів фону + авто, налаштування прозорості та кольору тексту
- Сповіщення про погоду (увімкнення / вимкнення)
- Тема: системна / світла / темна
- Фонове оновлення через WorkManager (2 / 12 / 24 год)
- Оновлення свайпом униз і офлайн-кеш
- Мови: українська, англійська, російська, чеська — перемикаються в застосунку без перезапуску; назви збережених міст автоматично перекладаються обраною мовою
- Одиниці температури: °C / °F
- Дизайн Material 3

## Встановлення

Завантажте APK зі [сторінки Releases](https://github.com/JastRedPanda/nimbus-android/releases). Потрібен Android 8.0+ (minSdk 26).

## Збірка з вихідного коду

Вимоги: JDK 17, Android SDK (compileSdk 35).

```
gradlew.bat assembleDebug
gradlew.bat test
```

Відкрийте репозиторій в Android Studio та запустіть модуль `app`.

## Джерело даних

Усі дані надходять з [Open-Meteo](https://open-meteo.com): прогноз погоди, геокодинг та якість повітря. Безкоштовно, без реєстрації та ключів API. Для кожного вибраного міста застосунок визначає часовий пояс (IANA).

## Технології

- Kotlin
- Jetpack Compose + Material 3
- MVVM (ViewModel + Repository)
- Retrofit + OkHttp + kotlinx.serialization
- Glance (віджети Android)
- WorkManager (фонові оновлення)
- DataStore Preferences
- Coroutines + Flow

## Віджет

Один віджет 1×4 на головному екрані: великий час міста зліва (з датою, текстовим або числовим форматом), температура справа. Налаштовуються фон (9 кольорів + авто, слайдер прозорості) та колір тексту (авто / чорний / білий). Натискання відкриває застосунок.

</details>
