import org.myire.munge.transform.TransformationSet

plugins {
    kotlin("jvm")
    id("org.myire.munge")
}

tasks.withType<Test> {
    reports.html.required = true
    reports.junitXml.required = true
    val outputDir = reports.junitXml.outputLocation
    jvmArgumentProviders += CommandLineArgumentProvider {
        listOf(
            "-Djunit.platform.reporting.open.xml.enabled=true",
            "-Djunit.platform.reporting.output.dir=${outputDir.get().asFile.absolutePath}"
        )
    }
    /*
        Alternatively, set values as such:
        systemProperty("-Djunit.platform.reporting.open.xml.enabled", "true")
     */
}

tasks.transform {
    tasks.withType<Test>().forEach {
        // make sure this runs after tests and it is a finalizer for the test
        // https://docs.gradle.org/current/userguide/controlling_task_execution.html#sec:finalizer_tasks
        mustRunAfter(it)
        it.finalizedBy(this)
    }

    isFailOnError = true
    saxonVersion = "12.5"

    saxon(closureOf<TransformationSet> {
        // add template
        val csvTemplate = project.rootDir.resolve("testreporting/open-test-to-csv.xsl")
        templates(csvTemplate)

        // add source files
        sources("build/test-results", closureOf<PatternFilterable> {
            include("**/open-test-report.xml")
        })

        // define outputs
        //setOutputDir(layout.buildDirectory.dir("reports/requirements"))
        outputMapping(KotlinClosure2<File, File, Any>({ src, tmpl ->
            val module = project.name
            val testTask = src.parentFile.name
            layout.buildDirectory.file("reports/requirements/$module-$testTask.csv")
        }))

        this.parameters.put("moduleName", project.name)
    })

}
