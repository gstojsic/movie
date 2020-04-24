import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Ver.kotlin
    kotlin("plugin.spring") version Ver.kotlin
    kotlin("kapt") version Ver.kotlin
}

group = "org.skunkworks"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)

    // configuration generator for service providers
    implementation(Libs.google_auto_service)
    kapt(Libs.google_auto_service)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
