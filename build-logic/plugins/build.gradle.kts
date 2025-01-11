plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
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
    implementation(libs.gradle.api)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        create("emojiDb") {
            id = "se.nullable.flickboard.build.emojidb"
            implementationClass = "se.nullable.flickboard.build.EmojiDbGeneratorPlugin"
        }
    }
}