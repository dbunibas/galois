package galois.test.experiments;

import galois.llm.algebra.config.OperatorsConfiguration;
import galois.optimizer.IOptimization;
import galois.optimizer.IOptimizer;
import galois.parser.IQueryPlanParser;
import galois.planner.IQueryPlanner;
import galois.test.experiments.metrics.IMetric;
import galois.test.utils.TestUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jdom2.Document;
import speedy.model.algebra.IAlgebraOperator;
import speedy.model.algebra.operators.ITupleIterator;
import speedy.model.database.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public final class Experiment {
    private final String name;
    private final String dbms;
    private final List<IMetric> metrics;
    private final List<IOptimizer> optimizers;
    private final OperatorsConfiguration operatorsConfiguration;
    private final Query query;
    private final String queryExecutor;

    @SuppressWarnings("unchecked")
    public Map<String, ExperimentResults> execute() {
        Map<String, ExperimentResults> results = new HashMap<>();

        // TODO: Make this generic (and remove annotation)
        IQueryPlanner<Document> planner = (IQueryPlanner<Document>) PlannerParserFactory.getPlannerFor(dbms, query.getAccessConfiguration());
        IQueryPlanParser<Document> parser = (IQueryPlanParser<Document>) PlannerParserFactory.getParserFor(dbms);

        Document queryPlan = planner.planFrom(query.getSql());
        IAlgebraOperator operator = parser.parse(queryPlan, query.getDatabase(), operatorsConfiguration);

        var unoptimizedResult = executeUnoptimizedExperiment(operator);
        results.put("Unoptimized", unoptimizedResult);

        List<IOptimizer> optimizersList = optimizers == null ? List.of() : optimizers;
        for (IOptimizer optimizer : optimizersList) {
            var result = executeOptimizedExperiment(operator, optimizer);
            results.put(optimizer.getName(), result);
        }

        return results;
    }

    private ExperimentResults executeUnoptimizedExperiment(IAlgebraOperator operator) {
        ITupleIterator iterator = operator.execute(query.getDatabase(), null);
        return toExperimentResults(iterator);
    }

    private ExperimentResults executeOptimizedExperiment(IAlgebraOperator operator, IOptimizer optimizer) {
        IAlgebraOperator optimizedOperator = optimizer.optimize(query.getDatabase(), query.getSql(), operator);
        ITupleIterator iterator = optimizedOperator.execute(query.getDatabase(), null);
        return toExperimentResults(iterator);
    }

    private ExperimentResults toExperimentResults(ITupleIterator iterator) {
        List<Tuple> results = TestUtils.toTupleList(iterator);
        List<Double> scores = metrics
                .stream()
                .map(m -> m.getScore(query.getDatabase(), query.getResults(), results))
                .toList();

        return new ExperimentResults(name, metrics, query.getResults(), results, scores, operatorsConfiguration.getScan().getQueryExecutor().toString(), query.getSql());
    }
}
