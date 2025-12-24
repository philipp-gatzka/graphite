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

description = "Graphite Maven Plugin - Maven plugin for GraphQL code generation"

val mavenVersion = "3.9.12"
val mavenPluginToolsVersion = "3.11.0"

dependencies {
    // Codegen module
    implementation(project(":graphite-codegen"))

    // Maven Plugin API
    compileOnly("org.apache.maven:maven-plugin-api:$mavenVersion")
    compileOnly("org.apache.maven:maven-core:$mavenVersion")
    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginToolsVersion")

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.testing)
    testImplementation("org.apache.maven:maven-plugin-api:$mavenVersion")
    testImplementation("org.apache.maven:maven-core:$mavenVersion")
    testImplementation("org.apache.maven.plugin-tools:maven-plugin-annotations:$mavenPluginToolsVersion")
}
