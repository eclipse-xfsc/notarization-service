import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import org.siouan.frontendgradleplugin.infrastructure.gradle.AssembleTask
import org.siouan.frontendgradleplugin.infrastructure.gradle.InstallFrontendTask
import org.siouan.frontendgradleplugin.infrastructure.gradle.RunNpm
import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(buildLibs.plugins.node.frontend)
    alias(buildLibs.plugins.docker)
}

frontend {
    nodeInstallDirectory = project.layout.projectDirectory.dir(".node")
    nodeVersion = "20.9.0"
    assembleScript = "run build:production"
    checkScript = "run test"
    cleanScript = "run clean"
}

tasks.register<RunNpm>("pruneProd") {
    dependsOn("assemble")
    script.set("prune --omit=dev")
}
tasks.register("assembleProd") {
    dependsOn("pruneProd")
}

tasks.create<DockerBuildImage>("imageBuild") {
    val tag = System.getenv()["TAG"] ?: "latest-dev"
    val additional_tags = System.getenv()["ADDITIONAL_TAGS"]    
    val registry = System.getenv()["IMAGE_REGISTRY"] ?: "node-654e3bca7fbeeed18f81d7c7.ps-xaas.io"
    val registry_group = System.getenv()["IMAGE_REGISTRY_GROUP"] ?: "not"

    val DATEFORMAT_RFC_3339 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    DATEFORMAT_RFC_3339.timeZone = TimeZone.getTimeZone("UTC")
    val buildTime = DATEFORMAT_RFC_3339.format(Date())

    labels.put("build-date", buildTime)
    labels.put("org.opencontainers.image.created", buildTime)
    labels.put("name", "notarization-api/ssi-issuance")
    labels.put("org.opencontainers.image.description", "The microservice 'ssi-issuance' as a component of the Notarization API.")
    labels.put("org.opencontainers.image.documentation", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    labels.put("org.opencontainers.image.source", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    labels.put("org.opencontainers.image.title", "notarization-api/ssi-issuance")
    labels.put("org.opencontainers.image.url", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    labels.put("org.opencontainers.image.vendor", "Gaia-X")
    labels.put("vendor", "Gaia-X")
    labels.put("summary", "The microservice 'ssi-issuance' as a component of the Notarization API.")
    labels.put("url", "https://gitlab.eclipse.org/eclipse/xfsc/notarization-service/not")
    labels.put("org.opencontainers.image.version", tag)
    labels.put("version", tag)

    inputDir = projectDir
    dockerFile = project.file("Dockerfile.gradle")

    images.add("${registry}/${registry_group}/ssi-issuance:${tag}")
    if (additional_tags != null) {
        images.add("${registry}/${registry_group}/ssi-issuance:${additional_tags}")
    }

    dependsOn("assembleProd")
}


tasks.named<InstallFrontendTask>("installFrontend") {
    val packageJsonLockFile = "${projectDir}/package-lock.json"
    val packageJsonFile = "${projectDir}/package.json"
    inputs.files(packageJsonFile, packageJsonLockFile).withPropertyName("metadataFiles")
    outputs.dir("${projectDir}/node_modules").withPropertyName("nodeModulesDirectory")
}

tasks.named<AssembleTask>("assembleFrontend").configure {
    inputs.files("${projectDir}/node_modules").withPropertyName("nodeModulesDirectory")
    inputs.files("${projectDir}/src").withPropertyName("sourcesDirectory")
    val tsconfigs = projectDir.listFiles { file -> file.name.matches("""^tsconfig\..*\.json""".toRegex()) }
    inputs.files(tsconfigs).withPropertyName("tsconfigFiles")
    outputs.dir("${projectDir}/dist").withPropertyName("distDirectory")
}
