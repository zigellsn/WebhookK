/*
 * Copyright 2019-2025 Simon Zigelli
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.dokka") version "2.1.0"
    id("org.jetbrains.dokka-javadoc") version "2.1.0"
    `maven-publish`
    `java-library`
}

group = "com.github.zigellsn"
version = "3.0.3"

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()
    explicitApiWarning()
    jvmToolchain(8)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    compileTestKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc)
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("html-doc")
}

tasks.build.configure {
    dependsOn(dokkaJavadocJar, dokkaHtmlJar)
}

publishing {
    publications {
        register<MavenPublication>("library") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)
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
        }
    }
}
