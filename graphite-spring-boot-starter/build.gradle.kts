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

dependencies {
    // BOM imports
    api(platform(libs.spring.boot.bom))
    annotationProcessor(platform(libs.spring.boot.bom))

    // Core module
    api(project(":graphite-core"))

    // Spring Boot
    implementation(libs.spring.boot.autoconfigure)
    annotationProcessor(libs.spring.boot.configuration.processor)

    // Optional observability dependencies
    compileOnly(libs.spring.boot.actuator)
    compileOnly(libs.spring.boot.actuator.autoconfigure)
    compileOnly(libs.micrometer.core)
    compileOnly(libs.micrometer.tracing)

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.micrometer.core)
    testImplementation(libs.micrometer.tracing)
    testImplementation(libs.spring.boot.actuator)
    testImplementation(libs.spring.boot.actuator.autoconfigure)
}
