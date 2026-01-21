package galois.test.evaluation;

import galois.utils.Configuration;
import galois.utils.excelreport.CellReport;
import galois.utils.excelreport.ReportExcel;
import galois.utils.excelreport.ReportRow;
import galois.utils.excelreport.SheetReport;
import galois.utils.excelreport.persistance.DAOReportExcel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class EvaluationResults {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

    private final List<EvaluationResult> results = new ArrayList<>();

    public void appendResult(EvaluationResult result) {
        results.add(result);
    }

    public void exportAsText(String experimentName) {
        createFolderIfNotExists(Paths.get(Configuration.getInstance().getResultsAbsolutePath()));
        String fileName = experimentName + "-" + DATE_FORMAT.format(new Date()) + ".txt";
        String fileNameCleaned = fileName.replace(' ', '_').replace(':', '-');
        Path path = Paths.get(Configuration.getInstance().getResultsAbsolutePath(), fileNameCleaned);
        try (FileOutputStream output = new FileOutputStream(path.toFile())) {
            for (EvaluationResult result : results) {
                output.write(result.toString().getBytes(StandardCharsets.UTF_8));
                output.write("\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exportAsExcel(String experimentName) {
        createFolderIfNotExists(Paths.get(Configuration.getInstance().getExportExcelAbsolutePath()));
        String fileName = experimentName + "-" + DATE_FORMAT.format(new Date()) + ".xlsx";
        String fileNameCleaned = fileName.replace(' ', '_').replace(':', '-');
        Path path = Paths.get(Configuration.getInstance().getExportExcelAbsolutePath(), fileNameCleaned);

        DAOReportExcel daoReportExcel = new DAOReportExcel();
        ReportExcel reportExcel = new ReportExcel(experimentName);

        for (EvaluationResult result : results) {
            SheetReport dataSheet = reportExcel.addSheet(result.getVariant().getQueryId());
            addScores(result.getScoresMap(), dataSheet);
            addStats(result, dataSheet);
        }

        daoReportExcel.saveReport(reportExcel, path.toFile());
    }

    private void createFolderIfNotExists(Path folder) {
        if (Files.exists(folder)) return;

        try {
            Files.createDirectory(folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addScores(Map<String, Double> scoresMap, SheetReport dataSheet) {
        List<String> orderedMetricNames = scoresMap.keySet().stream().sorted().toList();
        for (String metric : orderedMetricNames) {
            ReportRow row = dataSheet.addRow();
            row.addCell(metric);
            Double score = scoresMap.get(metric) == null || scoresMap.get(metric).isNaN() ? 0.0 : scoresMap.get(metric);
            row.addCell(DECIMAL_FORMAT.format(score));
        }
    }

    private void addStats(EvaluationResult result, SheetReport dataSheet) {
        dataSheet.addRow();

        ReportRow rowTotalRequest = dataSheet.addRow();
        rowTotalRequest.addCell("# requests");
        rowTotalRequest.addCell(result.getLlmRequest());

        ReportRow inputTokens = dataSheet.addRow();
        inputTokens.addCell("# input tokens");
        inputTokens.addCell(result.getLlmTokensInput());

        ReportRow outputTokens = dataSheet.addRow();
        outputTokens.addCell("# output tokens");
        outputTokens.addCell(result.getLlmTokensOutput());

        ReportRow timeMS = dataSheet.addRow();
        timeMS.addCell("time (ms)");
        timeMS.addCell(result.getTimeMs());
    }
}
