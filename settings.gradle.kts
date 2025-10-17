rootProject.name = "bsf"

val queryExecutorRoot = "./query-executor"

include(":query-executor-core", ":jep")
project(":query-executor-core").projectDir = File("${queryExecutorRoot}/core")
project(":jep").projectDir = File("${queryExecutorRoot}/jep")

include(":core")
include(":sql-parser")