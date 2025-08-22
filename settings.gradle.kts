rootProject.name = "galois"

//val speedyRoot = settings.extra.properties["speedyRoot"]
val speedyRoot = "./Speedy"

include(":speedy-core", ":jep")
project(":speedy-core").projectDir = File("${speedyRoot}/core")
project(":jep").projectDir = File("${speedyRoot}/jep")

include(":core")
include(":sql-parser")