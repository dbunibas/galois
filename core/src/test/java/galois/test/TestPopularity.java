package galois.test;

import galois.Constants;
import galois.llm.models.TogetherAIModel;
import galois.llm.models.togetherai.TogetherAIConstants;
import galois.llm.query.utils.QueryUtils;
import galois.parser.ParserWhere;
import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.model.ExpVariant;
import galois.utils.Mapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.Test;
import speedy.model.database.Attribute;
import speedy.model.database.IDatabase;
import speedy.model.database.ITable;
import speedy.model.database.Key;

@Slf4j
public class TestPopularity {

    public List<ExpVariant> getGEOVariants() {
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
                .querySql("SELECT state_name, population, area FROM target.usa_state")
                .prompt("List the state name, population and area from USA states")
                .optimizers(List.of())
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT us.state_name, us.capital, us.area FROM target.usa_state us")
                .prompt("List the state name, capital and area from USA states")
                .optimizers(List.of())
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT state_name, population, area FROM target.usa_state where capital = 'Frankfort'")
                .prompt("List the state name, population and area from USA states where the capital is Frankfort")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT us.state_name, us.population, us.capital FROM target.usa_state us where us.population > 5000000")
                .prompt("List the state name, population and capital from USA states where the population is greater than 5000000")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT us.state_name, us.capital FROM public.usa_state us where us.population > 5000000 AND us.density < 1000")
                .prompt("List the state name and capital from USA states where the population is greater than 5000000 and the density is lower than 1000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT us.state_name, us.capital, us.density, us.population FROM target.usa_state us where us.population > 5000000 AND us.density < 1000 AND us.area < 50000")
                .prompt("List the state name, capital, density and population from USA states where the population is greater than 5000000, the density is lower than 1000 and the area is lower than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select us.state_name, us.capital from usa_state us where us.population > 3000000 AND us.area > 50000 order by us.capital")
                .prompt("List the state name and capital ordered by capital from USA states where the population is greater than 3000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("select us.state_name, us.capital, us.population from usa_state us where us.population > 3000000 AND us.population < 8000000 AND us.area > 50000 order by us.population")
                .prompt("List the state name capital and population ordered by population from USA states where the population is greater than 3000000 and lower than 8000000 and the area is greater than 50000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("select us.state_name, us.capital, us.population, us.area from usa_state us where us.population = 4700000 AND area=56153")
                .prompt("List the state name, the capital, the popoulation and the area from USA states where the population is 4700000 and the are is 56153")
                .optimizers(multipleConditionsOptimizers)
                .build();

        return List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
    }

    public List<ExpVariant> getUSAPresidentsVariants() {
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
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='United States'")
                .prompt("List the name and party of USA presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name and party of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) as party from target.international_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("Count the number of US presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='United States' AND p.party='Republican'")
                .prompt("List the name of USA presidents where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='United States' AND p.party='Republican' AND p.start_year > 1980")
                .prompt("List the name of USA presidents after 1980 where party is Republican")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party from target.international_presidents p WHERE p.country='United States'")
                .prompt("List the name, the start year, the end year, the number of president and the party of USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num from target.international_presidents p WHERE p.country='United States' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more USA presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) from target.international_presidents p where p.country='United States' AND p.start_year >= 1990  AND p.start_year < 2000")
                .prompt("count U.S. presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name from target.international_presidents p where p.country='United States' AND p.party='Whig' order by p.end_year desc limit 1")
                .prompt("List the name of the last USA president where party is Whig")
                .optimizers(multipleConditionsOptimizers)
                .build();

        return List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
    }

    public List<ExpVariant> getVenezuelaPresidentsVariants() {
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
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name and party of Venezuela presidents.")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q2 = ExpVariant.builder()
                .queryNum("Q2")
                .querySql("SELECT p.name, p.party from target.international_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("List the name and party of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q3 = ExpVariant.builder()
                .queryNum("Q3")
                .querySql("SELECT count(p.party) as party from target.international_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("Count the number of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='Venezuela' AND p.party='Liberal'")
                .prompt("List the name of Venezuela presidents where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("SELECT p.name from target.international_presidents p WHERE p.country='Venezuela' AND p.party='Liberal' AND p.start_year > 1858")
                .prompt("List the name of Venezuela presidents after 1858 where party is Liberal")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("SELECT p.name, p.start_year, p.end_year, p.cardinal_number, p.party from target.international_presidents p WHERE p.country='Venezuela'")
                .prompt("List the name, the start year, the end year, the number of president and the party of Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("SELECT p.party, count(p.party) num from target.international_presidents p WHERE p.country='Venezuela' group by p.party order by num desc limit 1")
                .prompt("List the party name and the number of presidents of the party with more Venezuela presidents")
                .optimizers(singleConditionOptimizers)
                .build();

        ExpVariant q8 = ExpVariant.builder()
                .queryNum("Q8")
                .querySql("SELECT count(*) from target.international_presidents p where p.country='Venezuela' AND p.start_year >= 1990  AND p.start_year < 2000")
                .prompt("count Venezuela presidents who began their terms in the 1990 and finish it in 2000.")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q9 = ExpVariant.builder()
                .queryNum("Q9")
                .querySql("SELECT p.name from target.international_presidents p where p.country='Venezuela' AND p.party = 'Military' order by p.end_year desc limit 1")
                .prompt("List the name of the last Venezuela president where party is Military")
                .optimizers(multipleConditionsOptimizers)
                .build();

        return List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
    }

    public List<ExpVariant> getMoviesVariants() {
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
                .querySql("select m.originaltitle, m.startyear from target.movie m where m.director='Richard Thorpe' AND m.startyear > 1950")
                .prompt("List the title and year of the movies directed by Richard Thorpe after the 1950")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q4 = ExpVariant.builder()
                .queryNum("Q4")
                .querySql("select m.originaltitle, m.startyear from target.movie m where m.director='Steven Spielberg' AND m.startyear > 2000")
                .prompt("List the title and year of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q5 = ExpVariant.builder()
                .queryNum("Q5")
                .querySql("select m.originaltitle, m.startyear, m.genres, m.birthyear from target.movie m where m.director='Steven Spielberg' AND m.startyear > 2000")
                .prompt("List the title, year, genres and birthyear of the movies directed by Steven Spielberg after the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q6 = ExpVariant.builder()
                .queryNum("Q6")
                .querySql("select m.originaltitle, m.startyear, m.genres, m.birthyear, m.deathyear, m.runtimeminutes from target.movie m where m.director = 'Steven Spielberg' AND m.startyear > 1990 AND m.startyear < 2000")
                .prompt("List the title, year, genres, birthyear, deathyear and runtimeminutes of the movies directed by Steven Spielberg between the 1990 and the 2000")
                .optimizers(multipleConditionsOptimizers)
                .build();

        ExpVariant q7 = ExpVariant.builder()
                .queryNum("Q7")
                .querySql("select m.startyear, count(*) as numMovies from target.movie m where m.director = 'Steven Spielberg' AND m.startyear is not null group by m.startyear")
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
                .querySql("select m.director, (m.startyear - m.birthyear) as director_age from target.movie m where m.startyear is not null AND m.birthyear is not null order by director_age desc limit 1")
                .prompt("Return the oldest film director")
                .optimizers(multipleConditionsOptimizers)
                .build();

        return List.of(q1, q2, q3, q4, q5, q6, q7, q8, q9);
    }

    @Test
    public void testPopularity() {
//        String expPath = "/geo_data/geo-llama3-table-experiment.json";
//        String tableName = "usa_state";
//        List<ExpVariant> variants = getGEOVariants();

//        String expPath = "/presidents/presidents-llama3-table-experiment.json";
//        String tableName = "international_presidents";
//        List<ExpVariant> variants = getUSAPresidentsVariants();
//        List<ExpVariant> variants = getVenezuelaPresidentsVariants();
        String expPath = "/movies/movies-llama3-table-experiment.json";
        String tableName = "movie";
        List<ExpVariant> variants = getMoviesVariants();
        try {
            for (ExpVariant variant : variants) {
                Experiment experiment = ExperimentParser.loadAndParseJSON(expPath);
                IDatabase database = experiment.getQuery().getDatabase();
                ITable table = database.getTable(tableName);
//                System.out.println("Table: " + table);
                List<Key> keys = database.getPrimaryKeys();
                Key key = keys.get(0);
//                System.out.println("key: " + key);
//                String jsonForKeys = QueryUtils.generateJsonSchemaForKeys(table);
                String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
//                System.out.println(jsonSchema);
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(variant.getQuerySql());
                String whereExpression = parserWhere.getWhereExpression();
                if (whereExpression != null && !whereExpression.isEmpty()) {
                    System.out.println("Where: " + whereExpression);
                }
                String prompt = "Given the following JSON schema:\n";
                prompt += jsonSchema + "\n";
                prompt += "What is the popularity in your knowledge of " + key.toString() + " of " + tableName;
                if (whereExpression != null && !whereExpression.trim().isEmpty()) {
                    prompt += " where " + whereExpression;
                }
                prompt += "?\n";
                prompt += """
                          Return a value between 0 and 1. Where 1 is very popular and 0 is not popular at all.
                          Respond with JSON only with a numerical property with name "popularity".""";
                System.out.println(variant.getQuerySql());
//                System.out.println("Prompt: " + prompt);
                TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_8B, TogetherAIConstants.STREAM_MODE);
                String response = model.generate(prompt);
//                System.out.println("Response: " + response);
                String cleanResponse = Mapper.toCleanJsonObject(response);
//                System.out.println("Response: " + cleanResponse);
                Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
                Double popularity = (Double) parsedResponse.getOrDefault("popularity", -1.0);
                System.out.println("Popularity: " + popularity);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void testCardinality() {
//        String expPath = "/geo_data/geo-llama3-table-experiment.json";
//        String tableName = "usa_state";
//        List<ExpVariant> variants = getGEOVariants();

//        String expPath = "/presidents/presidents-llama3-table-experiment.json";
//        String tableName = "international_presidents";
//        List<ExpVariant> variants = getUSAPresidentsVariants();
//        List<ExpVariant> variants = getVenezuelaPresidentsVariants();
        String expPath = "/movies/movies-llama3-table-experiment.json";
        String tableName = "movie";
        List<ExpVariant> variants = getMoviesVariants();
        try {
            for (ExpVariant variant : variants) {
                Experiment experiment = ExperimentParser.loadAndParseJSON(expPath);
                IDatabase database = experiment.getQuery().getDatabase();
                ITable table = database.getTable(tableName);
//                System.out.println("Table: " + table);
                List<Key> keys = database.getPrimaryKeys();
                Key key = keys.get(0);
//                System.out.println("key: " + key);
//                String jsonForKeys = QueryUtils.generateJsonSchemaForKeys(table);
                String jsonSchema = QueryUtils.generateJsonSchemaListFromAttributes(table, table.getAttributes());
//                System.out.println(jsonSchema);
                ParserWhere parserWhere = new ParserWhere();
                parserWhere.parseWhere(variant.getQuerySql());
                String whereExpression = parserWhere.getWhereExpression();
                String prompt = "Given the following JSON schema:\n";
                prompt += jsonSchema + "\n";
                prompt += "Estimate the cardinality of " + key.toString() + " of " + tableName;
                String promptAllConditionPushDown = prompt;
                List<String> promptSinglePushDown = new ArrayList<>();
                for (Expression expression : parserWhere.getExpressions()) {
                    String singleCondition = prompt + " where " + expression.toString();
                    promptSinglePushDown.add(singleCondition);
                }
                if (whereExpression != null && !whereExpression.trim().isEmpty()) {
                    promptAllConditionPushDown += " where " + whereExpression;
                }
                String endPrompt = "\n";
                endPrompt += """
                          Respond with JSON only with a numerical property with name "cardinality".""";

                String promptNoPushDown = prompt + endPrompt;
                promptAllConditionPushDown += endPrompt;
                List<String> promptSingleConditionPushDown = new ArrayList<>();
                for (String single : promptSinglePushDown) {
                    promptSingleConditionPushDown.add(single + endPrompt);
                }
                System.out.println(variant.getQuerySql());
                Integer cardinalityNoPushDown = getCardinality(promptNoPushDown);
                System.out.println(promptNoPushDown);
                System.out.println("Cardinality No Push Down: " + cardinalityNoPushDown);
                if (whereExpression != null && !whereExpression.trim().isEmpty()) {
                    Integer cardinalityAllConditionPushDown = getCardinality(promptAllConditionPushDown);
                    System.out.println(promptAllConditionPushDown);
                    System.out.println("Cardinality All Condition Push Down: " + cardinalityAllConditionPushDown);
                    if (parserWhere.getExpressions() != null && parserWhere.getExpressions().size() > 1) {
                        for (String singleCondition : promptSingleConditionPushDown) {
                            Integer cardinalitySingleCondition = getCardinality(singleCondition);
                            System.out.println(singleCondition);
                            System.out.println("Cardinality Single Condition Push Down: " + cardinalitySingleCondition);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private Integer getCardinality(String prompt) {
        TogetherAIModel model = new TogetherAIModel(Constants.TOGETHERAI_API, TogetherAIConstants.MODEL_LLAMA3_8B, TogetherAIConstants.STREAM_MODE);
        String response = model.generate(prompt);
        String cleanResponse = Mapper.toCleanJsonObject(response);
        Map<String, Object> parsedResponse = Mapper.fromJsonToMap(cleanResponse);
        Integer cardinality = (Integer) parsedResponse.getOrDefault("cardinality", -1.0);
        return cardinality;
    }

}
