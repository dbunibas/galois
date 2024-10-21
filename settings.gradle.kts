rootProject.name = "floq"

include(":engine-core", ":jep")
project(":engine-core").projectDir = File("./engine/core")
project(":jep").projectDir = File("./engine/jep")

include(":core")
include(":sql-parser")