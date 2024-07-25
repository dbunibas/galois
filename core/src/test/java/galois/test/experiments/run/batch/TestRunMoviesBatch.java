package galois.test.experiments.run.batch;

import com.galois.sqlparser.SQLQueryParser;
import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.test.model.ExpVariant;
import galois.test.utils.ExcelExporter;
import galois.test.utils.TestRunner;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static speedy.utility.SpeedyUtility.printMap;

@Slf4j
public class TestRunMoviesBatch {
    private static final String EXP_NAME = "MOVIES";
    private static final String RESULT_FILE_DIR = "src/test/resources/results/";
    private static final String RESULT_FILE = "movies-results.txt";

    private static final TestRunner testRunner = new TestRunner();
    private static final ExcelExporter exportExcel = new ExcelExporter();

    private final List<ExpVariant> variants;

    public TestRunMoviesBatch() {
        List<String> singleConditionOptimizers = List.of(
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );
        List<String> multipleConditionsOptimizers = List.of(
                "SingleConditionsOptimizerFactory",
                "SingleConditionsOptimizerFactory-WithFilter",
                "AllConditionsPushdownOptimizer",
                "AllConditionsPushdownOptimizer-WithFilter"
        );

        ExpVariant q1 = ExpVariant.builder()
                .queryNum("Q1")
                .querySql("select m.originaltitle from target.movie m where m.director='Richard Thorpe'")
                .prompt("List the title of the movies directed by Richard Thorpe")
                .optimizers(singleConditionOptimizers)
                .build();
        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("select m.originaltitle from target.movie m where m.director='Steven Spielberg'")
                .prompt("List the title of the movies directed by Steven Spielberg")
                .optimizers(singleConditionOptimizers)
                .build();
        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("select m.originaltitle, m.startyear from target.movie m where m.director='Richard Thorpe' and m.startyear > 1950")
                .prompt("List the title and year of the movies directed by Richard Thorpe after the 1950")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select m.originaltitle, m.startyear from target.movie m where m.director='Steven Spielberg' and m.startyear > 2000")
                .prompt("List the title and year of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select m.originaltitle, m.startyear, m.genres, m.birthyear from target.movie m where m.director='Steven Spielberg' and m.startyear > 2000")
                .prompt("List the title, year, genres and birthyear of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("select m.originaltitle, m.startyear, m.genres, m.birthyear, m.deathyear, m.runtimeminutes from target.movie m where m.director = 'Steven Spielberg' and m.startyear > 1990 and m.endyear < 2000")
                .prompt("List the title, year, genres, birthyear, deathyear and runtimeminutes of the movies directed by Steven Spielberg between the 1990 and the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select m.startyear, count(*) as numMovies from target.movie m where m.director = 'Steven Spielberg' and m.startyear is not null group by m.startyear")
                .prompt("List the year and the number of produced movies in that year directed by Steven Spielberg.")
                .optimizers(multipleConditionsOptimizers)
                .build();
        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select m.startyear, count(*) as count from target.movie m where m.director = 'Tim Burton' group by m.startyear order by count desc limit 1")
                .prompt("Return the most prolific year of Tim Burton")
                .optimizers(singleConditionOptimizers)
                .build();
        // FIXME: Which Speedy tree can execute this query?
        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select m.director, (m.startyear - m.birthyear) as director_age from target.movie m where m.startyear is not null and m.birthyear is not null order by director_age desc limit 1")
                .prompt("Return the oldest film director")
                .optimizers(multipleConditionsOptimizers)
                .build();
        variants = List.of(q1, q2, q3, q4, q5, q6, q7);
    }

    @Test
    public void testCanParseSQLQueries() {
        SQLQueryParser sqlQueryParser = new SQLQueryParser();
        for (ExpVariant variant : variants) {
            log.info("Parsing query {}", variant.getQueryNum());
            assertDoesNotThrow(() -> sqlQueryParser.parse(variant.getQuerySql()));
        }
    }

    @Test
    public void testRunBatch() {
        List<IMetric> metrics = new ArrayList<>();
        Map<String, Map<String, ExperimentResults>> results = new HashMap<>();
        for (ExpVariant variant : variants) {
            testRunner.execute("/movies/movies-llama3-nl-experiment.json", "NL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/movies/movies-llama3-sql-experiment.json", "SQL", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/movies/movies-llama3-table-experiment.json", "TABLE", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/movies/movies-llama3-key-experiment.json", "KEY", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
            testRunner.execute("/movies/movies-llama3-key-scan-experiment.json", "KEY-SCAN", variant, metrics, results, RESULT_FILE_DIR, RESULT_FILE);
        }
        log.info("Results\n{}", printMap(results));
        exportExcel.export(EXP_NAME, metrics, results);
    }

}
