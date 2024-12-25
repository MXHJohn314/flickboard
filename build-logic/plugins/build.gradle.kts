plugins {
    `java-gradle-plugin`
    val kotlinVersion = "2.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

repositories {
    google()
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation("com.android.tools.build:gradle-api:8.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

gradlePlugin {
    plugins {
        create("emojiDb") {
            id = "se.nullable.flickboard.build.emojidb"
            implementationClass = "se.nullable.flickboard.build.EmojiDbGeneratorPlugin"
        }
    }
}