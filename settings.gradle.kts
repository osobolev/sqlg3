plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("0.8.0")
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
add("remote-common")
add("remote-client")
add("remote-server")
add("remote-kryo")
add("runtime")
add("tx-api")
add("tx-runtime")
