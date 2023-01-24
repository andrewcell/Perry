import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version "1.7.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
    id("idea")
    id("eclipse")
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
    implementation("io.netty:netty-all:4.1.86.Final")
    implementation("mysql", "mysql-connector-java", "8.0.31")
    implementation("org.graalvm.js", "js-scriptengine", "22.3.0")
    implementation("org.graalvm.js", "js", "22.3.0")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("io.github.microutils", "kotlin-logging", "3.0.4")
    //implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.1.0.jre11-preview")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("org.xerial:sqlite-jdbc:3.40.0.0")
    implementation("com.h2database:h2:2.1.214")
    implementation("com.beust", "klaxon", "5.6")
    implementation("ch.qos.logback:logback-classic:1.4.5")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.2")
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
    dokkaGfmPlugin("org.jetbrains.dokka:jekyll-plugin:1.7.20")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClassName = "MainKt"
}

tasks.dokkaHtml.configure {
    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(true)
            samples.from("src/test/kotlin/Sample.kt")
        }
    }
}