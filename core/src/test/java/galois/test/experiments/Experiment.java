package galois.test.experiments;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.parser.IQueryPlanParser;
import galois.planner.IQueryPlanner;
import galois.test.experiments.metrics.IMetric;
import galois.test.utils.TestUtils;
import lombok.Data;
import org.jdom2.Document;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;

import java.util.List;

@Data
public final class Experiment {
    private final String name;
    private final String dbms;
    private final List<IMetric> metrics;
    private final OperatorsConfiguration operatorsConfiguration;
    private final Query query;
    private final String queryExecutor;

    @SuppressWarnings("unchecked")
    public ExperimentResults execute() {
        // TODO: Make this generic (and remove annotation)
        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);

        Document queryPlan = planner.planFrom(query.getSql());
        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration);

        ITupleIterator iterator = operator.execute(query.getDatabase(), null);

        List<Tuple> results = TestUtils.toTupleList(iterator);
        List<Double> scores = metrics
                .stream()
                .map(m -> m.getScore(query.getDatabase(), query.getResults(), results))
                .toList();

        return new ExperimentResults(name, metrics, query.getResults(), results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql());
    }

}
