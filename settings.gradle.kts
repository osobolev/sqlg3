plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

rootProject.name = "sqlg3"

fun add(name: String) {
    val mname = "modules/$name"
    include(mname)
    project(":$mname").name = "sqlg3-$name"
}

add("types")
add("preprocess")
add("preprocess-ant")
add("runtime")
