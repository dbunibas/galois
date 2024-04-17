package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.*;

public class TupleSimilarityConstraint implements  IMetric{
    @Override
    public String getName() {
        return "TupleSimilarityConstraint";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellNormalizer cellNormalizer = new CellNormalizer();
        EditDistance editDist= new EditDistance();

        List<Tuple> expectedSorted = expected.stream()
                .sorted(Comparator.comparing(tuple -> tuple.getCells().get(1).getValue().toString()))
                .toList();

        List<List<String>> expectedCells = expectedSorted.stream().map(tuple -> tuple.getCells().stream()
                        .filter(cell -> !cell.isOID())
                        .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                        .toList())
                .toList();

        List<Tuple> resultSorted = result.stream()
                .sorted(Comparator.comparing(tuple -> tuple.getCells().get(1).getValue().toString()))
                .toList();

        List<List<String>> resultCells = resultSorted.stream().map(tuple -> tuple.getCells().stream()
                        .filter(cell -> !cell.isOID())
                        .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                        .toList())
                .toList();

        double similaritySum = 0.0;
        int totalComparisons = 0;
        double countSimilarity = 0.0;

        // iterate over corresponding cells in the expected and result tuples
        for (int i = 0; i < expectedCells.size(); i++) {
            List<String> expectedTuple = expectedCells.get(i);
            List<String> resultTuple = resultCells.get(i);
            for (int j = 0; j < expectedTuple.size(); j++) {
                String expectedCell = expectedTuple.get(j);
                String resultCell = resultTuple.get(j);
                double threshold = expectedCell.length() * 0.1;
                EditDistance editDistance = new EditDistance();
                if(editDistance.getScoreForCells(expectedCell, resultCell, threshold)){
                    similaritySum++;
                }
                totalComparisons++;
            }
            if(similaritySum == expectedTuple.size()){
                countSimilarity++;
            }
        }
        // Calculate average similarity
        return countSimilarity / expectedCells.size();
    }
}