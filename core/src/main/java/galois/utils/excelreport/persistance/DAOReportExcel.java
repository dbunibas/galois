package galois.utils.excelreport.persistance;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import galois.utils.excelreport.*;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import speedy.utility.SpeedyUtility;

@Slf4j
public class DAOReportExcel {

    private static final DAOReportExcel singleton = new DAOReportExcel();

    public static DAOReportExcel getInstance() {
        return singleton;
    }

    public DAOReportExcel() {
    }

    public void readResultsForDataset(File filePath, List<String> queriesName, List<String> statsToExport, List<String> metricsToAverage) {
        try {
            Map<String, Map<String, Map<String, Double>>> results = new HashMap<>();
            Workbook workbook = new XSSFWorkbook(filePath);
            for (String qName : queriesName) {
                Sheet sheet = workbook.getSheet(qName);
                log.debug("Query: " + qName);
                Map<String, Map<String, Double>> statisticsForQuery = getStatistics(sheet);
                results.put(qName, statisticsForQuery);
            }
            List<List<String>> queriesStats = new ArrayList<>();
            queriesStats.add(List.of("","NL", "SQL", "KEY", "TABLE"));
            for (String qName : queriesName) {
                Map<String, Map<String, Double>> resultsForQuery = results.get(qName);
                Map<String, Double> resultsForNL = resultsForQuery.get("NL-Unoptimized");
                Map<String, Double> resultsForSQL = resultsForQuery.get("SQL-Unoptimized");
                String bestStrategyForKeyScan = getBestResultsAveragedFor("KEY-SCAN", resultsForQuery, metricsToAverage);
                Map<String, Double> resultsForBestKeyScan = resultsForQuery.get(bestStrategyForKeyScan);
                String bestStrategyForTable = getBestResultsAveragedFor("TABLE", resultsForQuery, metricsToAverage);
                Map<String, Double> resultsForBestTable = resultsForQuery.get(bestStrategyForTable);
                log.info("QUERY: {}", qName);
                log.info("NL: {}", SpeedyUtility.printMap(resultsForNL));
                log.info("SQL: {}", SpeedyUtility.printMap(resultsForSQL));
                log.info("KEY-SCAN: ({}) {}", bestStrategyForKeyScan, SpeedyUtility.printMap(resultsForBestKeyScan));
                log.info("TABLE: ({}) {}", bestStrategyForTable, SpeedyUtility.printMap(resultsForBestTable));
                List<String> query = new ArrayList<>();
                query.add(qName);
                for (String statName : statsToExport) {
                    if (statName.equals("avg")) {
                        double avgNL = computeAverage(resultsForNL, metricsToAverage);
                        double avgSQL = computeAverage(resultsForSQL, metricsToAverage);
                        double avgKeyScan = computeAverage(resultsForBestKeyScan, metricsToAverage);
                        double avgTable = computeAverage(resultsForBestTable, metricsToAverage);
                        query.add((avgNL + "").replace(".", ","));
                        query.add((avgSQL + "").replace(".", ","));
                        query.add((avgKeyScan + "").replace(".", ","));
                        query.add((avgTable + "").replace(".", ","));
                    } else {
                        double valNL = resultsForNL.get(statName);
                        double valSQL = resultsForSQL.get(statName);
                        double valKeyScan = resultsForBestKeyScan.get(statName);
                        double valTable = resultsForBestTable.get(statName);
                        query.add((valNL + "").replace(".", ","));
                        query.add((valSQL + "").replace(".", ","));
                        query.add((valKeyScan + "").replace(".", ","));
                        query.add((valTable + "").replace(".", ","));
                    }
                }
                queriesStats.add(query);
            }
            exportToCSV(queriesStats, filePath);
        } catch (Exception e) {
            log.error("Unable to open " + filePath + " * Exception: ", e);
        }
    }

    public void saveReport(ReportExcel report, File file) {
        loadFonts();
        FileOutputStream fileOut = null;
        try {
            file.getParentFile().mkdirs();
            fileOut = new FileOutputStream(file);
            saveReport(report, fileOut);
        } catch (Exception ex) {
            log.error("Unable to save report", ex);
            throw new DAOException("Unable to save report " + ex.getLocalizedMessage());
        } finally {
            try {
                if (fileOut != null) fileOut.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void saveReport(ReportExcel report, OutputStream os) {
        loadFonts();
        try (os; SXSSFWorkbook wb = new SXSSFWorkbook(100)) {
            Map<String, CellStyle> stylesMap = new HashMap<>();
            for (SheetReport sheetReport : report.getSheets()) {
                String cleanedName = WorkbookUtil.createSafeSheetName(sheetReport.getName());
                Sheet sheet = wb.createSheet(cleanedName);
                sheet.setDefaultColumnWidth(30);
                sheet.setDisplayGridlines(sheetReport.isDisplayGridlines());
                setTrackedColumns(sheet, sheetReport);
                setFilter(sheet, sheetReport);
                setLock(sheet, sheetReport);
                writeSheet(sheet, sheetReport, stylesMap);
                resizeColumns(sheet, sheetReport);
                hideColumns(sheet, sheetReport);
                stileRighePari(sheetReport, sheet, "#EDEDED");
            }
            wb.write(os);
            wb.dispose();
        } catch (Exception ex) {
            log.error("Unable to save report", ex);
            throw new DAOException("Unable to save report " + ex.getLocalizedMessage());
        }
    }

    private void stileRighePari(SheetReport sheetReport, Sheet sheet, String hexColor) {
        Region regione = sheetReport.getRegion();
        if (regione == null) {
            return;
        }
        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        // Condizione: Righe pari in grigio
        ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule("MOD(ROW(), 2) = 0");
        PatternFormatting fill = rule.createPatternFormatting();
        //fill.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        fill.setFillBackgroundColor(getXSSFColor(hexColor));
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
        // Apply
        //int numberOfRows = sheet.getPhysicalNumberOfRows();
        CellRangeAddressList regions = new CellRangeAddressList(regione.getFirstRow(), regione.getLastRow(), regione.getFirstCol(), regione.getLastCol());
        sheetCF.addConditionalFormatting(regions.getCellRangeAddresses(), rule);
    }

    private void loadFonts() {
        File libDir = new File(System.getProperty("java.home"), "lib");
        List<String> fileList = new ArrayList<>();
        if (libDir.list() != null) {
            fileList.addAll(Arrays.asList(libDir.list()));
        }
        String fontString = fileList.stream().filter(file -> file.contains("fontconfig")).findFirst().orElse(null);
        if (fontString != null) {
            log.debug("Found fontconfig file {}", fontString);
            File fontFile = new File(libDir, fontString);
            System.setProperty("sun.awt.fontconfig", fontFile.getPath());
        }
    }

//    public void saveReportOnZip(List<ReportExcel> listaReportExcelSoggetti, File fileReportZip) throws DAOException {
//        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(fileReportZip))) {
//            for (ReportExcel reportExcel : listaReportExcelSoggetti) {
//                File fileReport = getTempFile(getEstensione());
//                saveReport(reportExcel, fileReport);
//                ZipEntry entry = new ZipEntry(reportExcel.getName() + "." + getEstensione());
//                zipOutputStream.putNextEntry(entry);
//                IOUtils.copy(new FileInputStream(fileReport), zipOutputStream);
//            }
//        } catch (IOException ex) {
//            throw new DAOException(ex);
//        }
//    }

    private void writeSheet(Sheet sheet, SheetReport sheetReport, Map<String, CellStyle> stylesMap) {
        int rowIndex = 0;
        for (ReportRow sheetRow : sheetReport.getRows()) {
            Row row = sheet.createRow(rowIndex++);
            int cellIndex = 0;
            if (sheetReport.isAddProgressiveColumn()) {
                Cell cell = row.createCell(cellIndex++);
                cell.setCellValue(cellIndex);
            }
            for (CellReport sheetCell : sheetRow.getCells()) {
                Cell cell = row.createCell(cellIndex++);
                buildCell(cell, sheetCell, stylesMap);
            }
            if (sheetReport.isDisableHeightAutosizeRow()) {
                if (sheetRow.getHeight() != null) {
                    row.setHeightInPoints(sheetRow.getHeight());
                } else {
                    row.setHeightInPoints((1 * sheet.getDefaultRowHeightInPoints()));
                }
            }
        }
    }

    private void buildCell(Cell cell, CellReport cellReport, Map<String, CellStyle> stylesMap) {
        if (cellReport == CellReport.EMPTY_CELL) {
            return;
        }
        Object value = cellReport.getValue();
        if (cellReport.isLink()) {
            setLink(cell, value);
        } else if (value != null) {
            setValue(value, cell);
        }
        if (!isEmpty(cellReport.getComment())) {
            CreationHelper factory = cell.getSheet().getWorkbook().getCreationHelper();
            Drawing<?> drawing = cell.getSheet().createDrawingPatriarch();
            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(cell.getColumnIndex() + 1);
            anchor.setCol2(cell.getColumnIndex() + cellReport.getCommentWidth());
            anchor.setRow1(cell.getRow().getRowNum());
            anchor.setRow2(cell.getRow().getRowNum() + cellReport.getCommentHeight());
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            Comment comment = drawing.createCellComment(anchor);
            RichTextString str = factory.createRichTextString(cellReport.getComment());
            comment.setString(str);
            comment.setRow(cell.getRowIndex());
            comment.setColumn(cell.getColumnIndex());
            cell.setCellComment(comment);
        }
        if (!cellReport.getAcceptedValues().isEmpty()) {
            DataValidationHelper validationHelper = cell.getSheet().getDataValidationHelper();
            cellReport.getAcceptedValues().toArray();
            DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(cellReport.getAcceptedValues().toArray(new String[cellReport.getAcceptedValues().size()]));
            CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(cell.getRowIndex(), cell.getRowIndex(), cell.getColumnIndex(), cell.getColumnIndex());
            DataValidation dataValidation = validationHelper.createValidation(constraint, cellRangeAddressList);
            dataValidation.setSuppressDropDownArrow(true);
            cell.getSheet().addValidationData(dataValidation);
        }
        if (cellReport.getColumnsNumber() > 1) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();
            CellRangeAddress cellRange = new CellRangeAddress(rowIndex, rowIndex + cellReport.getRowsToMerge(), columnIndex, columnIndex + cellReport.getColumnsNumber() - 1);
            cell.getSheet().addMergedRegion(cellRange);
        }
        String styleKey = buildStyleKey(cellReport);
        if (!isEmpty(styleKey)) {
            CellStyle cellStyle = buildCellStyle(cellReport, styleKey, stylesMap, cell.getSheet().getWorkbook());
            cell.setCellStyle(cellStyle);
        }
//        if (cellReport.getType().equals(CellReport.NUMERIC_TYPE) && !emptyString(cellReport.getNumberFormat())) {
        // deprecated, from doc: Use explicit Cell.setCellFormula(String), setCellValue(...) or Cell.setBlank() to get the desired result.
//            cell.setCellType(CellType.NUMERIC);
//        }
    }

    private void setLink(Cell cell, Object value) {
        CreationHelper createHelper = cell.getSheet().getWorkbook().getCreationHelper();
        Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
        link.setAddress(value.toString());
        cell.setCellValue(value.toString());
        cell.setHyperlink(link);
    }

    private void setValue(Object value, Cell cell) {
        if (value instanceof LocalDate) {
            Date data = Date.from(((LocalDate) value).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            cell.setCellValue(data);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Double) {
            cell.setCellValue(((Double) value));
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            String stringValue = value + "";
            if (stringValue.length() > 32000) {
                stringValue = stringValue.substring(0, 32000) + "...";
            }
            cell.setCellValue(stringValue);
        }
    }

    private CellStyle buildCellStyle(CellReport cellReport, String styleKey, Map<String, CellStyle> styleMap, Workbook wb) {
        CellStyle style = styleMap.get(styleKey);
        if (style == null) {
            style = wb.createCellStyle();
            XSSFFont font = (XSSFFont) wb.createFont();
            style.setFont(font);
            if (cellReport.isBold()) {
                font.setBold(true);
            }
            if (cellReport.isItalic()) {
                font.setItalic(true);
            }
            if (cellReport.getFontSize() != null) {
                font.setFontHeightInPoints(cellReport.getFontSize());
            }
            if (cellReport.getFontName() != null) {
                font.setFontName(cellReport.getFontName());
            }
            if (!isEmpty(cellReport.getBackgroundColor())) {
                ((XSSFCellStyle) style).setFillForegroundColor(getXSSFColor(cellReport.getBackgroundColor()));
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            if (!isEmpty(cellReport.getTextColor())) {
                font.setColor(getXSSFColor(cellReport.getTextColor()));
            }
            if (cellReport.isCenter()) {
                style.setAlignment(HorizontalAlignment.CENTER);
            }
            if (cellReport.isEndOfLine()) {
                style.setAlignment(HorizontalAlignment.RIGHT);
            }
            if (cellReport.getVerticalAlignment() != null) {
                style.setVerticalAlignment(cellReport.getVerticalAlignment());
            }
            if (cellReport.isMultiLine()) {
                style.setWrapText(true);
            }
            if (cellReport.isLink()) {
                font.setUnderline(XSSFFont.U_SINGLE);
                font.setColor(HSSFColor.HSSFColorPredefined.BLUE.getIndex());
            }
            if (!isEmpty(cellReport.getDateFormat())) {
                DataFormat format = wb.createDataFormat();
                style.setDataFormat(format.getFormat(cellReport.getDateFormat()));
            }
            if (!isEmpty(cellReport.getNumberFormat())) {
                DataFormat format = wb.createDataFormat();
                style.setDataFormat(format.getFormat(cellReport.getNumberFormat()));
            }
            CellReport.CellBorderStyle bordersStyle = cellReport.getBordersStyle();
            if (bordersStyle != null) {
                if (bordersStyle.getTop() != null) {
                    style.setBorderTop(bordersStyle.getTop());
                }
                if (bordersStyle.getBottom() != null) {
                    style.setBorderBottom(bordersStyle.getBottom());
                }
                if (bordersStyle.getLeft() != null) {
                    style.setBorderLeft(bordersStyle.getLeft());
                }
                if (bordersStyle.getRight() != null) {
                    style.setBorderRight(bordersStyle.getRight());
                }
            }
            styleMap.put(styleKey, style);
        }
        return style;
    }

    private XSSFColor getXSSFColor(String RGB) {
        return new XSSFColor(convertHexToColor(RGB), new DefaultIndexedColorMap());
    }

//    private XSSFColor getXSSFColor(String RGB) {
//        RGB = RGB.replaceAll("#", "");
//        if (RGB.length() != 6) throw new IllegalArgumentException("Uncorrect value for RGB");
//        int red = Integer.parseInt(RGB.substring(0,2), 16);
//        int green = Integer.parseInt(RGB.substring(2,4), 16);
//        int blue = Integer.parseInt(RGB.substring(4,6), 16);
//        //add alpha to avoid bug 51236
//        byte[] rgb = new byte[] { (byte) -1, (byte) red, (byte) green, (byte) blue };
//        logger.info("RGB {} -> R {} G {} B {}", RGB, red, green, blue);
//        return new XSSFColor(index -> rgb);
//    }

    private Color convertHexToColor(String hexColor) {
        if (hexColor == null || isEmpty(hexColor) || !hexColor.startsWith("#") || hexColor.length() != 7) {
            return Color.black;
        }
        return new Color(
                Integer.valueOf(hexColor.substring(1, 3), 16),
                Integer.valueOf(hexColor.substring(3, 5), 16),
                Integer.valueOf(hexColor.substring(5, 7), 16));
    }

    private String buildStyleKey(CellReport cellReport) {
        if (standardStyle(cellReport)) {
            return "";
        }
        StringBuilder key = new StringBuilder();
        CellReport.CellBorderStyle bordersStyle = cellReport.getBordersStyle();
        key.append((cellReport.isBold() ? "bold" : "no-bold"));
        key.append((cellReport.isItalic() ? "italic" : "no-italic"));
        key.append((cellReport.getFontSize() != null ? "fs-" + cellReport.getFontSize() : "no-fsize"));
        key.append((cellReport.isLink() ? "link" : "no-link"));
        key.append((cellReport.isCenter() ? "center" : "no-cent"));
        key.append((cellReport.isEndOfLine() ? "allign-r" : "no-all-r"));
        key.append((cellReport.isMultiLine() ? "multiline" : "no-multil"));
        key.append((cellReport.getVerticalAlignment() != null ? "vall-" + cellReport.getVerticalAlignment() : "no-vall"));
        if (bordersStyle != null) {
            key.append((bordersStyle.getTop() != null ? "top-border" + bordersStyle.getTop().getCode() : "no-top-border"));
            key.append((bordersStyle.getBottom() != null ? "bottom-border" + bordersStyle.getBottom().getCode() : "no-bottom-border"));
            key.append((bordersStyle.getLeft() != null ? "left-border" + bordersStyle.getLeft().getCode() : "no-left-border"));
            key.append((bordersStyle.getRight() != null ? "right-border" + bordersStyle.getRight().getCode() : "no-right-border"));
        }
        key.append((!isEmpty("s" + cellReport.getBackgroundColor())) ? cellReport.getBackgroundColor() : "");
        key.append((!isEmpty("t" + cellReport.getTextColor())) ? cellReport.getTextColor() : "");
        key.append((!isEmpty("df" + cellReport.getDateFormat())) ? cellReport.getDateFormat() : "");
        key.append((!isEmpty("nf" + cellReport.getNumberFormat())) ? cellReport.getNumberFormat() : "");
        return key.toString();
    }

    private boolean standardStyle(CellReport cellReport) {
        if (cellReport.isCenter() || cellReport.isEndOfLine() || cellReport.getBordersStyle() == null || cellReport.getFontSize() == null || cellReport.isBold() || cellReport.isItalic() || cellReport.isMultiLine() || cellReport.isLink() || !isEmpty(cellReport.getDateFormat()) || cellReport.getVerticalAlignment() == null) {
            return false;
        }
        return isEmpty(cellReport.getBackgroundColor()) && isEmpty(cellReport.getTextColor());
    }

    private void resizeColumns(Sheet sheet, SheetReport sheetReport) {
        for (Integer column : sheetReport.getColumnsToAutosize()) {
            sheet.autoSizeColumn(column, true);
        }
        for (Integer column : sheetReport.getColumnsWidth().keySet()) {
            int width = sheetReport.getColumnsWidth().get(column) * 256;
            sheet.setColumnWidth(column, width);
        }
    }

    private void hideColumns(Sheet sheet, SheetReport sheetReport) {
        for (Integer column : sheetReport.getColumnsToHide()) {
            sheet.setColumnHidden(column, true);
        }
    }

    private void setTrackedColumns(Sheet sheet, SheetReport sheetReport) {
        SXSSFSheet sxssfSheet = (SXSSFSheet) sheet;
        for (Integer column : sheetReport.getColumnsToAutosize()) {
            sxssfSheet.trackColumnForAutoSizing(column);
        }
    }

    public String getExtension() {
        return "xlsx";
    }

    private void setFilter(Sheet sheet, SheetReport sheetReport) {
        if (sheetReport.getFilterRow() == null || sheetReport.getFilterColumnStart() == null || sheetReport.getFilterColumnEnd() == null) {
            return;
        }
        sheet.setAutoFilter(new CellRangeAddress(sheetReport.getFilterRow(), sheetReport.getFilterRow(), sheetReport.getFilterColumnStart(), sheetReport.getFilterColumnEnd()));
    }

    private void setLock(Sheet sheet, SheetReport sheetReport) {
        if (sheetReport.getLockColumn() == null || sheetReport.getLockRow() == null) {
            return;
        }
        sheet.createFreezePane(sheetReport.getLockColumn(), sheetReport.getLockRow());
    }

    public static File getTempFile(String extension) throws IOException {
        return File.createTempFile("report", "." + extension);
    }

    private Map<String, Map<String, Double>> getStatistics(Sheet sheet) {
        Map<String, Map<String, Double>> stats = new HashMap<>();
        String[] metricNames = {
            "CellPrecision",
            "CellSimilarityPrecision",
            "CellRecall",
            "CellSimilarityRecall",
            "F1ScoreMetric",
            "CellSimilarityF1Score",
            "TupleCardinality",
            "TupleConstraint",
            "TupleSimilarityConstraint",
            "LLM Total Requests",
            "LLM Total Input Tokens",
            "LLM Total Output Tokens",
            "LLM Total Tokens",
            "LLM Time (ms)"
        };
        Row strategyRows = sheet.getRow(0);
        for (Cell strategyCell : strategyRows) {
            if (!strategyCell.getStringCellValue().trim().isEmpty()) {
                int columnIndex = strategyCell.getColumnIndex();
                String strategy = strategyCell.getStringCellValue().trim();
                log.debug("Strategy: " + strategy);
                Map<String, Double> resultsForStrategy = new HashMap<>();
                for (int i = 1; i <= metricNames.length; i++) {
                    String metricName = metricNames[i-1];
                    Row metricRow = sheet.getRow(i);
                    Cell cellMetric = metricRow.getCell(columnIndex);
                    double metric = 0;
                    try {
                        if (cellMetric.getCellType().equals(CellType.NUMERIC)) {
                            metric = cellMetric.getNumericCellValue();
                        } else {
                            String stringCellValue = cellMetric.getStringCellValue();
                            stringCellValue = stringCellValue.replace(",", ".");
                            metric = Double.parseDouble(stringCellValue);
                        }
                    } catch (Exception e) {
                        log.error("Exception: " + e + " with parsing of " + cellMetric);
                    }
                    log.debug("Metric: " + metricName + " value: " + metric);
                    resultsForStrategy.put(metricName, metric);
                }
                stats.put(strategy, resultsForStrategy);
            }
        }  
        return stats;
    }

    private String getBestResultsAveragedFor(String strategyName, Map<String, Map<String, Double>> resultsForQuery, List<String> metricsToAverage) {
        String bestStrategy = "";
        Double maxValue = 0.0;
        for (String key : resultsForQuery.keySet()) {
            if (key.startsWith(strategyName)) {
                Map<String, Double> metrics = resultsForQuery.get(key);
                if (bestStrategy.isEmpty()) {
                    bestStrategy = key;
                    maxValue = computeAverage(metrics, metricsToAverage);
                } else {
                    double currentValue = computeAverage(metrics, metricsToAverage);
                    if (currentValue > maxValue) {
                        maxValue = currentValue;
                        bestStrategy = key;
                    }
                }
            }
         }
        return bestStrategy;
    }

    private Double computeAverage(Map<String, Double> metrics, List<String> metricsToAverage) {
        if (metrics == null || metrics.isEmpty()) {
            return -1.0;
        }
        double sum = 0.0;
        for (String mName : metricsToAverage) {
            sum += metrics.get(mName);
        }
        return sum / metricsToAverage.size();
    }

    private void exportToCSV(List<List<String>> queriesStats, File filePath) {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setDelimiter("\t").build();
        String newFilePath = filePath.getAbsolutePath();
        newFilePath = newFilePath.replace(".xlsx", ".csv");
        CSVPrinter printer = null;
        try {
            Appendable output = new FileWriter(newFilePath);
            printer = new CSVPrinter(output, csvFormat);
            for (List<String> queriesStat : queriesStats) {
                printer.printRecord(queriesStat);
            }
        } catch (Exception e) {
            log.error("Exception in writing {}: {}", newFilePath, e);
        } finally {
            if (printer != null) {
                try {
                    printer.close();
                } catch (Exception e) {
                    
                }
            }
        }
    }

}
