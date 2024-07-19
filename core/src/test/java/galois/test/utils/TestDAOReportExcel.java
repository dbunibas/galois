package galois.test.utils;

import galois.utils.excelreport.ReportExcel;
import galois.utils.excelreport.ReportRow;
import galois.utils.excelreport.SheetReport;
import galois.utils.excelreport.persistance.DAOReportExcel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;

@Slf4j
@Disabled
public class TestDAOReportExcel {

    private DAOReportExcel daoReportExcel = new DAOReportExcel();

    @Test
    public void testExport(){
        ReportExcel reportExcel = new ReportExcel("Experiment 1");
        SheetReport dataSheet = reportExcel.addSheet("Data");
        ReportRow rowHeader = dataSheet.addRow();
        rowHeader.addCell("Col1");
        rowHeader.addCell("Col2");
        for (int i = 0; i < 10; i++){
            ReportRow dataRow = dataSheet.addRow();
            dataRow.addCell("String value " + i);
            dataRow.addCell(i * 1.0F);
        }
        File exportFile = new File(FileUtils.getTempDirectoryPath() + "/export.xlsx");
        log.info("Writing file {}", exportFile);
        daoReportExcel.saveReport(reportExcel, exportFile);
        try {
            Desktop.getDesktop().open(exportFile);
        } catch (IOException e) {
        }
    }

}
