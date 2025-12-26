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
    alias(libs.plugins.owasp.dependency.check)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.release)
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
            testSuiteName = "test"
        }
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("jacocoAggregatedReport"))
}

// Configure wrapper task
tasks.wrapper {
    gradleVersion = "9.2.1"
    distributionType = Wrapper.DistributionType.ALL
}

// Configure OWASP Dependency Check
dependencyCheck {
    // Fail the build on high or critical vulnerabilities
    failBuildOnCVSS = 7f

    // Scan all configurations
    scanConfigurations = listOf("runtimeClasspath", "compileClasspath")

    // Suppress known false positives (create suppression file if needed)
    suppressionFiles = listOf("$projectDir/config/owasp-suppressions.xml")

    // Output format
    formats = listOf("HTML", "JSON")

    // Include all subprojects
    analyzedTypes = listOf("jar")
}

// Configure SonarCloud
sonar {
    properties {
        property("sonar.projectKey", "philipp-gatzka_graphite")
        property("sonar.organization", "philipp-gatzka-1")
        property("sonar.host.url", "https://sonarcloud.io")

        property("sonar.java.binaries", layout.buildDirectory.dir("classes").get().asFile.path)

        // Coverage reporting
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/reports/jacoco/test/jacocoTestReport.xml")

	// SpotBugs reporting
        // property("sonar.java.spotbugs.reportPaths", "**/build/reports/spotbugs/*.xml")

        // Source encoding
        property("sonar.sourceEncoding", "UTF-8")

        // Java version
        property("sonar.java.source", "21")
        property("sonar.java.target", "21")

        // Exclude generated code and build directories
        property("sonar.exclusions", "**/build/**,**/generated/**")
    }
}

// Configure release plugin
release {
    git {
        requireBranch.set("main")
    }
}

// Publish all subprojects after release build
tasks.named("afterReleaseBuild") {
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("publish") })
}
