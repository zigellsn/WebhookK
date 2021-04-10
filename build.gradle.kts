/*
 * Copyright 2019-2020 Simon Zigelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.jvm.tasks.Jar

val kotlinVersion by extra ("1.4.32")
val ktorVersion by extra ("1.5.3")
val coroutinesVersion by extra ("1.4.3")

plugins {
    kotlin("jvm") version "1.4.32"
    kotlin("plugin.serialization") version "1.4.32"
    id("org.jetbrains.dokka") version "1.4.30"
    `maven-publish`
    `java-library`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.zigellsn"
            artifactId = "webhookk"
            version = "1.1.0-beta01"

            from(components["java"])
        }
    }
}

repositories {
    mavenCentral()
    maven(url="https://dl.bintray.com/kotlin/dokka")
    maven(url="https://dl.bintray.com/kotlin/kotlinx")
}

kotlin {
    explicitApi()
    explicitApiWarning()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    api("io.ktor:ktor-client:${ktorVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.ktor:ktor-client-mock:${ktorVersion}")
    testImplementation("io.ktor:ktor-client-mock-jvm:${ktorVersion}")
    testImplementation("com.github.marschall:memoryfilesystem:2.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${coroutinesVersion}")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}
