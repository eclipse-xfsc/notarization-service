description = "scheduler"

plugins {
    id("notarization-api.quarkus-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))
    implementation(quarkusModules.bundles.basics)

    implementation(quarkusModules.bundles.status)
    implementation(quarkusModules.bundles.build)

    implementation(quarkusModules.bundles.jaxrs.reactive)
    implementation(quarkusModules.bundles.jaxrs.client.reactive)
    implementation(quarkusModules.bundles.database)
    implementation(quarkusModules.flyway)
    implementation(quarkusModules.quartz)

    testImplementation(quarkusModules.bundles.test.basics)
}

sourceSets {
    main {
        resources {
            srcDir("src/main/jib")
        }
    }
}
