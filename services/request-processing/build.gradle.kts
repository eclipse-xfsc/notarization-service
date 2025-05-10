description = "request-processing"

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
    implementation(quarkusModules.cache)

    implementation(quarkusModules.messaging.amqp)
    testImplementation(quarkusModules.messaging.inmemory)

    implementation(libs.st4)
    implementation(libs.jsonld)
    implementation(libs.jsonpatch)
    implementation(libs.uuid4j)

    testImplementation(quarkusModules.bundles.test.basics)
    testImplementation(quarkusModules.bundles.test.mockito)
    testImplementation(quarkusModules.bundles.test.restassured)
//    testImplementation(quarkusModules.testcontainers)
    implementation(libs.wiremock)
    testImplementation(quarkusModules.vertxTest)

    implementation("commons-codec:commons-codec:1.18.0")
    implementation(libs.parsson)
//    implementation("org.bitbucket.b_c:jose4j:0.7.10")
//    implementation("commons-io:commons-io:2.11.0")
}


sourceSets {
    main {
        resources {
            srcDir("src/main/jib")
        }
    }
}
