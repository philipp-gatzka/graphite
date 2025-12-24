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
    `java-gradle-plugin`
}

description = "Graphite Gradle Plugin - Gradle plugin for GraphQL code generation"

dependencies {
    // Codegen module
    implementation(project(":graphite-codegen"))

    // Gradle API is provided by java-gradle-plugin

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("graphite") {
            id = "io.github.graphite"
            displayName = "Graphite GraphQL Plugin"
            description = "Generates type-safe Java code from GraphQL schema"
            implementationClass = "io.github.graphite.gradle.GraphitePlugin"
            tags.set(listOf("graphql", "codegen", "type-safe"))
        }
    }
}

// Disable JaCoCo coverage verification for plugin module
// TestKit integration tests run in separate Gradle processes and don't contribute to coverage
// The unit tests still run and coverage is reported, but the threshold is not enforced
tasks.named("jacocoTestCoverageVerification") {
    enabled = false
}
