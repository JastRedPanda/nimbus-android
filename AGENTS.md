# Nimbus Weather (nimbus-android)

Android-приложение погоды: Open-Meteo, Compose, Glance-виджет, 4 локали (RU/UK/EN/CS).

## Стек

| Компонент | Выбор |
|---|---|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Виджет | Glance (androidx.glance 1.2.0-rc01) |
| Архитектура | MVVM (ViewModel + Repository) |
| Сеть | Retrofit + OkHttp + kotlinx.serialization |
| Асинхронность | Coroutines + Flow |
| DI | Ручной (без фреймворка) |
| Настройки | DataStore Preferences |
| Фон | WorkManager |
| minSdk 26, targetSdk 35 | Релизы: постоянная debug-подпись (см. «Подводные камни»), versionCode автоинкремент, versionName вручную. Публикация — GitHub Actions на тег `v*` (`.github/workflows/release.yml`) |

## Структура пакета `com.nimbus.weather`

- `data/api` — Retrofit-интерфейсы (Weather/Geocoding/AirQuality) + `ApiClient`
- `data/model` — DTO (kotlinx.serialization)
- `data/repository` — `WeatherRepository` (погода + AQI + поиск городов), `WeatherCache` (JSON в cacheDir, TTL)
- `data/local` — `SettingsDataStore` (Preferences + JSON-списки избранных/недавних)
- `ui/{home,settings,location,onboarding,widgetcustomize,components,theme}` — экраны и компоненты; навигация NavHost прямо в `MainActivity`
- `widget` — `ClockTempWidget` (Glance) + `WidgetPalette` (фон/прозрачность/текст)
- `service` — `WeatherUpdateWorker` (WorkManager), `WeatherUpdateScheduler`, `NotificationHelper`, `WidgetUpdateManager`
- `util` — `CityNameResolver` (локализация названий городов), `LanguageHelper`, `DateTimeUtils`, `WeatherCodeUtils` (WMO), `TemperatureUtils`, `WindDirection`

## Конвенции

- Строки UI — всегда в ресурсы, все 4 локали: `values/`, `values-ru/`, `values-uk/`, `values-cs/`
- Единицы измерения (m/s, hPa, °C/°F) — в ресурсы, локализованы в каждой из 4 локалей
- Commit-стиль — Conventional Commits (feat:, fix:, docs:, refactor:)
- Вся документация (AGENTS.md, docs/) — внутренняя. На GitHub не публиковать.

## Сборка и проверка

```
gradlew.bat assembleDebug test
```

Юниты: JUnit + coroutines-test + MockK + Turbine. Сборка должна проходить без ошибок (deprecation-предупреждения допустимы).

## Подводные камни (проверено на практике)

- **Glance 1.2.0-rc01**: нет `clip`, нет `Surface` в glance-material3, `background` не принимает форму. Скругление углов — только `androidx.glance.appwidget.cornerRadius(dp)`. Детали: @docs/architecture.md
- **AQI**: отдельный Retrofit на `air-quality-api.open-meteo.com`, не на `api.open-meteo.com`
- **Уведомления**: на Android 13+ требуется `POST_NOTIFICATIONS` в манифесте + runtime-запрос (MainActivity), иначе `showWeatherNotification` тихо выходит
- **versionCode — автоинкремент**: `preBuild` в app/build.gradle.kts +1 к `app/version.properties` (gitignored) при каждой сборке; установка «поверх» работает. versionName меняется вручную при релизе (1.1, 1.2, …)
- **Релиз — только тег**: `git tag v1.6 && git push origin v1.6`; workflow собирает release-APK (`-PversionName` из тега, `-PversionCode` из счёта коммитов, имя файла — `Nimbus <версия>.apk`), подписывает постоянным ключом из `DEBUG_KEYSTORE_B64` и публикует GitHub Release. Без тега ничего не публикуется
- **Подпись НЕ менять никогда**: релизы v1.2–v1.5 подписывались разными ключами (секрет перевыставлялся) — установка «поверх» не работала. С v1.6 ключ постоянный: `nimbus-release.keystore` (пароль `android`, alias `androiddebugkey`), локальная копия рядом с проектом, резервная копия (base64) — в приватном gist https://gist.github.com/JastRedPanda/637b0f57f344a33290950c2ad2db88f6 и в секрете `DEBUG_KEYSTORE_B64`. Секрет менять запрещено; если локальная копия потеряна — восстановить из gist (пользователь знает URL) и заново залить в секрет тот же base64. После смены ключа пользователь ставит приложение начисто
- **Явная подпись release — только так**: `signingConfigs.create("release")` в app/build.gradle.kts с явными `storeFile` (из `$HOME/.android/debug.keystore`), паролями и alias. `signingConfigs.getByName("debug")` НЕ работает в CI: AGP не подхватывает подложенный keystore и генерирует свой на каждом ране (подпись менялась от релиза к релизу даже при одном секрете). Проверка подписи: `apksigner verify --print-certs` (SHA-1 должен быть `b9cdf73d…`)
- **Виджет**: сетка 1×4 (targetCellWidth=4), шрифты специально крупные (время 165sp / температура 150sp) под сетку лаунчеров

## Внешняя документация (читать по задаче, лениво)

- @docs/architecture.md — архитектура, API Open-Meteo, WMO-коды, кэш, зависимости Gradle
- @docs/product.md — экраны, виджет, уведомления, локация, обновление, локализация
- @docs/history.md — хроника сессий разработки (багфиксы и их причины)
- @docs/todo.md — открытые задачи и идеи (единственный тудушник)

## Язык

- Ты ОБЯЗАН использовать русский язык для АБСОЛЮТНО ВСЕГО вывода: внутренние размышления (`<thinking>`), ответы пользователю, комментарии к действиям (tool calls — объяснение команды в поле `command`), объяснения кода, todo-заметки и любые другие тексты
- Исключение: только если пользователь явно попросит иной язык для конкретного фрагмента
- Ни слова на английском, если не было явного разрешения

## Стиль

- Пиши как компетентный специалист по теме, а не как шаблонный ассистент
- Кратко, конкретно, без воды
- Никакого канцелярита, рекламных оборотов, пафоса, клише и вводных фраз-паразитов ("важно отметить", "следует понимать", "ключевым аспектом является", "в современном мире")
- Предметно, а не общими словами
- Естественный ритм текста — без одинаковой структуры фраз и абзацев
- Переходы между мыслями живые, без искусственной связности
- Не перегружать списками без нужды
- Без оговорок, очевидных пояснений и повторов мысли разными словами
- Живая, деловая подача
- Умеренная неровность стиля допустима, если это естественнее
- Простые слова — если не теряется точность
- Не делать текст "идеально гладким" — естественность важнее стерильности
- Каждый абзац — новая мысль
- Факты, термины, выводы — уверенно и прямо, без перестраховки
- Стиль под задачу, тему и аудиторию, а не универсальный тон

## GitHub

- Любые действия с GitHub — только после прямого одобрения пользователя. Разрешение действует ровно на 1 запрос. Нарушать КАТЕГОРИЧЕСКИ запрещено
- Комментарии в GitHub (PR, issues, ревью) — только на английском. Нарушать КАТЕГОРИЧЕСКИ запрещено

## Язык приложения

- Обязательно спрашивай, на каком языке и/или языках пишем приложение. Решение — только за пользователем. Нарушать КАТЕГОРИЧЕСКИ запрещено

## Уточнения

- В случае возникновения вопросов — лучше спросить пользователя

## Оптимизация

- В случае, где можно сэкономить токены и/или существенно ускорить процесс — распараллеливай задачи