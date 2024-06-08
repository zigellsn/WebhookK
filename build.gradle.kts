/*
 * Copyright 2019-2024 Simon Zigelli
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
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    alias(libs.plugins.dokka)
    `maven-publish`
    `java-library`
}

group = "com.github.zigellsn"
version = "2.0.6"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/kotlin/dokka")
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

kotlin {
    explicitApi()
    explicitApiWarning()
    jvmToolchain(8)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", libs.versions.kotlin.get()))
    api(libs.kotlin.coroutine)
    api(libs.ktor.client)
    implementation(libs.kotlin.serialization)

    testImplementation(libs.test.kotlin)
    testImplementation(libs.test.ktor.mock)
    testImplementation(libs.test.ktor.mockjvm)
    testImplementation(libs.test.memoryfilesystem)
    testImplementation(libs.test.kotlin.coroutine)
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(layout.buildDirectory.dir("javadoc"))
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

tasks.build.configure {
    dependsOn(dokkaJar)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(dokkaJar)
            pom {
                name.set("WebhookK")
                description.set("A Kotlin webhook provider")
                url.set("https://github.com/zigellsn/WebhookK/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("zigellsn")
                        name.set("Simon Zigelli")
                        email.set("zigellsn@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/zigellsn/WebhookK.git")
                    developerConnection.set("scm:git:ssh://github.com/zigellsn/WebhookK.git")
                    url.set("https://github.com/zigellsn/WebhookK/")
                }
            }
            from(components["java"])
        }
    }
}