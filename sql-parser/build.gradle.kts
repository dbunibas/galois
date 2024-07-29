plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
}

version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":speedy-core"))

    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-core:1.5.1")
    testImplementation("ch.qos.logback:logback-classic:1.5.1")

    implementation("com.github.jsqlparser:jsqlparser:5.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
}

tasks.test {
    useJUnitPlatform()
}