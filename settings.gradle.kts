import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
if (rootProject.projectDir.resolve("local.properties").exists()) {
    localProperties.load(rootProject.projectDir.resolve("local.properties").inputStream())
}
val githubToken = localProperties.getProperty("github.token") ?: System.getenv("GITHUB_TOKEN")
?: throw IllegalStateException("GITHUB_TOKEN not found")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven {
            name = "drop-core GitHub Packages"
            url = uri("https://maven.pkg.github.com/oluiscabral/drop-core")
            credentials {
                username = "token"
                password = githubToken
            }
        }
    }

}

rootProject.name = "Drop"
include(":app")
 