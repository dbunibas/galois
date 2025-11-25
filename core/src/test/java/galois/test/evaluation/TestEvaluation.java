package galois.test.evaluation;

import galois.test.experiments.metrics.IMetric;
import galois.test.experiments.metrics.TupleCardinalityMetric;
import galois.test.experiments.metrics.TupleCellSimilarityFilteredAttributes;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;

@Slf4j
public class TestEvaluation {
    // Default metrics to evaluate
    private static final List<IMetric> DEFAULT_METRICS = List.of(
            new TupleCardinalityMetric(),
            new TupleCellSimilarityFilteredAttributes()
    );

    @Test
    public void testEvaluation() {
        // 0. Definire schema DB e dati (in resources cartella con schema -> file json, dati -> file csv)
        //  - Schema: nome DB, list tabelle - ogni tabella lista di attributi
        //  - Connessione: parserizzare la AccessConfiguration
        // Ref. generazione e caricamento dati Experiment::createDatabaseForExpected

        // 1. Definire le query (nuova versione ExpVariant)
        //   - Obbligatorie: queryNum, querySQL - per GT, queryUDF - per valutazione
        //   - Opzionali: optimizers - default vuoti, prompt - NL per retrocompatibilità

        // 2. Valutazione
        // 2.1 Creare e popolare automaticamente il DB (vedi 0. Ref.)
        // 2.2 Eseguire querySQL per ottenere la ground truth
        // 2.3 Eseguire queryUDF per expected
        // 2.4 Confrontare i risultati con le metriche DEFAULT_METRICS
        // Ref. Experiment::executeSingle

        // 3. Export dei risultati
    }
}
