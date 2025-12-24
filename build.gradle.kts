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

/**
 * Root build file for the Graphite project.
 *
 * <p>Graphite is a type-safe GraphQL client library for Spring Boot that provides:
 * <ul>
 *   <li>Code generation from GraphQL schema</li>
 *   <li>Type-safe query builder API</li>
 *   <li>Spring Boot auto-configuration</li>
 *   <li>Gradle and Maven plugins</li>
 * </ul>
 *
 * @see <a href="https://github.com/philipp-gatzka/graphite">GitHub Repository</a>
 */

plugins {
    base
    id("jacoco-report-aggregation")
}

description = "Graphite - Type-safe GraphQL client for Spring Boot"

// Aggregate JaCoCo reports from all subprojects
dependencies {
    subprojects.filter { it.plugins.hasPlugin("jacoco") }.forEach {
        jacocoAggregation(project(it.path))
    }
}

reporting {
    reports {
        register<JacocoCoverageReport>("jacocoAggregatedReport") {
            testType.set(TestSuiteType.UNIT_TEST)
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("jacocoAggregatedReport"))
}

// Configure wrapper task
tasks.wrapper {
    gradleVersion = "8.12"
    distributionType = Wrapper.DistributionType.ALL
}
