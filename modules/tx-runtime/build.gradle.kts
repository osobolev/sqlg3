plugins {
    `module-lib`
    `lib`
}

dependencies {
    api(project(":sqlg3-runtime"))
    api(project(":sqlg3-tx-api"))
}
