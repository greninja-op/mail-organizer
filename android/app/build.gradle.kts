import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Room schema export (Phase 2): keeps every DB version's schema JSON under
// app/schemas so migrations stay reviewable and testable.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.greninjaop.mailorganizer"
    // compileSdk 35: verified against AAR metadata; the newest AndroidX
    // releases need SDK 36/37 which are not yet usable in this environment.
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.greninjaop.mailorganizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        // Phase 4 Gmail synchronization engine. Versioning scheme (semantic) lands in Phase 29.
        versionName = "0.1.0-phase4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<Test> {
    maxHeapSize = "1g"
    testLogging {
        events("passed", "failed", "skipped")
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Coroutines — async foundation for sync, DB and UI state (Phase 0+).
    implementation(libs.kotlinx.coroutines.android)

    // Room — KMP-compatible SQLite persistence. The local database is the
    // rebuildable intelligence layer (Phase 2 expands the schema).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences — app settings and (later) account-scoped prefs.
    implementation(libs.androidx.datastore.preferences)

    // Compose (versions managed by the Compose BOM)
    val bom = libs.compose.bom
    implementation(platform(bom))
    androidTestImplementation(platform(bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    // ---- Test foundation (all actually exercised in Phase 0) ----
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine) // Flow testing
    testImplementation(libs.robolectric) // JVM integration tests (Room)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core) // ApplicationProvider for Robolectric

    debugImplementation(libs.androidx.compose.ui.tooling)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
