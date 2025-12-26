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
 * Convention plugin for publishing Graphite modules to Maven Central and GitHub Packages.
 *
 * <p>This plugin configures:
 * <ul>
 *   <li>Maven publication with POM metadata</li>
 *   <li>GPG signing for releases</li>
 *   <li>Publishing to Maven Central and GitHub Packages</li>
 * </ul>
 */

plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("Graphite - Type-safe GraphQL client for Spring Boot")
                url.set("https://github.com/philipp-gatzka/graphite")
                inceptionYear.set("2024")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("philipp-gatzka")
                        name.set("Philipp Gatzka")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/philipp-gatzka/graphite.git")
                    developerConnection.set("scm:git:ssh://github.com/philipp-gatzka/graphite.git")
                    url.set("https://github.com/philipp-gatzka/graphite")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/philipp-gatzka/graphite/issues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/philipp-gatzka/graphite")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
        maven {
            name = "MavenCentral"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: System.getenv("OSSRH_USERNAME") ?: ""
                password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: System.getenv("OSSRH_PASSWORD") ?: ""
            }
        }
    }
}

signing {
    val signingKeyId = System.getenv("SIGNING_KEY_ID")
    val signingKey = System.getenv("SIGNING_KEY") ?: System.getenv("GPG_SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: System.getenv("GPG_SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        if (signingKeyId != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        } else {
            useInMemoryPgpKeys(signingKey, signingPassword)
        }
        sign(publishing.publications["maven"])
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}
