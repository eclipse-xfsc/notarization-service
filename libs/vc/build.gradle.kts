description = "VC library"

plugins {
    id("notarization-api.libs-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))

    api(quarkusModules.jackson.module.kotlin)

    api(libs.kotlin.coroutines)
    api(libs.kotlin.serialization)

    api(libs.vcJava)
    implementation(libs.bbs)
    api(libs.walt.did)
    api(libs.walt.sdjwt)
//    implementation(libs.walt.vc)

    testImplementation(quarkusModules.bundles.test.basics)
}
