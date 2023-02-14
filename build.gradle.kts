plugins {
    id("com.github.ben-manes.versions") version "0.45.0"
}

fun getMajor(version: String, majorDepth: Int): String {
    var p = -1;
    for (i in 0 until majorDepth) {
        p = version.indexOf('.', p + 1)
        if (p < 0) return version
    }
    return if (p < 0) "" else version.substring(0, p)
}

fun getMajorDepth(mod: ModuleComponentIdentifier): Int {
    if (mod.group == "org.antlr") return 2
    return 1
}

tasks.withType(com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class).configureEach {
    resolutionStrategy {
        componentSelection {
            all {
                val majorDepth = getMajorDepth(candidate)
                if (getMajor(candidate.version, majorDepth) != getMajor(currentVersion, majorDepth)) {
                    reject("Major update")
                }
            }
        }
    }
}
