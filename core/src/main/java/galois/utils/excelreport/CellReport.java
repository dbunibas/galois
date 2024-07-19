package galois.utils.excelreport;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class CellReport {

    public static final String STRING_TYPE = "STRING TYPE";
    public static final String NUMERIC_TYPE = "NUMERIC TYPE";
    public static CellReport EMPTY_CELL = new CellReport(null);

    @Setter(AccessLevel.NONE)
    private Object value;
    private boolean bold;
    private boolean italic;
    private Short fontSize;
    private String fontName;
    private String textColor;
    private String backgroundColor;
    private int columnsNumber = 1;
    private int rowsToMerge = 0;
    private boolean center;
    private boolean endOfLine;
    private boolean multiLine;
    private boolean link;
    private String comment;
    private int commentWidth = 2;
    private int commentHeight = 2;
    private VerticalAlignment verticalAlignment;
    private List<String> acceptedValues = new ArrayList<>();
    private String type = STRING_TYPE;
    private String dateFormat;
    private String numberFormat;

    @Setter(AccessLevel.NONE)
    private CellBorderStyle bordersStyle;

    public CellReport(Object value) {
        this.value = value;
    }

    public CellReport setBordersStyle(BorderStyle top, BorderStyle bottom, BorderStyle left, BorderStyle right) {
        this.bordersStyle = new CellBorderStyle(top, bottom, left, right);
        return this;
    }

    public CellReport setBordersStyle(BorderStyle style) {
        this.bordersStyle = new CellBorderStyle(style);
        return this;
    }

    @Override
    public String toString() {
        return value + "";
    }

    @Getter
    @AllArgsConstructor
    public static class CellBorderStyle {

        private final BorderStyle top;
        private final BorderStyle bottom;
        private final BorderStyle left;
        private final BorderStyle right;

        private CellBorderStyle(BorderStyle style) {
            this.top = style;
            this.bottom = style;
            this.left = style;
            this.right = style;
        }

    }

}
