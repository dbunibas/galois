package galois.test.experiments.run.batch;

import galois.Constants;
import galois.llm.algebra.config.OperatorsConfiguration;
import galois.llm.algebra.config.ScanConfiguration;
import galois.llm.query.*;
import galois.optimizer.IOptimizer;
import galois.test.experiments.Experiment;
import galois.test.experiments.json.parser.ExperimentParser;
import galois.test.experiments.json.parser.OptimizersFactory;
import galois.test.model.ExpVariant;
import galois.test.utils.CacheHitCounter;
import galois.test.utils.CacheOnlyEntityQueryExecutorAdapter;
import galois.test.utils.CacheOnlyKeyBasedQueryExecutorAdapter;
import galois.test.utils.CacheOnlyNLQueryExecutorAdapter;
import galois.test.utils.CacheOnlySQLQueryExecutorAdapter;
import galois.test.utils.ICacheOnlyQueryExecutorAdapter;
import galois.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;

@Slf4j
public class TestBenchLLMCacheHits {

    private static final String QUERIES_PATH = "src/test/resources/llm-bench/dataset.xlsx";

//    private final String executorModel = "llama3";
    private final String executorModel = "gpt";

    private List<ExpVariant> variants;
    private Map<String, VariantConfig> variantConfigs;

    private final Map<String, CacheStats> stats = new HashMap<>();

    public TestBenchLLMCacheHits() {
        initVariants();
    }

    @Test
    public void testCountCacheHitsLatestQwen37Plus() {
        CacheHitCounter.getInstance().reset();
        CacheHitCounter.getInstance().setCacheOnly(true);

        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        String model = getModelName();

        log.info("{} variants, replaying the experiments of the model {}", variants.size(), model);
        int i = 1;

        try {
            for (ExpVariant variant : this.variants) {
                VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
                String dbID = vc.db_id;
                String dataset = vc.dataset;

                log.info("Variant {} / {}", i, variants.size());
                i += 1;

                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", model + "-TABLE", variant, null);
                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", model + "-TABLE-OPTIMIZED", variant, allConditionPushdownWithFilter);
                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-csv-table-experiment.json", model + "-CSV", variant, allConditionPushdownWithFilter);
            }
        } finally {
            CacheHitCounter.getInstance().setCacheOnly(false);
        }

        log.info("### Cache hits of the model {}\n{}", model, report());
    }

    @Test
    public void testCountCacheHitsDataset() {
        CacheHitCounter.getInstance().reset();
        CacheHitCounter.getInstance().setCacheOnly(true);

        IOptimizer allConditionPushdownWithFilter = OptimizersFactory.getOptimizerByName("AllConditionsPushdownOptimizer-WithFilter");
        String model = getModelName();

        log.info("{} variants, replaying the experiments of the model {}", variants.size(), model);
        int i = 1;

        try {
            for (ExpVariant variant : this.variants) {
                VariantConfig vc = this.variantConfigs.get(variant.getQueryNum());
                String dbID = vc.db_id;
                String dataset = vc.dataset;

                log.info("Variant {} / {}", i, variants.size());
                i += 1;

                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-nl-experiment.json", model + "-NL", variant, null);
                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-sql-experiment.json", model + "-SQL", variant, null);
//                replay("/llm-bench/" + dataset + "/" + dbID + "-" + executorModel + "-table-experiment.json", model + "-TABLE-OPTIMIZED", variant, allConditionPushdownWithFilter);
            }
        } finally {
            CacheHitCounter.getInstance().setCacheOnly(false);
        }

        log.info("### Cache hits of the model {}\n{}", model, report());
    }

    // Executes the experiment as TestRunner#executeSingle does, but with a cache only scan executor
    private void replay(String path, String type, ExpVariant variant, IOptimizer optimizer) {
        CacheHitCounter counter = CacheHitCounter.getInstance();
        long hits = counter.getHits();
        long misses = counter.getMisses();
        long judgeHits = counter.getJudgeHits();
        long judgeMisses = counter.getJudgeMisses();
        CacheStats cacheStats = stats.computeIfAbsent(type, k -> new CacheStats());

        try {
            log.info("*** Replaying experiment {} with variant {} ***", path, variant.getQueryNum());
            Experiment experiment = ExperimentParser.loadAndParseJSON(path);
            experiment.setName(experiment.getName().replace("{{QN}}", variant.getQueryNum()));
            experiment.getQuery().setSql(variant.getQuerySql());

            IQueryExecutor queryExecutor = experiment.getOperatorsConfiguration().getScan().getQueryExecutor();
            if (queryExecutor instanceof INLQueryExectutor nlExecutor) {
                nlExecutor.setNaturalLanguagePrompt(variant.getPrompt());
                optimizer = null;
            } else if (queryExecutor instanceof ISQLExecutor sqlExecutor) {
                sqlExecutor.setSql(variant.getQuerySql());
                optimizer = null;
            } else if (queryExecutor instanceof IGaloisOriginalExecutor) {
                optimizer = null;
            }

            cacheOnlyExperiment(experiment).executeSingle(optimizer);
            cacheStats.executed += 1;
        } catch (Exception e) {
            log.error("Unable to replay experiment {}", path, e);
            cacheStats.failed += 1;
        }

        long experimentHits = counter.getHits() - hits;
        long experimentMisses = counter.getMisses() - misses;
        long experimentJudgeHits = counter.getJudgeHits() - judgeHits;
        long experimentJudgeMisses = counter.getJudgeMisses() - judgeMisses;
        cacheStats.hits += experimentHits;
        cacheStats.misses += experimentMisses;
        cacheStats.judgeHits += experimentJudgeHits;
        cacheStats.judgeMisses += experimentJudgeMisses;
        // An experiment failed before its first request has no miss, but it is not served by the cache either
        boolean cached = experimentHits + experimentJudgeHits > 0;
        if (experimentMisses == 0 && experimentJudgeMisses == 0 && cached) cacheStats.fullyCached += 1;
        log.info(
                "Experiment {} of the variant {}: {} cache hits and {} misses, {} judge cache hits and {} misses",
                type,
                variant.getQueryNum(),
                experimentHits,
                experimentMisses,
                experimentJudgeHits,
                experimentJudgeMisses
        );
    }

    /**
     * Wraps the scan executor so that both the parsed one and the ones rebuilt by the optimizers are served by
     * the cache only. The metrics are kept: their LLM judge is cache only as well, see CacheHitCounter.
     */
    private Experiment cacheOnlyExperiment(Experiment experiment) {
        ScanConfiguration scan = experiment.getOperatorsConfiguration().getScan();

        ScanConfiguration cacheOnlyScan = new ScanConfiguration(
                cacheOnlyAdapter(scan.getQueryExecutor()),
                // The factory generates the executor of the scan node: it must receive the original executor as base
                base -> cacheOnlyAdapter(scan.createQueryExecutor(unwrap(base))),
                scan.getNormalizationStrategy(),
                scan.getLlmProbThreshold()
        );

        return new Experiment(
                experiment.getName(),
                experiment.getDbms(),
                experiment.getMetrics(),
                null,
                new OperatorsConfiguration(cacheOnlyScan),
                experiment.getQuery(),
                experiment.getQueryExecutor()
        );
    }

    private IQueryExecutor cacheOnlyAdapter(IQueryExecutor queryExecutor) {
        // The natural language and SQL executors have their own execute method, hence their own adapters
        if (queryExecutor instanceof INLQueryExectutor)
            return new CacheOnlyNLQueryExecutorAdapter(queryExecutor);
        if (queryExecutor instanceof ISQLExecutor)
            return new CacheOnlySQLQueryExecutorAdapter(queryExecutor);
        if (queryExecutor instanceof AbstractEntityQueryExecutor)
            return new CacheOnlyEntityQueryExecutorAdapter(queryExecutor);
        if (queryExecutor instanceof AbstractKeyBasedQueryExecutor)
            return new CacheOnlyKeyBasedQueryExecutorAdapter(queryExecutor);
        // Any other executor iterates on its own prompts, hence its requests cannot be replayed by these adapters
        throw new UnsupportedOperationException("Cannot replay the executor " + queryExecutor.getClass().getSimpleName());
    }

    private IQueryExecutor unwrap(IQueryExecutor queryExecutor) {
        return queryExecutor instanceof ICacheOnlyQueryExecutorAdapter cacheOnly ? cacheOnly.getQueryExecutor() : queryExecutor;
    }

    private String report() {
        StringBuilder report = new StringBuilder("| Experiment | Hits | Misses | Hit rate | Judge hits | Judge misses | Judge hit rate | Fully cached | Executed | Failed |\n");
        report.append("|-|-|-|-|-|-|-|-|-|-|\n");
        CacheStats total = new CacheStats();
        for (String type : stats.keySet()) {
            CacheStats cacheStats = stats.get(type);
            report.append("| ").append(type).append(" | ").append(cacheStats.toReport()).append(" |\n");
            total.add(cacheStats);
        }
        return report.append("| TOTAL | ").append(total.toReport()).append(" |").toString();
    }

    private String getModelName() {
        return Configuration.getInstance().getLLMProvider().equals(Constants.PROVIDER_OPENAI) ?
                Configuration.getInstance().getOpenaiModelName() :
                Configuration.getInstance().getTogetheraiModel();
    }

    private void initVariants() {
        variants = new ArrayList<>();
        variantConfigs = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(new File(QUERIES_PATH)); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheet("queries");
            if (sheet == null) {
                log.error("Sheet 'queries' not found!");
                return;
            }
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Cell q_idCell = row.getCell(0);
                Cell dbIdCell = row.getCell(2);
                Cell queryCell = row.getCell(3);
                Cell questionCell = row.getCell(4);
                Cell datasetCell = row.getCell(1);

                String dbId = (dbIdCell != null) ? dbIdCell.toString() : "";
                String query = (queryCell != null) ? queryCell.toString() : "";
                String question = (questionCell != null) ? questionCell.toString() : "";
                String dataset = (datasetCell != null) ? datasetCell.toString() : "";
                String queryNum = (q_idCell != null) ? q_idCell.toString() : "";

                if (dbId.isEmpty()) break;

                ExpVariant ev = ExpVariant.builder()
                        .queryNum(queryNum)
                        .querySql(query)
                        .prompt(question)
                        .optimizers(List.of("AllConditionsPushdownOptimizer-WithFilter"))
                        .build();
                variants.add(ev);
                variantConfigs.put(ev.getQueryNum(), new VariantConfig(ev, dataset, dbId));
            }
        } catch (IOException e) {
            log.error("Error in loading queries - File: {}", QUERIES_PATH, e);
        }
    }

    private static class CacheStats {
        private long hits = 0;
        private long misses = 0;
        private long judgeHits = 0;
        private long judgeMisses = 0;
        private int fullyCached = 0;
        private int executed = 0;
        private int failed = 0;

        private void add(CacheStats cacheStats) {
            hits += cacheStats.hits;
            misses += cacheStats.misses;
            judgeHits += cacheStats.judgeHits;
            judgeMisses += cacheStats.judgeMisses;
            fullyCached += cacheStats.fullyCached;
            executed += cacheStats.executed;
            failed += cacheStats.failed;
        }

        private String toReport() {
            return String.format("%d | %d | %s | %d | %d | %s | %d | %d | %d",
                    hits, misses, hitRate(hits, misses),
                    judgeHits, judgeMisses, hitRate(judgeHits, judgeMisses),
                    fullyCached, executed, failed);
        }

        private String hitRate(long hits, long misses) {
            long requests = hits + misses;
            return requests == 0 ? "-" : String.format("%.2f%%", (100.0 * hits) / requests);
        }
    }

    private static class VariantConfig {
        private final ExpVariant variant;
        private final String dataset;
        private final String db_id;

        public VariantConfig(ExpVariant variant, String dataset, String db_id) {
            this.variant = variant;
            this.dataset = dataset;
            this.db_id = db_id;
        }
    }
}
