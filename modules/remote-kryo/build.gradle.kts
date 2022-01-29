plugins {
    `module-lib`
    `lib`
}

dependencies {
    api(project(":sqlg3-remote-common"))
    api("com.esotericsoftware:kryo:5.2.1")
}
