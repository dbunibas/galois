plugins {
    id("java")
}

group = "org"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":jep"))

    implementation("com.mchange:c3p0:0.9.5.5")

    implementation("commons-io:commons-io:2.15.1")
    implementation("commons-lang:commons-lang:2.6")
    implementation("commons-logging:commons-logging:1.3.0")
    implementation("org.apache.commons:commons-math3:3.6.1")

    implementation("concurrent:concurrent:1.3.4")

    implementation("com.google.guava:guava:33.0.0-jre")

    implementation("com.fasterxml.jackson.core:jackson-annotations:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.16.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.16.1")

    implementation("org.jdom:jdom:1.1")

    implementation("com.mchange:mchange-commons-java:0.2.20")
    implementation("org.mybatis:mybatis:3.5.9")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.postgresql:postgresql:42.2.27")
    implementation("xerces:xercesImpl:2.12.2")

    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-core:1.5.1")
    testImplementation("ch.qos.logback:logback-classic:1.5.1")

    testImplementation("com.h2database:h2:2.2.222")
    testImplementation("org.hamcrest:hamcrest-core:1.3")

    testImplementation("junit:junit:4.13.2")
}
