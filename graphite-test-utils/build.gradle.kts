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

description = "Graphite Test Utils - Testing utilities for GraphQL client testing"

dependencies {
    // Core module
    api(project(":graphite-core"))

    // Testing dependencies (as API since this is a test utility library)
    api(platform(libs.junit.bom))
    api(libs.junit.jupiter.api)
    api(libs.assertj.core)
    api(libs.wiremock)

    // Testing for this module
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
