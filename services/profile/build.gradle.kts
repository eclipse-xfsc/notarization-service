description = "profile"

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
    implementation(quarkusModules.oidc)
    implementation(libs.uuid4j)

    implementation(quarkusModules.jwt)
    implementation(quarkusModules.config.yaml)
    runtimeOnly(quarkusModules.config.converter)

    testImplementation(quarkusModules.bundles.test.basics)
//    testImplementation(quarkusModules.bundles.test.mockito)
    testImplementation(quarkusModules.bundles.test.restassured)
    testImplementation(libs.wiremock)
    testImplementation(quarkusModules.vertxTest)
}

sourceSets {
    main {
        resources {
            srcDir("src/main/jib")
        }
    }
}
