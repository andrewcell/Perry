import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
    id("idea")
    id("eclipse")
    application
}

group = "me.andrew"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        name = "ktor-eap"
    }
}

val exposedVersion: String by project
val ktorVersion = "1.6.7"
dependencies {
    implementation("io.netty:netty-all:4.1.77.Final")
    implementation("mysql", "mysql-connector-java", "8.0.25")
    implementation("org.graalvm.js", "js-scriptengine", "21.2.0")
    implementation("org.graalvm.js", "js", "21.2.0")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    implementation("io.github.microutils", "kotlin-logging", "2.0.11")
    //implementation("org.slf4j:slf4j-simple:1.7.26")
    implementation("com.microsoft.sqlserver", "mssql-jdbc", "9.2.1.jre11")
    implementation("org.postgresql:postgresql:42.3.4")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("com.h2database:h2:2.1.212")
    implementation("com.beust", "klaxon", "5.5")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.236-kotlin-1.5.30")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.0.1")
    implementation("io.ktor:ktor-server-auth:2.0.1")
    implementation("io.ktor:ktor-server-core-jvm:2.0.1")
    implementation("io.ktor:ktor-server-netty-jvm:2.0.1")
    implementation("io.ktor:ktor-server-html-builder-jvm:2.0.1")
    implementation("io.ktor:ktor-server-sessions-jvm:2.0.1")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.0.1")
    dokkaGfmPlugin("org.jetbrains.dokka:jekyll-plugin:1.6.20")
    dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.6.10")
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