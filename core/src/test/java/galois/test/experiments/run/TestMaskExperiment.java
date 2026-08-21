package galois.test.experiments.run;

import dev.langchain4j.chain.ConversationalChain;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import galois.llm.query.IQueryExecutor;
import galois.llm.query.IQueryExecutorBuilder;
import galois.llm.query.utils.cache.LLMCache;
import galois.prompt.EPrompts;
import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.metrics.LLMDistance;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.*;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.persistence.relational.QueryManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static galois.llm.query.ConversationalChainFactory.buildTogetherAIConversationalChain;
import static galois.test.utils.TestUtils.toTupleList;
import static java.util.Collections.shuffle;

@Slf4j
public class TestMaskExperiment {
    static final int RANDOM_SEED = 42;

    static final PromptTemplate TEMPLATE = PromptTemplate.from("""
            You're given the following tuple from the {{tableName}} table:
            
            {{maskedTuple}}
            
            Return the value of the masked attribute "{{attribute}}".
            Only return the value, without any additional comment.
            Return the value as simple, unquoted text or plain number (integer or float).
            """);

    private final IQueryExecutor mockMaskTogetheraiQueryExecutor = new MockMaskTogetheraiQueryExecutor();

    @Test
    public void testWholeDataset() {
        int limit = 10;

        List<ExperimentInstance> instances = List.of(
                // Bird
                new ExperimentInstance("/llm-bench/bird/address-llama3-table-experiment.json", "bird-address", "state", "abbreviation"),
                new ExperimentInstance("/llm-bench/bird/books-llama3-table-experiment.json", "bird-books", "book", "publication_date"),
                new ExperimentInstance("/llm-bench/bird/college_completion-llama3-table-experiment.json", "bird-college_completion", "institution_details", "city"),
                new ExperimentInstance("/llm-bench/bird/cookbook-llama3-table-experiment.json", "bird-cookbook", "ingredient", "category"),
                new ExperimentInstance("/llm-bench/bird/disney-llama3-table-experiment.json", "bird-disney", "characters", "hero"),
                new ExperimentInstance("/llm-bench/bird/mondial_geo-llama3-table-experiment.json", "bird-mondial_geo", "mountain", "height"),
                new ExperimentInstance("/llm-bench/bird/movie-llama3-table-experiment.json", "bird-movie", "actor", "date_of_birth"),
                new ExperimentInstance("/llm-bench/bird/movies_4-llama3-table-experiment.json", "bird-movies_4", "person", "person_name", "SELECT T1.title, T2.job, T3.person_name FROM movie AS T1 INNER JOIN movie_crew AS T2 ON T1.movie_id = T2.movie_id INNER JOIN person AS T3 ON T2.person_id = T3.person_id and T2.job = 'Director' ORDER BY T1.oid"),
                new ExperimentInstance("/llm-bench/bird/olympics-llama3-table-experiment.json", "bird-olympics", "city", "city_name", "SELECT T3.games_year, T3.games_name, T3.season, T2.city_name FROM games_city AS T1 INNER JOIN city AS T2 ON T1.city_id = T2.id INNER JOIN games AS T3 ON T1.games_id = T3.id ORDER BY T2.oid"),
                new ExperimentInstance("/llm-bench/bird/university-llama3-table-experiment.json", "bird-university", "university", "country_name", "SELECT u.oid, university_name, country_name FROM university u JOIN country c ON u.country_id = c.id"),
                new ExperimentInstance("/llm-bench/bird/world-llama3-table-experiment.json", "bird-world", "country", "continent"),

                // Galois
                new ExperimentInstance("/llm-bench/galois/flight_2-llama3-table-experiment.json", "galois-flight_2", "usa_airline_companies", "call_sign"),
                new ExperimentInstance("/llm-bench/galois/flight_4-llama3-table-experiment.json", "galois-flight_4", "airports", "country"),
                new ExperimentInstance("/llm-bench/galois/movies-llama3-table-experiment.json", "galois-movies", "movie", "director"),
                new ExperimentInstance("/llm-bench/galois/spider_geo-llama3-table-experiment.json", "galois-spider_geo", "usa_lake", "state_name"),
                new ExperimentInstance("/llm-bench/galois/presidents-llama3-table-experiment.json", "galois-presidents", "world_presidents", "party"),
                new ExperimentInstance("/llm-bench/galois/world1-llama3-table-experiment.json", "galois-world1", "country", "name"),

                // Qatch
                new ExperimentInstance("/llm-bench/qatch/nobel_prize-llama3-table-experiment.json", "qatch-nobel_prize", "nobel_prize", "nobel_prize_year"),
                new ExperimentInstance("/llm-bench/qatch/chemical_element-llama3-table-experiment.json", "qatch-chemical_element", "chemical_element", "symbol"),
                new ExperimentInstance("/llm-bench/qatch/web_search_engine-llama3-table-experiment.json", "qatch-web_search_engine", "web_search_engine", "is_active"),
                new ExperimentInstance("/llm-bench/qatch/airport-llama3-table-experiment.json", "qatch-airport", "airport", "iata_code"),
                new ExperimentInstance("/llm-bench/qatch/video_game_publisher-llama3-table-experiment.json", "qatch-video_game_publisher", "video_game_publisher", "nation"),

                // Spider1
                new ExperimentInstance("/llm-bench/spider1/movie_1-llama3-table-experiment.json", "spider1-movie_1", "movie", "director"),
                new ExperimentInstance("/llm-bench/spider1/architecture-llama3-table-experiment.json", "spider1-architecture", "bridge", "location"),
                new ExperimentInstance("/llm-bench/spider1/geo-llama3-table-experiment.json", "spider1-geo", "usa_state", "capital"),
                new ExperimentInstance("/llm-bench/spider1/academic-llama3-table-experiment.json", "spider1-academic", "academic_journal", "homepage"),
                new ExperimentInstance("/llm-bench/spider1/imdb-llama3-table-experiment.json", "spider1-imdb", "director", "nationality")
        );

        List<ExperimentResult> results = executeInstances(instances, limit);
        saveToCSV(results, "masked-dataset");
    }

    @Test
    public void testQatchHighLimit() {
        int limit = 50;

        List<ExperimentInstance> instances = List.of(
                new ExperimentInstance("/llm-bench/qatch/nobel_prize-llama3-table-experiment.json", "qatch-nobel_prize", "nobel_prize", "nobel_prize_year"),
                new ExperimentInstance("/llm-bench/qatch/chemical_element-llama3-table-experiment.json", "qatch-chemical_element", "chemical_element", "symbol"),
                new ExperimentInstance("/llm-bench/qatch/web_search_engine-llama3-table-experiment.json", "qatch-web_search_engine", "web_search_engine", "is_active"),
                new ExperimentInstance("/llm-bench/qatch/airport-llama3-table-experiment.json", "qatch-airport", "airport", "iata_code"),
                new ExperimentInstance("/llm-bench/qatch/video_game_publisher-llama3-table-experiment.json", "qatch-video_game_publisher", "video_game_publisher", "nation")
        );

        List<ExperimentResult> results = executeInstances(instances, limit);
        saveToCSV(results, "qatch-high-limit");
    }

    @Test
    public void testGaloisPresidents() throws IOException {
        String experimentPath = "/llm-bench/galois/presidents-llama3-table-experiment.json";
        String databaseName = "galois-presidents";
        String tableName = "world_presidents";
        String sql = "SELECT * from " + tableName;
        String attribute = "party";

        int limit = 10;
        var results = executeExperimentForDataset(experimentPath, databaseName, tableName, sql, attribute, limit);
        saveToCSV(results, "galois-presidents-party");
    }

    @Test
    public void testBirdUniversitiesJoin() throws IOException {
        String experimentPath = "/llm-bench/bird/university-llama3-table-experiment.json";
        String databaseName = "bird-university";
        String tableName = "university";
        String sql = "SELECT u.oid, university_name, country_name FROM university u JOIN country c ON u.country_id = c.id";
        String attribute = "country_name";

        int limit = 10;
        var results = executeExperimentForDataset(experimentPath, databaseName, tableName, sql, attribute, limit);
        saveToCSV(results, "bird-university-country_name");
    }

    @Test
    public void testQatchWebSearchEngines() throws IOException {
        String experimentPath = "/llm-bench/qatch/web_search_engine-llama3-table-experiment.json";
        String databaseName = "qatch-web_search_engine";
        String tableName = "web_search_engine";
        String sql = "SELECT * FROM " + tableName;
        String attribute = "is_active";

        int limit = 10;
        var results = executeExperimentForDataset(experimentPath, databaseName, tableName, sql, attribute, limit);
        saveToCSV(results, "qatch-web_search_engine-is_active");
    }

    @Test
    public void testBirdMovies4() throws IOException {
        String experimentPath = "/llm-bench/bird/movies_4-llama3-table-experiment.json";
        String databaseName = "bird-movies_4";
        String tableName = "person";
        String sql = "SELECT T1.title, T2.job, T3.person_name FROM movie AS T1 INNER JOIN movie_crew AS T2 ON T1.movie_id = T2.movie_id INNER JOIN person AS T3 ON T2.person_id = T3.person_id and T2.job = 'Director' ORDER BY T1.oid";
        String attribute = "person_name";

        int limit = 10;
        var results = executeExperimentForDataset(experimentPath, databaseName, tableName, sql, attribute, limit);
        saveToCSV(results, "bird-movies_4-person_name");
    }

    @Test
    public void testBirdOlympics() throws IOException {
        String experimentPath = "/llm-bench/bird/olympics-llama3-table-experiment.json";
        String databaseName = "bird-olympics";
        String tableName = "city";
        String sql = "SELECT T3.games_year, T3.games_name, T3.season, T2.city_name FROM games_city AS T1 INNER JOIN city AS T2 ON T1.city_id = T2.id INNER JOIN games AS T3 ON T1.games_id = T3.id ORDER BY T2.oid";
        String attribute = "city_name";

        int limit = 10;
        var results = executeExperimentForDataset(experimentPath, databaseName, tableName, sql, attribute, limit);
        saveToCSV(results, "bird-olympics-city_name");
    }

    private List<ExperimentResult> executeInstances(List<ExperimentInstance> instances, int limit) {
        List<ExperimentResult> results = new ArrayList<>();
        for (ExperimentInstance instance : instances) {
            try {
                var instanceResults = executeExperimentForDataset(
                        instance.path(),
                        instance.databaseName(),
                        instance.tableName(),
                        instance.query(),
                        instance.attribute(),
                        limit
                );
                results.addAll(instanceResults);
            } catch (Exception ex) {
                log.error("Cannot execute experiment for database {}! Skipping...", instance.databaseName(), ex);
            }
        }
        return results;
    }

    private List<ExperimentResult> executeExperimentForDataset(String path, String databaseName, String tableName, String query, String attribute, int limit) throws IOException {
        List<ExperimentResult> results = new ArrayList<>();
        LLMDistance judge = new LLMDistance();

        Experiment experiment = ExperimentParser.loadAndParseJSON(path);
        DBMSDB db = experiment.createDatabaseForExpected();

        List<Tuple> tuples = getTuplesByQuery(query, db, limit);
        String attributeTableName = tuples.getFirst().getCells().getFirst()
                .getAttributeRef()
                .getTableName();
        // HACK: handles possible joins without aliases
        if (attributeTableName == null) attributeTableName = "";

        List<String> attributes = tuples.getFirst().getCells().stream()
                .map(Cell::getAttribute)
                .filter(name -> !name.equals("oid"))
                .toList();

        for (Tuple tuple : tuples) {
            // 1. Generate prompt
            StringBuilder maskedTuple = new StringBuilder();
            for (String attributeName : attributes) {
                maskedTuple.append(attributeName).append(": ");
                String value = attributeName.equalsIgnoreCase(attribute) ?
                        "XXXX" :
                        tuple.getCell(new AttributeRef(attributeTableName, attributeName)).getValue().toString();
                maskedTuple.append(value).append("\n");
            }
            Map<String, Object> params = Map.of(
                    "tableName", tableName,
                    "maskedTuple", maskedTuple.toString(),
                    "attribute", attribute
            );
            String prompt = TEMPLATE.apply(params).text();
            log.info(prompt);

            // 2. Execute and fetch result
            String result;
            if (LLMCache.getInstance().containsQuery(prompt, 0, mockMaskTogetheraiQueryExecutor, prompt)) {
                result = LLMCache.getInstance().getResponse(prompt, 0, mockMaskTogetheraiQueryExecutor, prompt).response();
            } else {
                ConversationalChain chain = buildTogetherAIConversationalChain(Configuration.getInstance().getTogetheraiApiKey(), Configuration.getInstance().getTogetheraiModel(), Configuration.getInstance().getTogetheraiReasoningEnabled());
                result = chain.execute(prompt);
                LLMCache.getInstance().updateCache(prompt, 0, mockMaskTogetheraiQueryExecutor, prompt, result, 0, 0, 0, 0);
            }

            // 3. Compute result
            String expected = tuple.getCell(new AttributeRef(attributeTableName, attribute)).getValue().toString();
            boolean areCellSimilar = judge.areCellSimilar(expected, result, null);
            log.info("expected: {} - result: {} - similar: {}", expected, result, areCellSimilar);
            results.add(new ExperimentResult(databaseName, tableName, attribute, maskedTuple.toString().replace("\n", "|"), expected, result, areCellSimilar));
        }

        return results;
    }

    private void saveToCSV(List<ExperimentResult> results, String experimentName) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss"));

        Path filePath = Paths.get(Configuration.getInstance().getResultsAbsolutePath(), experimentName + "-" + currentDate + ".csv");
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException ioe) {
            log.error("Cannot create the results directory {}!", filePath.getParent(), ioe);
            log.warn("Computed results for experiment {}: {}", experimentName, results);
            return;
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("databaseName", "tableName", "attribute", "maskedTuple", "expected", "result", "areCellSimilar")
                .build();
        try (PrintWriter writer = new PrintWriter(filePath.toFile());
             CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
            for (ExperimentResult result : results) {
                printer.printRecord(
                        result.databaseName(),
                        result.tableName(),
                        result.attribute(),
                        result.maskedTuple(),
                        result.expected(),
                        result.result(),
                        result.areCellSimilar()
                );
            }
        } catch (IOException ioe) {
            log.error("Cannot export the results in CSV format!", ioe);
            log.warn("Computed results for experiment {}: {}", experimentName, results);
        }
    }

    private List<Tuple> getTuplesByQuery(String query, DBMSDB database, int limit) {
        String resultsQuery = !query.toLowerCase().contains("order by") ? query + " ORDER BY oid" : query;
        resultsQuery = !resultsQuery.toLowerCase().contains("limit") ? resultsQuery + " LIMIT 100" : resultsQuery;
        ResultSet resultSet = QueryManager.executeQuery(resultsQuery, database.getAccessConfiguration());
        ITupleIterator iterator = new DBMSTupleIterator(resultSet);
        List<Tuple> results = new ArrayList<>(toTupleList(iterator));
        iterator.close();
        shuffle(results, new Random(RANDOM_SEED));
        return results.subList(0, Math.min(limit, results.size()));
    }

    record ExperimentInstance(String path, String databaseName, String tableName, String attribute,
                              String query) {
        public ExperimentInstance(String path, String databaseName, String tableName, String attribute) {
            this(path, databaseName, tableName, attribute, "SELECT * from " + tableName);
        }
    }

    private record ExperimentResult(String databaseName, String tableName, String attribute, String maskedTuple,
                                    String expected, String result, boolean areCellSimilar) {
    }

    // HACK: mock class, allows simple cache usage with the Togetherai model
    static final class MockMaskTogetheraiQueryExecutor implements IQueryExecutor {
        @Override
        public List<Tuple> execute(IDatabase database, TableAlias tableAlias, Double llmProbThreshold) {
            return List.of();
        }

        @Override
        public void setAttributes(List<AttributeRef> attributes) {
        }

        @Override
        public EPrompts getFirstPrompt() {
            return null;
        }

        @Override
        public EPrompts getIterativePrompt() {
            return null;
        }

        @Override
        public int getMaxIterations() {
            return 0;
        }

        @Override
        public ContentRetriever getContentRetriever() {
            return null;
        }

        @Override
        public IQueryExecutorBuilder getBuilder() {
            return null;
        }
    }
}
