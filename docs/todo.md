# TODO (открытые задачи и идеи)

Единственный список задач/идей. Всё, что не делается сейчас, — сюда.

## В работе

(пусто)

## Очередь

Поставлено 2026-11-13 по итогам ревизии кода. Приоритеты: P0 — баги и риски, P1 — инженерия, P2 — продукт/UX, P3 — рефакторинг.

### P0 — баги и риски

- [ ] **Кэш погоды привязать к городу.** `WeatherRepository.getWeather()` при ошибке сети берёт `WeatherCache.getCachedWeather(allowExpired = true)` без проверки координат: после смены города офлайн показывается погода предыдущего. Сравнивать `WeatherResponse.latitude/longitude` (допуск ~0.01), для AQI хранить lat/lon в метаданных кэша. `WidgetUpdateManager.cachedForCity` тоже сравнивает по имени — перевести на координаты. Тест: смена города без сети.
- [ ] **Восстановление фонового обновления после перезагрузки.** `WeatherAlarmReceiver` слушает `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED`/`TIME_SET`, но в манифесте нет `RECEIVE_BOOT_COMPLETED`, а receiver объявлен `exported="false"` — системные broadcast не доходят. Решить: вернуть разрешение и `exported="true"` с проверкой action либо убрать мёртвые фильтры и зафиксировать ограничение в docs.
- [ ] **Убрать остатки точного будильника.** Точный будильник заменён на неточный (`setAndAllowWhileIdle`) ради соответствия Google Play — возвращать `SCHEDULE_EXACT_ALARM`/`canScheduleExactAlarms()` не нужно. В коде остались мёртвые `needsExactPermission()` (всегда `false`), строки `exact_alarm_*` и кнопка в настройках — удалить. Формулировки в `AGENTS.md`/`architecture.md`/`product.md` уже поправлены на «неточный будильник».
- [ ] **values-cs: добавить строку `language`.** В чешской локали 130 строк против 131 в остальных — пункт «Язык» падает в английский.
- [ ] **versionName по умолчанию.** В `app/build.gradle.kts` дефолт `"1.1"`: локальная release-сборка даёт `Nimbus 1.1.apk` при актуальной 1.48. Хранить versionName в `version.properties` или ставить `"dev"` и требовать `-PversionName` для релиза.
- [ ] **Убрать `runBlocking` из `attachBaseContext`.** На первом старте без кэша языка главный поток блокируется на чтение DataStore — риск ANR. Обойтись SharedPreferences-кэшем и фоновой миграцией.

### P1 — инженерия

- [ ] **CI: lint + release-сборка.** Добавить `lintDebug` и `assembleRelease` на PR, в `release.yml` — прогон `test` до публикации. Сейчас ошибки R8 и missing translations ловятся только на теге.
- [ ] **DI и тесты ViewModel.** `HomeViewModel` (проверить и остальные) создаёт `WeatherRepository()`/`SettingsDataStore` внутри конструктора — подменить в тестах нельзя. Ввести `AppContainer` в `Application` + фабрики ViewModel, покрыть загрузку, ошибку сети, кэш и смену языка.
- [ ] **Расширить тесты.** `SettingsDataStore` (миграции, corruption, JSON-списки), `WidgetRender`/`WidgetPalette`, `LocationSearchViewModel` (debounce/fallback), `NotificationHelper`; Compose UI-тесты с assert (сейчас `HomeScrollMetricsTest` только логирует метрики); screenshot-тесты (Paparazzi/Roborazzi); `@Preview` для ключевых экранов.
- [ ] **Производительность и размер.** Baseline profile; `isShrinkResources = true`; проверить `material-icons-extended`; AQI запрашивать параллельно с погодой (`async`); `collectAsStateWithLifecycle` вместо `collectAsState` на экранах; Compose stability (`@Immutable`, immutable-коллекции) по compiler reports.
- [ ] **Обновить стек, сохранив Android 8+ (minSdk 26).** AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, targetSdk 35 — обновить до актуальных. **minSdk 26 не поднимать**; после обновления проверить сборку и запуск на API 26 и на последнем актуальном API; новые API использовать только под `Build.VERSION.SDK_INT` guard; targetSdk повышать отдельным шагом с проверкой поведения на старых версиях. Заодно заменить deprecated `applicationVariants.all` и `kotlinOptions`, перейти на `libs.versions.toml`.
- [ ] **Надёжность кэша и фона.** Атомарная запись `WeatherCache` (temp + rename) вместо `writeText`; `WeatherAlarmReceiver` не должен тянуть сеть в `goAsync` — ставить WorkManager; `MainActivity` не должен звать `updateFromTargetCity`, когда виджетов нет.

### P2 — продукт и UX

- [ ] **Единицы.** Добавить давление в мм рт. ст. (для RU) и скорость ветра в км/ч — настройки + строки во все 4 локали.
- [ ] **Возраст кэша.** Показывать «данные за N ч назад» вместо просто «Offline cached data»; TTL сейчас не ограничивает fallback (`allowExpired = true`), может показаться кэш недельной давности.
- [ ] **Системные мелочи Android.** `localeConfig` (per-app language), `enableOnBackInvokedCallback` (predictive back), `<monochrome>` в adaptive icon, `dataExtractionRules` при `allowBackup="true"`, настроить `Theme.SplashScreen` + `setKeepOnScreenCondition` (сейчас между splash и `NavHost` возможен пустой кадр).
- [ ] **Виджет.** `resizeMode="horizontal|vertical"` (фит по высоте уже есть), `widgetFeatures="reconfigurable"`, `previewLayout`/`description` для Android 12+; монохромная small icon для уведомления вместо adaptive foreground.
- [ ] **Диагностика в настройках.** Последнее обновление, статус WorkManager, выданные разрешения, состояние будильника — чтобы баг-репорты не начинались с «не обновляется на MIUI».

### P3 — рефакторинг

- [ ] Разбить крупные файлы: `WidgetCustomizeScreen` (650 строк), `HomeScreen` (556), `SettingsScreen` (482), `SettingsDataStore` (437) — вынести секции в компоненты/сторы.
- [ ] Почистить `docs/` после правок: обновить architecture.md (стек, будильники), product.md (кэш, виджет), history.md — записью о ревизии.

## Готово

- 2026-09-30 — Фит виджета по высоте (v1.48, обрезка низа у товарища)
- 2026-09-29 — Большие иконки погоды: главная 112, дни 56, часы 32 (v1.46–v1.47)
- 2026-09-29 — Адаптивные иконки виджета + центрирование колонок + HEX-ввод (v1.46)
- 2026-09-29 — Свой цвет фона виджета (v1.45)
- 2026-09-29 — Двухстрочный виджет по референсу (v1.43); фикс слайдера шрифта, живые чипы даты (v1.44)
- 2026-08-11 — Перевод названий городов при смене языка (словарь + сетевой геокодинг, кэш в localNames)
- 2026-08-11 — Фикс кодировки strings.xml (кракозябры в v1.5/v1.6)
- 2026-08-11 — README (en + uk под спойлером)
- 2026-08-11 — Постоянная подпись релизов (SHA-1 b9cdf73d, v1.6)
- 2026-08-11 — Удаление GPS-определения города (релиз v1.5)
- 2026-08-10 — Свайп вниз (pull-to-refresh) на главном экране
- 2026-08-10 — Автоинкремент versionCode (только в большую сторону)
- 2026-08-10 — Автоматический релиз: GitHub Actions на тег `v*`