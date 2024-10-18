package floq.utils.excelreport;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.poi.ss.usermodel.BorderStyle;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class ReportRow {

    @Setter(AccessLevel.NONE)
    private List<CellReport> cells = new ArrayList<>();
    private Float height;

    public ReportRow(Float height) {
        this.height = height;
    }

    public CellReport addCell(Object value) {
        CellReport cell = new CellReport(value);
        this.cells.add(cell);
        return cell;
    }

    public void addEmptyCells(int cellNumber) {
        for (int i = 0; i < cellNumber; i++) {
            cells.add(CellReport.EMPTY_CELL);
        }
    }

    public void addEmptyCells(int cellNumber, BorderStyle top, BorderStyle bottom, BorderStyle left, BorderStyle right) {
        for (int i = 0; i < cellNumber; i++) {
            cells.add(new CellReport(null).setBordersStyle(top, bottom, left, right));
        }
    }

    public void addEmptyCells(int cellNumber, BorderStyle style) {
        for (int i = 0; i < cellNumber; i++) {
            cells.add(new CellReport(null).setBordersStyle(style));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CellReport cell : cells) {
            sb.append(cell).append("\t");
        }
        return sb.toString() + "\n";
    }

}