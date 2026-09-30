# Nimbus Weather

Free and open-source Android weather app. No ads, no trackers, no API keys.

## Features

- Current weather, hourly strip and 7-day forecast: temperature, feels like, humidity, pressure, wind, UV index, precipitation, sunrise / sunset
- Dynamic sky background following weather and time of day; frosted-glass cards tinted by the app theme with adaptive text
- Hourly temperature sparkline scrolling together with the strip, colored from frost blue to heat red (frost threshold 0 °C)
- Air quality index (AQI) with scale marker and PM2.5 / PM10 / O₃ readings (toggleable)
- City search with debounce, favourites and search history (multilingual fallback with transliteration ranking)
- Tablet-friendly layout on screens ≥ 600 dp
- Home screen widget 2×4 (RemoteViews + TextClock, ticks without the app process): current temperature with weather icon and time, feels-like/min-max with date; 6 background colors + auto + custom color dialog (tone grid + HEX input), transparency, text color, date format and font size settings
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
- WorkManager (background updates)
- DataStore Preferences
- Coroutines + Flow

## Widget

Single 2×4 home screen widget (classic RemoteViews; time and date are drawn by the launcher via TextClock, so they tick even with the app process dead): top row — current temperature with weather icon and time, bottom row — feels-like/min-max with date. Adaptive font fits both width and height. Customizable background (6 colors + auto + custom color, transparency slider), text color (auto / black / white), date format and font size. Tap opens the app.

---

<details>
<summary>Українською</summary>

# Nimbus Weather

Безкоштовний застосунок погоди для Android. Без реклами, без трекерів, без ключів API.

## Можливості

- Поточна погода, погодинний прогноз і прогноз на 7 днів: температура, відчуття, вологість, тиск, вітер, УФ-індекс, опади, схід / захід сонця
- Динамічне небо, що підлаштовується під погоду й час доби; матове скло карток у тоні теми застосунку з адаптивним текстом
- Спарклайн температури в погодинному прогнозі, що прокручується разом зі стрічкою, колір від морозно-синього до спекотно-червоного (поріг морозу 0 °C)
- Індекс якості повітря (AQI) зі шкалою та показниками PM2.5 / PM10 / O₃ (вимикається)
- Пошук міст, обране та історія пошуку (багатомовний пошук із транслітерацією)
- Планшетне компонування на екранах ≥ 600 dp
- Віджет на головному екрані 2×4 (RemoteViews + TextClock, цокає без процесу застосунку): поточна температура зі значком погоди й час, відчуття/мін-макс із датою; 6 кольорів фону + авто + свій колір (сітка тонів + HEX-ввід), прозорість, колір тексту, формат дати та розмір шрифту
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
- WorkManager (фонові оновлення)
- DataStore Preferences
- Coroutines + Flow

## Віджет

Один віджет 2×4 на головному екрані (класичні RemoteViews; час і дату малює лаунчер через TextClock, тож вони цокають навіть без процесу застосунку): верхній рядок — поточна температура зі значком погоди й час, нижній — відчуття/мін-макс із датою. Адаптивний шрифт під ширину й висоту. Налаштовуються фон (6 кольорів + авто + свій колір, слайдер прозорості), колір тексту (авто / чорний / білий), формат дати та розмір шрифту. Натискання відкриває застосунок.

</details>
