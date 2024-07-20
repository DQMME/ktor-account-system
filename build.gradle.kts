val ktorVersion: String by project
val kmongoVersion: String by project
val kotlinxDatetimeVersion: String by project
val bcryptVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    `maven-publish`
}

group = "de.dqmme"
version = "0.1.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")

    implementation("org.litote.kmongo:kmongo-coroutine-serialization:$kmongoVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

    implementation("at.favre.lib:bcrypt:$bcryptVersion")
}

val javaVersion = 17

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()

        options.release.set(javaVersion)
    }
    
    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

kotlin {
    jvmToolchain(javaVersion)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.dqmme"
            artifactId = "ktor-account-system"
            version = "0.1.2"

            from(components["java"])
        }
    }
}