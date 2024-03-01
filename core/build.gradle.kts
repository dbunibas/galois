plugins {
    id("java")
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

    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("jaxen:jaxen:2.0.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}