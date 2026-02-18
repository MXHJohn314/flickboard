import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.flickboard.emojidb)
    alias(libs.plugins.flickboard.dependencycollector)
}

val keystorePropertiesFile = rootProject.file("../flickboard.keystore.properties")
val keystoreProperties = keystorePropertiesFile
    .takeIf { it.exists() }
    ?.let { propFile ->
        Properties().also { props ->
            FileInputStream(propFile).use(props::load)
        }
    }

android {
    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        } else {
            logger.warn("$keystorePropertiesFile not found, no release signing config loaded")
        }
    }
    namespace = "se.nullable.flickboard"
    compileSdk = 36

    defaultConfig {
        applicationId = "se.nullable.flickboard"
        minSdk = 26
        targetSdk = 36
        versionCode = 107
        versionName = "0.3.2"
        // This is actually a global, but set here since it depends on the versionName
        // THIS IS NOT UPDATED FOR VARIANTS' versionNames!
        base.archivesName = "flickboard-v${versionName}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreProperties != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        create("unsignedRelease") {
            initWith(getByName("release"))
            applicationIdSuffix = ".unsigned"
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            isShrinkResources = false
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
//    testBuildType = "unsignedRelease"
    flavorDimensions += "purpose"
    productFlavors {
        create("plain") {
            dimension = "purpose"
        }
        create("beta") {
            dimension = "purpose"
        }
        create("screengrab") {
            dimension = "purpose"
            applicationIdSuffix = ".screengrab"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.autofill)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    // Should be included transitively, but doesn't actually seem to render emojis
    // without a direct dependency.
    // Emojis still not rendering properly? Try gradle clean and then rebuilding.
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.androidx.emoji2.bundled)
    implementation(libs.androidx.navigation.compose)
    // Runtime library for kotlin serialization plugin
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.fastlane.screengrab)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.runtime.ktx)
    implementation(libs.androidx.paging.compose)
}