import java.nio.file.Files

plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven {
        url = uri("https://maven.quiltmc.org/repository/release/")
    }
}

architectury {
    platformSetupLoomIde()
    loader("quilt")
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentQuilt: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentQuilt.extendsFrom(common)

    all { exclude(group = "net.fabricmc", module = "fabric-loader") }
}

dependencies {
    modImplementation("org.quiltmc:quilt-loader:${getVar("vQuiltLoader")}")
    modApi("org.quiltmc.quilted-fabric-api:quilted-fabric-api:${parseVarStr("{vQFAPI}+{vFAPI}-{vMinecraft}")}")
    modApi("org.quiltmc:qsl:${parseVarStr("{vQSL}+{vMinecraft}")}") // TODO is this really needed? it feels wierd
    modApi("org.quiltmc.quilt-kotlin-libraries:quilt-kotlin-libraries:${parseVarStr("{vQuiltKotlin}+kt.{vKotlin}+flk.{vFabricKotlin}")}")


    // Remove the next few lines if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${getVar("vArchitectury")}") {
        // We must not pull Fabric Loader from Architectury Fabric
        exclude("net.fabricmc")
        exclude("net.fabricmc.fabric-api")
    }

    common(project(":common", "namedElements")) {
        isTransitive = false
    }
    shadowCommon(project(":common", "transformProductionQuilt")){
        isTransitive = false
    }
    common(project(":fabric-like", "namedElements")){
        isTransitive = false
    }
    shadowCommon(project(":fabric-like", "transformProductionQuilt")) {
        isTransitive = false
    }
}

tasks.processResources {
    inputs.property("group", getVar("maven_group"))
    inputs.property("version", project.version)

    filesMatching("quilt.mod.json") {
        expand(mapOf(
            "group" to getVar("maven_group"),
            "version" to project.version,
            "name" to getVar("human_name"),
            "desc" to getVar("human_desc"),
            "source" to getVar("source"),
            "license" to getVar("license"),

            "mod_id" to getVar("mod_id"),
            "vMinecraft" to getVar("vMinecraft"),
            "vArchitectury" to getVar("vArchitectury")
        ))
    }
}

tasks.shadowJar {
    project(":common").file("src/main/resources/mcspider-common.mixins.json").copyTo(project.file("build/resources/main/mcspider-common.mixins.json"), true) // shitty solution TODO fix this better
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener.set(true)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.sourcesJar {
    val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

fun parseVarStr(format: String): String {
    val parseVarRgx = Regex("\\{([^{}]+)\\}")
    return parseVarRgx.replace(format) { match ->
        return@replace getVar(match.groups[1]!!.value)
    }
}

fun getVar(name: String): String {
    return rootProject.property(name).toString()
}