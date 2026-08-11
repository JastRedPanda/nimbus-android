# История сессий разработки Nimbus Weather

Хроника багфиксов и решений с причинами. Читать при работе с соответствующими компонентами.

## Сессия 2026-08-08 — первая волна (текущая на старте)

- **AQI не появлялся на главном экране**: AQI запрашивался с `api.open-meteo.com` → 404 (хосты Open-Meteo раздельные). Фикс: отдельный Retrofit на `air-quality-api.open-meteo.com` в `ApiClient` (константа `AIR_QUALITY_BASE_URL`)
- **Краш при пустом/неверном заряде батареи** (`NumberFormatException` из-за зарядки `"None"`/`"null"` и fallback-значения `""`): фикс — `isNotEmpty().toIntOrNull()` в `Util.batteryLevel`; строка ресурса; мин-ширина батареи 100dp (под широкий текст в чешской локали)
- **Сброс УФ-индекса при движении пальца по слайдеру** (Slider отдаёт дробные значения кванта 0.2, RangeSlider отдавал дробные промежуточные состояния): фикс — принудительный каст в целое при сбросе; сброс на начальное состояние (1) — «по шкале первых», никаких значений "None"/0
- **УФ-индекс частично на английском**: `formatUvIndex()` хардкодил «Low/Moderate/…». Фикс — `uvCategory()` возвращает id ресурса, строки `uv_low`..`uv_extreme` в 4 локалях, формат «7 (Высокий)»
- **Краш «Виджеты → Внешний вид»**: конструктор `WidgetCustomizeViewModel(application, settings = ...)` — два параметра, `viewModel()` его не создаёт → RuntimeException. Фикс — конструктор только `(Application)`, `settings` создаётся внутри
- **Названия городов не локализовались в настройках**: фикс — `CityNameResolver` с 3 активными локалями (RU/UK/CS); geocoding `local_names` (по языку приложения) + ручные словари; фолбэк на name API. Запрос к geocoding без языка — fallback EN (имена по-английски, если не найдено)
- **UX избранных городов**: звёздочка с главного экрана убрана (методы addCurrentCityToFavourites/removeFavouriteCity удалены); недавние города — `KEY_RECENT_CITIES` (JSON, max 5, новые сверху); под поиском блок «Выбранные города»: звёздочка — в избранное, крест — удалить из истории; клик по городу больше не закрывает экран (кроме онбординга — маршрут `location_search?closeOnSelect=true`); длинное нажатие в списке — меню (переименовать/удалить); перетаскивание при edit-mode (LongPressDragAndDropSource); пустое состояние с кнопкой «Найти город»
- **Экран истории погоды удалён** как лишний (вместо полноценного History остался небольшой History-экран внутри приложения, показ последних 14 обновлений)
- **Цвета онбординга согласованы с темой приложения**: первая версия была «мышино-серая»; полигонов градиент, насыщенность 0.65–0.85, тень 0.3 — тон как у главного экрана

## Сессия 2026-08-08 — вторая волна

- Почасовой прогноз — раскадровка на 24 часа: `DEFAULT_HOURLY_INTERVAL_HOURS = 1`, `mapHourly`: `endIndex = startIndex + 24` (1 ч → 24 карточки, 3 ч → 8, 6 ч → 4; раньше всегда 8); в настройках пункт «Каждый час» первым, выше 3/6 ч
- **Уведомления**: информер (title «Сейчас 22°» из `weather_update_notification_title` с %1$d, x4 локали; `setAutoCancel(false)` + `setOnlyAlertOnce(true)` — висит в шторке, обновляется без звука); настройка интервала уведомлений (15, 30 мин, 1/2/3/6 ч); показ при КАЖДОЙ загрузке главного экрана (HomeViewModel.loadWeather), фоново (Worker), при включении тумблера (SettingsViewModel)
- **Локализация названий городов по языку приложения** (не системы): язык читается из `settings.appLanguage.first()` (не из неинициализированного state — убирает первый кадр с системным языком); при смене языка — сразу reload города (collect appLanguage → loadWeather); геокодинг на языке приложения (`GeocodingApi.searchCities(language)` без дефолта, `WeatherRepository.searchCities(query, language)`); fallback `CityNameResolver.KNOWN_TRANSLATIONS` (Киев/Київ/Kyiv); замечание: города, сохранённые в избранном/истории ДО фикса, переводятся только если есть `local_names` в записи (или в KNOWN_TRANSLATIONS) — повторный выбор города в поиске записывает переводы

## Сессия 2026-08-08 — третья волна

- **Сброс настроек**: `SettingsDataStore.resetAll()` (prefs.clear()); кнопка «Сбросить настройки» (красная OutlinedButton) внизу SettingsScreen + AlertDialog подтверждения; после сброса — перепланировка WorkManager на дефолт и перезапуск приложения (как при смене языка)
- **ClockTempWidget — время слева**: некомпактный (>200dp): `Row` время слева (weight), справа Column (температура крупно + описание мелко); компактный 1×1 — только температура
- **Цвета виджета не работали**: `parseHex("..."/"#FFFFFF")` дважды добавлял `#` → `##FFFFFF` → `parseColor` бросал → прозрачный/дефолт. Исправлено нормализацией префикса в `WidgetPalette.parseHexColor` и `WidgetCustomizeScreen.parseHex` — кружочки палитры, превью и сами виджеты теперь красятся
- **Чешская локаль**: «Вам осадки больше не нужны» — обновление переводов

## Сессия 2026-08-08 — четвёртая волна (GPS-таймзона)

- `onMyLocationClick` больше не пишет жёстко `Europe/Kiev`: `resolvePlace(lat, lon)` одним вызовом Geocoder достаёт и город, и IANA-таймзону из `Address.extras["timezone"]`
- Валидация: `DateTimeUtils.isValidTimeZoneId` (кэш `TimeZone.getAvailableIDs()`), fallback `Europe/Kiev`
- В SDK нет константы `Address.EXTRA_TIMEZONE_ID` (проверено на compileSdk 35) — используется строковый ключ `"timezone"`

## Сессия 2026-08-08 — пятая волна (багфиксы по тесту пользователя)

- **Уведомление не появлялось**: в манифесте не было `POST_NOTIFICATIONS` → на Android 13+ `checkSelfPermission` молча выходил. Добавлены permission и runtime-запрос (rememberLauncherForActivityResult) в MainActivity; при отказе (Prevent) уведомление не показывается
- **Второй виджет удалён**: `TempForecastWidget` (класс, receiver в манифесте, `xml`, вызовы из `WidgetUpdateManager`, превью) — пользователь хотел один виджет
- **ClockTempWidget** компакт: время слева, температура справа — в любом размере; порог 200→160dp; в маленьком размере мельче шрифт (36/44sp), без описания погоды
- **Палитра виджетов**: убран серый `#E0E0E0` из `BG_PALETTE`; `ColorDot` — сплошной круг 40dp + у выбранного внутреннее белое кольцо (28dp), чтобы цвет читался на тёмных фонах
- **Недельный прогноз**: «Ощущается» → фактические макс/мин, подпись «Температура» (новая строка `temperature` в 4 локалях); уточнены значения AQI (форс-индекс европейской шкалы, столбцы гистограммы)
- **Онбординг**: только выбор города (убран выбор °C/°F), `onFinish()` без параметра

## Сессия 2026-08-08 — шестая волна (UI-фиксы по тесту пользователя)

- **Недельный прогноз — выравнивание**: первая попытка центрирования пар ячеек (`weight(1f)` + Center) — пользователю не понравилось («поблочно, а нужно построчно»); `DailyDetailItem` получил `align`: левая ячейка `Alignment.Start`, правая `Alignment.End` (значение и подпись прижаты к краю карточки), обе `weight(1f)`
- **Виджет 1×4**: `minWidth=250dp`, `minHeight=40dp`, `targetCellWidth=4`, `targetCellHeight=1`
- **Шрифты виджета**: время 165sp, температура 150sp (пользователь: «не меньше 65sp» — позже увеличены до 165/150); компакт (<160dp): 36/44sp
- **Описание погоды из виджета убрано** (не помещалось в строку)
- **Скругление 16dp**: в Glance 1.2.0-rc01 нет `clip`, `Surface` (glance-material3) и `background(цвет, форма)` — проверено по AAR; единственный путь `androidx.glance.appwidget.cornerRadius(16.dp)`
- **versionCode = 1 зашит** в build.gradle.kts: установка APK «поверх» молча не работает (система не видит обновления). Тестирование — полной переустановкой. Автоинкремент versionCode предложен, пользователь пока отклонил.
- Превью виджета в настройках синхронизировано (Row: время слева / темп справа)
- Сборка `gradlew assembleDebug` — OK

## Сессия 2026-08-10 — pull-to-refresh

- **Принудительное обновление по свайпу вниз**: `PullToRefreshBox` (material3 1.3.1) вокруг контента главного экрана; `HomeViewModel.refresh()` — отдельный флаг `refreshing` в `HomeUiState`, не трогает полноэкранный `loading`; общий `performLoad()` для `loadWeather()` и `refresh()`; повторный `refresh()` при активном обновлении игнорируется
- Экран ошибки получил `verticalScroll` — свайп-вниз работает и там (как retry)
- Из тудушника удалены отложенные: планшетный NavigationRail, дата в виджете (product.md «Открытые задачи» + history.md «Вне скоупа»)

## Сессия 2026-08-10 — релиз 1.1

- **versionCode — автоинкремент «только в большую сторону»**: `preBuild` в app/build.gradle.kts читает `app/version.properties` (gitignored, `Properties`), +1 при каждой сборке и пишет обратно; `versionCode = nextVersionCode` в defaultConfig. Первый APK собрался с versionCode 1 (не встал бы поверх старого) — пересборка дала 2
- **Release подписывается debug-ключом**: `signingConfig = signingConfigs.getByName("debug")` в release buildType (раньше собрался бы APK без подписи)
- **versionName "1.1"**, впредь меняется вручную (1.2, 1.3, …)
- GitHub Release `v1.1` с APK (проверено: `aapt dump badging` — versionCode 2 / versionName 1.1; `apksigner verify` — Android Debug V2)

## Сессия 2026-08-10 — авторелиз (CI)

- **`.github/workflows/release.yml`**: пуш тега `v*` → `-PversionName` из тега (v1.2 → 1.2), `-PversionCode` из `git rev-list --count HEAD` (монотонно, без локального version.properties); APK подписывается debug-ключом из секрета `DEBUG_KEYSTORE_B64` (base64 локального `~/.android/debug.keystore` — подпись совпадает с той, что уже на телефоне) и публикуется в GitHub Release (`--generate-notes`)
- **Gradle**: `-PversionCode`/`-PversionName` переопределяют defaultConfig; при `-PversionCode` preBuild не инкрементит локальный файл. Проверено: `assembleRelease -PversionName=9.99 -PversionCode=777` → badging показывает 777/9.99
- **Имя APK**: `applicationVariants` + `BaseVariantOutputImpl.outputFileName` — release собирается как `Nimbus.apk` (debug остаётся `app-debug.apk`), workflow указывает новый путь
- **Имя APK с версией**: release → `Nimbus <versionName>.apk` (например `Nimbus 1.3.apk`), версия подставляется из `-PversionName` (в CI — из тега); локальная release-сборка без `-P` получит дефолтный дефолт `Nimbus 1.1.apk`

## Сессия 2026-08-11 — GPS-фикс

- **«Моя локация» крутился и падал в ошибку**: ждали только `PRIORITY_HIGH_ACCURACY` 30 сек (нужны спутники, в помещении/без GPS не даёт фикса), а fallback `lastLocation` почти всегда пуст → до ~40 сек ожидания и глухой тост «Не удалось получить местоположение»
- **Фикс**: проверка `LocationManager.isProviderEnabled(GPS/NETWORK)` до запроса — при выключенной геолокации мгновенный тост `gps_disabled` («Геолокация выключена…», x4 локали) вместо крутилки; координаты: `lastLocation` (8с) → `BALANCED_POWER_ACCURACY` (12с) → `HIGH_ACCURACY` (15с); причины логируются `Log.w(TAG)`
- **Лог в доступное место**: `util/LocationLog` пишет построчно в `Download/nimbus_location.log` (MediaStore, API 29+; на API 26–28 — внешний каталог приложения, обрезка 200 строк) — состояние провайдеров, каждый шаг каскада, итог; в logcat тоже дублируется

## Сессия 2026-08-11 — удаление GPS (релиз 1.5)

- **GPS удалён полностью** по решению пользователя: `LocationLog.kt`, `onMyLocationClick`, каскад координат, `resolvePlace`/Geocoder, тосты (`gps_no_signal` и др.), кнопка «Моя локация» (LocationSearch и онбординг), `ACCESS_FINE/COARSE_LOCATION` из манифеста, строки `use_gps`/`gps_*` из 4 локалей, зависимость `play-services-location` (21.3.0)
- Дефолтный город — Киев (50.4501, 30.5234, `Europe/Kiev`); таймзона города — из ответа геокодинга Open-Meteo (поле `timezone`), валидация `DateTimeUtils.isValidTimeZoneId`
- Релиз **v1.5** (`18b3890`)

## Сессия 2026-08-11 — постоянная подпись (релиз 1.6)

- **Установка «поверх» не работала между релизами**: v1.2 `cb08fb2e`, v1.3 `22f0c113`, v1.4 `8320f624`, v1.5 `f7ec9e86` — каждый подписан своим ключом (секрет `DEBUG_KEYSTORE_B64` перевыставлялся, но подписи не совпадали)
- Создан постоянный ключ `nimbus-release.keystore` (пароль `android`, alias `androiddebugkey`, SHA-1 `b9cdf73d…`, SHA-256 `5E:92:A5:4C:…`); base64-копия — в секрете `DEBUG_KEYSTORE_B64` и в приватном gist https://gist.github.com/JastRedPanda/637b0f57f344a33290950c2ad2db88f6 (сверено побайтово); локальные копии — в .gitignore
- **Первый v1.6 оказался подписан чужим ключом** (`89b55b51` в CI): `signingConfigs.getByName("debug")` игнорировал подложенный keystore — AGP на каждом ране генерировал свой debug-ключ (поэтому подписи различались и раньше). Диагностика через keytool в workflow подтвердила, что keystore в CI правильный, а подпись — нет
- **Фикс**: явная `signingConfigs.create("release")` в app/build.gradle.kts (`storeFile = $HOME/.android/debug.keystore`, пароль/alias `android`/`androiddebugkey`); тег v1.6 пересоздан на `8a1cb2c`, workflow зелёный, подпись `b9cdf73d…` подтверждена apksigner на скачанном APK
- Правило «подпись не менять никогда» + восстановление из gist — зафиксированы в AGENTS.md

## Сессия 2026-08-11 — README

- Двуязычный `README.md`: английский + украинский под `<details>`-спойлером; фичи, установка (с v1.6 обновление поверх), сборка, Open-Meteo, стек, виджет (время+дата+температура, палитры, прозрачность), планшетный лейаут
- Актуализация docs/: product.md (убраны GPS/FAB/placeholder-виджета/интервал уведомлений/переименование городов; добавлены дата виджета, адаптивные шрифты, тема, режим температуры виджета), architecture.md (убрана play-services-location, обновлены подводные камни), history.md