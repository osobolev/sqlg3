rootProject.name = "sqlg3"

fun add(name: String) {
    val mname = "modules/$name"
    include(mname)
    project(":$mname").name = "sqlg3-$name"
}

add("core")
add("preprocess")
add("preprocess-ant")
add("remote-common")
add("remote-client")
add("remote-server")
add("remote-kryo")
add("runtime")
