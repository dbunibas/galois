plugins {
    id("java")
    id("io.freefair.lombok") version "8.14.1"
}

version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":speedy-core"))
    implementation(project(":sql-parser"))

    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-core:1.5.1")
    testImplementation("ch.qos.logback:logback-classic:1.5.1")

    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("jaxen:jaxen:2.0.0")

    implementation("dev.langchain4j:langchain4j:0.35.0")
    implementation("dev.langchain4j:langchain4j-ollama:0.35.0")
    implementation("dev.langchain4j:langchain4j-easy-rag:0.35.0")
    implementation("dev.langchain4j:langchain4j-chroma:0.35.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:0.35.0")

    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.18.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.11.0")

    implementation("commons-lang:commons-lang:2.6")
    //implementation("com.github.jsqlparser:jsqlparser:4.9")
    implementation("com.github.jsqlparser:jsqlparser:5.0")

    implementation("org.apache.commons:commons-text:1.10.0")

    implementation("com.knuddels:jtokkit:1.0.0")

    implementation("org.apache.commons:commons-csv:1.11.0")

    implementation("org.apache.poi:poi-ooxml:5.2.3")

    implementation("com.google.guava:guava:33.3.1-jre")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}