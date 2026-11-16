rootProject.name = "Ink"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Mosaic's Compose runtime depends on androidx artifacts that are only published to Google's Maven repository.
        google {
            content { includeGroupByRegex("androidx.*") }
        }
    }
}

include(":library")
include(":mosaic")
include(":example")
