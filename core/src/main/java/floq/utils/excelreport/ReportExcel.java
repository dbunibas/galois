package floq.utils.excelreport;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(chain = true)
public class ReportExcel {

    private final String name;

    private List<SheetReport> sheets = new ArrayList<>();

    public ReportExcel(String name) {
        this.name = name;
    }

    public SheetReport addSheet(String sheetName) {
        SheetReport sheet = new SheetReport(sheetName);
        this.sheets.add(sheet);
        return sheet;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append(" #\n");
        for (SheetReport sheet : sheets) {
            sb.append(sheet.toString());
            sb.append("-----------------\n");
        }
        return sb.toString();
    }

}