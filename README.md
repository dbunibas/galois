# FLOQ


### Requirements
FLOQ is written in Java, and require a working JDK (>=21).

In addition, the following tools/subscriptions are needed:
- **PostgreSQL** - required for all projects
- **Ollama** - required to run experiments locally. In order to use Ollama, the `Constants.EXECUTOR` should be set to `OLLAMA_EXECUTOR` (default value). Note that Ollama might have low perfomances on some hosts, and it is suggested only for small models (e.g.: llama3.1:8b).
- **TogetherAI API Key** - optional - A valid subscription on TogetherAI is needed to run larger models (as `llama3.1:70b`), or to run RAG experiments. In order to use TogetherAI, the `Constants.EXECUTOR` should be set to `TOGETHERAI_EXECUTOR`, and the `TOGETHERAI_API` need to be specified.
- **ChromaDB** - optional - Needed only for RAG experiments

### Datasets
All datasets are available in the [resources](core/src/test/resources) folder

### Experiments
The project contains several Experiments. For each experiments, queries and prompt are defined in the corresponding test executor classes:
- [TestRunFlight2Batch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunFlight2Batch.java)
- [TestRunFlight4Batch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunFlight4Batch.java)
- [TestRunMoviesBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunMoviesBatch.java)
- [TestRunRAGFortuneBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunRAGFortuneBatch.java)
- [TestRunRAGPremierLeagueBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunRAGPremierLeagueBatch.java)
- [TestRunSpiderGeoBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunSpiderGeoBatch.java)
- [TestRunUSAPresidentsBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunUSAPresidentsBatch.java)
- [TestRunVenezuelaPresidentsBatch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunVenezuelaPresidentsBatch.java)
- [TestRunWorld1Batch.java](core/src/test/java/floq/test/experiments/run/batch/TestRunWorld1Batch.java)

### How to run an experiment

`./gradlew -i :core:test --tests "floq.test.experiments.run.batch.<EXPERIMENT-TO-EXECUTE>"`

For example

`./gradlew -i :core:test --tests "floq.test.experiments.run.batch.TestRunFlight2Batch"`
