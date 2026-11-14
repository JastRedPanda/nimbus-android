import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 несёт встроенную поддержку Kotlin: отдельный kotlin-android
    // подключать нельзя, его применение падает с явной ошибкой.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        FileInputStream(versionPropsFile).use { load(it) }
    }
}
val localNextVersionCode: Int = (versionProps.getProperty("versionCode")?.toIntOrNull() ?: 0) + 1
val ciVersionCode: Int? = (project.findProperty("versionCode") as String?)?.toIntOrNull()
// versionName передаётся при релизе из тега. Локально берём из version.properties
// (файл gitignored), а без него ставим "dev" — чтобы сборка не выдавала
// чужую версию вроде 1.1 и не путала с релизом.
val storedVersionName: String? = versionProps.getProperty("versionName")?.takeIf { it.isNotBlank() }
val releaseVersionName: String =
    (project.findProperty("versionName") as String?)?.takeIf { it.isNotBlank() }
        ?: storedVersionName
        ?: "dev"
val userHome: String = System.getenv("HOME") ?: System.getenv("USERPROFILE") ?: "."
val releaseKeystore = file("$userHome/.android/debug.keystore")
// На чистом клоне и в PR ключа нет. Тогда release собирается unsigned:
// CI проверяет R8 и компиляцию, а подпись появляется только в release.yml,
// где keystore восстанавливается из секрета до сборки.
val hasReleaseKeystore: Boolean = releaseKeystore.exists()

android {
    namespace = "com.nimbus.weather"
    compileSdk = 37
    // Свежие AndroidX (Compose BOM 2026.09) требуют компиляции против 37,
    // но targetSdk остаётся 36: новые runtime-поведения не проверены на устройстве.
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.nimbus.weather"
        minSdk = 26
        targetSdk = 36
        versionCode = ciVersionCode ?: localNextVersionCode
        versionName = releaseVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        // mockk ставит ByteBuddy-агент в тестовую JVM. Gradle 9 запускает
        // воркер без разрешения self-attach, и без флага все тесты падают
        // на инициализации JvmMockKGateway.
        unitTests.all {
            it.jvmArgs("-Djdk.attach.allowAttachSelf=true", "-XX:+EnableDynamicAgentLoading")
        }
    }
}

// Имя APK задаётся через новый API вариантов: applicationVariants удалён в AGP 9.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val fileName =
                if (variant.buildType == "release") "Nimbus $releaseVersionName.apk" else "app-${variant.name}.apk"
            output.outputFileName.set(fileName)
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.named("preBuild") {
    doLast {
        if (ciVersionCode == null) {
            versionProps.setProperty("versionCode", localNextVersionCode.toString())
            FileOutputStream(versionPropsFile).use {
                versionProps.store(it, "Auto-incremented build number (not committed)")
            }
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Activity Compose + Lifecycle
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Core
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    // Instrumented UI metrics
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
