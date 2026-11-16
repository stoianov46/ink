plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginCompose)
    application
}

dependencies {
    implementation(project(":mosaic"))
    implementation(libs.kotlinxCoroutines)
    testImplementation(kotlin("test"))
}

application {
    mainClass = "com.idea.ink.example.MainKt"
    applicationName = "ink"
}
