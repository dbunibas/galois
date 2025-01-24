package floq.llm.models.togetherai;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Getter
public class TupleProb {
    
    private List<CellProb> cells = new ArrayList<CellProb>();
    
    public void addCell(CellProb cell) {
        this.cells.add(cell);
    }
    
    
}
