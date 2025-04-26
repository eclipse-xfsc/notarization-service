description = "Notarization-API test utilities"

plugins {
    id("notarization-api.libs-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))

    api(quarkusModules.jackson.module.kotlin)

    api(quarkusModules.bundles.test.basics)
}
