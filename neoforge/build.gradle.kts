plugins {
    id("com.github.johnrengelman.shadow")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    neoForge.apply {
//        convertAccessWideners.set(true)
//        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)

//        mixinConfig("mcspider-common.mixins.json")
//        mixinConfig("mcspider.mixins.json")
    }
}

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentNeoForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentNeoForge.extendsFrom(common)
}

repositories {
    // neoforge
    maven {
        name = "NeoForged"
        setUrl("https://maven.neoforged.net/releases")
    }
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}


dependencies {
    neoForge("net.neoforged:neoforge:${getVar("vNeoforge")}")

    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-neoforge:${getVar("vArchitectury")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionNeoForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${getVar("vForgeKotlin")}")
}

tasks.processResources {
    inputs.property("group", getVar("maven_group"))
    inputs.property("version", project.version)

    filesMatching("META-INF/mods.toml") {
        expand(mapOf(
            "group" to getVar("maven_group"),
            "version" to project.version,
            "name" to getVar("human_name"),
            "desc" to getVar("human_desc"),
            "source" to getVar("source"),
            "license" to getVar("license"),

            "mod_id" to getVar("mod_id"),
            "vMinecraft" to getVar("vMinecraft"),
            "vArchitectury" to getVar("vArchitectury"),
            "vForgeKotlin" to getVar("vForgeKotlin")
        ))
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
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
