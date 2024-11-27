
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.dokka") version "1.9.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("idea")
    id("eclipse")
    id("com.adarshr.test-logger") version "4.0.0"
    application
}

group = "me.andrew"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val exposedVersion: String by project
val ktorVersion: String by project

dependencies {
    implementation("io.netty:netty-all:4.1.106.Final")
    implementation("com.mysql", "mysql-connector-j", "8.0.33")
    implementation("org.graalvm.js", "js-scriptengine", "23.0.1")
    implementation("org.graalvm.js", "js", "23.0.1")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("io.github.microutils", "kotlin-logging", "3.0.5")
    //implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.5.0.jre11-preview")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("com.h2database:h2:2.2.220")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    dokkaGfmPlugin("org.jetbrains.dokka:jekyll-plugin:1.8.20")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.8.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.1.0")
    // https://mvnrepository.com/artifact/io.github.oshai/kotlin-logging-jvm
    //runtimeOnly("io.github.oshai:kotlin-logging:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                DokkaConfiguration.Visibility.PUBLIC,
                DokkaConfiguration.Visibility.PROTECTED,
                DokkaConfiguration.Visibility.INTERNAL,
                DokkaConfiguration.Visibility.PRIVATE
            )
        )
    }
}

testlogger {
    theme = ThemeType.MOCHA
    showExceptions = true
    showStackTraces = true
    showFullStackTraces = false
    showCauses = true
    slowThreshold = 2000
    showSummary = true
    showSimpleNames = false
    showPassed = true
    showSkipped = true
    showFailed = true
    showOnlySlow = false
    showStandardStreams = false
    showPassedStandardStreams = true
    showSkippedStandardStreams = true
    showFailedStandardStreams = true
    logLevel = LogLevel.LIFECYCLE
}

