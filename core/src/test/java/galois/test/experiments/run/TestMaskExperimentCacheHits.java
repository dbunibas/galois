package galois.test.experiments.run;

import galois.llm.query.IQueryExecutor;
import galois.llm.query.utils.cache.LLMCache;
import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.AttributeRef;
import speedy.model.database.Cell;
import speedy.model.database.Tuple;
import speedy.model.database.dbms.DBMSDB;
import speedy.model.database.dbms.DBMSTupleIterator;
import speedy.persistence.relational.QueryManager;

import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static galois.test.experiments.run.TestMaskExperiment.RANDOM_SEED;
import static galois.test.experiments.run.TestMaskExperiment.TEMPLATE;
import static galois.test.utils.TestUtils.toTupleList;
import static java.util.Collections.shuffle;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TestMaskExperimentCacheHits {
    private final IQueryExecutor mockMaskTogetheraiQueryExecutor = new TestMaskExperiment.MockMaskTogetheraiQueryExecutor();

    @Test
    public void testCheckCacheWholeDataset() throws IOException {
        int limit = 10;

        List<TestMaskExperiment.ExperimentInstance> instances = List.of(
                // Bird
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/address-llama3-table-experiment.json", "bird-address", "state", "abbreviation"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/books-llama3-table-experiment.json", "bird-books", "book", "publication_date"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/college_completion-llama3-table-experiment.json", "bird-college_completion", "institution_details", "city"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/cookbook-llama3-table-experiment.json", "bird-cookbook", "ingredient", "category"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/disney-llama3-table-experiment.json", "bird-disney", "characters", "hero"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/mondial_geo-llama3-table-experiment.json", "bird-mondial_geo", "mountain", "height"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/movie-llama3-table-experiment.json", "bird-movie", "actor", "date_of_birth"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/movies_4-llama3-table-experiment.json", "bird-movies_4", "person", "person_name", "SELECT T1.title, T2.job, T3.person_name FROM movie AS T1 INNER JOIN movie_crew AS T2 ON T1.movie_id = T2.movie_id INNER JOIN person AS T3 ON T2.person_id = T3.person_id and T2.job = 'Director' ORDER BY T1.oid"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/olympics-llama3-table-experiment.json", "bird-olympics", "city", "city_name", "SELECT T3.games_year, T3.games_name, T3.season, T2.city_name FROM games_city AS T1 INNER JOIN city AS T2 ON T1.city_id = T2.id INNER JOIN games AS T3 ON T1.games_id = T3.id ORDER BY T2.oid"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/university-llama3-table-experiment.json", "bird-university", "university", "country_name", "SELECT u.oid, university_name, country_name FROM university u JOIN country c ON u.country_id = c.id"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/bird/world-llama3-table-experiment.json", "bird-world", "country", "continent"),

                // Galois
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/flight_2-llama3-table-experiment.json", "galois-flight_2", "usa_airline_companies", "call_sign"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/flight_4-llama3-table-experiment.json", "galois-flight_4", "airports", "country"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/movies-llama3-table-experiment.json", "galois-movies", "movie", "director"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/spider_geo-llama3-table-experiment.json", "galois-spider_geo", "usa_lake", "state_name"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/presidents-llama3-table-experiment.json", "galois-presidents", "world_presidents", "party"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/galois/world1-llama3-table-experiment.json", "galois-world1", "country", "name"),

                // Qatch
                new TestMaskExperiment.ExperimentInstance("/llm-bench/qatch/nobel_prize-llama3-table-experiment.json", "qatch-nobel_prize", "nobel_prize", "nobel_prize_year"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/qatch/chemical_element-llama3-table-experiment.json", "qatch-chemical_element", "chemical_element", "symbol"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/qatch/web_search_engine-llama3-table-experiment.json", "qatch-web_search_engine", "web_search_engine", "is_active"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/qatch/airport-llama3-table-experiment.json", "qatch-airport", "airport", "iata_code"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/qatch/video_game_publisher-llama3-table-experiment.json", "qatch-video_game_publisher", "video_game_publisher", "nation"),

                // Spider1
                new TestMaskExperiment.ExperimentInstance("/llm-bench/spider1/movie_1-llama3-table-experiment.json", "spider1-movie_1", "movie", "director"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/spider1/architecture-llama3-table-experiment.json", "spider1-architecture", "bridge", "location"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/spider1/geo-llama3-table-experiment.json", "spider1-geo", "usa_state", "capital"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/spider1/academic-llama3-table-experiment.json", "spider1-academic", "academic_journal", "homepage"),
                new TestMaskExperiment.ExperimentInstance("/llm-bench/spider1/imdb-llama3-table-experiment.json", "spider1-imdb", "director", "nationality")
        );

        for (TestMaskExperiment.ExperimentInstance instance : instances) {
            assertTrue(
                    checkValuesInCache(
                            instance.path(),
                            instance.databaseName(),
                            instance.tableName(),
                            instance.query(),
                            instance.attribute(),
                            limit
                    )
            );
        }
    }

    private boolean checkValuesInCache(String path, String databaseName, String tableName, String query, String attribute, int limit) throws IOException {
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

        List<Boolean> results = new ArrayList<>();
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
            results.add(LLMCache.getInstance().containsQuery(prompt, 0, mockMaskTogetheraiQueryExecutor, prompt));
        }

        for (Boolean result : results) {
            if (!result) return false;
        }

        return true;
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
}
