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
 * Convention plugin for Java modules in the Graphite project.
 *
 * <p>This plugin configures:
 * <ul>
 *   <li>Java 21 toolchain</li>
 *   <li>Compiler options with strict warnings</li>
 *   <li>Google Java Format via Spotless</li>
 *   <li>Checkstyle for code style enforcement</li>
 *   <li>SpotBugs for static analysis</li>
 *   <li>JaCoCo for code coverage</li>
 *   <li>JUnit 5 for testing</li>
 * </ul>
 */

plugins {
    `java-library`
    checkstyle
    jacoco
    id("com.diffplug.spotless")
    id("com.github.spotbugs")
}

group = "io.github.graphite"

// Support for Java version override via Gradle property for compatibility testing
val javaVersion = providers.gradleProperty("javaVersion").getOrElse("21").toInt()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all",
            "-Xlint:-processing",
            "-Werror"
        )
    )
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        links("https://docs.oracle.com/en/java/javase/$javaVersion/docs/api/")
    }
}

spotless {
    java {
        googleJavaFormat("1.24.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(rootProject.file("gradle/license-header.txt"))
    }
}

checkstyle {
    toolVersion = "10.21.1"
    configFile = rootProject.file("gradle/checkstyle.xml")
    configDirectory.set(rootProject.file("gradle"))
    isIgnoreFailures = false
    maxWarnings = 0
}

spotbugs {
    toolVersion.set("4.8.6")
    // Set to true to allow build to pass while SonarCloud reports issues
    ignoreFailures.set(true)
    showStackTraces.set(true)
    showProgress.set(false)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    excludeFilter.set(rootProject.file("gradle/spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("xml") {
        required.set(true)
    }
    reports.create("html") {
        required.set(true)
        setStylesheet("fancy-hist.xsl")
    }
}

// Only run SpotBugs on main source set, not test code
tasks.matching { it.name == "spotbugsTest" }.configureEach {
    enabled = false
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                // 75% coverage minimum - can be increased as test coverage improves
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
