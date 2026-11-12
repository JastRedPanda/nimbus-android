<div align="center">

# Nimbus Weather

Free and open-source Android weather app. No ads, no trackers, no API keys.

[![API 26+](https://img.shields.io/badge/API-26%2B-3DDC84?logo=android&logoColor=white)](#install)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE)

[![Release](https://img.shields.io/github/v/release/JastRedPanda/nimbus-android?include_prereleases&logo=github)](https://github.com/JastRedPanda/nimbus-android/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/JastRedPanda/nimbus-android/android.yml?branch=main&logo=github)](https://github.com/JastRedPanda/nimbus-android/actions/workflows/android.yml)

</div>

[Features](#features) · [Install](#install) · [Build from source](#build-from-source) · [Contributing](#contributing) · [License](#license)

## About

Nimbus is built around one screen: current conditions, an hourly strip and the week at a glance. Data comes from Open-Meteo, so there is no account to create and no key to paste. The home screen widget draws time and date in the launcher process, so it keeps ticking with the app closed. Interface and saved city names are available in four languages.

## Features

- Current conditions: temperature, feels like, humidity, pressure, wind, UV index, precipitation, sunrise / sunset
- Hourly strip: time, icon and precipitation on top, temperature sparkline in the middle, temperature, wind and humidity below; 1 / 3 / 6 hour interval
- Weekly overview: seven days in one row without scrolling — day and date, icon, precipitation and high; below, low and wind speed with direction; independent sparkline bands for highs and lows
- Detailed weekly forecast behind a spoiler button: full day names, scrolling description, highs / lows, rain chance, wind, UV, sunrise / sunset
- Dynamic sky background following weather and time of day: animated rain, snow, fog, drifting clouds, lightning and stars, switchable in settings; frosted-glass cards tinted by the app theme with adaptive text
- Temperature sparklines colored from frost blue to heat red (frost threshold 0 °C)
- Day and night colored weather icons that pulse for precipitation and thunderstorms
- Air quality index (AQI) with scale marker and PM2.5 / PM10 / O₃ readings (toggleable)
- City search with debounce, favourites and search history (multilingual fallback with transliteration ranking); swipe left or right on the main screen to open the favourites picker
- Tablet-friendly layout on screens ≥ 600 dp
- Home screen widget 4×2 (RemoteViews + TextClock, ticks without the app process): current temperature with weather icon and time, feels-like/min-max with date; 6 background colors + auto + custom color dialog (tone grid + HEX input), transparency, text color, date format and font size settings
- Weather notifications (toggle)
- Theme: system / light / dark
- Background updates via WorkManager (2 / 12 / 24 h) plus an exact alarm when the system allows it, with a warning and a shortcut to system settings otherwise
- Optional "restart on close" foreground service so updates keep running after swipe-to-close
- Pull-to-refresh, offline cache and an "updated at" stamp on the main screen
- Languages: English, Ukrainian, Russian, Czech — switchable in-app, no restart; saved city names translate automatically to the chosen language
- Temperature units: °C / °F
- Material 3 design

## Install

Download the APK from [Releases](https://github.com/JastRedPanda/nimbus-android/releases). Requires Android 8.0+ (minSdk 26).

Every release is signed with the same certificate, so a new version installs over the previous one. To check a downloaded APK, run `apksigner verify --print-certs <apk>` or compare the certificate fingerprint:

```
SHA-256 5E:92:A5:4C:09:B0:73:AD:0A:72:C0:BB:A9:FE:6D:32:53:08:64:5F:E1:FB:35:C8:03:87:DA:A2:96:B9:9B:24
```

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

Single 4×2 home screen widget (four columns, two rows; classic RemoteViews): top row — current temperature with weather icon and time, bottom row — feels-like/min-max with date. Time and date are drawn by the launcher via TextClock, so they tick even with the app process dead. Adaptive font fits both width and height. Customizable background (6 colors + auto + custom color, transparency slider), text color (auto / black / white), date format and font size. Tap opens the app.

## Contributing

Bug reports and feature requests are welcome in [Issues](https://github.com/JastRedPanda/nimbus-android/issues). For code changes, open an issue first: the project has one maintainer and a deliberately narrow scope. All UI strings live in four locale folders (`values`, `values-ru`, `values-uk`, `values-cs`), commits follow Conventional Commits.

## License

Nimbus Weather is released under the [GNU General Public License v3.0](LICENSE).

---

<details>
<summary>Українською</summary>

# Nimbus Weather

Безкоштовний застосунок погоди для Android. Без реклами, без трекерів, без ключів API.

## Про застосунок

Nimbus будується навколо одного екрана: поточні умови, погодинна стрічка й тиждень одним поглядом. Дані дає Open-Meteo, тож не треба ні акаунта, ні ключа. Віджет малює час і дату в процесі лаунчера, тому цокає й із закритим застосунком. Інтерфейс і назви збережених міст доступні чотирма мовами.

## Можливості

- Поточна погода: температура, відчуття, вологість, тиск, вітер, УФ-індекс, опади, схід / захід сонця
- Погодинний прогноз: зверху час, значок і опади, посередині спарклайн температури, знизу температура, вітер і вологість; інтервал 1 / 3 / 6 год
- Тижневий огляд: сім днів в один рядок без прокрутки — день і дата, значок, опади й максимум; нижче мінімум і швидкість вітру з напрямком; окремі смуги спарклайнів для максимумів і мінімумів
- Детальний тижневий прогноз під кнопкою-спойлером: повні назви днів, опис, що біжить, макс/мін, імовірність опадів, вітер, УФ, схід / захід сонця
- Динамічне небо, що підлаштовується під погоду й час доби: анімовані дощ, сніг, туман, клуби хмар, блискавки та зорі, вимикається в налаштуваннях; матове скло карток у тоні теми застосунку з адаптивним текстом
- Спарклайни температури з кольором від морозно-синього до спекотно-червоного (поріг морозу 0 °C)
- Кольорові значки погоди для дня й ночі з пульсацією під час опадів і гроз
- Індекс якості повітря (AQI) зі шкалою та показниками PM2.5 / PM10 / O₃ (вимикається)
- Пошук міст, обране та історія пошуку (багатомовний пошук із транслітерацією); свайп ліворуч або праворуч на головному екрані відкриває вибір з обраного
- Планшетне компонування на екранах ≥ 600 dp
- Віджет на головному екрані 4×2 (RemoteViews + TextClock, цокає без процесу застосунку): поточна температура зі значком погоди й час, відчуття/мін-макс із датою; 6 кольорів фону + авто + свій колір (сітка тонів + HEX-ввід), прозорість, колір тексту, формат дати та розмір шрифту
- Сповіщення про погоду (увімкнення / вимкнення)
- Тема: системна / світла / темна
- Фонове оновлення через WorkManager (2 / 12 / 24 год) і точний будильник, коли система його дозволяє; інакше — попередження з переходом у системні налаштування
- Необов'язковий foreground-сервіс «Перезапуск при закритті», щоб оновлення тривали після свайпу в списку нещодавніх
- Оновлення свайпом униз, офлайн-кеш і мітка часу останнього оновлення на головному екрані
- Мови: українська, англійська, російська, чеська — перемикаються в застосунку без перезапуску; назви збережених міст автоматично перекладаються обраною мовою
- Одиниці температури: °C / °F
- Дизайн Material 3

## Встановлення

Завантажте APK зі [сторінки Releases](https://github.com/JastRedPanda/nimbus-android/releases). Потрібен Android 8.0+ (minSdk 26).

Кожен реліз підписано тим самим сертифікатом, тому нова версія ставиться поверх попередньої. Перевірити завантажений APK можна через `apksigner verify --print-certs <apk>` або порівнявши відбиток сертифіката:

```
SHA-256 5E:92:A5:4C:09:B0:73:AD:0A:72:C0:BB:A9:FE:6D:32:53:08:64:5F:E1:FB:35:C8:03:87:DA:A2:96:B9:9B:24
```

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

Один віджет 4×2 на головному екрані (чотири стовпці, два ряди; класичні RemoteViews): верхній рядок — поточна температура зі значком погоди й час, нижній — відчуття/мін-макс із датою. Час і дату малює лаунчер через TextClock, тож вони цокають навіть без процесу застосунку. Адаптивний шрифт під ширину й висоту. Налаштовуються фон (6 кольорів + авто + свій колір, слайдер прозорості), колір тексту (авто / чорний / білий), формат дати та розмір шрифту. Натискання відкриває застосунок.

## Долучитися

Повідомлення про помилки та пропозиції — у [Issues](https://github.com/JastRedPanda/nimbus-android/issues). Для змін у коді спершу відкрийте issue: у проєкту один супровідник і свідомо вузькі рамки. Усі рядки інтерфейсу живуть у чотирьох папках локалей (`values`, `values-ru`, `values-uk`, `values-cs`), коміти — у стилі Conventional Commits.

## Ліцензія

Nimbus Weather поширюється під [GNU General Public License v3.0](LICENSE).

</details>
