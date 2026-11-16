plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

dependencies {
    // No runtime dependencies: colours, themes and terminal utilities are plain Kotlin.
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "ink"
            pom {
                name.set("Ink - Terminal Design System (core)")
                description.set("Colour maths, themes and terminal utilities shared by Arcana and CosmoCLI")
            }
        }
    }
    repositories { mavenLocal() }
}
