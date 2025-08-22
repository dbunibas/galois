package galois.test.utils;

import galois.test.experiments.ExperimentResults;
import galois.test.experiments.metrics.IMetric;
import galois.utils.Configuration;
import galois.utils.excelreport.ReportExcel;
import galois.utils.excelreport.ReportRow;
import galois.utils.excelreport.SheetReport;
import galois.utils.excelreport.persistance.DAOReportExcel;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ExcelExporter {

    public String getFileName(String expName) {
        String pathExport = Configuration.getInstance().getExportExcelAbsolutePath();
        String fileName = pathExport + "/" + expName + "-" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + ".xlsx";
        return fileName;
    }

    public void export(String fileName, String expName, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results) {
        exportExcel(expName, fileName, metrics, results);
    }

    private void exportExcel(String expName, String fileName, List<IMetric> metrics, Map<String, Map<String, ExperimentResults>> results) {
        if (results.isEmpty()) {
            return;
        }
        DAOReportExcel daoReportExcel = new DAOReportExcel();
        ReportExcel reportExcel = new ReportExcel(expName);
        for (String queryNumber : results.keySet()) {
            SheetReport dataSheet = reportExcel.addSheet(queryNumber);
            Map<String, ExperimentResults> resultExp = results.get(queryNumber);
            List<String> optmizers = new ArrayList<>(resultExp.keySet());
            Collections.sort(optmizers);
            createHeaders(optmizers, dataSheet);
            for (IMetric metric : metrics) {
                ReportRow rowMetric = dataSheet.addRow();
                rowMetric.addCell(metric.getName());
                for (String optmizer : optmizers) {
                    ExperimentResults expResult = resultExp.get(optmizer);
                    Double value = expResult.getMetrics().get(metric.getName());
                    if (value == null || value.isNaN()) {
                        value = 0.0;
                    }
                    rowMetric.addCell(new DecimalFormat("#.###").format(value));
                }
            }
            addLLMStats(optmizers, resultExp, dataSheet);
        }
        File exportFile = new File(fileName);
        log.info("Writing file {}", exportFile);
        daoReportExcel.saveReport(reportExcel, exportFile);
//        try {
//            Desktop.getDesktop().open(exportFile);
//        } catch (IOException e) {
//        }
    }

    private void createHeaders(List<String> optmizers, SheetReport dataSheet) {
        ReportRow rowHeader = dataSheet.addRow();
        rowHeader.addCell("");
        for (String optmizer : optmizers) {
            rowHeader.addCell(optmizer);
        }
    }

    private void addLLMStats(List<String> optmizers, Map<String, ExperimentResults> results, SheetReport dataSheet) {
        ReportRow rowTotalRequest = dataSheet.addRow();
        rowTotalRequest.addCell("LLM Total Requests");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            rowTotalRequest.addCell(expResult.getLlmRequest());
        }
        ReportRow towTotalInputTokens = dataSheet.addRow();
        towTotalInputTokens.addCell("LLM Total Input Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalInputTokens.addCell(expResult.getLlmTokensInput());
        }
        ReportRow towTotalOutputTokens = dataSheet.addRow();
        towTotalOutputTokens.addCell("LLM Total Output Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalOutputTokens.addCell(expResult.getLlmTokensOutput());
        }
        ReportRow towTotalTokens = dataSheet.addRow();
        towTotalTokens.addCell("LLM Total Tokens");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalTokens.addCell(expResult.getLlmTokensInput() + expResult.getLlmTokensOutput());
        }
        ReportRow towTotalTime = dataSheet.addRow();
        towTotalTime.addCell("LLM Time (ms)");
        for (String optmizer : optmizers) {
            ExperimentResults expResult = results.get(optmizer);
            towTotalTime.addCell(expResult.getTimeMs());
        }
    }
}
