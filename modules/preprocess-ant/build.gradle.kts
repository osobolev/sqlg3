plugins {
    `lib`
}

dependencies {
    api(project(":sqlg3-preprocess"))
    compileOnly("org.apache.ant:ant:1.10.15")
}
