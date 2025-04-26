description = "Service that is used for TRAIN enrollment."

plugins {
    id("notarization-api.quarkus-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))
    implementation(quarkusModules.bundles.basics)

    implementation(project(":libs:interfaces"))

    implementation(quarkusModules.bundles.status)
    implementation(quarkusModules.bundles.build)

    implementation(quarkusModules.bundles.jaxrs.reactive)
    implementation(quarkusModules.bundles.jaxrs.client.reactive)
    implementation(quarkusModules.bundles.database)

    implementation(libs.uuid4j)

    implementation(quarkusModules.kotlin.logging)
    implementation(quarkusModules.jackson.module.kotlin)

    implementation(quarkusModules.oidc.reactive.filter)

    testImplementation(quarkusModules.flyway)
    testImplementation(quarkusModules.postgres)

    testImplementation(quarkusModules.bundles.test.basics)
    testImplementation(quarkusModules.bundles.test.mockito)
    testImplementation(quarkusModules.bundles.test.restassured)
    testImplementation(libs.wiremock)
}


sourceSets {
    main {
        resources {
            srcDir("src/main/jib")
        }
    }
}
