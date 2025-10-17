plugins {
    id("java")
}

version = "2.4.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-logging:commons-logging:1.3.0")

    testImplementation("junit:junit:4.13.2")
}
