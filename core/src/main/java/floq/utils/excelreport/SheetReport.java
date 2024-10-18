package floq.utils.excelreport;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class SheetReport {

    @Setter(AccessLevel.NONE)
    private final String name;

    @Setter(AccessLevel.NONE)
    private List<ReportRow> rows = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private Map<Integer, Integer> columnsWidth = new HashMap<>();

    @Setter(AccessLevel.NONE)
    private List<Integer> columnsToAutosize = new ArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<Integer> columnsToHide = new ArrayList<>();
    private boolean addProgressiveColumn;
    private boolean disableHeightAutosizeRow;
    private Integer filterRow;
    private Integer filterColumnStart;
    private Integer filterColumnEnd;
    private boolean displayGridlines;

    @Setter(AccessLevel.NONE)
    private Integer lockColumn;
    @Setter(AccessLevel.NONE)
    private Integer lockRow;

    private Region region;

    public SheetReport(String name) {
        this.name = name;
    }

    public ReportRow addRow() {
        ReportRow reportRow = new ReportRow();
        this.rows.add(reportRow);
        return reportRow;
    }

    public void addRow(ReportRow reportRow) {
        this.rows.add(reportRow);
    }

    public int getRowsNumber() {
        return rows.size();
    }

    public void addColumnsToAutosize(int... columns) {
        for (int column : columns) {
            columnsToAutosize.add(column);
        }
    }

    public void addColumnsToHide(int... columns) {
        for (int column : columns) {
            columnsToHide.add(column);
        }
    }

    public void putColumnWidth(int column, int width) {
        this.columnsWidth.put(column, width);
    }

    public void setColumnAndRowToLock(int column, int row) {
        this.lockColumn = column;
        this.lockRow = row;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(name).append(" #\n");
        for (ReportRow row : rows) {
            sb.append(row.toString());
        }
        return sb.toString();
    }

}