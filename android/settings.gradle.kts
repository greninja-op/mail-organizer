pluginManagement {
    repositories {
        // Local file repository (Phase 0): the sandbox blocks JVM network
        // access via the egress proxy's per-process policy, so all Maven
        // artifacts were pre-fetched with curl into ~/local-m2.
        // This repo is authoritative here; the remotes below are fallbacks.
        // See docs/development-status.md for details.
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("file:///home/hatch/local-m2") }
        google()
        mavenCentral()
    }
}
rootProject.name = "mail-organizer"
include(":app")
