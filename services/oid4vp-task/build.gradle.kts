description = "OpenID Connect for Verifiable Presentation Service"

plugins {
    id("notarization-api.quarkus-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))
    implementation(quarkusModules.bundles.basics)

    implementation(project(":libs:interfaces"))
    implementation(project(":libs:utils"))
    implementation(project(":libs:vc"))

    implementation(quarkusModules.bundles.status)
    implementation(quarkusModules.bundles.build)
    // quarkus openapi generator
    implementation(quarkusModules.qOpenApiGen)

    implementation(quarkusModules.bundles.jaxrs.reactive)
    implementation(quarkusModules.bundles.jaxrs.client.reactive)
    implementation(quarkusModules.bundles.database)
    implementation(quarkusModules.jwt)
    implementation(quarkusModules.quartz)
    implementation(quarkusModules.cache)
//    implementation(quarkusModules.jsonb)

    implementation(quarkusModules.flyway)

    implementation(libs.uuid4j)
    implementation(libs.jose4j)

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

// openapi generator emits the source files in the java folder, add this as gradle sources
sourceSets["main"].kotlin.srcDir(layout.buildDirectory.file("classes/java/quarkus-generated-sources/open-api-yaml"))
