description = "cucumber-specs"

plugins {
    id("notarization-api.quarkus-conventions")
    id("notarization-api.testreport-conventions")
}

dependencies {
    implementation(platform(quarkusModules.bom))
    implementation(quarkusModules.bundles.basics)

    testImplementation(quarkusModules.bundles.jaxrs.reactive)
    testImplementation(quarkusModules.messaging.amqp)

    testImplementation(quarkusModules.bundles.database)
    testImplementation(quarkusModules.panacheJava)

    testImplementation(quarkusModules.keycloak.core)
    testImplementation(quarkusModules.keycloak.admin)

    testImplementation(quarkusModules.oidc.client.filter)

    implementation(libs.vcJava)
    implementation(libs.walt.did)
    implementation(libs.walt.sdjwt)
    implementation(libs.kotlin.serialization)

    testImplementation(libs.jose4j)

    testImplementation(quarkusModules.bundles.test.basics)
    testImplementation(quarkusModules.bundles.test.restassured)
    testImplementation(quarkusModules.quarkus.cucumber)
}

//<dependency>
//  <groupId>org.glassfish</groupId>
//  <artifactId>javax.json</artifactId>
//  <version>1.1.4</version>
//</dependency>

// execute tests only when executing the acceptanceTest job

gradle.taskGraph.whenReady {
    if (allTasks.find { it.name == "acceptanceTest" } == null) {
        tasks.getByName("test").enabled = false
    }
}

tasks.register("acceptanceTest") {
    group = "verification"
    description = "Runs the acceptance tests."
    dependsOn("check")
}
