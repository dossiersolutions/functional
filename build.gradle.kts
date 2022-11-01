group = "no.dossier.libraries"
version = 0.1

repositories {
    mavenCentral()
}

plugins {
    id("org.gradle.maven-publish") // no version spec needed (Gradle built-in)
    kotlin("multiplatform") version "1.7.20"
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.testcontainers:junit-jupiter:1.16.2")
                implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
            }
        }
    }
}

publishing {
    repositories {
        mavenCentral()
    }
}