// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("androidx.room") version "2.6.1" apply false

    val kotlinVersion = "2.1.0"
    kotlin("android") version kotlinVersion apply false
    kotlin("plugin.compose") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
    id("com.google.devtools.ksp") version "$kotlinVersion-1.0.29" apply false
}