import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version Ver.spring_boot
    id("io.spring.dependency-management") version Ver.spring_dependency_management
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
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(Libs.kotlin_stdlib)
    implementation(Libs.kotlin_reflect)
    implementation(Libs.kotlin_coroutines)
    implementation(Libs.kotlin_logging)

    implementation(project(":codegen"))
    kapt(project(":codegen"))

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
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
