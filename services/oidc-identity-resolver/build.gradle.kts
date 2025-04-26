description = "oidc-identity-resolver"

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
    implementation(quarkusModules.bundles.databaseReactive)
    implementation(quarkusModules.flyway)
    implementation(quarkusModules.postgres)

    implementation(quarkusModules.oidc)
    testImplementation(quarkusModules.test.keycloak)

    testImplementation(quarkusModules.bundles.test.basics)
//    testImplementation(quarkusModules.bundles.test.mockito)
    testImplementation(quarkusModules.bundles.test.restassured)
//    testImplementation(quarkusModules.testcontainers)
    testImplementation(libs.wiremock)
}


sourceSets {
    main {
        resources {
            //srcDir("src/test/resources")
            srcDir("src/main/jib")
        }
    }
}
