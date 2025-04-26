plugins {
    alias(buildLibs.plugins.versions)
    alias(buildLibs.plugins.owasp)
}

tasks.dependencyUpdates {
    checkConstraints = true
    checkBuildEnvironmentConstraints = true

    resolutionStrategy {
        componentSelection {
            all {
                if (candidate.version.contains(Regex("(snapshot|alpha|beta|rc|cr)", RegexOption.IGNORE_CASE))) {
                    reject("Ignore pre-release versions")
                }

                // use currentVersion to abort the search for some transitive dependencies
                if (candidate.group.startsWith("jakarta.") && candidate.version != currentVersion) {
                    reject("Ignore javax dependencies managed by quarkus")
                }
                if (candidate.group.startsWith("org.keycloak") && candidate.version != currentVersion) {
                    reject("Ignore quarkus managed keycloak dependencies")
                }
                if (candidate.group.startsWith("com.fasterxml.jackson") && candidate.version != currentVersion) {
                    reject("Ignore quarkus managed jackson dependencies")
                }
                if (candidate.group.startsWith("io.smallrye.reactive") && candidate.version != currentVersion) {
                    reject("Ignore quarkus managed smallrye dependencies")
                }
            }
        }
    }
}

tasks.dependencyCheckAggregate {
    config.failOnError = false
}
