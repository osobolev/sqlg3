fun requiredMajor(mod: ModuleComponentIdentifier): String {
    return ""
}

tasks.withType(com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask::class).configureEach {
    rejectVersionIf {
        !candidate.version.startsWith(requiredMajor(candidate))
    }
}
