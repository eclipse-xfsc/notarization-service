plugins {
    kotlin("jvm")
    kotlin("plugin.allopen")
    id("org.kordamp.gradle.jandex")
    id("jacoco")
}

val javaToolchain: String by project
//java.sourceCompatibility = JavaVersion.VERSION_1_8
kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchain)
    }
}
java {
    withSourcesJar()
    withJavadocJar()
}

allOpen {
    annotation("jakarta.persistence.Entity")
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

val testHeapSize: String by project
tasks.withType<Test> {
    maxHeapSize = testHeapSize

}
tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.named("javadoc").configure {
    mustRunAfter("jandex")
}
tasks.named("test").configure {
    mustRunAfter("jandex")
}
