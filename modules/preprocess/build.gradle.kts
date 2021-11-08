plugins {
    `module-lib`
    `lib`
}

val antlr by configurations.creating

dependencies {
    api(project(":sqlg3-runtime"))
    implementation("org.antlr:antlr4-runtime:4.9.2")

    testRuntimeOnly("org.postgresql:postgresql:42.2.24")

    antlr("org.antlr:antlr4:4.9.2")
}

tasks.register("lexer", JavaExec::class) {
    inputs.files("grammar/Java8Lexer.g4")
    outputs.files("src/sqlg3/preprocess/lexer/Java8Lexer.java")
    mainClass.set("org.antlr.v4.Tool")
    classpath = antlr
    args("-package", "sqlg3.preprocess.lexer", "-Xexact-output-dir", "-o", "src/sqlg3/preprocess/lexer", "-encoding", "UTF-8", "grammar/Java8Lexer.g4")
}
tasks {
    withType(JavaCompile::class) {
        dependsOn("lexer")
    }
}
tasks.named("sourcesJar") {
    dependsOn("lexer")
}
