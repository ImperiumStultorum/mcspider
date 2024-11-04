architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

loom {
    accessWidenerPath.set(file("src/main/resources/mcspider.accesswidener"))
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${getVar("vFabricLoader")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury:${getVar("vArchitectury")}")
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