plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginCompose) // Mosaic renders Compose, so the module needs the compiler plugin
    `maven-publish`
}

dependencies {
    // Mosaic is part of Ink's API: the components take and return Mosaic colours, modifiers and composables.
    api(libs.mosaicRuntime)
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "ink"
            pom {
                name.set("Ink - Terminal Design System")
                description.set("Colours, themes, text editing, keys and the Mosaic components shared by Arcana and CosmoCLI")
            }
        }
    }
    repositories { mavenLocal() }
}
