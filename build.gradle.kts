plugins {
    application
    idea
}

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

sourceSets {
    getByName("main").apply {
        java.srcDir("app/src/main/java")
        resources.srcDir("app/src/main/resources")
    }
    getByName("test").apply {
        java.srcDir("app/src/test/java")
        resources.srcDir("app/src/test/resources")
    }
}

dependencies {
    implementation("mysql:mysql-connector-java:8.0.25")

    implementation("com.fasterxml.jackson.core:jackson-core:2.11.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.11.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.2")
    implementation("org.apache.commons:commons-collections4:4.4")

//    implementation("org.jdbi:jdbi3-postgres:3.19.0")
    implementation("org.jdbi:jdbi3-json:3.20.0")

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // This dependency is used by the application.
    implementation("com.google.guava:guava:29.0-jre")
}

application {
    // Define the main class for the application.
    mainClass.set("com.example.App")
}

tasks.test {
    testLogging.showStandardStreams = true
    testLogging.showExceptions = true
    useJUnitPlatform()
}

tasks.wrapper {
    gradleVersion = "7.1"
    distributionType = Wrapper.DistributionType.BIN
}
