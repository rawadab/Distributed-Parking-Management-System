plugins {
    id("java") // Apply the Java plugin
    id("application") // Apply the Application plugin
}

repositories {
    mavenCentral() // Use Maven Central for dependencies
}

dependencies {
    implementation(project(":Shared"))
    implementation(project(":recommender"))
    implementation(project(":Queries")) // Add dependency on the Queries module
    implementation("org.openjfx:javafx-controls:19.0.2") // JavaFX dependency
    implementation("org.openjfx:javafx-fxml:19.0.2") // JavaFX FXML dependency

    // Add JUnit 5 for unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:4.3.1")

}

application {
    mainClass = "com.example.customerui.CustomerUI"
}

tasks.withType<JavaExec> {
    jvmArgs = listOf(
            "--module-path", configurations.runtimeClasspath.get().asPath,
            "--add-modules", "javafx.controls,javafx.fxml"
    )
}

tasks.withType<JavaCompile> {
    modularity.inferModulePath.set(false)
}

// ✅ Enable JUnit 5 for testing
tasks.withType<Test> {
    useJUnitPlatform()
}