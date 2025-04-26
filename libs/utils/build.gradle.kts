description = "Notarization-API utilities"

plugins {
    id("notarization-api.libs-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))

    api(quarkusModules.qkotlin)
    api(quarkusModules.jackson.module.kotlin)
    api(quarkusModules.narayana.jta)
    api(quarkusModules.persistence.api)
    api(quarkusModules.jwt)

    testImplementation(quarkusModules.bundles.test.basics)
}
