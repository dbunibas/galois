# Beyond Single Facts
### A Benchmark for Evaluating Relational Fact Retrieval from Large Language Models


### Requirements
 Beyond Single Facts is written in Java, and require a working JDK (>=21).

In addition, the following tools/subscriptions are needed:
- **PostgreSQL** - required for all projects
- **OpenAI API Key** - optional - A valid subscription on OpenAI is needed to run proprietary models (as `gpt-4.1`). In order to use OpenAI, the `Constants.LLM_MODEL` should be set to `Constants.MODEL_GPT`, and the `Constants.OPEN_AI_API_KEY` need to be specified.
- **TogetherAI API Key** - optional - A valid subscription on TogetherAI is needed to run larger models (as `llama3.1:70b`), or to run RAG experiments. In order to use TogetherAI, the `Constants.LLM_MODEL` should be set to `Constants.MODEL_TOGETHERAI`, and the `Constants.TOGETHERAI_API` need to be specified.

To simplify project startup, a `docker-compose.yml` file containing a postgres DBMS is available in the repository.

```shell
cd docker
docker compose up -d
```

This command will start PostgreSQL in the background. 

### How to Run Experiments

All 696 queries in the benchmark can be automatically executed using the [`TestBenchLLM`](core/src/test/java/bsf/test/experiments/run/batch/TestBenchLLM.java) class.

By default, the test runs all queries listed in the [`Excel file`](core/src/test/resources/llm-bench/dataset-soccer.xlsx) specified by the `TestBenchLLM.QUERIES_PATH` property.  
You can also create a custom Excel file with a subset of queries if you want to run a smaller experiment.

The language model used for the experiments is defined by the `Constants.LLM_MODEL` property. You can change this to select a different model (e.g., GPT, LLaMA).

Once the project is properly configured, you can launch the benchmark with the following command:

`./gradlew -i :core:test --rerun --tests "bsf.test.experiments.run.batch.TestBenchLLM"`
