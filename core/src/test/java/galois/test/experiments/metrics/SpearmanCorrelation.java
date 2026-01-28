package galois.test.experiments.metrics;

import java.util.*;
import java.util.stream.IntStream;

import org.apache.poi.hpsf.Array;

import speedy.model.database.IDatabase;
import speedy.model.database.Cell;
import speedy.model.database.Tuple;

public class SpearmanCorrelation implements IMetric {

    /**
     * Calculates Spearman Correlation for two lists of scores.
     * Aligns data by index (assuming index 0 in list1 corresponds to index 0 in list2).
     * Filters out NaN values to maintain alignment of "Common IDs".
     */
    
    @Override
    public String getName() {
        return "SpearmanCorrelation";
    }



    // @Override
    // public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
    //     if (expected.isEmpty() || result.isEmpty()) {
    //         return (expected.isEmpty() && result.isEmpty()) ? 1.0 : 0.0;
    //     }

    //     // 1. Map IDs to Scores (Assuming Col 0 is ID, Col 1 is Score)
    //     Map<String, Double> expectedMap = extractScoreMap(expected);
    //     Map<String, Double> resultMap = extractScoreMap(result);

    //     // 2. Find Common IDs (Alignment)
    //     List<Double> alignedExpected = new ArrayList<>();
    //     List<Double> alignedResult = new ArrayList<>();

    //     for (String id : expectedMap.keySet()) {
    //         if (resultMap.containsKey(id)) {
    //             alignedExpected.add(expectedMap.get(id));
    //             alignedResult.add(resultMap.get(id));
    //         }
    //     }

    //     // Need at least 2 common pairs to calculate correlation
    //     if (alignedExpected.size() < 2) return 0.0;

    //     // 3. Convert to Ranks and Calculate
    //     double[] expectedRanks = calculateRanks(alignedExpected);
    //     double[] resultRanks = calculateRanks(alignedResult);

    //     return computePearson(expectedRanks, resultRanks);
    // }

    private Map<String, Double> extractScoreMap(List<Tuple> tuples) {
        Map<String, Double> map = new HashMap<>();
        for (Tuple t : tuples) {
            List<Cell> cells = t.getCells();
            if (cells.size() < 2) continue;
            
            String id = cells.get(0).getValue().toString();
            String scoreStr = cells.get(1).getValue().toString();
            
            try {
                // Handle SQL/Python NaN and different decimal separators
                if (scoreStr == null || scoreStr.equalsIgnoreCase("nan")) continue;
                double score = Double.parseDouble(scoreStr.replace(",", "."));
                map.put(id, score);
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        return map;
    }

    private Map<String, Double> extractScoreMap(List<Tuple> tuples, Set<String> attributes) {
        Map<String, Double> map = new HashMap<>();
        for (Tuple t : tuples) {
            List<Cell> cells = t.getCells();
            if (cells.size() < 2) continue;

            List<Cell> newCells = new ArrayList<>();
            
            for (Cell cell: cells){
                if (attributes.contains(cell.getAttribute())){
                    newCells.add(cell);
                }
            }

            String id = newCells.get(0).getValue().toString();
            String scoreStr = newCells.get(1).getValue().toString();
            
            try {
                // Handle SQL/Python NaN and different decimal separators
                if (scoreStr == null || scoreStr.equalsIgnoreCase("nan")) continue;
                double score = Double.parseDouble(scoreStr.replace(",", "."));
                map.put(id, score);
            } catch (NumberFormatException e) {
                // Skip non-numeric values
            }
        }
        return map;
    }

    // private double[] calculateRanks(List<Double> values) {
    //     int n = values.size();
    //     double[] ranks = new double[n];
    //     Integer[] indices = IntStream.range(0, n).boxed().toArray(Integer[]::new);

    //     Arrays.sort(indices, Comparator.comparingDouble(values::get));

    //     for (int i = 0; i < n; i++) {
    //         int j = i + 1;
    //         while (j < n && values.get(indices[j]).equals(values.get(indices[i]))) {
    //             j++;
    //         }
    //         double avgRank = (i + 1 + j) / 2.0; // Tie handling
    //         for (int k = i; k < j; k++) {
    //             ranks[indices[k]] = avgRank;
    //         }
    //         i = j - 1;
    //     }
    //     return ranks;
    // }

    // private double computePearson(double[] x, double[] y) {
    //     int n = x.length;
    //     double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
    //     for (int i = 0; i < n; i++) {
    //         sumX += x[i]; sumY += y[i];
    //         sumXY += x[i] * y[i];
    //         sumX2 += x[i] * x[i]; sumY2 += y[i] * y[i];
    //     }
    //     double num = (n * sumXY) - (sumX * sumY);
    //     double den = Math.sqrt(((n * sumX2) - (sumX * sumX)) * ((n * sumY2) - (sumY * sumY)));
    //     return (den == 0) ? 0.0 : num / den;
    // }
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {
        // 1. Validazione veloce
        if (expected == null || result == null || expected.isEmpty() || result.isEmpty()) {
            return 0.0;
        }

        // 2. Costruiamo una Mappa per la Ground Truth per un lookup O(1)
        // Gestiamo i duplicati tenendo l'ultimo valore (comportamento standard delle Map)
        

        // 3. Allineamento dei dati (Intersezione)
        // Usiamo liste dinamiche perché non sappiamo quanti ID comuni troveremo
        List<Double> resValuesList = new ArrayList<>();
        List<Double> gtValuesList = new ArrayList<>();


        Tuple reference = result.get(0);
        Set<String> attributes = new HashSet<>();
        List<Cell> cells = reference.getCells();
        for (int i=0; i<cells.size(); i++){
            attributes.add(cells.get(i).getAttribute());
        }
        if (attributes.size()>2 && attributes.contains("oid")){
            attributes.remove("oid");
        }

        Map<String, Double> resultMap = extractScoreMap(result);

        Map<String, Double> expectedMap = extractScoreMap(expected, attributes);

        
        for (String t : resultMap.keySet()) {
            // Se l'ID del risultato esiste nella Ground Truth
            if (t != null && resultMap.get(t) != null) {
                Double gtScore = expectedMap.get(t);
                if (gtScore != null) {
                    resValuesList.add(resultMap.get(t));
                    gtValuesList.add(gtScore);
                }
            }
        }

        // Se abbiamo meno di 2 punti in comune, la correlazione non è calcolabile
        if (resValuesList.size() < 2) {
            return 0.0;
        }

        // 4. Conversione in array primitivi per il calcolo
        double[] x = resValuesList.stream().mapToDouble(d -> d).toArray();
        double[] y = gtValuesList.stream().mapToDouble(d -> d).toArray();

        // 5. Calcolo Spearman: Pearson sui Ranghi
        return calculateSpearman(x, y);
    }

    // --- Metodi di supporto per il calcolo statistico ---

    private double calculateSpearman(double[] x, double[] y) {
        // Passo A: Convertire i valori in Ranghi (gestendo i ties)
        double[] ranksX = getRanks(x);
        double[] ranksY = getRanks(y);

        // Passo B: Calcolare la correlazione di Pearson sui ranghi
        return calculatePearson(ranksX, ranksY);
    }

    /**
     * Converte un array di valori in ranghi.
     * Gestisce i ties assegnando la media dei ranghi (es. 1, 2, 2, 4 -> 1, 2.5, 2.5, 4).
     */
    private double[] getRanks(double[] values) {
        int n = values.length;
        
        // Creiamo coppie (valore, indice_originale) per poter ordinare
        RankPair[] pairs = new RankPair[n];
        for (int i = 0; i < n; i++) {
            pairs[i] = new RankPair(values[i], i);
        }

        // Ordiniamo per valore
        Arrays.sort(pairs, Comparator.comparingDouble(p -> p.value));

        double[] ranks = new double[n];
        
        // Assegnazione ranghi con gestione ties
        int i = 0;
        while (i < n) {
            int j = i;
            // Troviamo quanti valori uguali consecutivi ci sono
            while (j < n - 1 && pairs[j].value == pairs[j + 1].value) {
                j++;
            }
            
            // Calcoliamo il rango medio
            // I ranghi sono 1-based teoricamente, ma qui usiamo i valori relativi. 
            // La formula media è (somma indici da i a j) / count + 1 (per base 1)
            int nTies = j - i + 1;
            double rankSum = 0;
            for (int k = i; k <= j; k++) {
                rankSum += (k + 1); // +1 perché i ranghi partono da 1
            }
            double averageRank = rankSum / nTies;

            // Assegniamo il rango medio a tutti gli elementi del gruppo
            for (int k = i; k <= j; k++) {
                ranks[pairs[k].originalIndex] = averageRank;
            }
            
            i = j + 1;
        }
        return ranks;
    }

    private double calculatePearson(double[] x, double[] y) {
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);

        double numerator = 0.0;
        double denomX = 0.0;
        double denomY = 0.0;

        for (int i = 0; i < x.length; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            
            numerator += diffX * diffY;
            denomX += diffX * diffX;
            denomY += diffY * diffY;
        }

        if (denomX == 0 || denomY == 0) return 0.0; // Evita divisione per zero
        
        return numerator / Math.sqrt(denomX * denomY);
    }

    // Classe helper interna per il ranking
    private static class RankPair {
        double value;
        int originalIndex;

        RankPair(double value, int originalIndex) {
            this.value = value;
            this.originalIndex = originalIndex;
        }
    }
}
