package galois.test;

import galois.utils.excelreport.persistance.DAOReportExcel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import speedy.persistence.Types;

@Slf4j
public class TestDaoReportExcel {
    
    @Test
    public void testLoadResults() {
        String filePath = "/Users/enzoveltri/Desktop/galois/WORLD1-CARD.xlsx";
        DAOReportExcel daoReport = new DAOReportExcel();
        File file = new File(filePath);
        List<String> statsToExport = List.of("avg");
//        List<String> statsToExport = List.of("CellSimilarityF1Score");
//        List<String> statsToExport = List.of("TupleCardinality");
//        List<String> statsToExport = List.of("TupleSimilarityConstraint");
//        List<String> statsToExport = List.of("LLM Total Tokens");
//        List<String> statsToExport = List.of("LLM Time (ms)");
        List<String> metricsToAverage = List.of("CellSimilarityF1Score", "TupleCardinality", "TupleSimilarityConstraint");
//        List<String> metricsToAverage = List.of("CellSimilarityF1Score");
//        List<String> metricsToAverage = List.of("TupleCardinality");
//        List<String> metricsToAverage = List.of("TupleSimilarityConstraint");
//        List<String> metricsToAverage = List.of("LLM Total Tokens");
//        List<String> metricsToAverage = List.of("LLM Time (ms)");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q11", "q12", "q13", "q14", "q15", "q16", "q17", "q20", "q21", "q22", "q23", "q24", "q26", "q29", "q30", "q31", "q32", "q33", "q34", "q35", "q36", "q37", "q38");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5");
//        List<String> queriesName = List.of("q1", "q7", "q9", "q10");
//        List<String> queriesName = List.of("q39", "q40", "q41", "q42", "q43", "q44", "q45", "q46", "q47", "q48");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7");
//        List<String> queriesName = List.of("q1", "q2", "q3", "q4", "q5", "q6", "q7", "q8", "q9", "q10", "q11", "q12", "q13", "q14", "q15", "q16", "q17");
        List<String> queriesName = List.of("q7", "q8", "q9", "q11");
//        List<String> queriesName = List.of("q1", "q2", "q3");
//        daoReport.readResultsForDataset(file, queriesName, statsToExport, metricsToAverage);
        
        List<String> statsExport = List.of("CellSimilarityF1Score", "TupleCardinality", "TupleSimilarityConstraint", "LLM Total Tokens", "LLM Time (ms)");
        List<String> strategies = List.of("TABLE-CARDINALITY");
        daoReport.readResultsForDataset2(file, queriesName, statsExport, strategies);
    }
    
    @Test
    public void testType() {
        String text1 = "10.0";
        String text2 = "10.0000";
        String text3 = "10.0001";         
        System.out.println(Types.checkType(Types.INTEGER, text1));
        System.out.println(Types.checkType(Types.LONG, text1));
        System.out.println(Types.checkType(Types.REAL, text1));
        System.out.println(Types.checkType(Types.INTEGER, text2));
        System.out.println(Types.checkType(Types.LONG, text2));
        System.out.println(Types.checkType(Types.REAL, text2));
        System.out.println(Types.checkType(Types.INTEGER, text3));
        System.out.println(Types.checkType(Types.LONG, text3));
        System.out.println(Types.checkType(Types.REAL, text3));
    }
}
