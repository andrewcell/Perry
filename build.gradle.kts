import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.dokka") version "1.8.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
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
    implementation("io.netty:netty-all:4.1.96.Final")
    implementation("com.mysql", "mysql-connector-j", "8.0.33")
    implementation("org.graalvm.js", "js-scriptengine", "23.0.1")
    implementation("org.graalvm.js", "js", "23.0.1")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("io.github.microutils", "kotlin-logging", "3.0.5")
    //implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("com.microsoft.sqlserver:mssql-jdbc:12.4.0.jre11")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("com.h2database:h2:2.2.220")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
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
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
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
