plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginCompose) // Mosaic renders Compose, so every module with @Composable code needs the compiler plugin
    `maven-publish`
}

dependencies {
    api(project(":library"))
    api(libs.mosaicRuntime)
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "ink-mosaic"
            pom {
                name.set("Ink - Terminal Design System (Mosaic components)")
                description.set("Palette, Label, Panel, Stack, tabs, status bar and application shell for Mosaic apps")
            }
        }
    }
    repositories { mavenLocal() }
}
