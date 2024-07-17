val ktorVersion: String by project
val kmongoVersion: String by project
val kotlinxDatetimeVersion: String by project
val bcryptVersion: String by project

plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("maven-publish")
}

group = "de.dqmme"
version = "1.0"

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            groupId = "de.dqmme"
            artifactId = "ktor-account-system"
            version = "1.1.1"

            pom {
                name.set("Ktor Account System")
                description.set("Small Ktor Account System")
                url.set("https://github.com/dqmme/ktor-account-system")
                developers {
                    developer {
                        id.set("dqmme")
                        name.set("Dominic H")
                        email.set("business@dqmme.de")
                    }
                }
            }
        }
    }
}