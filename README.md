# Galois

[Logical and Physical Optimizations for SQL Query Execution over Large Language Models](https://dl.acm.org/doi/10.1145/3725411) code base.

## System Requirements

Galois is written in Java, and require a working JDK (>=21).

To execute the experiments, the following tools / subscriptions are needed:

- a **PostgreSQL 11** instance - required for all experiments;
- a **ChromaDB** instance - required only for RAG experiments;
- a **TogetherAI API Key** - a valid subscription to [TogetherAI](https://www.together.ai/) is needed to run the experiments using the TogetherAI hosted open source models (ex. Llama 3.1 70B), or to run RAG experiments;
- an **OpenAI API Key** - a valid subscription to [OpenAI](https://openai.com/api/) is needed to run the experiments using the OpenAI proprietary models.

As the model inference step is performed using cloud services, there is no need of dedicated hardware in order to execute the experiments.

## Cloning the Repo

The repository uses git submodules to import an external Java dependency, [Speedy](https://github.com/dbunibas/Speedy/tree/gradle) (gradle branch).

The repository can be cloned with all the dependencies using the command:

```shell
git clone -b reproducibility https://github.com/dbunibas/galois.git --recurse-submodules
```

---

Note: cloning the repository without the `--recurse-submodules` flag will not download the [Speedy](https://github.com/dbunibas/Speedy/tree/gradle) dependency and will incur in a missing dependency error - `Could not resolve project :speedy-core.`.

In this case the missing dependency can be initialized using the command:

```shell
git submodule update -init
```

## Execute the Experiments

### System Setup

To simplify project startup, a `docker-compose.yml` file containing all the necessary tools (PostgreSQL and ChromaDB) is available in the repository.

To run the containers, from the root folder:

```shell
cd docker
docker compose up -d
```

This command will create a new container named "galois" running two services: PostgreSQL and ChromaDB.

**PostgreSQL**: a Postgres 11.10 instance, exposing port 5432.

**ChromaDB**: a Chroma 0.6.4.dev226 instance, exposing port 8000.

The database can be accessed using external tools (e.g. DBeaver) using the following configuration (specified in the docker compose file):

```text
Database name: galois
Database user: pguser
Database pass: pguser
```

---

A snapshot of the database and the vector store is available as a [zip archive](docker/docker-data.zip).

Unzipping the archive in the docker folder before running the container is sufficient to get the system up and running.

Note: the snapshot is not required in order to successfully run the experiments, but using it can improve the execution times especially in the RAG scenario.

### System Configuration

All the configuration is handled through a `configuration.properties` file in the [resources](core/src/main/resources).

A [template](core/src/main/resources/configuration.properties.template) is provided as a starting point and contains the description of all the keys.

The default keys are valid for the majority of properties except for the absolute paths of files, in particular:

```text
cache.path
export.results-path
export.excel-path
```

### Datasets

All datasets are available in the [resources](core/src/test/resources) folder.

### Experiment Execution

The experimets can be executed using the executors in the [executors](executors).

There are 9 experiments available:
- Flight 2;
- Flight 4;
- Movies;
- Presidents USA;
- Presidents Venezuela;
- RAG Fortune;
- RAG Premier League;
- Spider Geo;
- Spider World;

Each experiment comes with two variants: the `-openai` and `-togetherai` one. The first one executes the experiment using the model hosted on the OpenAI platform, while the second one executes the experiment using the model hosted on the TogetherAI platform.

---

Note: the [scripts](scripts) folder contains three scripts to execute all the experiments in batch:

- execute-all-experiments - execute both variants of all the experiments;
- execute-experiments-openai - execute the OpenAI variant of all the experiments;
- execute-experiments-togetherai - execute the TogetherAI variant of all the experiments;

---

Additional note: an executor is a straightforward way of executing one (or more) test(s) from the codebase. Each executor is a wrapper that run its relative test using the Gradle wrapper.

## Execute a User-Defined Query

The system can execute a user-defined query using the [execute-query-json.sh](scripts/execute-query-json.sh) script.

The script executes the full Galois pipeline starting from a query defined in json format. In order to be executable a JSON query must specify:

- databaseDriver - the JDBC driver to use;
- databaseURI - the JDBC URI;
- databaseSchema - the database schema
- databaseUser - the database user for granting access;
- databasePassword - the database password for granting access;
- sql - the SQL query
- confidenceThreshold - the confidence threshold (between 0 and 1, default 0.6).

Example:

```json
{
  "databaseDriver": "org.postgresql.Driver",
  "databaseURI": "jdbc:postgresql:llm_directors_movies",
  "databaseSchema": "public",
  "databaseUser": "pguser",
  "databasePassword": "pguser",
  "sql": "SELECT m.originaltitle FROM movie m WHERE m.director='Steven Spielberg'",
  "confidenceThreshold": 0.6
}
```

To execute the query pass the absolute path of the JSON query as the first argument of the script:

```shell
./execute-query-json.sh /ABSOULTE/PATH/TO/query.json
```