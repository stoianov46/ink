plugins {
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinPluginCompose) apply false
}

allprojects {
    group = "com.idea"
    version = "0.4.0"
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> { jvmToolchain(21) }
        tasks.withType<Test>().configureEach { useJUnitPlatform() }
    }
}
