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
            id = libs.plugins.flickboard.emojidb.get().pluginId
            implementationClass = "se.nullable.flickboard.build.EmojiDbGeneratorPlugin"
        }

        create("dependencyCollector") {
            id = libs.plugins.flickboard.dependencycollector.get().pluginId
            implementationClass = "se.nullable.flickboard.build.DependencyCollectorPlugin"
        }
    }
}