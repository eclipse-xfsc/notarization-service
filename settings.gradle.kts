pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "notarization-api"

include("libs:test-utils")
include("libs:interfaces")
include("libs:utils")
include("libs:vc")

include("services:auto-notary")
include("services:oidc-identity-resolver")
include("services:scheduler")
include("services:revocation")
include("services:profile")
include("services:request-processing")
include("services:ssi-issuance")
include("services:oid4vci")
include("services:compliance-task")
include("services:oid4vp-task")
include("services:train-enrollment")
include("services:ssi-issuance2")
include("e2e:cucumber-specs")


dependencyResolutionManagement {

    repositories {
        mavenCentral()
        // VC repos
        maven {
            url = uri("https://repo.danubetech.com/repository/maven-releases/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
        // walt.id repos
        maven {
            url = uri("https://maven.waltid.dev/snapshots")
            mavenContent { snapshotsOnly() }
        }
        maven {
            url = uri("https://maven.waltid.dev/releases")
        }
        // for the patched version of the danube lib
        maven {
            url = uri("https://mvn.ecsec.de/repository/openecard-snapshot/")
        }
    }

    versionCatalogs {
        create("quarkusModules") {
            val quarkusPlatformGroupId: String by settings
            val quarkusPlatformArtifactId: String by settings
            val quarkusPlatformVersion: String by settings

            version("qversion", quarkusPlatformVersion)
            library("bom", "${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}")

            val qgroup = "io.quarkus"

            // kotlin essentials
            library("qkotlin", qgroup, "quarkus-kotlin").withoutVersion()
            library("arc", qgroup, "quarkus-arc").withoutVersion()
            library("fault-tolerance", qgroup, "quarkus-smallrye-fault-tolerance").withoutVersion()
            library("kotlin-logging", "io.github.microutils", "kotlin-logging-jvm").version("3.0.5")
            bundle("basics", listOf("qkotlin", "arc", "fault-tolerance", "kotlin-logging"))

            // standard interface modules
            library("jaxrs-api", "jakarta.ws.rs", "jakarta.ws.rs-api").withoutVersion()
            library("validation-api", "jakarta.validation", "jakarta.validation-api").withoutVersion()
            library("transaction-api", "jakarta.transaction", "jakarta.transaction-api").withoutVersion()
            library("persistence-api", "jakarta.persistence", "jakarta.persistence-api").withoutVersion()
            library("openapi-api", "org.eclipse.microprofile.openapi", "microprofile-openapi-api").withoutVersion()

            // status and metrics
            library("status-openapi", qgroup, "quarkus-smallrye-openapi").withoutVersion()
            library("status-health", qgroup, "quarkus-smallrye-health").withoutVersion()
            library("status-prometheus",qgroup, "quarkus-micrometer-registry-prometheus").withoutVersion()
            library("status-otel", qgroup, "quarkus-opentelemetry").withoutVersion()
            bundle("status", listOf("status-openapi", "status-health", "status-otel", "status-prometheus"))

            // build related
            library("build-jib", qgroup, "quarkus-container-image-jib").withoutVersion()
            library("build-kubernetes", qgroup, "quarkus-kubernetes").withoutVersion()
            bundle("build", listOf("build-jib", "build-kubernetes"))

            // web stuff
            library("resteasy-reactive", qgroup, "quarkus-resteasy-reactive").withoutVersion()
            library("resteasy-reactive-jackson", qgroup, "quarkus-resteasy-reactive-jackson").withoutVersion()
            library("jackson-module-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").withoutVersion()
            library("jackson-jsr353", "com.fasterxml.jackson.datatype", "jackson-datatype-jakarta-jsonp").withoutVersion()
            library("mutiny-kotlin", "io.smallrye.reactive", "mutiny-kotlin").withoutVersion()
            bundle("jaxrs-reactive", listOf("resteasy-reactive", "resteasy-reactive-jackson", "jackson-module-kotlin", "jackson-jsr353", "mutiny-kotlin"))
            library("rest-client-reactive", qgroup, "quarkus-rest-client-reactive").withoutVersion()
            library("rest-client-reactive-jackson", qgroup, "quarkus-rest-client-reactive-jackson").withoutVersion()
            bundle("jaxrs-client-reactive", listOf("rest-client-reactive", "rest-client-reactive-jackson", "jackson-module-kotlin", "jackson-jsr353"))
            library("jsonb", qgroup, "quarkus-jsonb").withoutVersion()
            library("quarkus-jackson", qgroup, "quarkus-jackson").withoutVersion()

            // messaging
            library("messaging-rabbitmq", qgroup, "quarkus-smallrye-reactive-messaging-rabbitmq").withoutVersion()
            library("messaging-amqp", qgroup, "quarkus-smallrye-reactive-messaging-amqp").withoutVersion()
            library("messaging-inmemory", "io.smallrye.reactive", "smallrye-reactive-messaging-in-memory").withoutVersion()

            // database
            library("narayana-jta", qgroup, "quarkus-narayana-jta").withoutVersion()
            library("flyway", qgroup, "quarkus-flyway").withoutVersion()
            library("panache", qgroup, "quarkus-hibernate-orm-panache-kotlin").withoutVersion()
            library("panacheJava", qgroup, "quarkus-hibernate-orm-panache").withoutVersion()
            library("postgres", qgroup, "quarkus-jdbc-postgresql").withoutVersion()
            bundle("database", listOf("panache", "postgres"))

            library("postgres-reactive", qgroup, "quarkus-reactive-pg-client").withoutVersion()
            library("panache-reactive", qgroup, "quarkus-hibernate-reactive-panache-kotlin").withoutVersion()
            library("quarkus-hibernate-types", "io.quarkiverse.hibernatetypes", "quarkus-hibernate-types").version("2.1.0")
            bundle("databaseReactive", listOf("hibernate-validator", "postgres-reactive", "panache-reactive", "quarkus-hibernate-types"))

            // oidc
            library("oidc", qgroup, "quarkus-oidc").withoutVersion()
            library("oidc-client-filter", qgroup, "quarkus-oidc-client").withoutVersion()
            library("oidc-reactive-filter", qgroup, "quarkus-oidc-client-reactive-filter").withoutVersion()

            // keycloak
            library("test-keycloak", qgroup, "quarkus-test-keycloak-server").withoutVersion()
            library("keycloak-core", "org.keycloak", "keycloak-core").withoutVersion()
            library("keycloak-admin", "org.keycloak", "keycloak-admin-client").withoutVersion()

            // batch
            library("quartz", qgroup, "quarkus-quartz").withoutVersion()
            library("scheduler", qgroup, "quarkus-scheduler").withoutVersion()

            // utils
            library("config-converter", "io.smallrye.config", "smallrye-config-converter-json").version("3.7.1")
            library("config-yaml", qgroup, "quarkus-config-yaml").withoutVersion()
            library("jwt", qgroup, "quarkus-smallrye-jwt").withoutVersion()
            library("cache", qgroup, "quarkus-cache").withoutVersion()
            library("hibernate-validator", qgroup, "quarkus-hibernate-validator").withoutVersion()
            library("qOpenApiGen", "io.quarkiverse.openapi.generator", "quarkus-openapi-generator").version("2.4.1")

            // testing
            library("junit", qgroup, "quarkus-junit5").withoutVersion()
            library("jacoco", qgroup, "quarkus-jacoco").withoutVersion()
            library("junit-pioneer", "org.junit-pioneer:junit-pioneer:2.3.0")
            library("junit-platform-reporting", "org.junit.platform:junit-platform-reporting:1.12.0")
            bundle("test-basics", listOf("junit", "jacoco", "junit-pioneer", "junit-platform-reporting"))

            library("mockito-junit", qgroup, "quarkus-junit5-mockito").withoutVersion()
            library("mockito-kotlin", "org.mockito.kotlin", "mockito-kotlin").version("5.2.1")
            bundle("test-mockito", listOf("mockito-junit", "mockito-kotlin"))

            library("restassured", "io.rest-assured", "rest-assured").withoutVersion()
            library("restassured-kotlin", "io.rest-assured", "kotlin-extensions").withoutVersion()
            bundle("test-restassured", listOf("restassured", "restassured-kotlin"))

            library("vertxTest", qgroup, "quarkus-test-vertx").withoutVersion()

            library("quarkus-cucumber", "io.quarkiverse.cucumber", "quarkus-cucumber").version("1.0.0")
        }

        create("libs") {
            library("st4", "org.antlr:ST4:4.3.4")
            library("jsonld", "com.apicatalog:titanium-json-ld:1.6.0")
            library("jsonpatch", "com.github.java-json-tools:json-patch:1.13")
            library("parsson", "org.eclipse.parsson:parsson:1.1.5")
            library("jose4j", "org.bitbucket.b_c", "jose4j").version("0.9.6")
            library("uuid4j", "org.electrologic", "uuid4j").version("0.9.0")

            library("kotlin-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-json").withoutVersion()
            library("kotlin-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").withoutVersion()

            version("ktor", "2.3.9")
            library("ktor-client-core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor-client-cio", "io.ktor", "ktor-client-cio").versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation").versionRef("ktor")
            library("ktor-serialization-kotlinx-json", "io.ktor", "ktor-serialization-kotlinx-json").versionRef("ktor")
            bundle("ktor", listOf("ktor-client-core", "ktor-client-cio", "ktor-client-content-negotiation", "ktor-serialization-kotlinx-json"))

            version("waltid", "0.5.0")
            library("walt-openid4vc", "id.walt.openid4vc", "waltid-openid4vc").versionRef("waltid")
            library("walt-crypto", "id.walt.crypto", "waltid-crypto").versionRef("waltid")
            library("walt-did", "id.walt.did", "waltid-did").versionRef("waltid")
            library("walt-sdjwt", "id.walt.sdjwt", "waltid-sdjwt-jvm").versionRef("waltid")
            library("walt-vc", "id.walt.credentials", "waltid-verifiable-credentials").versionRef("waltid")

            library("bbs", "com.github.mattrglobal", "bbs.signatures").version("2.0")
            // patched version of the danube lib
            // https://github.com/ecsec/verifiable-credentials-java/tree/presentation-validation
            //library("vcJava", "com.danubetech", "verifiable-credentials-java").version("1.9.0")
            library("vcJava", "com.danubetech", "verifiable-credentials-java-ec").version("1.10-SNAPSHOT")

            library("ironVc", "com.apicatalog", "iron-verifiable-credentials").version("0.14.0")
            library("wiremock", "org.wiremock:wiremock:3.4.2")
//            library("wiremock", "com.github.tomakehurst:wiremock-jre8:2.35.2")
        }

        create("buildLibs") {
            plugin("node-frontend", "org.siouan.frontend-jdk17").version("8.0.0")
            plugin("docker", "com.bmuschko.docker-remote-api").version("9.4.0")
            plugin("versions", "com.github.ben-manes.versions").version("0.51.0")
            plugin("owasp", "org.owasp.dependencycheck").version("9.0.10")
        }
    }

}
