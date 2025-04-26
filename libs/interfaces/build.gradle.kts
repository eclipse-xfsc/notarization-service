description = "Notarization-API interfaces"

plugins {
    id("notarization-api.libs-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))

    api(quarkusModules.jaxrs.api)
    api(quarkusModules.jackson.module.kotlin)
    api(quarkusModules.validation.api)
    api(quarkusModules.openapi.api)
    api(quarkusModules.mutiny.kotlin)
    api(libs.jose4j)

    api(quarkusModules.quarkus.jackson)

    testImplementation(quarkusModules.bundles.test.basics)
    testImplementation(project(":libs:test-utils"))
}
