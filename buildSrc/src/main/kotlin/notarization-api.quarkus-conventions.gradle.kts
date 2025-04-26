import java.text.SimpleDateFormat
import java.util.*

plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("io.quarkus")
}

val javaToolchain: String by project
//java.sourceCompatibility = JavaVersion.VERSION_1_8
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchain)
    }
}

quarkus {
    // set java toolchain executable for quarkus build commands as long as quarkus does not honor toolchains
    // https://github.com/quarkusio/quarkus/issues/20452
    buildForkOptions {
        maxHeapSize = "4g"
        val launcher = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(javaToolchain)
        }
        val javaBin = launcher.map { it.executablePath.asFile.absolutePath }
        javaBin.orNull?.let { executable = it }
    }

    val tag = System.getenv()["TAG"] ?: "latest"
    val DATEFORMAT_RFC_3339 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    DATEFORMAT_RFC_3339.timeZone = TimeZone.getTimeZone("UTC")
    val buildTime = DATEFORMAT_RFC_3339.format(Date())
    val summary = "The microservice '${project.name}' as a component of the Notarization API."
    set("container-image.labels.\"org.opencontainers.image.title\"", "notarization-api/" + project.name)
    set("container-image.labels.\"org.opencontainers.image.description\"", summary)
    set("container-image.labels.\"org.opencontainers.image.created\"", buildTime)
    set("container-image.labels.\"org.opencontainers.image.url\"", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    set("container-image.labels.\"org.opencontainers.image.source\"", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    set("container-image.labels.\"org.opencontainers.image.documentation\"", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    set("container-image.labels.\"io.k8s.display-name\"", "notarization-api/" + project.name)
    set("container-image.labels.url", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    set("container-image.labels.name", "notarization-api/" + project.name)
    set("container-image.labels.summary", summary)
    set("container-image.labels.build-date", buildTime)
    set("container-image.labels.vendor", "Gaia-X")
    set("container-image.labels.\"org.opencontainers.image.vendor\"", "Gaia-X")
    set("container-image.labels.\"org.opencontainers.image.version\"", tag)
    set("container-image.labels.\"version\"", tag)
    set("quarkus.smallrye-openapi.auto-add-security", "false")
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
    annotation("io.quarkus.test.junit.QuarkusIntegrationTest")
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:unchecked")
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.javaParameters = true
}

tasks.compileKotlin {
    mustRunAfter("quarkusGeneratedSourcesClasses")
}
tasks.compileTestKotlin {
    mustRunAfter("quarkusTestGeneratedSourcesClasses")
}

val testHeapSize: String by project
tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
    maxHeapSize = testHeapSize
}

// TODO: adjust or remove when it is not needed anymore
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "io.grpc" && requested.name.startsWith("grpc-")) {
            useVersion("1.59.1")
            because("Defined by quarkus and needs to be this exact version")
        }
        if (requested.group == "com.google.api.grpc" && requested.name == "proto-google-common-protos") {
            useVersion("2.22.0")
            because("Defined by quarkus and needs to be this exact version")
        }
        if (requested.group == "com.google.protobuf" && requested.name == "protobuf-java") {
            useVersion("3.23.2")
            because("Defined by quarkus and needs to be this exact version")
        }
    }
}
