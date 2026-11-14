# TODO (открытые задачи и идеи)

Единственный список задач/идей. Всё, что не делается сейчас, — сюда.

## В работе

(пусто)

## Очередь

Поставлено 2026-11-13 по итогам ревизии кода. Приоритеты: P1 — инженерия, P2 — продукт/UX, P3 — рефакторинг. P0 закрыт 2026-11-14 (см. «Готово»).

### P1 — инженерия

- [x] **Обновлён стек, Android 8+ сохранён (2026-11-14).** AGP 8.7.3 → 9.5.1, Gradle 8.9 → 9.9.1, Kotlin 2.0.21 → 2.4.21, Compose BOM 2024.12.01 → 2026.09.00, Retrofit 2.11 → 3.0, OkHttp 4.12 → 5.5.1; `compileSdk = 37.2`, `targetSdk = 36`, minSdk 26 не тронут. Версии вынесены в `gradle/libs.versions.toml`, `kotlinOptions` заменён на `kotlin.compilerOptions`, `applicationVariants` — на `androidComponents.onVariants`. Сборка, тесты и lint зелёные. **Запуск на API 26 и API 36/37 не проверен** — на машине нет эмулятора и устройства. `targetSdk` 36 — отдельный шаг, поведение на старых версиях ещё предстоит проверить
- [ ] **Расширить тесты (остаток).** `NotificationHelper`; Compose UI-тесты с assert (сейчас `HomeScrollMetricsTest` только логирует метрики); screenshot-тесты (Paparazzi/Roborazzi); `@Preview` для ключевых экранов
- [ ] **Производительность (остаток).** Baseline profile; Compose stability (`@Immutable`, immutable-коллекции) по compiler reports. `isShrinkResources`, `collectAsStateWithLifecycle`, параллельный AQI — сделаны

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

- 2026-11-14 — P1: CI гоняет lint и release (R8) на каждый PR, unit-тесты перед релизом; ручной DI (`AppContainer` + `NimbusViewModelFactory`), тесты `HomeViewModel`/`LocationSearchViewModel`/`SettingsDataStore`/`WidgetPalette`; атомарная запись кэша, будильник через WorkManager, виджет не обновляется без виджетов; AQI параллельно с погодой, `collectAsStateWithLifecycle`, `isShrinkResources`
- 2026-11-14 — P0 из ревизии: кэш по координатам города (погода, AQI, виджет), `RECEIVE_BOOT_COMPLETED` для перепланирования будильника, удалены остатки точного будильника, `values-cs/language`, versionName по умолчанию без фантомной 1.1, `attachBaseContext` без `runBlocking`
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