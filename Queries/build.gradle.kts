plugins {
    id("java") // Apply the Java plugin
}

repositories {
    mavenCentral() // Use Maven Central for dependencies
}

dependencies {
    implementation(project(":Shared")) // Add dependency on the Shared module
    implementation("com.rabbitmq:amqp-client:5.18.0") // RabbitMQ dependency
    implementation("org.xerial:sqlite-jdbc:3.41.2.2") // SQLite dependency
}


tasks.withType<JavaCompile> {
    modularity.inferModulePath.set(false)
}