description = "Service that automatically accepts notarization requests."

plugins {
    id("notarization-api.quarkus-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))
    implementation(quarkusModules.bundles.basics)

    implementation(quarkusModules.bundles.status)
    implementation(quarkusModules.bundles.build)

    implementation(quarkusModules.oidc.reactive.filter)
    implementation(quarkusModules.bundles.jaxrs.reactive)
    implementation(quarkusModules.bundles.jaxrs.client.reactive)

    implementation(libs.uuid4j)

    implementation(quarkusModules.openapi.api)
    implementation(quarkusModules.scheduler)

    testImplementation(quarkusModules.bundles.test.basics)
    testImplementation(quarkusModules.bundles.test.mockito)
    testImplementation(quarkusModules.bundles.test.restassured)
    testImplementation(libs.wiremock)
}
