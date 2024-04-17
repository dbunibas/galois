package galois.test.experiments.metrics;

import speedy.model.database.IDatabase;
import speedy.model.database.Tuple;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CellSimilarityPrecision implements IMetric {

    @Override
    public String getName() {
        return "CellSimilarityPrecision";
    }

    @Override
    public Double getScore(IDatabase database, List<Tuple> expected, List<Tuple> result) {

        CellNormalizer cellNormalizer = new CellNormalizer();

        Set<String> expectedCells = expected.stream()
                .flatMap(tuple -> tuple.getCells().stream())
                .filter(cell -> !cell.isOID())
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .collect(Collectors.toSet());

        Set<String> resultCells = result.stream().flatMap(tuple -> tuple.getCells().stream())
                .filter(cell -> !cell.isOID())
                .map(cell -> cellNormalizer.normalize(cell.getValue().toString()))
                .collect(Collectors.toSet());

        double count = 0.0;
        double totCells = resultCells.size(); // exclude the OIDs

        EditDistance editDist = new EditDistance();

        for (String resultCell : resultCells) {
            for (String expectedCell : expectedCells) {
                double threshold = expectedCell.length() * 0.1;
                if (editDist.getScoreForCells(expectedCell, resultCell, threshold)) {
                    count++;
                    break;
                }
            }
        }
        //System.out.println("Count: "+count);
        //System.out.println("Total Cells: "+totCells);

        return count / totCells;
    }
}
