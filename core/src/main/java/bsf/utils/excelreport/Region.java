package bsf.utils.excelreport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@Accessors(chain = true)
public class Region {

    private int firstRow;
    private int lastRow;
    private int firstCol;
    private int lastCol;

}
