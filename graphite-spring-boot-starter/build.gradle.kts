/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("graphite.java-conventions")
    id("graphite.publish-conventions")
}

description = "Graphite Spring Boot Starter - Auto-configuration for Spring Boot applications"

// Support for Spring Boot version override via Gradle property
val springBootVersion: String by project

val springBootBom = if (project.hasProperty("springBootVersion")) {
    "org.springframework.boot:spring-boot-dependencies:$springBootVersion"
} else {
    libs.spring.boot.bom.get().toString()
}

dependencies {
    // API dependencies
    api(platform(springBootBom))
    api(project(":graphite-core"))

    // Implementation dependencies
    implementation(libs.spring.boot.autoconfigure)

    // Compile-only dependencies
    compileOnly(libs.spring.boot.actuator)
    compileOnly(libs.spring.boot.actuator.autoconfigure)
    compileOnly(libs.micrometer.core)
    compileOnly(libs.micrometer.tracing)

    // Annotation processors
    annotationProcessor(platform(springBootBom))
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Test dependencies
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.micrometer.tracing)
    testImplementation(libs.spring.boot.actuator)
    testImplementation(libs.spring.boot.actuator.autoconfigure)
}
