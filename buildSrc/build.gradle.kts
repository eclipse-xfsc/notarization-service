import org.jetbrains.kotlin.konan.properties.loadProperties

val propsFile = buildFile.parentFile.parentFile.resolve("gradle.properties")
val props = loadProperties(propsFile.absolutePath)


repositories {
    gradlePluginPortal()
//	mavenCentral()
}

plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
}


val kotlinPluginVersion = "1.9.20"
val quarkusPlatformVersion: String by props
dependencies {
    implementation("org.jetbrains.kotlin.jvm", "org.jetbrains.kotlin.jvm.gradle.plugin", kotlinPluginVersion)
    implementation("org.jetbrains.kotlin.plugin.allopen", "org.jetbrains.kotlin.plugin.allopen.gradle.plugin", kotlinPluginVersion )

    implementation("io.quarkus", "io.quarkus.gradle.plugin", quarkusPlatformVersion)
    implementation("org.kordamp.gradle", "jandex-gradle-plugin", "1.1.0")

    implementation("org.myire.munge", "org.myire.munge.gradle.plugin", "1.1")
}
