plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.plugin)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin {
            exclude("**/VoskTest.kt")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.kotlinx.serialization.json)

    // Compose 1.7.0 requests coroutines 1.8.0 which is not in the offline cache; 1.9.0 is.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
    }
}

compose.desktop {
    application {
        mainClass = "com.learnagent.MainKt"
    }
}